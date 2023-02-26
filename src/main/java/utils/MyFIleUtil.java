package utils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class MyFIleUtil {
    public static String getStrByTxtFile(String fileName) throws Exception {
        //method1 buffered reader
//        BufferedReader in = new BufferedReader(new FileReader(fileName));
//        String str;
//        while ((str = in.readLine()) != null) {
//            System.out.println(str);
//        }
//        System.out.println(str);

        //method2
        Path path = Paths.get(fileName);
        byte[] bytes = Files.readAllBytes(path);
        List<String> directions = Files.readAllLines(path, StandardCharsets.UTF_8);
        var first = directions.stream().findFirst();
        if (!first.isPresent()){
            throw new Exception("invalid direction");
        }
        return first.get();

    }
}
