package main.java.data;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FileHandler {

    public static void serialize(String file, Object... obs) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
            for (Object o : obs)
                oos.writeObject(o);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> readLines(String pathToFile) {
        try {
            return Files.readAllLines(Paths.get(pathToFile));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object[] deserialize(String file, int objects) {
        Object[] obs = new Object[objects];
        try {
            ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)));
            for (int i = 0; i < objects; i++)
                obs[i] = ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return obs;
    }
}
