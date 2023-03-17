package utils;

import io.github.techgnious.IVCompressor;
import io.github.techgnious.dto.IVSize;
import io.github.techgnious.dto.ResizeResolution;
import io.github.techgnious.dto.VideoFormats;
import io.github.techgnious.exception.VideoException;

import java.io.File;
import java.io.IOException;

public class VideoCompressor {
    public static void main(String[] args) throws IOException, VideoException {
        IVCompressor compressor = new IVCompressor();
        IVSize customRes = new IVSize();
        customRes.setWidth(400);
        customRes.setHeight(300);
        File file = new File("D:/Testing/20.mp4");
        compressor.reduceVideoSizeAndSaveToAPath(file, VideoFormats.MP4, ResizeResolution.R480P, "D:/Testing/Custome");
    }
}

