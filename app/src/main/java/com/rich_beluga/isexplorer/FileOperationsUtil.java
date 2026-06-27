package com.rich_beluga.isexplorer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public final class FileOperationsUtil {

    private FileOperationsUtil() { /* тут нихуя нету */ }

    public static void copy(File source, File targetDir) throws IOException {
        File destination = new File(targetDir, source.getName());
        if (source.isDirectory()) {
            copyDirectory(source, destination);
        } else {
            copyFile(source, destination);
        }
    }

    private static void copyDirectory(File source, File dest) throws IOException {
        if (!dest.exists() && !dest.mkdirs()) {
            throw new IOException("Не удалось создать папку: " + dest.getAbsolutePath());
        }
        File[] children = source.listFiles();
        if (children == null) return;
        for (File child : children) {
            File childDest = new File(dest, child.getName());
            if (child.isDirectory()) {
                copyDirectory(child, childDest);
            } else {
                copyFile(child, childDest);
            }
        }
    }

    private static void copyFile(File source, File dest) throws IOException {
        try (
            FileInputStream  fis = new FileInputStream(source);
            FileOutputStream fos = new FileOutputStream(dest);
            FileChannel in  = fis.getChannel();
            FileChannel out = fos.getChannel()
        ) {
            in.transferTo(0, in.size(), out);
        }
    }

    public static void move(File source, File targetDir) throws IOException {
        File destination = new File(targetDir, source.getName());
        if (!source.renameTo(destination)) {
            copy(source, targetDir);
            deleteRecursively(source);
        }
    }

    public static void deleteRecursively(File target) throws IOException {
        if (target.isDirectory()) {
            File[] children = target.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (!target.delete()) {
            throw new IOException("Не удалось удалить: " + target.getAbsolutePath());
        }
    }
}
