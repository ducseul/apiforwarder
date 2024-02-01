package com.ducseul.apiforwarder.utils;

import java.io.*;
import java.util.Base64;
public class SerializableUtils  {
    public static String serializeToString(Serializable object) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(object);
            objectOutputStream.flush();

            byte[] serializedBytes = byteArrayOutputStream.toByteArray();
            return Base64.getEncoder().encodeToString(serializedBytes);
        } catch (Exception exception){
            return "";
        }
    }

    public static Object deserializeFromString(String serializedString) throws IOException, ClassNotFoundException {
        byte[] serializedBytes = Base64.getDecoder().decode(serializedString);
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedBytes);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {

            return objectInputStream.readObject();
        }
    }
}
