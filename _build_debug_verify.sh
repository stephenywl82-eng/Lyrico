#!/usr/bin/env bash
# Lyrico Phase 4 验证包：重打 debug，确认顶栏 ⋮ 菜单恢复 + SDW/Miuix 观感统一。
# 关键环境约束：
#   - kspDebugKotlin 在本机会挂死（0% CPU 无输出），用 D:/tmp/_noksp.init.gradle 禁用该 task，
#     复用 build/generated/ksp/debug/kotlin 里已生成的 48 个源码。
#   - 沙箱禁止「修改/删除已存在的文件」，Gradle 清理 stale 输出会撞墙 —— 必须在沙箱外跑。
#   - 临时目录全部指向 E 盘，不占 C 盘。
export MSYS_NO_PATHCONV=1
set -x

export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
export ANDROID_HOME='D:\Android\Sdk'
export ANDROID_SDK_ROOT='D:\Android\Sdk'
export TMP='E:\tmp'
export TEMP='E:\tmp'
mkdir -p /e/tmp

cd /e/WorkBuddy/Claw/Lyrico || exit 1

STAMP=$(date +%Y%m%d_%H%M)
LOG="/e/WorkBuddy/Claw/Lyrico/build_debug_${STAMP}.log"

echo "=== 开始编译 $(date) ==="
./gradlew \
  -I 'D:/tmp/_noksp.init.gradle' \
  -Dorg.gradle.jvmargs="-Xmx2800m -Dfile.encoding=UTF-8 -Djava.io.tmpdir=E:\\tmp" \
  --no-daemon --no-configuration-cache --no-watch-fs --max-workers=3 \
  :lyrico-app:assembleDebug \
  > "$LOG" 2>&1
GRADLE_EXIT=$?
echo "GRADLE_EXIT=$GRADLE_EXIT"

tail -40 "$LOG"

if [ "$GRADLE_EXIT" != "0" ]; then
  echo "---- 编译失败，日志: $LOG"
  exit "$GRADLE_EXIT"
fi

echo "=== 产物 ==="
ls -l lyrico-app/build/outputs/apk/debug/*.apk

APK=$(ls -t lyrico-app/build/outputs/apk/debug/*.apk | head -1)
OUT="/e/WorkBuddy/Claw/Lyrico/Lyrico_SDW验证包_${STAMP}.apk"
cp "$APK" "$OUT"
echo "=== 归档: $OUT ==="
ls -l "$OUT"

ADB='E:\Android\Sdk\platform-tools\adb.exe'
if "$ADB" devices | grep -q 'device$'; then
  echo "=== 设备在线，装机中 ==="
  "$ADB" install -r "$OUT"
  "$ADB" shell monkey -p com.lonx.lyrico.debug 1
  echo "装完: $OUT"
else
  echo "=== 设备离线，未装机。插回 USB 后执行: adb install -r \"$OUT\" ==="
fi
