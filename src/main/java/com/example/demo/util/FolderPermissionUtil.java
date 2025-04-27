package com.example.demo.util;

public class FolderPermissionUtil {

    public static boolean canRead(int chmod) {
        return (chmod & 0b001) > 0;
    }

    public static boolean canWrite(int chmod) {
        return (chmod & 0b010) > 0;
    }

    public static boolean canDelete(int chmod) {
        return (chmod & 0b100) > 0;
    }

    public static String toString(int chmod) {
        StringBuilder sb = new StringBuilder();
        if (canRead(chmod)) sb.append("r");
        if (canWrite(chmod)) sb.append("w");
        if (canDelete(chmod)) sb.append("d");
        return sb.toString();
    }
}