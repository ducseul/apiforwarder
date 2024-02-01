package com.ducseul.apiforwarder.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class FileUtils {
    public static String getFileContent(String filename) {
        StringBuilder fileContent = new StringBuilder();
        File myObj = new File(filename);
        Scanner myReader = null;
        try {
            myReader = new Scanner(myObj);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            fileContent.append(data);
        }
        myReader.close();
        return fileContent.toString();
    }
}
