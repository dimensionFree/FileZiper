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
        //TODO :add FileSize Comparison before and after
        var sizeBefore =  FileUtils.sizeOfDirectory(ROOT);

        moveToRootAndZipPic(isMoveToRoot, isZipPic);

        Thread.sleep(1000);
        var sizeAfter = FileUtils.sizeOfDirectory(ROOT);
        //todo: change all sout to log.info--
        System.out.println(
                "sizeBefore:"+ ByteConvertUtils.bytesToMbStr(sizeBefore) +
                        "\nsizeAfter:"+ ByteConvertUtils.bytesToMbStr(sizeAfter)+
                        "\nreduced:"+ByteConvertUtils.bytesToMbStr(sizeBefore-sizeAfter));

    }

    private static void moveToRootAndZipPic(boolean isMoveToRoot, boolean isZipPic) throws IOException {
        if (isMoveToRoot){
            MoveToRoot.ROOT = ROOT;
            MoveToRoot.moveOrNext(ROOT);
        }
        if (isZipPic) {
            ZipPic.ROOT= ROOT;
            ZipPic.zipPic(ROOT);
        }

    }


}
