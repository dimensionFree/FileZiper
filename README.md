# FileZiper

FileZiper 是一个本地批处理工具，用来整理文件目录并压缩媒体文件。

当前主要包含两类功能：

- 图片流程：把子目录文件移动到根目录，并压缩较大的图片。
- 视频流程：独立扫描指定目录下的大 MP4 视频，使用 FFmpeg 压缩到新的输出目录。

项目是 Java/Maven 工具型项目，更偏个人本地批处理脚本，不是通用 GUI 软件。

## 环境要求

- Java 10 或以上。
- Maven。
- FFmpeg 和 ffprobe。

本项目的视频压缩会优先按以下顺序寻找 FFmpeg：

1. `ffmpegPath.txt` 中配置的路径。
2. 项目目录下的 `./ffmpeg/bin/ffmpeg.exe` 和 `./ffmpeg/bin/ffprobe.exe`。
3. 系统 PATH 中的 `ffmpeg` 和 `ffprobe`。

当前 `.gitignore` 已忽略本地 FFmpeg 二进制目录和下载包，因此这些文件不会提交到仓库。

## 准备操作

1. 在项目根目录创建或修改 `sourceDirection.txt`。

   内容只写一行，需要处理的目标目录，例如：

   ```text
   E:\j\mmd\bunnya\testCompress
   ```

2. 准备 FFmpeg。

   如果不想改系统 PATH，可以把 Windows 版 FFmpeg 解压到：

   ```text
   D:\project\FileZiper\ffmpeg\bin
   ```

   目录中应包含：

   ```text
   ffmpeg.exe
   ffprobe.exe
   ```

   或者在项目根目录创建 `ffmpegPath.txt`，写入 FFmpeg 的 `bin` 目录：

   ```text
   D:\tools\ffmpeg\bin
   ```

   也可以写 `ffmpeg.exe` 的完整路径：

   ```text
   D:\tools\ffmpeg\bin\ffmpeg.exe
   ```

3. 编译检查：

   ```powershell
   E:\apache-maven-3.8.2\bin\mvn.cmd test
   ```

## 视频压缩

入口类：

```text
src/main/java/utils/VideoCompressor.java
```

运行 `utils.VideoCompressor.main()`。

当前默认行为：

- 读取 `sourceDirection.txt` 指向的目录。
- 递归扫描 `.mp4` 文件。
- 跳过小于 `500MB` 的视频。
- 默认使用 NVIDIA GPU 编码器 `h264_nvenc`。
- 将宽度超过 `1920` 的视频压到 1080p 宽度以内，保持比例。
- 目标输出大小默认约 `300MB` 以内。
- 输出到源目录下的 `compressed-video` 目录。
- 输出文件名为：`原文件名.compressed.mp4`。
- 不删除、不覆盖原视频，是否替换由人工决定。

示例输出目录：

```text
E:\j\mmd\bunnya\testCompress\compressed-video
```

### GPU/CPU 切换

`VideoCompressor` 默认使用 GPU：

```java
public static final boolean IS_USE_GPU = true;
```

如果要切回 CPU 方案，改成：

```java
public static final boolean IS_USE_GPU = false;
```

CPU 模式使用 `libx264`，会先尝试 CRF 压缩，若仍超过目标大小，再按目标码率重压。CPU 通常画质/体积效率更稳，但速度明显慢于 GPU。

GPU 模式使用 `h264_nvenc`，直接按目标码率单次压缩。速度快，适合批量处理 Koikatsu/MMD 这类 4K60 视频。

## 视频完整性验证

压缩完成后，可以用 ffprobe 查看基本信息：

```powershell
.\ffmpeg\bin\ffprobe.exe -v error -show_entries format=duration,size,bit_rate -show_entries stream=index,codec_name,codec_type,width,height,avg_frame_rate -of default=noprint_wrappers=1 "压缩后文件.mp4"
```

也可以完整解码检查是否有损坏帧或尾部错误：

```powershell
.\ffmpeg\bin\ffmpeg.exe -v error -i "压缩后文件.mp4" -f null -
```

如果命令没有输出错误，并且退出码为 0，通常说明容器和音视频流可以完整读取。

注意：完整性检查只能确认文件结构和解码是否正常，不能判断原视频内容是否正确。例如原片中已有的 `Media offline` 画面，压缩后也会保留。

## 图片整理和压缩

入口类：

```text
src/main/java/MoveToRootAndZipPic.java
```

当前行为：

- 读取 `sourceDirection.txt` 指向的目录。
- `MoveToRoot` 会把子目录中的文件移动到根目录，并用父目录名参与重命名。
- `ZipPic` 会压缩大于 `2MB` 的图片。
- 支持 `.png`、`.jpg`、`.jpeg`、`.bmp`。
- 图片会被压到 `1920x1080` 以内，质量参数为 `0.8`。

重要：当前图片压缩会原地覆盖图片文件，视频压缩则不会覆盖原视频。

## 安全注意事项

- 视频压缩流程默认非破坏性，只生成新文件。
- 图片压缩流程当前是破坏性的，会覆盖原图。
- 第一次处理真实数据前，建议先复制少量文件到测试目录。
- 中断 FFmpeg 进程可能留下 `.tmp.mp4` 临时文件。中断产物即使能播放，也建议用完整性验证命令检查后再保留。
- 原始 4K60 视频播放时出现画面落后于时间轴，可能是播放器解码跟不上。压缩后的 1080p60 文件更容易流畅播放。

## 当前实测情况

在本机 NVIDIA GeForce RTX 4060 级别显卡上，Koikatsu/MMD 风格 4K60 MP4 使用 GPU 模式压缩时，观察到约 `5x-6x` 实时速度。

示例：

- 4 分 5 秒视频，约 592MB 输入，输出约 274.7MB，耗时约 41 秒。
- 9 分 14 秒视频，约 2.1GB 输入，输出约 255.8MB，耗时约 1 分 47 秒。
- 17 分 6 秒视频，约 2.3GB 输入，输出约 262.2MB，耗时约 2 分 57 秒。

实际速度会受显卡、源视频复杂度、分辨率、磁盘速度和目标大小影响。

