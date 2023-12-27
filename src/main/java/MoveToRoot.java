import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class MoveToRoot {

    static Set<String> parentSet= new HashSet<String>();
    static File ROOT;
    static int FLOOR_MAX=100;
    public static void moveOrNext() {
	    // write your code here
        System.out.println(ROOT);
        moveOrNextInDir(ROOT, 0);
    }




    public static void moveOrNextInDir(File directionOrFile, int floor){
        if (directionOrFile.isFile()){

            File parentFile = directionOrFile.getParentFile();
            //return if file in root
            if (parentFile.equals(ROOT)){
                return;
            }
            String parentName = parentFile.getName();
            parentName = getParentNameIfDuplicate(parentFile, parentName);
            String newFilename = parentName + directionOrFile.getName();

//            //replace parent folderName duplicate
//            newFilename = newFilename.replaceAll("[" + parentName + "]+", "");
            //todo replace duplicate String in fileNames
            String pathname = checkAndBuildFileName(parentFile, newFilename);
            System.out.println("source: "+directionOrFile);
            System.out.println("des: "+pathname);
//            //move file
            if (!parentName.equals(ROOT.getName())){
                tryMoveFile(directionOrFile,pathname,parentFile,newFilename);
            }

        }else {
            if (floor<FLOOR_MAX){
                floor++;
                File[] secondFolders = directionOrFile.listFiles();
                for (File sndFolder : secondFolders) {
                    moveOrNextInDir(sndFolder, floor);
                }
            }else {
                String pathname = ROOT + File.separator + directionOrFile.getName();
                System.out.println("source: "+directionOrFile);
                System.out.println("des: "+pathname);
                //move dir
                System.out.println("moving dir");
                System.out.println(directionOrFile.renameTo(new File(pathname)));

                System.out.println("no!!!");
                floor=FLOOR_MAX-1;
            }

        }
    }

    private static String checkAndBuildFileName(File parentFile, String newFilename) {
        newFilename = getNewFilename(parentFile, newFilename);
        String pathname = ROOT + File.separator + newFilename;
        return pathname;
    }

    private static String getNewFilename(File parentFile, String newFilename) {
        if (parentSet.contains(newFilename)){
            newFilename = parentFile.getParentFile().getName()+"_"+ newFilename;
        }else {
            parentSet.add(newFilename);
        }
        return newFilename;
    }

    private static void tryMoveFile(File directionOrFile, String pathname, File parentFile, String newFilename) {
        boolean moveResult = directionOrFile.renameTo(new File(pathname));
        System.out.println(moveResult);
        if (!moveResult){
            //build new name with further parent
            newFilename = getNewFilename(parentFile, newFilename);
            pathname = checkAndBuildFileName(parentFile, newFilename);
            System.out.println();
            tryMoveFile(directionOrFile, pathname, parentFile,newFilename);
        }

    }

    private static String getParentNameIfDuplicate(File parentFile, String parentName) {
        String superParentName = parentFile.getParentFile().getName();
        if (parentName.matches("[0-9]*")||parentName.length()==1){
            parentName = getParentNameIfDuplicate(parentFile.getParentFile(),superParentName)+parentName;
        }
        return parentName;
    }



}
