package com.ducseul.apiforwarder.utils;

import java.io.*;
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

    public static byte[] readStreamBytes(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        }
    }
}
