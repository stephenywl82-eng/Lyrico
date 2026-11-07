#include "tfilestream.h"
#include "utils.h"

#include <mpegfile.h>
#include <vorbisfile.h>
#include <flacfile.h>
#include <opusfile.h>
#include <mp4file.h>
#include <wavfile.h>
#include <apefile.h>
#include <tbytevectorstream.h>

#include <memory>
#include <stdexcept>
#include <android/log.h>

template<typename FileType>
TagLib::File *createSupportedFile(TagLib::IOStream *stream,
                                  bool readAudioProperties,
                                  TagLib::AudioProperties::ReadStyle audioPropertiesStyle) {
    stream->seek(0);
    if (!FileType::isSupported(stream)) {
        return nullptr;
    }
    stream->seek(0);
    return new FileType(stream, readAudioProperties, audioPropertiesStyle);
}

namespace {

// Locate the byte offset of the last *real* FLAC metadata block header that is
// missing its "last-block" flag. Returns -1 when the chain already terminates
// correctly or when we cannot safely determine the boundary.
//
// Some encoders (e.g. certain music-app downloads) write the metadata blocks
// without setting the final last-block flag. TagLib's FLAC::File::scan() then
// keeps reading past the tags into the audio data, hits a block whose declared
// length exceeds the file, and rejects the whole file -- even though the audio
// and tags are perfectly fine. Players tolerate this; strict tag parsers don't.
TagLib::offset_t findFlacMissingLastFlag(TagLib::IOStream *stream) {
    stream->seek(0);
    TagLib::ByteVector magic = stream->readBlock(4);
    if (magic.size() < 4 || magic != "fLaC") return -1;

    const TagLib::offset_t total = stream->length();
    if (total <= 4) return -1;

    TagLib::offset_t pos = 4;
    TagLib::offset_t lastMetaPos = -1;
    bool sawStreamInfo = false;
    int guard = 0;

    while (pos + 4 <= total) {
        stream->seek(pos);
        TagLib::ByteVector hdr = stream->readBlock(4);
        if (hdr.size() < 4) return -1;

        const unsigned char h = static_cast<unsigned char>(hdr[0]);
        const unsigned int type = h & 0x7F;
        const bool isLast = (h & 0x80) != 0;
        const unsigned int blockLen =
                (static_cast<unsigned int>(static_cast<unsigned char>(hdr[1])) << 16) |
                (static_cast<unsigned int>(static_cast<unsigned char>(hdr[2])) << 8) |
                static_cast<unsigned int>(static_cast<unsigned char>(hdr[3]));

        if (isLast) return -1;                 // already properly terminated
        if (type > 6) break;                   // reserved/invalid type => past metadata
        if (type == 0) {
            if (sawStreamInfo) break;          // duplicate STREAMINFO => inside audio data
            sawStreamInfo = true;
        }

        lastMetaPos = pos;
        const TagLib::offset_t next = pos + 4 + blockLen;
        if (next + 4 > total) break;           // next block would exceed EOF
        pos = next;
        if (++guard > 100000) break;
    }
    return lastMetaPos;
}

// Set the last-block flag on the header byte in-place. Only succeeds when the
// stream is actually writable (used by the save/write path so changes persist to
// disk). For read-only streams writeBlock is a safe no-op, so this returns false
// and the caller falls back to the in-memory repair.
bool patchFlacLastFlagInPlace(TagLib::IOStream *stream, TagLib::offset_t headerPos) {
    const TagLib::offset_t total = stream->length();
    if (headerPos < 0 || headerPos + 1 > total) return false;

    stream->seek(headerPos);
    TagLib::ByteVector b = stream->readBlock(1);
    if (b.size() != 1) return false;

    const unsigned char orig = static_cast<unsigned char>(b[0]);
    if (orig & 0x80) return true; // already set

    const unsigned char patched = static_cast<unsigned char>(orig | 0x80);
    stream->seek(headerPos);
    stream->writeBlock(TagLib::ByteVector(1, static_cast<char>(patched)));

    // Verify the byte was actually written (writeBlock is a no-op on read-only streams).
    stream->seek(headerPos);
    TagLib::ByteVector verify = stream->readBlock(1);
    return verify.size() == 1 && (static_cast<unsigned char>(verify[0]) & 0x80) != 0;
}

// A FLAC::File that owns the in-memory ByteVectorStream it was parsed from.
//
// TagLib::File(IOStream*) does NOT take ownership of the stream (see
// FilePrivate(stream, /*owner=*/false) in tfile.cpp). On the read path the
// caller passes a read-only FileStream, so the in-place patch cannot write to it
// and we must parse from a private in-memory copy. That copy must outlive the
// returned File (the caller keeps the File and calls further methods like
// getPictures on it), otherwise the File ends up holding a dangling IOStream*
// and any later virtual call aborts with "Pure virtual function called!".
// Holding the stream as a member makes its lifetime bound to the File's.
class OwnedStreamFlacFile : public TagLib::FLAC::File {
public:
    OwnedStreamFlacFile(std::unique_ptr<TagLib::ByteVectorStream> stream,
                        bool readAudioProperties,
                        TagLib::AudioProperties::ReadStyle style)
            : TagLib::FLAC::File(stream.get(), readAudioProperties, style),
              m_stream(std::move(stream)) {}

private:
    std::unique_ptr<TagLib::ByteVectorStream> m_stream;
};

// In-memory repair for read-only streams: copy the file, set the flag, and parse
// from a ByteVectorStream owned by the returned File. Used by the read path
// (metadata / audio properties / pictures), where we must not mutate the
// underlying file.
TagLib::File *createFlacRepairedInMemory(TagLib::IOStream *stream,
                                         bool readAudioProperties,
                                         TagLib::AudioProperties::ReadStyle style,
                                         TagLib::offset_t headerPos) {
    const TagLib::offset_t total = stream->length();
    if (total <= 0 || total > 100 * 1024 * 1024) return nullptr;

    stream->seek(0);
    TagLib::ByteVector all = stream->readBlock(static_cast<unsigned int>(total));
    if (all.size() != total || headerPos < 0 || headerPos >= all.size()) return nullptr;

    all[headerPos] = static_cast<char>(
            static_cast<unsigned char>(all[headerPos]) | 0x80);
    auto mem = std::make_unique<TagLib::ByteVectorStream>(all);
    mem->seek(0);
    if (!TagLib::FLAC::File::isSupported(mem.get())) {
        return nullptr;
    }
    mem->seek(0);
    return new OwnedStreamFlacFile(std::move(mem), readAudioProperties, style);
}

} // namespace

// ---------------------------------------------------------------------------
// Diagnostic channel: when a read/write fails we stash a human-readable reason
// here so the Kotlin layer can surface it in the app's own metadata error log
// (which the user can copy without adb). Reset at the start of every call that
// can fail so a stale message is never reported.
// ---------------------------------------------------------------------------
#include <sstream>
#include <iomanip>

static std::string g_lastTagError;

static std::string hexDump(TagLib::IOStream *stream, TagLib::offset_t count) {
    stream->seek(0);
    TagLib::ByteVector v = stream->readBlock(static_cast<unsigned int>(count));
    std::ostringstream oss;
    oss << std::hex << std::setfill('0');
    for (unsigned int i = 0; i < v.size(); ++i) {
        oss << std::setw(2)
            << (static_cast<unsigned int>(static_cast<unsigned char>(v[i]))) << " ";
    }
    return oss.str();
}

static void setLastTagError(const std::string &msg) {
    g_lastTagError = msg;
}

TagLib::File* createFileFromContent(TagLib::IOStream *stream,
                                    bool readAudioProperties,
                                    TagLib::AudioProperties::ReadStyle audioPropertiesStyle) {
    TagLib::File *file = nullptr;
    g_lastTagError.clear();

    // APE has a distinctive signature. Check it before MPEG's frame scan,
    // which can mistake compressed lossless data for an MPEG frame header.
    file = createSupportedFile<TagLib::APE::File>(
            stream, readAudioProperties, audioPropertiesStyle);
    if (!file) {
        file = createSupportedFile<TagLib::MPEG::File>(
                stream, readAudioProperties, audioPropertiesStyle);
    }
    if (!file) {
        file = createSupportedFile<TagLib::Ogg::Vorbis::File>(
                stream, readAudioProperties, audioPropertiesStyle);
    }
    if (!file) {
        file = createSupportedFile<TagLib::FLAC::File>(
                stream, readAudioProperties, audioPropertiesStyle);
    }
    if (!file) {
        file = createSupportedFile<TagLib::Ogg::Opus::File>(
                stream, readAudioProperties, audioPropertiesStyle);
    }
    if (!file) {
        file = createSupportedFile<TagLib::MP4::File>(
                stream, readAudioProperties, audioPropertiesStyle);
    }
    if (!file) {
        file = createSupportedFile<TagLib::RIFF::WAV::File>(
                stream, readAudioProperties, audioPropertiesStyle);
    }

    if (!file) {
        // Detect known DRM / non-audio wrappers so the app can show a clear,
        // actionable message instead of a cryptic "no supported file type".
        // "yeelion-kuwo-tme" is the header Kuwo Music (TME) stamps on its
        // DRM-protected downloads -- the payload is encrypted and not a real
        // audio container, so neither reading nor writing is possible.
        static const TagLib::ByteVector kuwoMagic("yeelion-kuwo-tme", 16);
        stream->seek(0);
        const TagLib::ByteVector head = stream->readBlock(16);
        stream->seek(0);
        if (head.size() >= 16 && head == kuwoMagic) {
            setLastTagError("KUWO_DRM_FILE");
            return nullptr;
        }
        setLastTagError(
            "[createFileFromContent] no supported file type could be created; "
            "head(hex)=" + hexDump(stream, 32) +
            " len=" + std::to_string(stream->length()));
        return nullptr;
    }

    bool hasTags = (file->tag() && !file->tag()->isEmpty()) || !file->properties().isEmpty();

    if (hasTags) {
        return file;
    }

    // `file` carries no usable tags. Two possibilities:
    //  - it is invalid (scan failed outright), or
    //  - it is *valid* but TagLib could read no tags. The latter happens for FLAC
    //    files whose final metadata block is missing the last-block flag: the
    //    STREAMINFO block still parses (so isValid() is true) yet scan() reads
    //    past the real tags into the audio frames and finds nothing. Patch the
    //    missing flag and re-parse to recover the tags.
    const TagLib::offset_t repairPos = findFlacMissingLastFlag(stream);
    __android_log_print(ANDROID_LOG_INFO, "AudioTagReader",
            "[flac-repair] invalid file w/o tags, repairPos=%lld",
            static_cast<long long>(repairPos));
    if (repairPos >= 0) {
        // Prefer an in-place patch (persists on write paths); fall back to an
        // in-memory copy for read-only streams.
        if (patchFlacLastFlagInPlace(stream, repairPos)) {
            stream->seek(0);
            TagLib::File *repaired = createSupportedFile<TagLib::FLAC::File>(
                    stream, readAudioProperties, audioPropertiesStyle);
            if (repaired && (repaired->isValid() ||
                    ((repaired->tag() && !repaired->tag()->isEmpty()) ||
                     !repaired->properties().isEmpty()))) {
                __android_log_print(ANDROID_LOG_INFO, "AudioTagReader",
                        "[flac-repair] in-place patch OK at %lld",
                        static_cast<long long>(repairPos));
                delete file;
                return repaired;
            }
            delete repaired;
            __android_log_print(ANDROID_LOG_INFO, "AudioTagReader",
                    "[flac-repair] in-place patch produced invalid file");
        } else {
            __android_log_print(ANDROID_LOG_INFO, "AudioTagReader",
                    "[flac-repair] in-place patch not applicable (read-only?), trying memory copy");
        }
        stream->seek(0);
        TagLib::File *repaired = createFlacRepairedInMemory(
                stream, readAudioProperties, audioPropertiesStyle, repairPos);
        if (repaired && (repaired->isValid() ||
                ((repaired->tag() && !repaired->tag()->isEmpty()) ||
                 !repaired->properties().isEmpty()))) {
            __android_log_print(ANDROID_LOG_INFO, "AudioTagReader",
                    "[flac-repair] in-memory repair OK at %lld",
                    static_cast<long long>(repairPos));
            delete file;
            return repaired;
        }
        delete repaired;
        __android_log_print(ANDROID_LOG_INFO, "AudioTagReader",
                "[flac-repair] all repair attempts failed for repairPos=%lld",
                static_cast<long long>(repairPos));
    }

    // No repair was applicable. A valid file with no tags is a legitimate
    // (untagged) file -- keep it. An invalid file without tags cannot be used.
    if (file->isValid()) {
        return file;
    }

    delete file;

    setLastTagError(
        "[createFileFromContent] file created but invalid and unrepairable; "
        "head(hex)=" + hexDump(stream, 32) +
        " len=" + std::to_string(stream->length()));
    return nullptr;
}


extern "C" {

JNIEXPORT jobject JNICALL
Java_com_lonx_audiotag_TagLib_getAudioProperties(
        JNIEnv *env, jclass, jint fd, jint read_style) {
    try {
        // fd readOnly = true
        auto stream = std::make_unique<TagLib::FileStream>(fd, true);
        const auto style = static_cast<TagLib::AudioProperties::ReadStyle>(read_style);

        std::unique_ptr<TagLib::File> file(createFileFromContent(stream.get(), true, style));

        if (!file) {
            return emptyAudioProperties(env);
        }

        return getAudioProperties(env, file.get());
    } catch (const std::exception &e) {
        LOGE("Error reading audio properties: %s", e.what());
        return emptyAudioProperties(env);
    } catch (...) {
        LOGE("Unknown error reading audio properties");
        return emptyAudioProperties(env);
    }
}

JNIEXPORT jobject JNICALL
Java_com_lonx_audiotag_TagLib_getMetadata(
        JNIEnv *env, jclass, jint fd, jboolean read_pictures) {
    try {
        auto stream = std::make_unique<TagLib::FileStream>(fd, true);
        std::unique_ptr<TagLib::File> file(createFileFromContent(stream.get(), false, TagLib::AudioProperties::Average));

        if (!file) {
            setLastTagError("[getMetadata] " + g_lastTagError);
            return nullptr;
        }

        jobject propertiesMap = getPropertyMap(env, file.get());
        jobjectArray pictures;
        if (read_pictures) {
            pictures = getPictures(env, file.get());
        } else {
            pictures = emptyPictureArray(env);
        }

        bool supportsPictureTypes = true;

        if (dynamic_cast<TagLib::MP4::File *>(file.get()) != nullptr) {
            supportsPictureTypes = false;
        }

        jboolean supports = supportsPictureTypes
                            ? JNI_TRUE
                            : JNI_FALSE;

        return env->NewObject(
                metadataClass,
                metadataConstructor,
                propertiesMap,
                pictures,
                supports
        );

    } catch (const std::exception &e) {
        setLastTagError(std::string("[getMetadata] exception: ") + e.what());
        LOGE("Error reading metadata: %s", e.what());
        return nullptr;
    }
}

JNIEXPORT jstring JNICALL
Java_com_lonx_audiotag_TagLib_getLastTagError(
        JNIEnv *env, jclass) {
    return env->NewStringUTF(g_lastTagError.c_str());
}

JNIEXPORT jobjectArray JNICALL
Java_com_lonx_audiotag_TagLib_getMetadataPropertyValues(
        JNIEnv *env, jclass, jint fd, jstring property_name) {

    const char *propertyName = env->GetStringUTFChars(property_name, nullptr);
    if (propertyName == nullptr) return nullptr;

    try {
        auto stream = std::make_unique<TagLib::FileStream>(fd, true);
        std::unique_ptr<TagLib::File> file(createFileFromContent(stream.get(), false, TagLib::AudioProperties::Average));

        if (!file) {
            env->ReleaseStringUTFChars(property_name, propertyName);
            return nullptr;
        }

        const auto propertyMap = file->properties();
        auto it = propertyMap.find(TagLib::String(propertyName));

        if (it == propertyMap.end()) {
            env->ReleaseStringUTFChars(property_name, propertyName);
            return nullptr;
        }

        const auto valueList = it->second;
        jobjectArray result = env->NewObjectArray(static_cast<jsize>(valueList.size()), stringClass, nullptr);

        int i = 0;
        for (const auto &value: valueList) {
            jstring jValue = env->NewStringUTF(value.toCString(true));
            env->SetObjectArrayElement(result, i, jValue);
            env->DeleteLocalRef(jValue);
            i++;
        }

        env->ReleaseStringUTFChars(property_name, propertyName);
        return result;

    } catch (const std::exception &e) {
        LOGE("Error reading property values: %s", e.what());
        env->ReleaseStringUTFChars(property_name, propertyName);
        return nullptr;
    }
}

JNIEXPORT jobjectArray JNICALL
Java_com_lonx_audiotag_TagLib_getPictures(
        JNIEnv *env, jclass, jint fd) {
    try {
        auto stream = std::make_unique<TagLib::FileStream>(fd, true);
        std::unique_ptr<TagLib::File> file(createFileFromContent(stream.get(), false, TagLib::AudioProperties::Average));

        if (!file) {
            return emptyPictureArray(env);
        }

        return getPictures(env, file.get());
    } catch (const std::exception &e) {
        LOGE("Error reading pictures: %s", e.what());
        return emptyPictureArray(env);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_lonx_audiotag_TagLib_savePropertyMap(
        JNIEnv *env, jclass, jint fd, jobject property_map) {
    try {
        // 写操作，fd readOnly = false
        auto stream = std::make_unique<TagLib::FileStream>(fd, false);
        std::unique_ptr<TagLib::File> file(createFileFromContent(stream.get(), false, TagLib::AudioProperties::Average));

        if (!file) {
            setLastTagError("[savePropertyMap] " + g_lastTagError);
            return false;
        }

        TagLib::PropertyMap props = file->properties();

        const PropertyMap updates = JniHashMapToPropertyMap(env, property_map);

        for (const auto & update : updates) {
            const TagLib::String &key = update.first;
            const TagLib::StringList &values = update.second;

            if (values.isEmpty() || (values.size() == 1 && values.front().isEmpty())) {
                props.erase(key);
            } else {
                props.replace(key, values);
            }
        }

        file->setProperties(props);
        if (!file->save()) {
            setLastTagError("[savePropertyMap] File created but save() returned false; head(hex)=" + hexDump(stream.get(), 32));
            return false;
        }
        return true;

    } catch (const std::exception &e) {
        setLastTagError(std::string("[savePropertyMap] exception: ") + e.what());
        LOGE("Error saving property map: %s", e.what());
        return false;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_lonx_audiotag_TagLib_savePictures(
        JNIEnv *env, jclass, jint fd, jobjectArray pictures) {
    try {
        // 写操作，fd readOnly = false
        auto stream = std::make_unique<TagLib::FileStream>(fd, false);
        std::unique_ptr<TagLib::File> file(createFileFromContent(stream.get(), false, TagLib::AudioProperties::Average));

        if (!file) {
            setLastTagError("[savePictures] " + g_lastTagError);
            return false;
        }

        auto pictureList = JniPictureArrayToPictureList(env, pictures);
        file->setComplexProperties("PICTURE", pictureList);

        if (!file->save()) {
            setLastTagError("[savePictures] File created but save() returned false; head(hex)=" + hexDump(stream.get(), 32));
            return false;
        }
        return true;
    } catch (const std::exception &e) {
        setLastTagError(std::string("[savePictures] exception: ") + e.what());
        LOGE("Error saving pictures: %s", e.what());
        return false;
    }
}

} // extern "C"
