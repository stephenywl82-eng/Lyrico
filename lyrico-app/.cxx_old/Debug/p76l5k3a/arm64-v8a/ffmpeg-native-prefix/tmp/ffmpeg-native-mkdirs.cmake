# Distributed under the OSI-approved BSD 3-Clause License.  See accompanying
# file LICENSE.rst or https://cmake.org/licensing for details.

cmake_minimum_required(VERSION ${CMAKE_VERSION}) # this file comes with cmake

# If CMAKE_DISABLE_SOURCE_CHANGES is set to true and the source directory is an
# existing directory in our source tree, calling file(MAKE_DIRECTORY) on it
# would cause a fatal error, even though it would be a no-op.
if(NOT EXISTS "E:/WorkBuddy/Claw/Lyrico/lyrico-app/src/main/cpp/ffmpeg")
  file(MAKE_DIRECTORY "E:/WorkBuddy/Claw/Lyrico/lyrico-app/src/main/cpp/ffmpeg")
endif()
file(MAKE_DIRECTORY
  "E:/WorkBuddy/Claw/Lyrico/lyrico-app/.cxx/Debug/p76l5k3a/arm64-v8a/ffmpeg-build"
  "E:/WorkBuddy/Claw/Lyrico/lyrico-app/.cxx/Debug/p76l5k3a/arm64-v8a/ffmpeg-native-prefix"
  "E:/WorkBuddy/Claw/Lyrico/lyrico-app/.cxx/Debug/p76l5k3a/arm64-v8a/ffmpeg-native-prefix/tmp"
  "E:/WorkBuddy/Claw/Lyrico/lyrico-app/.cxx/Debug/p76l5k3a/arm64-v8a/ffmpeg-native-prefix/src/ffmpeg-native-stamp"
  "E:/WorkBuddy/Claw/Lyrico/lyrico-app/.cxx/Debug/p76l5k3a/arm64-v8a/ffmpeg-native-prefix/src"
  "E:/WorkBuddy/Claw/Lyrico/lyrico-app/.cxx/Debug/p76l5k3a/arm64-v8a/ffmpeg-native-prefix/src/ffmpeg-native-stamp"
)

set(configSubDirs )
foreach(subDir IN LISTS configSubDirs)
    file(MAKE_DIRECTORY "E:/WorkBuddy/Claw/Lyrico/lyrico-app/.cxx/Debug/p76l5k3a/arm64-v8a/ffmpeg-native-prefix/src/ffmpeg-native-stamp/${subDir}")
endforeach()
if(cfgdir)
  file(MAKE_DIRECTORY "E:/WorkBuddy/Claw/Lyrico/lyrico-app/.cxx/Debug/p76l5k3a/arm64-v8a/ffmpeg-native-prefix/src/ffmpeg-native-stamp${cfgdir}") # cfgdir has leading slash
endif()
