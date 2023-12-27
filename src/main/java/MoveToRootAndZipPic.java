import org.apache.commons.io.FileUtils;
import utils.ByteConvertUtils;

import java.io.File;
import java.io.IOException;

import static utils.MyFIleUtil.getStrByTxtFile;

public class MoveToRootAndZipPic {

    public static final String SOURCE_DIRECTION = "./sourceDirection.txt";
    static File ROOT;
    public static void main(String[] args) throws Exception {
	// write your code here
        //1.read direction from txt
        var directionStr = getStrByTxtFile(SOURCE_DIRECTION);
        //todo: add sub folder to zippic task
        ROOT =new File(directionStr);

        boolean isMoveToRoot = true;
        boolean isZipPic = true;
        boolean isZipSubDir=false;
//        int floorMax=2;
//        MoveToRoot.FLOOR_MAX=floorMax;
        ZipPic.IS_ZIP_SUB_DIR=isZipSubDir;

        //TODO :add FileSize Comparison before and after
        var sizeBefore =  FileUtils.sizeOfDirectory(ROOT);


        try {
            moveToRootAndZipPic(isMoveToRoot, isZipPic);
        } finally {
            //size gotten is a little inaccurate,according to the sleeping time
            Thread.sleep(1000);
            var sizeAfter = FileUtils.sizeOfDirectory(ROOT);
            //todo: change all sout to log.info--
            System.out.println(
                    "sizeBefore:"+ ByteConvertUtils.bytesToMbStr(sizeBefore) +
                            "\nsizeAfter:"+ ByteConvertUtils.bytesToMbStr(sizeAfter)+
                            "\nreduced:"+ByteConvertUtils.bytesToMbStr(sizeBefore-sizeAfter));
        }


    }

    private static void moveToRootAndZipPic(boolean isMoveToRoot, boolean isZipPic) throws Exception {
        if (isMoveToRoot){
            MoveToRoot.ROOT = ROOT;
            MoveToRoot.moveOrNext();
        }

        try {
            if (isZipPic) {

                ZipPic.ROOT= ROOT;
                ZipPic.zipPic();
            }
        } finally {
            ZipPic.THREAD_POOL.destroy();
            System.out.println("t is gonna destroy");
        }

    }


}
