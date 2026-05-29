package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VideoCompressor {

    public static final String SOURCE_DIRECTION = "./sourceDirection.txt";
    public static final String FFMPEG_PATH_FILE = "./ffmpegPath.txt";
    public static final long VIDEO_MIN_SIZE = ByteConvertUtils.mbToBytes(500);
    public static final long TARGET_MAX_SIZE = ByteConvertUtils.mbToBytes(300);
    public static final int MAX_WIDTH = 1920;
    public static final int CRF = 26;
    public static final String PRESET = "medium";
    public static final boolean IS_USE_GPU = true;
    public static final String GPU_ENCODER = "h264_nvenc";
    public static final String GPU_PRESET = "p5";
    public static final int AUDIO_BITRATE_KBPS = 128;
    public static final boolean IS_COMPRESS_SUB_DIR = true;
    public static final boolean IS_STRICT_TARGET_SIZE = true;
    public static final String OUTPUT_DIR_NAME = "compressed-video";
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<String>(
            Arrays.asList(".mp4")
    );
    static File ROOT;
    static File OUTPUT_ROOT;
    static String FFMPEG_COMMAND = "ffmpeg";
    static String FFPROBE_COMMAND = "ffprobe";

    public static void main(String[] args) throws Exception {
        var directionStr = MyFIleUtil.getStrByTxtFile(SOURCE_DIRECTION);
        var root = new File(directionStr);
        compressVideos(root);
    }

    public static void compressVideos(File root) {
        if (root == null || !root.exists()) {
            throw new RuntimeException("invalid video root: " + root);
        }
        ROOT = root;
        OUTPUT_ROOT = new File(ROOT, OUTPUT_DIR_NAME);
        if (!OUTPUT_ROOT.exists() && !OUTPUT_ROOT.mkdirs()) {
            throw new RuntimeException("failed to create video output dir: " + OUTPUT_ROOT);
        }
        System.out.println("视频根目录: " + root.getAbsolutePath());
        System.out.println("视频输出目录: " + OUTPUT_ROOT.getAbsolutePath());
        checkFFmpeg();
        List<VideoTask> tasks = collectVideoTasks(root);
        printEstimate(tasks);
        for (VideoTask task : tasks) {
            compressOneVideo(task.file, task.videoInfo);
        }
    }

    private static List<VideoTask> collectVideoTasks(File root) {
        List<File> videos = new ArrayList<File>();
        collectVideosInDir(root, videos);
        List<VideoTask> tasks = new ArrayList<VideoTask>();
        for (File video : videos) {
            try {
                VideoInfo videoInfo = probeVideo(video);
                if (videoInfo.isValid()) {
                    tasks.add(new VideoTask(video, videoInfo));
                } else {
                    System.out.println("跳过视频，无法读取有效信息: " + video.getAbsolutePath());
                }
            } catch (Exception e) {
                System.out.println("跳过视频，读取视频信息失败: " + video.getAbsolutePath());
                e.printStackTrace();
            }
        }
        return tasks;
    }

    private static void collectVideosInDir(File direction, List<File> videos) {
        var files = direction.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                if (file.equals(OUTPUT_ROOT)) {
                    continue;
                }
                if (IS_COMPRESS_SUB_DIR) {
                    collectVideosInDir(file, videos);
                }
                continue;
            }
            if (isVideo(file) && file.length() > VIDEO_MIN_SIZE) {
                videos.add(file);
            }
        }
    }

    private static void compressOneVideo(File source, VideoInfo videoInfo) {
        var start = System.currentTimeMillis();
        long inputSize = source.length();
        System.out.println("开始压缩视频: " + source.getAbsolutePath());
        System.out.println("输入大小: " + ByteConvertUtils.bytesToMbStr(inputSize));
        System.out.println("时长: " + formatSeconds(videoInfo.durationSeconds)
                + ", 分辨率: " + videoInfo.width + "x" + videoInfo.height
                + ", 预计耗时: " + formatSeconds(estimateSeconds(videoInfo)[0])
                + " - " + formatSeconds(estimateSeconds(videoInfo)[1]));

        File finalOutput = buildOutputFile(source);
        File crfOutput = buildTempFile(finalOutput, "crf");
        File targetOutput = buildTempFile(finalOutput, "target");
        try {
            ensureParentDir(finalOutput);
            deleteIfExists(crfOutput);
            deleteIfExists(targetOutput);
            deleteIfExists(finalOutput);

            File bestOutput;
            if (IS_USE_GPU) {
                System.out.println("使用 GPU 编码器: " + GPU_ENCODER);
                boolean targetSuccess = runFFmpeg(buildGpuTargetCommand(source, targetOutput, videoInfo));
                if (targetSuccess && isUsefulOutput(source, targetOutput)) {
                    bestOutput = targetOutput;
                } else {
                    bestOutput = null;
                }
            } else {
                boolean crfSuccess = runFFmpeg(buildCrfCommand(source, crfOutput, videoInfo));
                bestOutput = crfSuccess && isUsefulOutput(source, crfOutput) ? crfOutput : null;

                if (IS_STRICT_TARGET_SIZE && needsTargetEncode(bestOutput)) {
                    boolean targetSuccess = runFFmpeg(buildTargetCommand(source, targetOutput, videoInfo));
                    if (targetSuccess && isUsefulOutput(source, targetOutput)) {
                        bestOutput = targetOutput;
                    }
                }
            }

            if (bestOutput == null) {
                System.out.println("跳过视频压缩，没有生成更小的输出文件");
                return;
            }
            if (IS_STRICT_TARGET_SIZE && bestOutput.length() > TARGET_MAX_SIZE) {
                System.out.println("跳过视频压缩，输出文件仍然超过目标大小: "
                        + ByteConvertUtils.bytesToMbStr(bestOutput.length()));
                return;
            }

            long outputSize = bestOutput.length();
            Files.move(bestOutput.toPath(), finalOutput.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("视频压缩完成: " + finalOutput.getAbsolutePath());
            System.out.println("输出大小: " + ByteConvertUtils.bytesToMbStr(outputSize));
            System.out.println("节省空间: " + ByteConvertUtils.bytesToMbStr(inputSize - outputSize));
        } catch (Exception e) {
            System.out.println("视频压缩失败: " + source.getAbsolutePath());
            e.printStackTrace();
        } finally {
            deleteQuietly(crfOutput);
            deleteQuietly(targetOutput);
            var cost = (System.currentTimeMillis() - start) / 1000.0;
            System.out.println("视频压缩实际耗时: " + formatSeconds(cost));
        }
    }

    private static void printEstimate(List<VideoTask> tasks) {
        long totalSize = 0;
        double totalDuration = 0;
        double minSeconds = 0;
        double maxSeconds = 0;
        for (VideoTask task : tasks) {
            totalSize += task.file.length();
            totalDuration += task.videoInfo.durationSeconds;
            double[] estimate = estimateSeconds(task.videoInfo);
            minSeconds += estimate[0];
            maxSeconds += estimate[1];
        }
        System.out.println("待处理视频数量: " + tasks.size());
        System.out.println("待处理视频总大小: " + ByteConvertUtils.bytesToMbStr(totalSize));
        System.out.println("待处理视频总时长: " + formatSeconds(totalDuration));
        System.out.println("视频编码模式: " + (IS_USE_GPU ? "GPU " + GPU_ENCODER : "CPU libx264"));
        System.out.println("预计总耗时: " + formatSeconds(minSeconds) + " - " + formatSeconds(maxSeconds));
    }

    private static double[] estimateSeconds(VideoInfo videoInfo) {
        double realtimeMin;
        double realtimeMax;
        if (IS_USE_GPU) {
            realtimeMin = 4.0;
            realtimeMax = 8.0;
        } else {
            realtimeMin = 0.25;
            realtimeMax = 1.2;
        }

        double pixelFactor = IS_USE_GPU ? 1.0 : Math.max(1.0, (videoInfo.width * videoInfo.height) / (1920.0 * 1080.0));
        double scaleFactor = videoInfo.width > MAX_WIDTH ? 1.25 : 1.0;
        double costFactor = pixelFactor * scaleFactor;

        double minSeconds = videoInfo.durationSeconds / realtimeMax * costFactor;
        double maxSeconds = videoInfo.durationSeconds / realtimeMin * costFactor;
        return new double[]{minSeconds, maxSeconds};
    }

    private static String formatSeconds(double seconds) {
        long roundedSeconds = Math.max(0, Math.round(seconds));
        long hours = roundedSeconds / 3600;
        long minutes = (roundedSeconds % 3600) / 60;
        long remainSeconds = roundedSeconds % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m " + remainSeconds + "s";
        }
        if (minutes > 0) {
            return minutes + "m " + remainSeconds + "s";
        }
        return remainSeconds + "s";
    }

    private static List<String> buildCrfCommand(File source, File output, VideoInfo videoInfo) {
        var command = new ArrayList<String>();
        command.add(FFMPEG_COMMAND);
        command.add("-y");
        command.add("-i");
        command.add(source.getAbsolutePath());
        appendScaleIfNeeded(command, videoInfo);
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add(PRESET);
        command.add("-crf");
        command.add(String.valueOf(CRF));
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add(AUDIO_BITRATE_KBPS + "k");
        command.add("-movflags");
        command.add("+faststart");
        command.add(output.getAbsolutePath());
        return command;
    }

    private static List<String> buildTargetCommand(File source, File output, VideoInfo videoInfo) {
        int videoBitrateKbps = calculateTargetVideoBitrateKbps(videoInfo);
        var command = new ArrayList<String>();
        command.add(FFMPEG_COMMAND);
        command.add("-y");
        command.add("-i");
        command.add(source.getAbsolutePath());
        appendScaleIfNeeded(command, videoInfo);
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add(PRESET);
        command.add("-b:v");
        command.add(videoBitrateKbps + "k");
        command.add("-maxrate");
        command.add(videoBitrateKbps + "k");
        command.add("-bufsize");
        command.add((videoBitrateKbps * 2) + "k");
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add(AUDIO_BITRATE_KBPS + "k");
        command.add("-movflags");
        command.add("+faststart");
        command.add(output.getAbsolutePath());
        return command;
    }

    private static List<String> buildGpuTargetCommand(File source, File output, VideoInfo videoInfo) {
        int videoBitrateKbps = calculateTargetVideoBitrateKbps(videoInfo);
        var command = new ArrayList<String>();
        command.add(FFMPEG_COMMAND);
        command.add("-y");
        command.add("-i");
        command.add(source.getAbsolutePath());
        appendScaleIfNeeded(command, videoInfo);
        command.add("-c:v");
        command.add(GPU_ENCODER);
        command.add("-preset");
        command.add(GPU_PRESET);
        command.add("-rc");
        command.add("vbr");
        command.add("-b:v");
        command.add(videoBitrateKbps + "k");
        command.add("-maxrate");
        command.add(videoBitrateKbps + "k");
        command.add("-bufsize");
        command.add((videoBitrateKbps * 2) + "k");
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add(AUDIO_BITRATE_KBPS + "k");
        command.add("-movflags");
        command.add("+faststart");
        command.add(output.getAbsolutePath());
        return command;
    }

    private static void appendScaleIfNeeded(List<String> command, VideoInfo videoInfo) {
        if (videoInfo.width > MAX_WIDTH) {
            command.add("-vf");
            command.add("scale=" + MAX_WIDTH + ":-2");
        }
    }

    private static int calculateTargetVideoBitrateKbps(VideoInfo videoInfo) {
        double totalKbps = TARGET_MAX_SIZE * 8.0 / videoInfo.durationSeconds / 1000.0;
        int videoKbps = (int) Math.floor(totalKbps * 0.92 - AUDIO_BITRATE_KBPS);
        return Math.max(videoKbps, 300);
    }

    private static VideoInfo probeVideo(File source) throws IOException, InterruptedException {
        var command = Arrays.asList(
                FFPROBE_COMMAND,
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1",
                source.getAbsolutePath()
        );
        CommandResult result = runCommand(command);
        if (result.exitCode != 0) {
            System.out.println(result.output);
            return new VideoInfo();
        }
        return VideoInfo.parse(result.output);
    }

    private static boolean runFFmpeg(List<String> command) throws IOException, InterruptedException {
        CommandResult result = runCommand(command);
        if (result.exitCode != 0) {
            System.out.println(result.output);
            return false;
        }
        return true;
    }

    private static CommandResult runCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        int exitCode = process.waitFor();
        return new CommandResult(exitCode, output.toString());
    }

    private static void checkFFmpeg() {
        resolveFFmpegCommands();
        try {
            runCommand(Arrays.asList(FFMPEG_COMMAND, "-version"));
            runCommand(Arrays.asList(FFPROBE_COMMAND, "-version"));
            System.out.println("ffmpeg 命令: " + FFMPEG_COMMAND);
            System.out.println("ffprobe 命令: " + FFPROBE_COMMAND);
        } catch (Exception e) {
            throw new RuntimeException(
                    "找不到 ffmpeg 或 ffprobe。请安装 FFmpeg、加入 PATH、放到 ./ffmpeg/bin，或在 "
                            + FFMPEG_PATH_FILE,
                    e
            );
        }
    }

    private static void resolveFFmpegCommands() {
        File configFile = new File(FFMPEG_PATH_FILE);
        if (configFile.exists()) {
            try {
                String path = MyFIleUtil.getStrByTxtFile(FFMPEG_PATH_FILE).trim();
                if (!path.isEmpty()) {
                    applyFFmpegPath(path);
                    return;
                }
            } catch (Exception e) {
                throw new RuntimeException("invalid ffmpeg path config: " + FFMPEG_PATH_FILE, e);
            }
        }

        File localBin = new File("./ffmpeg/bin");
        File localFFmpeg = new File(localBin, windowsExeName("ffmpeg"));
        File localFFprobe = new File(localBin, windowsExeName("ffprobe"));
        if (localFFmpeg.exists() && localFFprobe.exists()) {
            FFMPEG_COMMAND = localFFmpeg.getAbsolutePath();
            FFPROBE_COMMAND = localFFprobe.getAbsolutePath();
        }
    }

    private static void applyFFmpegPath(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            FFMPEG_COMMAND = new File(file, windowsExeName("ffmpeg")).getAbsolutePath();
            FFPROBE_COMMAND = new File(file, windowsExeName("ffprobe")).getAbsolutePath();
            return;
        }
        if (file.getName().toLowerCase().startsWith("ffmpeg")) {
            FFMPEG_COMMAND = file.getAbsolutePath();
            FFPROBE_COMMAND = new File(file.getParentFile(), windowsExeName("ffprobe")).getAbsolutePath();
            return;
        }
        throw new RuntimeException("ffmpegPath.txt 应该填写 FFmpeg 的 bin 目录或 ffmpeg 可执行文件路径");
    }

    private static String windowsExeName(String command) {
        return System.getProperty("os.name").toLowerCase().contains("win") ? command + ".exe" : command;
    }

    private static boolean needsTargetEncode(File output) {
        return output == null || output.length() > TARGET_MAX_SIZE;
    }

    private static boolean isUsefulOutput(File source, File output) {
        return output.exists() && output.length() > 0 && output.length() < source.length();
    }

    private static boolean isVideo(File file) {
        String name = file.getName().toLowerCase();
        for (String extension : VIDEO_EXTENSIONS) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private static File buildTempFile(File source, String tag) {
        String name = source.getName();
        int dotIndex = name.lastIndexOf('.');
        String baseName = dotIndex > 0 ? name.substring(0, dotIndex) : name;
        return new File(source.getParentFile(), baseName + ".videozip." + tag + ".tmp.mp4");
    }

    private static void deleteIfExists(File file) throws IOException {
        Files.deleteIfExists(file.toPath());
    }

    private static void deleteQuietly(File file) {
        try {
            deleteIfExists(file);
        } catch (Exception ignored) {
        }
    }

    private static File buildOutputFile(File source) {
        String name = source.getName();
        int dotIndex = name.lastIndexOf('.');
        String baseName = dotIndex > 0 ? name.substring(0, dotIndex) : name;
        String extension = dotIndex > 0 ? name.substring(dotIndex) : ".mp4";
        File relativeParent = getRelativeParent(source);
        return new File(relativeParent, baseName + ".compressed" + extension);
    }

    private static File getRelativeParent(File source) {
        try {
            File parent = source.getParentFile();
            String rootPath = ROOT.getCanonicalPath();
            String parentPath = parent.getCanonicalPath();
            if (parentPath.equals(rootPath)) {
                return OUTPUT_ROOT;
            }
            if (parentPath.startsWith(rootPath + File.separator)) {
                String relativePath = parentPath.substring(rootPath.length() + 1);
                return new File(OUTPUT_ROOT, relativePath);
            }
        } catch (IOException ignored) {
        }
        return OUTPUT_ROOT;
    }

    private static void ensureParentDir(File file) {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new RuntimeException("创建输出目录失败: " + parent);
        }
    }

    static class VideoInfo {
        int width;
        int height;
        double durationSeconds;

        static VideoInfo parse(String output) {
            var videoInfo = new VideoInfo();
            String[] lines = output.split("\\R");
            for (String line : lines) {
                String[] parts = line.split("=", 2);
                if (parts.length != 2) {
                    continue;
                }
                if ("width".equals(parts[0])) {
                    videoInfo.width = parseInt(parts[1]);
                } else if ("height".equals(parts[0])) {
                    videoInfo.height = parseInt(parts[1]);
                } else if ("duration".equals(parts[0])) {
                    videoInfo.durationSeconds = parseDouble(parts[1]);
                }
            }
            return videoInfo;
        }

        boolean isValid() {
            return width > 0 && height > 0 && durationSeconds > 0;
        }

        private static int parseInt(String value) {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                return 0;
            }
        }

        private static double parseDouble(String value) {
            try {
                return Double.parseDouble(value);
            } catch (Exception e) {
                return 0;
            }
        }
    }

    static class VideoTask {
        File file;
        VideoInfo videoInfo;

        VideoTask(File file, VideoInfo videoInfo) {
            this.file = file;
            this.videoInfo = videoInfo;
        }
    }

    static class CommandResult {
        int exitCode;
        String output;

        CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}
