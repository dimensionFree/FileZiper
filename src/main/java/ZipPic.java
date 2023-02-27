import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.compress.utils.Lists;
import utils.ThreadPoolImpl;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ZipPic{

    public static final int ZIP_PIC_MIN_SIZE = 2097152;
    static Set<String> parentSet= new HashSet<String>();
    static File ROOT;
    static float SCREEN_WIDTH =1920;
    static float SCREEN_HEIGHT = 1080;
    public static String ZIP_PATH = ROOT + File.separator + "zip";

    public static void main(String[] args) throws IOException {
	// write your code here
        zipPic(ROOT);
    }


    public static void zipPic(File direction) throws IOException {
        ZIP_PATH = ROOT + File.separator + "zip";
        ThreadPoolImpl t=new ThreadPoolImpl(7);
        //“D:\zip”目录现在不存在,make dir "zip" direction
//        String dirStr = ROOT+File.separator+"zip";
//        File directory = new File(dirStr);
//
//        //mkdir
//        boolean hasSucceeded = directory.mkdir();
//        System.out.println("创建文件夹结果（不含父文件夹）：" + hasSucceeded);


        File[] files = direction.listFiles();

        try {

            for (final File file : files) {
                String fileName = file.getName();
                String lowerCase = fileName.toLowerCase();
                if (lowerCase.endsWith(".png") || lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg") || lowerCase.endsWith(".gif")|| lowerCase.endsWith(".bmp")) {
                    //2m
                    if (file.length()> ZIP_PIC_MIN_SIZE){
                        t.execute(new zipFileClass(file));
                    }
                }
            }
            System.out.println("t is:"+t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            t.destroy();
            System.out.println("t is gonna destroy");
        }


    }

    static class zipFileClass implements Runnable{

        File file;
        float scale=1.0f;

        float quality=0.75f;
        private static volatile int i = 1;

        public zipFileClass() {
        }

        public zipFileClass(File file) throws IOException {
            this.file = file;
            BufferedImage bimg = ImageIO.read(file);
            int width = bimg.getWidth();
            int height = bimg.getHeight();
            scale = Math.min(SCREEN_WIDTH / width,SCREEN_HEIGHT/height);
            scale=Math.max(scale,0.75f);
            bimg.flush();

        }

        @Override
        public void run() {
            String source = ROOT + File.separator+file.getName();
            //save it to extra "zip" folder
//            String des = ZIP_PATH + File.separator + file.getName();
            String des = source;
            System.out.println("当前处理的线程:" + Thread.currentThread().getName() + " 执行任务" + (i++) + " 开始");

//            System.out.println("source: "+ source);
//            System.out.println("des: "+des);

            try {

                zipFile(source,des,scale,quality);

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
