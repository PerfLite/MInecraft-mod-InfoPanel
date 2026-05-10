package com.infopanel.event;

public class Lang {
    public static boolean isRussian() {
        try {
            String lang = net.minecraft.client.Minecraft.getInstance().options.languageCode;
            return lang != null && lang.startsWith("ru");
        } catch (Exception e) {
            return false;
        }
    }
}
