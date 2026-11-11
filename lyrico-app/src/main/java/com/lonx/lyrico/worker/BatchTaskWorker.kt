package com.lonx.lyrico.worker

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.lonx.lyrico.R
import com.lonx.lyrico.data.model.log.AppLogLevel
import com.lonx.lyrico.data.model.log.AppLogType
import com.lonx.lyrico.data.model.BatchTaskType
import com.lonx.lyrico.data.repository.AppLogRepository
import com.lonx.lyrico.data.repository.BatchTaskRepository
import com.lonx.lyrico.worker.processor.BatchTaskProcessorFactory
import com.lonx.lyrico.worker.processor.BatchExportTaskConfig
import com.lonx.lyrico.worker.processor.BatchTaskSkippedException
import com.lonx.lyrico.worker.processor.EditTagsTaskConfig
import com.lonx.lyrico.worker.processor.MatchMetadataTaskConfig
import com.lonx.lyrico.worker.processor.RenameFilesTaskConfig
import com.lonx.lyrico.viewmodel.LyricsFormatConfig
import com.lonx.lyrico.worker.processor.ReplayGainTaskConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.int
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

class BatchTaskWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val taskRepository: BatchTaskRepository by inject()
    private val processorFactory: BatchTaskProcessorFactory by inject()
    private val appLogRepository: AppLogRepository by inject()

    private val taskId: String = inputData.getString(KEY_TASK_ID) ?: ""

    // 限流前台通知刷新：每首歌都 setForeground 会触发系统丢弃大量更新，
    // 导致进度条卡在中间不前进。间隔刷新既平滑又保证最终 100% 能下发。
    private var lastForegroundUpdateMs = 0L
    private val foregroundThrottleMs = 250L

    override suspend fun doWork(): Result {
        if (taskId.isEmpty()) return Result.failure()

        val wakeLock = acquireWakeLock()
        BatchTaskNotification.ensureChannel(applicationContext)

        try {
            val task = taskRepository.getTask(taskId) ?: return Result.failure()
            val title = getTaskTitle(task.type)
            val total = task.total
            val startedAt = System.currentTimeMillis()
            val configSummary = buildConfigSummary(task.type, task.configJson)

            taskRepository.markRunning(taskId)

            val processor = processorFactory.create(task.type)
            val items = taskRepository.getPendingItems(taskId)
            val itemTotal = items.size

            updateForeground(title, 0, itemTotal)

            if (items.isEmpty()) {
                taskRepository.markSucceeded(taskId)
                logBatch(
                    level = AppLogLevel.INFO,
                    message = "Batch task finished: ${task.type} (0/$itemTotal processed)",
                    detail = buildBatchSummary(
                        taskType = task.type,
                        status = "succeeded",
                        configSummary = configSummary,
                        startedAt = startedAt,
                        total = total,
                        pending = 0,
                        concurrency = 0,
                        processed = 0,
                        success = 0,
                        skipped = 0,
                        failure = 0,
                        itemDetails = emptyList()
                    ),
                    relatedId = taskId
                )
                return Result.success()
            }

            val concurrency = parseConcurrency(task.configJson, task.type)
            val semaphore = Semaphore(concurrency)
            val processedCount = AtomicInteger(0)
            val successCount = AtomicInteger(0)
            val skippedCount = AtomicInteger(0)
            val failureCount = AtomicInteger(0)
            val itemDetails = ConcurrentLinkedQueue<String>()

            try {
                coroutineScope {
                    val jobs = items.mapIndexed { index, item ->
                        async(Dispatchers.IO) {
                            semaphore.withPermit {
                                if (isStopped) return@async
                                try {
                                    taskRepository.markItemRunning(item.itemId)
                                    maybeUpdateForeground(title, processedCount.get(), itemTotal, item.fileName)
                                    logBatch(
                                        level = AppLogLevel.INFO,
                                        message = "处理第 ${index + 1}/$itemTotal 首: ${item.fileName}",
                                        relatedId = taskId
                                    )
                                    val result = withTimeout(ITEM_PROCESS_TIMEOUT_MILLIS) {
                                        processor.process(task, item) { progress ->
                                            taskRepository.updateItemProgress(item.itemId, progress)
                                        }
                                    }
                                    if (result.updatedFilePath != null && result.updatedFileName != null) {
                                        taskRepository.updateItemFileInfo(
                                            itemId = item.itemId,
                                            filePath = result.updatedFilePath,
                                            fileName = result.updatedFileName
                                        )
                                    }
                                    taskRepository.markItemSucceeded(item.itemId, result.resultJson)
                                    successCount.incrementAndGet()
                                } catch (e: BatchTaskSkippedException) {
                                    Log.i(
                                        TAG,
                                        "Item processing skipped: ${item.fileName}, reason=${e.message ?: "No reason"}"
                                    )
                                    taskRepository.markItemSkipped(item.itemId, e.message)
                                    skippedCount.incrementAndGet()
                                    itemDetails.add("SKIPPED ${item.fileName}: ${e.message ?: "No reason"}")
                                    logBatch(
                                        level = AppLogLevel.INFO,
                                        message = "跳过 ${item.fileName}: ${e.message ?: "No reason"}",
                                        relatedId = taskId
                                    )
                                } catch (e: TimeoutCancellationException) {
                                    Log.e(
                                        TAG,
                                        "Item processing timed out (${ITEM_PROCESS_TIMEOUT_MILLIS / 1000}s): ${item.fileName}"
                                    )
                                    taskRepository.markItemFailed(
                                        item.itemId,
                                        "处理超时（>${ITEM_PROCESS_TIMEOUT_MILLIS / 1000}s，已跳过该文件）"
                                    )
                                    failureCount.incrementAndGet()
                                    itemDetails.add("TIMEOUT ${item.fileName}: >${ITEM_PROCESS_TIMEOUT_MILLIS / 1000}s")
                                    logBatch(
                                        level = AppLogLevel.WARNING,
                                        message = "超时（>${ITEM_PROCESS_TIMEOUT_MILLIS / 1000}s）已跳过: ${item.fileName}",
                                        relatedId = taskId
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Item processing failed: ${item.fileName}", e)
                                    taskRepository.markItemFailed(item.itemId, e.message)
                                    failureCount.incrementAndGet()
                                    itemDetails.add(
                                        buildString {
                                            appendLine("FAILED ${item.fileName}: ${e.message ?: e::class.java.simpleName}")
                                            append(Log.getStackTraceString(e))
                                        }
                                    )
                                    logBatch(
                                        level = AppLogLevel.ERROR,
                                        message = "失败 ${item.fileName}: ${e.message ?: e::class.java.simpleName}",
                                        relatedId = taskId
                                    )
                                } finally {
                                    val current = processedCount.incrementAndGet()
                                    taskRepository.updateProgressFromItems(taskId, item.fileName)
                                    maybeUpdateForeground(title, current, itemTotal, item.fileName)
                                }
                            }
                        }
                    }
                    jobs.awaitAll()
                }
            } finally {
                withContext(NonCancellable) {
                    if (isStopped) {
                        taskRepository.markCancelled(taskId)
                        logBatch(
                            level = AppLogLevel.WARNING,
                            message = buildBatchMessage(
                                task.type,
                                "cancelled",
                                processedCount.get(),
                                successCount.get(),
                                skippedCount.get(),
                                failureCount.get()
                            ),
                            detail = buildBatchSummary(
                                taskType = task.type,
                                status = "cancelled",
                                configSummary = configSummary,
                                startedAt = startedAt,
                                total = total,
                                pending = itemTotal,
                                concurrency = concurrency,
                                processed = processedCount.get(),
                                success = successCount.get(),
                                skipped = skippedCount.get(),
                                failure = failureCount.get(),
                                itemDetails = itemDetails.toList()
                            ),
                            relatedId = taskId
                        )
                    } else {
                        taskRepository.markSucceeded(taskId)
                        logBatch(
                            level = if (failureCount.get() > 0) AppLogLevel.WARNING else AppLogLevel.INFO,
                            message = buildBatchMessage(
                                task.type,
                                "finished",
                                processedCount.get(),
                                successCount.get(),
                                skippedCount.get(),
                                failureCount.get()
                            ),
                            detail = buildBatchSummary(
                                taskType = task.type,
                                status = if (failureCount.get() > 0) "finished_with_errors" else "succeeded",
                                configSummary = configSummary,
                                startedAt = startedAt,
                                total = total,
                                pending = itemTotal,
                                concurrency = concurrency,
                                processed = processedCount.get(),
                                success = successCount.get(),
                                skipped = skippedCount.get(),
                                failure = failureCount.get(),
                                itemDetails = itemDetails.toList()
                            ),
                            relatedId = taskId
                        )
                    }
                }
            }

            // 收尾：把进度与总数对齐到 100%，并把常驻进度通知替换为完成通知，
            // 避免进度条停在中间不消失。
            taskRepository.updateProgressFromItems(taskId, null)
            updateForeground(title, itemTotal, itemTotal)
            val finishedTask = taskRepository.getTask(taskId)
            if (finishedTask != null) {
                try {
                    val finishedNotification = BatchTaskNotification.buildFinishedNotification(
                        applicationContext,
                        title,
                        finishedTask.successCount,
                        finishedTask.failureCount
                    )
                    (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                        .notify(BatchTaskNotification.NOTIFICATION_ID, finishedNotification)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to post finished notification", e)
                }
            }

            return Result.success()
        } catch (e: Exception) {
            logBatchException(
                message = "Batch task crashed",
                throwable = e,
                relatedId = taskId
            )
            throw e
        } finally {
            releaseWakeLock(wakeLock)
        }
    }

    private fun acquireWakeLock(): PowerManager.WakeLock? {
        return try {
            val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "$WAKE_LOCK_TAG_PREFIX:$taskId"
            ).apply {
                setReferenceCounted(false)
                acquire(WAKE_LOCK_TIMEOUT_MILLIS)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire wake lock", e)
            null
        }
    }

    private fun releaseWakeLock(wakeLock: PowerManager.WakeLock?) {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release wake lock", e)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo("Batch Task", 0, 0)
    }

    private suspend fun updateForeground(
        title: String,
        current: Int,
        total: Int,
        currentFile: String? = null
    ) {
        withContext(Dispatchers.Main) {
            try {
                setForeground(createForegroundInfo(title, current, total, currentFile))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update foreground notification", e)
            }
        }
    }

    /**
     * 限流版的前台通知刷新。Android 对前台服务通知刷新有严格节流，高频 setForeground 会被丢弃，
     * 导致进度条卡在中间。这里按 [foregroundThrottleMs] 间隔刷新，既平滑又保证最终 100% 能下发。
     */
    private suspend fun maybeUpdateForeground(
        title: String,
        current: Int,
        total: Int,
        currentFile: String? = null
    ) {
        val now = System.currentTimeMillis()
        if (now - lastForegroundUpdateMs >= foregroundThrottleMs) {
            lastForegroundUpdateMs = now
            updateForeground(title, current, total, currentFile)
        }
    }

    private fun createForegroundInfo(
        title: String,
        current: Int,
        total: Int,
        currentFile: String? = null
    ): ForegroundInfo {
        val notification = BatchTaskNotification.buildRunningNotification(
            context = applicationContext,
            taskId = taskId,
            title = title,
            currentFile = currentFile,
            current = current,
            total = total
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                BatchTaskNotification.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                BatchTaskNotification.NOTIFICATION_ID,
                notification
            )
        }
    }

    private fun getTaskTitle(type: BatchTaskType): String {
        return when (type) {
            BatchTaskType.MATCH_METADATA -> applicationContext.getString(R.string.batch_task_match_tags)
            BatchTaskType.MATCH_LYRICS -> applicationContext.getString(R.string.batch_task_match_lyrics)
            BatchTaskType.MATCH_COVER -> applicationContext.getString(R.string.batch_task_match_cover)
            BatchTaskType.EDIT_TAGS -> applicationContext.getString(R.string.batch_task_edit_tags)
            BatchTaskType.RENAME_FILES -> applicationContext.getString(R.string.batch_task_rename_files)
            BatchTaskType.CONVERT_LYRICS_FORMAT -> applicationContext.getString(R.string.lyrics_process_title)
            BatchTaskType.SCAN_REPLAY_GAIN -> applicationContext.getString(R.string.batch_task_scan_replay_gain)
            BatchTaskType.EXPORT_LYRICS -> applicationContext.getString(R.string.batch_task_export_lyrics)
            BatchTaskType.EXPORT_COVER -> applicationContext.getString(R.string.batch_task_export_cover)
        }
    }

    private fun parseConcurrency(configJson: String?, type: BatchTaskType): Int {
        if (configJson == null) return 1
        return try {
            val obj = kotlinx.serialization.json.Json.parseToJsonElement(configJson).jsonObject
            val concurrency = obj["concurrency"]?.jsonPrimitive?.int
            concurrency?.coerceIn(1, 5) ?: 3
        } catch (e: Exception) {
            3
        }
    }

    private fun buildBatchSummary(
        taskType: BatchTaskType,
        status: String,
        configSummary: String,
        startedAt: Long,
        total: Int,
        pending: Int,
        concurrency: Int,
        processed: Int,
        success: Int,
        skipped: Int,
        failure: Int,
        itemDetails: List<String>
    ): String = buildString {
        val finishedAt = System.currentTimeMillis()
        appendLine("taskId=$taskId")
        appendLine("type=$taskType")
        appendLine("status=$status")
        appendLine()
        appendLine("configuration:")
        appendLine(configSummary.prependIndent("  "))
        appendLine()
        appendLine("startedAt=$startedAt")
        appendLine("finishedAt=$finishedAt")
        appendLine("durationMs=${finishedAt - startedAt}")
        appendLine("total=$total")
        appendLine("items=$pending")
        appendLine("concurrency=$concurrency")
        appendLine("processed=$processed")
        appendLine("success=$success")
        appendLine("skipped=$skipped")
        appendLine("failure=$failure")
        if (itemDetails.isNotEmpty()) {
            appendLine()
            appendLine("items:")
            itemDetails.take(MAX_ITEM_DETAILS_IN_LOG).forEach { detail ->
                appendLine(detail.trimEnd())
                appendLine()
            }
            val omitted = itemDetails.size - MAX_ITEM_DETAILS_IN_LOG
            if (omitted > 0) {
                appendLine("... $omitted more item details omitted")
            }
        }
    }

    private fun buildConfigSummary(type: BatchTaskType, configJson: String?): String {
        if (configJson.isNullOrBlank()) return "config=(none)"
        return runCatching {
            when (type) {
                BatchTaskType.MATCH_METADATA,
                BatchTaskType.MATCH_LYRICS,
                BatchTaskType.MATCH_COVER -> summarizeMatchConfig(configJson)
                BatchTaskType.RENAME_FILES -> summarizeRenameConfig(configJson)
                BatchTaskType.EDIT_TAGS -> summarizeEditTagsConfig(configJson)
                BatchTaskType.CONVERT_LYRICS_FORMAT -> summarizeLyricsFormatConfig(configJson)
                BatchTaskType.SCAN_REPLAY_GAIN -> summarizeReplayGainConfig(configJson)
                BatchTaskType.EXPORT_LYRICS,
                BatchTaskType.EXPORT_COVER -> summarizeExportConfig(configJson)
            }
        }.getOrElse { e ->
            buildString {
                appendLine("configParseError=${e.message ?: e::class.java.simpleName}")
                appendLine("rawConfig=${configJson.take(MAX_CONFIG_LOG_LENGTH)}")
            }.trimEnd()
        }
    }

    private fun summarizeMatchConfig(configJson: String): String {
        val config = Json.decodeFromString<MatchMetadataTaskConfig>(configJson)
        return buildString {
            appendLine("concurrency=${config.concurrency}")
            appendLine("separator=${config.separator}")
            appendLine("preferFileName=${config.matchConfig.preferFileName}")
            appendLine("enabledSources=${config.enabledSourceOrderIds.joinToString(" > ").ifBlank { "(default)" }}")
            appendLine("fields=${config.matchConfig.targetModes.toSortedMap(compareBy { it.name }).entries.joinToString(", ") { "${it.key.name}:${it.value.name}" }}")
        }.trimEnd()
    }

    private fun summarizeRenameConfig(configJson: String): String {
        val config = Json.decodeFromString<RenameFilesTaskConfig>(configJson)
        return buildString {
            appendLine("renameFormat=${config.renameFormat}")
            appendLine("characterMappingRules=${config.characterMappingRules.size}")
            val customizedRules = config.characterMappingRules.filter { rule ->
                rule.charMappings.isNotEmpty()
            }
            appendLine("customizedMappingRules=${customizedRules.joinToString(", ") { it.name }.ifBlank { "(none)" }}")
        }.trimEnd()
    }

    private fun summarizeEditTagsConfig(configJson: String): String {
        val config = Json.decodeFromString<EditTagsTaskConfig>(configJson)
        val keep = EditTagsTaskConfig.KEEP_VALUE
        val modifiedFields = buildList {
            if (config.title != keep) add("title")
            if (config.artist != keep) add("artist")
            if (config.albumArtist != keep) add("albumArtist")
            if (config.album != keep) add("album")
            if (config.date != keep) add("date")
            if (config.genre != keep) add("genre")
            if (config.trackNumber != keep) add("trackNumber")
            if (config.discNumber != keep) add("discNumber")
            if (config.composer != keep) add("composer")
            if (config.lyricist != keep) add("lyricist")
            if (config.copyright != keep) add("copyright")
            if (config.comment != keep) add("comment")
            if (config.lyrics != keep) add("lyrics")
            if (config.ratingModified) add("rating")
            if (config.coverUri != null) add("cover")
            if (config.removeCover) add("removeFrontCover")
            if (config.lyricsOffset.isNotBlank()) add("lyricsOffset")
            if (config.replayGainTrackGain != keep) add("replayGainTrackGain")
            if (config.replayGainTrackPeak != keep) add("replayGainTrackPeak")
            if (config.replayGainAlbumGain != keep) add("replayGainAlbumGain")
            if (config.replayGainAlbumPeak != keep) add("replayGainAlbumPeak")
            if (config.replayGainReferenceLoudness != keep) add("replayGainReferenceLoudness")
            if (config.customFields.isNotEmpty()) add("customFields")
        }

        return buildString {
            appendLine("concurrency=${config.concurrency}")
            appendLine("modifiedFields=${modifiedFields.joinToString(", ").ifBlank { "(none)" }}")
            if (config.ratingModified) appendLine("rating=${config.rating}")
            if (config.lyricsOffset.isNotBlank()) appendLine("lyricsOffset=${config.lyricsOffset}")
            appendLine("customFieldKeys=${config.customFields.joinToString(", ") { it.key }.ifBlank { "(none)" }}")
            appendSanitizedValue("title", config.title, keep)
            appendSanitizedValue("artist", config.artist, keep)
            appendSanitizedValue("albumArtist", config.albumArtist, keep)
            appendSanitizedValue("album", config.album, keep)
            appendSanitizedValue("date", config.date, keep)
            appendSanitizedValue("genre", config.genre, keep)
            appendSanitizedValue("trackNumber", config.trackNumber, keep)
            appendSanitizedValue("discNumber", config.discNumber, keep)
            appendSanitizedValue("composer", config.composer, keep)
            appendSanitizedValue("lyricist", config.lyricist, keep)
            appendSanitizedValue("copyright", config.copyright, keep)
            appendSanitizedValue("comment", config.comment, keep)
            appendSanitizedValue("replayGainTrackGain", config.replayGainTrackGain, keep)
            appendSanitizedValue("replayGainTrackPeak", config.replayGainTrackPeak, keep)
            appendSanitizedValue("replayGainAlbumGain", config.replayGainAlbumGain, keep)
            appendSanitizedValue("replayGainAlbumPeak", config.replayGainAlbumPeak, keep)
            appendSanitizedValue("replayGainReferenceLoudness", config.replayGainReferenceLoudness, keep)
            if (config.lyrics != keep) appendLine("lyrics=(modified, ${config.lyrics.length} chars)")
            if (config.coverUri != null) appendLine("coverUri=(set)")
            if (config.removeCover) appendLine("removeFrontCover=true")
        }.trimEnd()
    }

    private fun summarizeLyricsFormatConfig(configJson: String): String {
        val config = Json.decodeFromString<LyricsFormatConfig>(configJson)
        return buildString {
            appendLine("targetFormat=${config.targetFormat?.name ?: "KEEP"}")
            appendLine("concurrency=${config.concurrency}")
            appendLine("operation=${config.operation}")
            appendLine("twoColumnMapping=${config.twoColumnMapping}")
            appendLine("threeColumnMapping=${config.threeColumnMapping}")
            appendLine("formatLineOrder=${config.formatLineOrder}")
            appendLine("removeTagLines=${config.removeTagLines}")
            appendLine("removeEmptyLines=${config.removeEmptyLines}")
        }.trimEnd()
    }

    private fun summarizeReplayGainConfig(configJson: String): String {
        val config = Json.decodeFromString<ReplayGainTaskConfig>(configJson)
        return buildString {
            appendLine("concurrency=${config.concurrency}")
            config.targetLoudness?.let { appendLine("targetLoudness=$it") }
            config.peakMode?.let { appendLine("peakMode=${it.name}") }
        }.trimEnd()
    }

    private fun summarizeExportConfig(configJson: String): String {
        val config = Json.decodeFromString<BatchExportTaskConfig>(configJson)
        return buildString {
            appendLine("destination=${config.destination}")
            if (config.destinationTreeUri != null) {
                appendLine("destinationTreeUri=${config.destinationTreeUri}")
            }
            appendLine("concurrency=${config.concurrency}")
        }.trimEnd()
    }

    private fun StringBuilder.appendSanitizedValue(
        name: String,
        value: String,
        keep: String
    ) {
        if (value == keep) return
        appendLine("$name=${value.sanitizeConfigValue()}")
    }

    private fun String.sanitizeConfigValue(): String {
        val oneLine = replace("\r", "\\r").replace("\n", "\\n")
        return if (oneLine.length <= MAX_CONFIG_VALUE_LENGTH) {
            oneLine
        } else {
            "${oneLine.take(MAX_CONFIG_VALUE_LENGTH)}... (${length} chars)"
        }
    }

    private fun buildBatchMessage(
        type: BatchTaskType,
        status: String,
        processed: Int,
        success: Int,
        skipped: Int,
        failure: Int
    ): String {
        return "Batch task $status: $type (processed=$processed, success=$success, skipped=$skipped, failure=$failure)"
    }

    private suspend fun logBatch(
        level: AppLogLevel,
        message: String,
        detail: String? = null,
        relatedId: String? = null
    ) {
        try {
            appLogRepository.log(
                level = level,
                type = AppLogType.BATCH,
                tag = TAG,
                message = message,
                detail = detail,
                relatedId = relatedId
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write batch log", e)
        }
    }

    private suspend fun logBatchException(
        message: String,
        throwable: Throwable,
        relatedId: String? = null
    ) {
        try {
            appLogRepository.logException(
                type = AppLogType.BATCH,
                tag = TAG,
                message = message,
                throwable = throwable,
                relatedId = relatedId
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write batch exception log", e)
        }
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        private const val TAG = "BatchTaskWorker"
        private const val WAKE_LOCK_TAG_PREFIX = "Lyrico:BatchTask"
        private const val WAKE_LOCK_TIMEOUT_MILLIS = 6 * 60 * 60 * 1000L
        private const val MAX_ITEM_DETAILS_IN_LOG = 50
        private const val MAX_CONFIG_LOG_LENGTH = 8_000
        private const val MAX_CONFIG_VALUE_LENGTH = 200
        private const val ITEM_PROCESS_TIMEOUT_MILLIS = 120_000L
    }
}
