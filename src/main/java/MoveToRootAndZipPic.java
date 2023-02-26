import java.io.File;
import java.io.IOException;

import static utils.MyFIleUtil.getStrByTxtFile;

public class MoveToRootAndZipPic {

    public static final String SOURCE_DIRECTION = "./sourceDirection.txt";
    static File root;
    public static void main(String[] args) throws Exception {
	// write your code here
        //1.read direction from txt
        var directionStr = getStrByTxtFile(SOURCE_DIRECTION);
        //todo: add sub folder to zippic task
        root=new File(directionStr);

        boolean isMoveToRoot = false;
        boolean isZipPic = false;
        moveToRootAndZipPic(isMoveToRoot, isZipPic);
    }

    private static void moveToRootAndZipPic(boolean isMoveToRoot, boolean isZipPic) throws IOException {
        if (isMoveToRoot){
            MoveToRoot.ROOT =root;
            MoveToRoot.moveOrNext(root);
        }
        if (isZipPic) {
            ZipPic.ROOT=root;
            ZipPic.zipPic(root);
        }

    }


}
