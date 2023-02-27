package utils;

public class ByteConvertUtils {


    public static long mbToBytes(long mb) {
        return mb * 1024 * 1024;
    }
    public static float bytesToMb(long bytes) {
        return (float) (bytes / 1024 / 1024.0);
    }

    public static String bytesToMbStr(long bytes){
        return bytesToMb(bytes)+"Mbs";
    }

}
