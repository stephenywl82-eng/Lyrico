# Lyrico（SDW 修改版）

[![GitHub Release](https://img.shields.io/github/v/release/stephenywl82-eng/lyrico?style=flat-square)](https://github.com/stephenywl82-eng/Lyrico/releases)
![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/stephenywl82-eng/lyrico/total?style=flat-square)
[![License](https://img.shields.io/github/license/stephenywl82-eng/Lyrico?style=flat-square)](./LICENSE)

> 本仓库是 [Replica0110/Lyrico](https://github.com/Replica0110/Lyrico) 的一个 **fork / 个人修改版**。它在原项目基础上把界面替换为 SDW（基于 Material 3 Expressive）风格，并修复、新增了若干功能。原项目的版权、许可证与致谢保持不变，本文档其余章节的描述仍适用于原版能力。

Lyrico 是一款面向 Android 的开源本地音乐标签编辑与歌词管理工具。它用于整理本地音乐库、读写音频元数据，并通过插件化搜索源补全歌词、封面和其他歌曲信息。

## 本分支相对原版的改动

本 fork 在保留原项目全部底层能力（音乐库管理、标签编辑、插件化搜索、歌词处理等）的基础上，对界面与若干功能做了重写与修复。

### 1. SDW 界面（Material 3 Expressive）

将原版的 Miuix / SaltUI 双界面整体替换为基于 Material 3 Expressive 的 SDW 主题：

- **动态取色**：Android 12+ 跟随系统壁纸自动派生主题色；低版本提供中性黑 / 浅色静态兜底，并支持深浅主题切换。
- **官方圆角刻度**：严格按 Material 3 的 10 级圆角刻度排版，信息密集的列表卡片回落到 12dp 官方语义间距，只有按钮、播放条等「主角」元素才使用胶囊等大圆角，疏密有致。
- **SDW 排版体系**：歌单名、专辑标题等使用超大超粗（ExtraBold）字体配合负字距，与普通正文形成强烈的字重落差，扫一眼即可抓住重点。
- **系统化动效**：内容切换、文件夹加号菜单展开、扫描 / 处理进度条统一走官方 MotionScheme 动效，FAB 为顺滑的 morph 形变而非硬切，进度条采用 Expressive 风格的流畅填充。
- **文件夹 FAB 紧凑化**：右下角加号展开菜单重做为紧凑分组结构，不再拖出一条长列表遮挡内容。

### 2. 批量重命名修复

针对原版在 Android 分区存储下重命名必败、进度条卡死的问题做了修复：

- **改用 MediaStore 改名**：分区存储下 `File.renameTo` 必然失败，现改用 `ContentResolver.update(DISPLAY_NAME)` 更新媒体库中的显示名来完成改名；当更新影响行数为 0 时，会解析规范 URI 后重试。
- **批量写权限前置申请**：执行重命名前，在 UI 层通过 `MediaStore.createWriteRequest` 统一申请批量写入权限（处理 `RecoverableSecurityException`）。因为重命名在后台 worker 中执行、无法自行弹窗，所以权限必须在进入任务前由界面预申请。
- **进度条修复**：修复了进度条卡在中间的问题——前台通知更新过于频繁（约每秒 40 次）会被系统丢弃，现做节流（约 500ms 一次）；应用内进度条分母采用 `max(total, current)` 的安全写法；任务结束时强制将进度置为 100%。
- **缺失文件降级跳过**：对于 MediaStore 中存在记录但文件已被删除的「幽灵记录」（打开即 `ENOENT`），从「失败」降级为「跳过」，避免误报失败。

### 3. 清理缺失文件

新增「设置 → 扫描设置 → 清理缺失文件」功能：

- **真实存在性判定**：通过 `AudioFileAccess.isUriMissing(uri)` 真正尝试打开文件描述符来判断歌曲对应的文件是否还在，绕过 MediaStore 未刷新导致的「假存在」假象。
- **联动清理**：移除文件已不存在的歌曲，并同步清理其关联的歌词、标签以及所属文件夹的计数，保证曲库数据一致。

## 功能特性

- **本地音乐库管理**：扫描本地音乐文件，按歌曲、艺术家、专辑、文件夹等维度浏览与搜索。
- **音频元数据编辑**：读取和修改标题、艺术家、专辑、专辑艺术家、年份、流派、音轨号、歌词、封面、注释和自定义标签等信息。
- **多格式标签读写**：基于 TagLib 处理 MP3、FLAC、OGG/Opus、M4A/AAC、WAV、APE 等常见音频格式。
- **歌词与封面补全**：通过插件搜索歌词、翻译、音译、封面和候选元数据，并写入本地音频文件。
- **歌词格式处理**：支持普通 LRC、增强型逐字歌词、TTML 等歌词内容的展示、匹配、转换与写入。
- **批量整理流程**：支持多选、批量匹配、批量编辑、批量重命名、批量响度分析、批量分享和批量删除。
- **插件化搜索源**：在线搜索能力由插件提供，应用本体负责运行时、配置界面、结果处理和标签写入。
- **SDW 动态主题**：主题色跟随系统壁纸自动派生，支持深浅色；排版与动效遵循 Material 3 Expressive 规范。

## 插件系统

Lyrico 的在线音乐信息搜索采用插件化架构。搜索源插件以 JavaScript 编写，运行在 Android 端嵌入式 [quickjs-ng](https://github.com/quickjs-ng/quickjs) 运行时中。

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

## 已搭载 Lyrico 外部编辑功能的播放器

接入方式见 [外部编辑接入指南](https://replica0110.github.io/Lyrico/guide/external-edit.html)。

| 播放器名称 | 适配版本 |
|---|---|
| [Moto Music](https://github.com/stephenywl82-eng/SDWMP3_CN) | 最新版 |

## 构建

推荐使用 Android Studio 打开项目，也可以直接通过 Gradle 构建。

基础环境：

- [JDK 21](https://adoptium.net/temurin/releases/?version=21)
- [Android SDK](https://developer.android.com/studio)
- [Android NDK 29](https://developer.android.com/ndk/downloads)
- [CMake 4.1.2](https://cmake.org/download/)
- [Git](https://git-scm.com/downloads/)（Windows 使用 [Git for Windows](https://gitforwindows.org/) 附带的 Bash）
- [GNU Make 4.x](https://community.chocolatey.org/packages/make)

首次构建前初始化源码子模块：

```powershell
git submodule update --init --recursive
```

常用命令：

```powershell
.\gradlew.bat :lyrico-app:assembleDebug
```

仅检查 Kotlin 编译：

```powershell
.\gradlew.bat :lyrico-app:compileDebugKotlin
```

主要模块：

- `lyrico-app`：Android 应用主模块，包含 UI、音乐库、搜索、插件运行时和业务逻辑。
- `lyrico-audiotag`：音频标签读写模块，封装底层音频元数据处理能力。
- `docs`：VitePress 文档站点，覆盖插件使用、插件开发和宿主 API。

## 维护取向

本 fork 为个人修改版，主要服务于自己的使用需求，不追求覆盖所有可能的个性化场景；底层能力与生态仍跟随原项目。原项目的维护取向可供参考：

项目开发主要围绕维护者自身的实际使用需求展开，不追求覆盖所有可能的个性化场景。主线功能通常会优先考虑：

- 对多数用户有实际价值
- 交互和概念足够简单
- 长期维护成本可控
- 能通过插件或配置扩展的能力不硬编码进应用本体

高度定制化、使用场景过窄或会明显增加复杂度的功能，可能不会纳入主线，更适合通过插件、配置或 fork 项目实现。

## 参考项目与第三方组件

项目在开发过程中参考或使用了以下优秀项目与组件：

### 界面与设计

- [Material 3 Expressive](https://developer.android.com/design/ui/mobile/expression)（Jetpack Compose Material 3）：本 fork 的 SDW 界面即基于其设计语言——跟随壁纸的动态取色、官方圆角刻度与 MotionScheme 动效。

### 音频与元数据

- [TagLib](https://github.com/taglib/taglib)：音频元数据读写库
- [libebur128](https://github.com/jiixyj/libebur128)：EBU R128 音频响度标准实现
- [quickjs-ng](https://github.com/quickjs-ng/quickjs)：嵌入式 JavaScript 运行时（插件系统）

### 歌词与搜索

- [LDDC](https://github.com/chenmozhijin/LDDC)：简单易用的精准歌词下载匹配工具
- [any-listen-extension-online-metadata](https://github.com/any-listen/any-listen-extension-online-metadata)：any-listen 音频元数据搜索插件

### 参考应用

- [音乐标签](https://www.cnblogs.com/vinlxc/p/11932130.html)：音频标签编辑应用

## 捐赠

如果 Lyrico 对你有帮助，可以通过原作者的 [爱发电](https://ifdian.net/a/replica0110) 支持项目维护。

[赞助者列表](https://replica0110.github.io/Sponsors/)

## 许可证

Lyrico 本体代码基于 [Apache License 2.0](./LICENSE) 开源，本 fork 的改动同样以 Apache License 2.0 发布。

项目使用的第三方组件仍遵循各自许可证。Gradle/Android 依赖的许可信息可在应用内“开源许可”页面查看；随源码引入或嵌入构建的组件见 [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md)。
