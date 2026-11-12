# Lyrico

[![GitHub Release](https://img.shields.io/github/v/release/stephenywl82-eng/lyrico?style=flat-square)](https://github.com/stephenywl82-eng/Lyrico/releases)
![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/stephenywl82-eng/lyrico/total?style=flat-square)
[![License](https://img.shields.io/github/license/stephenywl82-eng/Lyrico?style=flat-square)](./LICENSE)

Lyrico 是一款面向 Android 的开源**本地音乐标签编辑与歌词管理工具**。它帮助你整理本地音乐库、读写音频元数据（标签），并通过插件化的在线搜索源，自动补全歌词、翻译、音译、封面以及候选元数据，再写回本地音频文件。所有整理工作都围绕「本地文件」展开，无需依赖云端账户。

> 官方上游仓库：[Moriafly/Lyrico](https://github.com/Moriafly/Lyrico)

## 目录

- [关于本 Fork](#关于本-fork)
- [功能特性](#功能特性)
- [SDW 设计语言界面](#sdw-设计语言界面)
- [日志系统与批量处理改进](#日志系统与批量处理改进)
- [安装](#安装)
- [构建](#构建)
- [插件系统](#插件系统)
- [已适配 Lyrico 外部编辑功能的播放器](#已适配-lyrico-外部编辑功能的播放器)
- [维护取向](#维护取向)
- [参考项目与第三方组件](#参考项目与第三方组件)
- [捐赠](#捐赠)
- [许可证](#许可证)

## 关于本 Fork

本仓库是 [Moriafly/Lyrico](https://github.com/Moriafly/Lyrico) 的个人 fork，在追踪上游改动的同时，加入了面向自用需求的调整。与上游的主要差异如下：

| 维度 | 上游 Moriafly/Lyrico | 本 Fork |
| --- | --- | --- |
| 界面风格 | 标准 Material Design 3，含 Miuix / SaltUI 双界面 | 迁移到 **Material 3 Expressive**，统一为 **SDW 设计语言**组件体系（Miuix / SaltUI 双界面已移除） |
| 发布架构 | 多架构 | 仅 **arm64-v8a**（不再构建 32 位 armeabi-v7a） |
| 发布构建 | 标准打包 | 开启 **R8** 代码压缩/混淆，并启用 `shrinkResources` 资源缩减 |
| 定位 | 官方完整版 | 个人定制版，自用调整优先 |

> **说明**：本 Fork 的界面与上游差异较大，属于个人定制版本。如需官方完整功能、多架构支持与官方支持，请以上游仓库为准。

### 预编译 Release APK

不想自己构建，可直接下载已编译好的 release 包：

- [Lyrico-1.7.0-SDW-M3Expressive-arm64-v8a.apk](./Lyrico-1.7.0-SDW-M3Expressive-arm64-v8a.apk)

| 项目 | 说明 |
| --- | --- |
| 版本 | 1.7.0（commit `b6ea6f8`，对应「完整对齐官方 M3 Expressive 并清理死代码」） |
| 包名 | `com.lonx.lyrico` |
| 架构 | arm64-v8a only |
| 主题 | Material 3 Expressive（SDW 设计语言） |
| 压缩 | R8 开启 + `shrinkResources` 资源缩减 |
| 签名 | debug keystore（非 test-only，普通安装即可） |
| 体积 | 约 14.5 MB |

## 功能特性

### 本地音乐库管理

- 扫描本地音乐文件，按**歌曲 / 艺术家 / 专辑 / 文件夹**等维度浏览与搜索。

### 音频元数据编辑

- 读取与修改标题、艺术家、专辑、专辑艺术家、年份、流派、音轨号、歌词、封面、注释以及自定义标签等。

### 多格式标签读写

- 基于 **TagLib** 处理 MP3、FLAC、OGG/Opus、M4A/AAC、WAV、APE 等常见音频格式。

### 歌词与封面补全

- 通过插件搜索歌词、翻译、音译、封面与候选元数据，并写回本地音频文件。

### 歌词格式处理

- 支持普通 **LRC**、增强型逐字歌词、**TTML** 等歌词内容的展示、匹配、转换与写入。

### 批量整理

- 多选、批量匹配、批量编辑、批量重命名、批量响度分析、批量分享、批量删除。

### 插件化搜索源

- 在线搜索能力由 JavaScript 插件提供，应用本体负责运行时、配置界面、结果处理与标签写入。

### SDW 设计语言界面

- 统一在 **Material 3 Expressive** 主题之上，采用 SDW 自定义组件体系（已移除 Miuix / SaltUI 双界面）：
  - 圆角遵循官方 MD3 刻度（卡片 `ShapeDefaults.Large`、底部弹层只圆上方两角等）；
  - Expressive 字阶（放大 display / headline 大档，强化 headline 观感）；
  - 统一 Motion 与主题令牌（SDWTheme / SDWTokens / Motion）。

### 日志系统与批量处理改进

本分支在日志与批量处理的可观测性上做了增强：

- **应用内日志系统与崩溃报告**：内置 AppLog 日志系统，关键异常与崩溃会记录堆栈；支持按级别 / 类型筛选查看、一键导出（导出内容附带设备型号、系统版本、Android 版本等诊断信息），并在日志页内设置日志保留期。
- **插件错误原因记录**：插件导入 / 安装失败时，自动记录具体失败原因（包括「无可安装插件」以及各来源的根路径与原因），便于排查。
- **批量处理失败原因显示**：批量编辑、批量重命名、匹配歌词 / 封面、响度分析等写标签操作失败时，会从 `SaveAudioTagsResult.failureReason()` 提取并展示 / 记录具体失败原因，而非笼统提示为「无响应」，便于定位问题；同时支持按筛选条件批量删除日志。

## 安装

### 方式一：直接安装 APK

1. 下载上方的预编译 APK；
2. 将 APK 传到手机，在文件管理器中点击安装；
3. 若系统拦截，请在「设置 → 安全 → 安装未知应用」中允许对应来源。

### 方式二：ADB 安装

```bash
adb install -r Lyrico-1.7.0-SDW-M3Expressive-arm64-v8a.apk
```

> 本包**未标记 test-only**，普通 `adb install -r` 即可安装。若个别环境仍提示 `INSTALL_FAILED_TEST_ONLY`，追加 `-t` 参数即可：
>
> ```bash
> adb install -r -t Lyrico-1.7.0-SDW-M3Expressive-arm64-v8a.apk
> ```

### 常见问题

- **签名冲突（`INSTALL_FAILED_UPDATE_INCOMPATIBLE`）**：设备上已存在用其他密钥签名的 Lyrico（如官方版、其他构建产物）。请先**卸载**原有的 Lyrico，再安装本包。
- **架构不符**：本包仅含 arm64-v8a，需在 64 位 ARM 设备上安装（当前绝大多数 Android 设备均满足）。
- **安装失败无明确原因**：尝试先卸载旧版本，并使用 `adb install -r` 重新安装；仍失败可查看 `adb logcat` 中 `PackageManager` 的报错。

## 构建

推荐使用 **Android Studio** 打开项目，也可以直接通过 Gradle 构建。

### 基础环境

- [JDK 21](https://adoptium.net/temurin/releases/?version=21)
- [Android SDK](https://developer.android.com/studio)
- [Android NDK 29](https://developer.android.com/ndk/downloads)
- [CMake 3.22.1](https://cmake.org/download/)（构建脚本已在 `build.gradle.kts` 中固定该版本）
- [Git](https://git-scm.com/downloads/)（Windows 使用 [Git for Windows](https://gitforwindows.org/) 附带的 Bash）
- [GNU Make 4.x](https://community.chocolatey.org/packages/make)

### 初始化子模块

首次构建前需拉取音频标签等底层依赖的源码子模块：

```powershell
git submodule update --init --recursive
```

### 常用命令

```powershell
# Debug 构建
.\gradlew.bat :lyrico-app:assembleDebug

# Release 构建（仅 arm64-v8a，R8 + 资源缩减）
.\gradlew.bat :lyrico-app:assembleRelease
```

仅检查 Kotlin 编译：

```powershell
.\gradlew.bat :lyrico-app:compileReleaseKotlin
```

> 发布构建仅生成 **arm64-v8a** 架构的 APK，产物位于 `lyrico-app/build/outputs/apk/release/`。请确保在 64 位 ARM 设备上安装使用。

### 主要模块

- `lyrico-app`：Android 应用主模块，包含 UI、音乐库、搜索、插件运行时与业务逻辑。
- `lyrico-audiotag`：音频标签读写模块，封装底层音频元数据处理能力（基于 TagLib 等）。
- `docs`：VitePress 文档站点，覆盖插件使用、插件开发与宿主 API。

## 插件系统

Lyrico 的在线音乐信息搜索采用**插件化架构**。搜索源插件以 JavaScript 编写，运行在 Android 端嵌入式 [quickjs-ng](https://github.com/quickjs-ng/quickjs) 运行时中。

插件可以声明并实现以下能力：

- 搜索歌曲信息
- 获取歌词
- 搜索封面
- 声明插件配置项

应用会根据插件 manifest 暴露配置界面、运行插件脚本，并将插件返回的结果转换为应用内部的歌曲、歌词和封面数据。插件文档从这里开始阅读：

- [插件使用](https://replica0110.github.io/Lyrico/plugins/using.html)
- [插件开发概览](https://replica0110.github.io/Lyrico/plugins/overview.html)
- [Manifest 参考](https://replica0110.github.io/Lyrico/plugins/manifest.html)
- [宿主 API](https://replica0110.github.io/Lyrico/plugins/host-api.html)

## 已适配 Lyrico 外部编辑功能的播放器

接入方式见 [外部编辑接入指南](https://replica0110.github.io/Lyrico/guide/external-edit.html)。如需加入下方列表，请提交 PR。

| 播放器名称                                                       | 适配版本                       |
|-------------------------------------------------------------|----------------------------|
| [光锥音乐](https://coneplayer.trantor.ink/#/)                   | 1.1.4+                     |
| [Halcyon](https://github.com/Kifranei/Halcyon)              | 1.1.0+                     |
| [棱镜音乐](https://github.com/Ryderwe/PrismMusic-Release)       | 0.1.3+                     |
| [mica-music](https://github.com/lecoix/mica-music)          | 0.1.9.1+                   |
| [Raws-Music](https://github.com/QFDY-GZC/RawS-Music)        | 0.9.39-beta+               |
| [Salt Player](https://github.com/Moriafly/SaltPlayerSource) | 12.4.0-alpha01-2026091601+ |
| [MD3Music](https://github.com/zzyoxml/md3Music)             | 5.5.0+                     |

## 维护取向

项目开发主要围绕维护者自身的实际使用需求展开，不追求覆盖所有可能的个性化场景。主线功能通常会优先考虑：

- 对多数用户有实际价值
- 交互和概念足够简单
- 长期维护成本可控
- 能通过插件或配置扩展的能力不硬编码进应用本体

高度定制化、使用场景过窄或会明显增加复杂度的功能，可能不会纳入主线，更适合通过插件、配置或 fork 项目实现。

## 参考项目与第三方组件

项目在开发过程中参考或使用了以下优秀项目：

- [LDDC](https://github.com/chenmozhijin/LDDC)：简单易用的精准歌词下载匹配工具
- [any-listen-extension-online-metadata](https://github.com/any-listen/any-listen-extension-online-metadata)：any-listen 音频元数据搜索插件
- [Miuix](https://github.com/compose-miuix-ui/miuix)：Xiaomi HyperOS 风格的 Compose UI 组件库
- [SaltUI](https://github.com/Moriafly/SaltUI)：Compose Multiplatform 平台化 UI 组件库
- [音乐标签](https://www.cnblogs.com/vinlxc/p/11932130.html)：音频标签编辑应用
- [quickjs-ng](https://github.com/quickjs-ng/quickjs)：嵌入式 JavaScript 运行时
- [TagLib](https://github.com/taglib/taglib)：音频元数据读写库
- [libebur128](https://github.com/jiixyj/libebur128)：EBU R128 音频响度标准实现

## 捐赠

如果 Lyrico 对你有帮助，可以通过 [爱发电](https://ifdian.net/a/replica0110) 支持项目维护。

[赞助者列表](https://replica0110.github.io/Sponsors/)

## 许可证

Lyrico 本体代码基于 [Apache License 2.0](./LICENSE) 开源。

项目使用的第三方组件仍遵循各自许可证。Gradle/Android 依赖的许可信息可在应用内“开源许可”页面查看；随源码引入或嵌入构建的组件见 [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md)。
