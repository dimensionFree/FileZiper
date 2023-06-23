import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.collections4.CollectionUtils;
import utils.ThreadPoolImpl;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class ZipPic {

    public static final int ZIP_PIC_MIN_SIZE = 2097152;
    public static final ThreadPoolImpl THREAD_POOL = new ThreadPoolImpl(7);
    public static boolean IS_ZIP_SUB_DIR = false;
    static Set<String> parentSet = new HashSet<String>();
    static File ROOT;
    static float SCREEN_WIDTH = 1920;
    static float SCREEN_HEIGHT = 1080;
    public static String ZIP_PATH = ROOT + File.separator + "zip";

    public static void zipPic(){
        // write your code here
        try {
            zipPicInDir(ROOT);
        } finally {
            THREAD_POOL.destroy();
            System.out.println("t is gonna destroy");
        }
    }


    public static void zipPicInDir(File direction)  {

        var files = direction.listFiles();
        var readOnlyFileSet=new HashSet<String>();
        try {

            for (File file : files) {
                if (!file.canWrite()){
                    //set writable
                    file.setWritable(true);
                }
                String fileName = file.getName();
                String lowerCase = fileName.toLowerCase();
                if (file.isFile()) {
                    if (lowerCase.endsWith(".png") || lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg") || lowerCase.endsWith(".bmp")) {
                        //2m
                        if (file.length() > ZIP_PIC_MIN_SIZE) {
                            THREAD_POOL.execute(new zipFileClass(file));
                        }
                    }
                } else if (IS_ZIP_SUB_DIR && file.isDirectory()) {
                    zipPicInDir(file);
                }

            }
            System.out.println("t is:" + THREAD_POOL);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    static class zipFileClass implements Runnable {

        File file;
        float scale = 1.0f;

        float quality = 0.8f;
        private static volatile int i = 1;

        public zipFileClass() {
        }

        public zipFileClass(File file) throws IOException {
            this.file = file;
            BufferedImage bimg = ImageIO.read(file);
            int width = bimg.getWidth();
            int height = bimg.getHeight();
            scale = Math.min(SCREEN_WIDTH / width, SCREEN_HEIGHT / height) ;
            float minimumScale = 0.6f;
            scale = Math.max(scale, minimumScale);
            bimg.flush();

        }

        @Override
        public void run() {
//            String source = ROOT + File.separator + file.getName();
            String source = file.getPath();
            //save it to extra "zip" folder
//            String des = ZIP_PATH + File.separator + file.getName();
            String des = source;
            System.out.println("当前处理的线程:" + Thread.currentThread().getName() + " 执行任务" + (i++) + " 开始");

//            System.out.println("source: "+ source);
//            System.out.println("des: "+des);

            try {

                zipFile(source, des, scale, quality);
                System.out.println("当前处理的线程:" + Thread.currentThread().getName() + " 执行任务" + (i++) + " 完成");
            } catch (IOException e) {
                System.out.println("当前处理的线程:" + Thread.currentThread().getName() + " 执行任务" + (i++) + " 失败");
                throw new RuntimeException(e);
            }
        }
    }

    private static void zipFile(String source, String des, float scale, float quality) throws IOException {


//        System.out.println("source: "+ source);
//        System.out.println("des: "+des);

        Thumbnails.of(source)
                .scale(scale)
                .outputQuality(quality)
                .toFile(des);
    }
}
