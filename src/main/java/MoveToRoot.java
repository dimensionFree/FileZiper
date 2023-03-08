import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class MoveToRoot {

    static Set<String> parentSet= new HashSet<String>();
    static File ROOT;
    public static void main(String[] args) throws Exception {
	    // write your code here
        System.out.println(ROOT);
        moveOrNext(ROOT);
    }




    public static void moveOrNext(File directionOrFile){
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
            if (parentSet.contains(newFilename)){
                newFilename=parentFile.getParentFile().getName()+"_"+newFilename;
            }else {
                parentSet.add(newFilename);
            }
            String pathname = ROOT + File.separator + newFilename;
            System.out.println("source: "+directionOrFile);
            System.out.println("des: "+pathname);
//            //move file
            if (!parentName.equals(ROOT.getName())){
                System.out.println(directionOrFile.renameTo(new File(pathname)));
            }

        }else {
            File[] secondFolders = directionOrFile.listFiles();
            for (File sndFolder : secondFolders) {
                moveOrNext(sndFolder);
            }
            System.out.println("no!!!");
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
