package com.infopanel.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class InfoPanelConfig {

    public enum Position { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;

    public static int bgAlpha           = 120;
    public static float scale           = 1.0f;
    public static Position position     = Position.TOP_LEFT;

    // Позиция панели при перетаскивании (-1 = не задана, используется position)
    public static int panelX = -1;
    public static int panelY = -1;

    // Позиция компаса (-1 = дефолт — по центру сверху)
    public static int compassX = -1;
    public static int compassY = -1;
    public static int durabilityX = -1;
    public static int durabilityY = -1;

    // Глобальная видимость HUD — переключается клавишей H (TOGGLE_HUD)
    public static boolean hudVisible = true;

    public static boolean showCompassBar = true; // полоса-компас (CompassRenderer)
    public static boolean showStructures      = false;
    public static boolean showStructVillage   = true;
    public static boolean showStructOutpost   = true;
    public static boolean showStructMonument  = true;
    public static boolean showStructStronghold = true;
    public static boolean showStructDesert    = true;
    public static boolean showStructFortress  = true;
    public static boolean showStructBastion   = true;

    public static boolean showCoords       = true;
    public static boolean showDirection    = true;
    public static boolean showBiome        = true;
    public static boolean showLight        = true;
    public static boolean showLightOverlay = false;
    public static boolean showFps         = true;
    public static boolean showPing        = true;
    public static boolean showTps         = true;
    public static boolean showTime        = true;
    public static boolean showSession     = true;
    public enum WailaPosition { TOP_CENTER, BOTTOM_CENTER, TOP_LEFT, TOP_RIGHT }

    public static boolean showTargetBlock  = true;
    public static boolean showEffectTimers = true;
    public static boolean showPlayers      = true;
    public static boolean showDurability   = true;
    public static boolean showSlimeChunks  = false;
    public static int wailaY              = 6;
    public static WailaPosition wailaPosition = WailaPosition.TOP_CENTER;
    public static int wailaBgAlpha        = 170;

    public static int colorCoords     = 0xFFFFFF;
    public static int colorDirection  = 0x55FF55;
    public static int colorBiome      = 0x55FFFF;
    public static int colorLightSafe  = 0x55FF55;
    public static int colorLightWarn  = 0xFF5555;
    public static int colorFps        = 0xFFFF55;
    public static int colorPing       = 0xFF9900;
    public static int colorTps        = 0x55FF55;
    public static int colorTime       = 0xAAAAAA;
    public static int colorSession    = 0xAAAAAA;
    public static int colorTargetBlock  = 0xFF55FF;
    public static int colorPlayers     = 0x55FFFF;
    public static int colorDurability  = 0x55FF55;
    public static int colorDurabilityWarn = 0xFF5555;

    public static void init() {
        load();
    }

    private static File getConfigFile() {
        if (configFile == null) {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && mc.gameDirectory != null)
                    configFile = new File(mc.gameDirectory, "config/infopanel.json");
            } catch (Exception ignored) {}
        }
        return configFile;
    }

    public static void load() {
        try {
            File f = getConfigFile();
            if (f != null && f.exists()) {
                FileReader reader = new FileReader(f);
                Data data = GSON.fromJson(reader, Data.class);
                reader.close();
                if (data != null) {
                    bgAlpha          = data.bgAlpha;
                    scale            = data.scale;
                    showCoords       = data.showCoords;
                    showDirection    = data.showDirection;
                    showBiome        = data.showBiome;
                    showLight        = data.showLight;
                    showLightOverlay = data.showLightOverlay;
                    showFps          = data.showFps;
                    showPing         = data.showPing;
                    showTps          = data.showTps;
                    showTime         = data.showTime;
                    showSession      = data.showSession;
                    showTargetBlock  = data.showTargetBlock;
                    showEffectTimers = data.showEffectTimers;
                    showPlayers      = data.showPlayers;
                    showDurability   = data.showDurability;
                    showSlimeChunks  = data.showSlimeChunks;
                    wailaY           = data.wailaY;
                    wailaBgAlpha     = data.wailaBgAlpha;
                    try { wailaPosition = WailaPosition.valueOf(data.wailaPosition); } catch (Exception ignored) {}
                    colorCoords      = data.colorCoords;
                    colorDirection   = data.colorDirection;
                    colorBiome       = data.colorBiome;
                    colorLightSafe   = data.colorLightSafe;
                    colorLightWarn   = data.colorLightWarn;
                    colorFps         = data.colorFps;
                    colorPing        = data.colorPing;
                    colorTps         = data.colorTps;
                    colorTime        = data.colorTime;
                    colorSession     = data.colorSession;
                    colorTargetBlock = data.colorTargetBlock;
                    colorPlayers     = data.colorPlayers;
                    colorDurability  = data.colorDurability;
                    colorDurabilityWarn = data.colorDurabilityWarn;
                    try { position = Position.valueOf(data.position); } catch (Exception ignored) {}
                    panelX   = data.panelX;
                    panelY   = data.panelY;
                    compassX = data.compassX;
                    compassY = data.compassY;
                    durabilityX = data.durabilityX;
                    durabilityY = data.durabilityY;
                    showCompassBar = data.showCompassBar;
                    showStructures     = data.showStructures;
                    showStructVillage  = data.showStructVillage;
                    showStructOutpost  = data.showStructOutpost;
                    showStructMonument = data.showStructMonument;
                    showStructStronghold = data.showStructStronghold;
                    showStructDesert   = data.showStructDesert;
                    showStructFortress = data.showStructFortress;
                    showStructBastion  = data.showStructBastion;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void save() {
        try {
            File f = getConfigFile();
            if (f == null) return;
            f.getParentFile().mkdirs();
            Data data = new Data();
            data.bgAlpha         = bgAlpha;
            data.scale           = scale;
            data.position        = position.name();
            data.panelX          = panelX;
            data.panelY          = panelY;
            data.compassX        = compassX;
            data.compassY        = compassY;
            data.durabilityX     = durabilityX;
            data.durabilityY     = durabilityY;
            data.showCompassBar  = showCompassBar;
            data.showStructures      = showStructures;
            data.showStructVillage   = showStructVillage;
            data.showStructOutpost   = showStructOutpost;
            data.showStructMonument  = showStructMonument;
            data.showStructStronghold = showStructStronghold;
            data.showStructDesert    = showStructDesert;
            data.showStructFortress  = showStructFortress;
            data.showStructBastion   = showStructBastion;
            data.showCoords      = showCoords;
            data.showDirection   = showDirection;
            data.showBiome       = showBiome;
            data.showLight       = showLight;
            data.showLightOverlay = showLightOverlay;
            data.showFps         = showFps;
            data.showPing        = showPing;
            data.showTps         = showTps;
            data.showTime        = showTime;
            data.showSession     = showSession;
            data.showTargetBlock = showTargetBlock;
            data.showEffectTimers = showEffectTimers;
            data.showPlayers     = showPlayers;
            data.showDurability  = showDurability;
            data.showSlimeChunks = showSlimeChunks;
            data.wailaY          = wailaY;
            data.wailaBgAlpha    = wailaBgAlpha;
            data.wailaPosition   = wailaPosition.name();
            data.colorCoords     = colorCoords;
            data.colorDirection  = colorDirection;
            data.colorBiome      = colorBiome;
            data.colorLightSafe  = colorLightSafe;
            data.colorLightWarn  = colorLightWarn;
            data.colorFps        = colorFps;
            data.colorPing       = colorPing;
            data.colorTps        = colorTps;
            data.colorTime       = colorTime;
            data.colorSession    = colorSession;
            data.colorTargetBlock = colorTargetBlock;
            data.colorPlayers    = colorPlayers;
            data.colorDurability = colorDurability;
            data.colorDurabilityWarn = colorDurabilityWarn;
            FileWriter writer = new FileWriter(f);
            GSON.toJson(data, writer);
            writer.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static int getBgAlpha()              { return bgAlpha; }
    public static void setBgAlpha(int v)        { bgAlpha = Math.max(0, Math.min(255, v)); save(); }
    public static float getScale()              { return scale; }
    public static void setScale(float v)        { scale = Math.max(0.5f, Math.min(2.0f, v)); save(); }
    public static Position getPosition()        { return position; }
    public static void setPosition(Position v)  { position = v; save(); }
    public static int getPanelX()               { return panelX; }
    public static int getPanelY()               { return panelY; }
    public static void setPanelPos(int x, int y){ panelX = x; panelY = y; save(); }
    public static boolean isShowCompassBar()          { return showCompassBar; }
    public static void setShowCompassBar(boolean v)   { showCompassBar = v; save(); }
    public static boolean isShowStructures()          { return showStructures; }
    public static void setShowStructures(boolean v)   { showStructures = v; save(); }
    public static boolean isShowStructVillage()       { return showStructVillage; }
    public static void setShowStructVillage(boolean v){ showStructVillage = v; save(); }
    public static boolean isShowStructOutpost()       { return showStructOutpost; }
    public static void setShowStructOutpost(boolean v){ showStructOutpost = v; save(); }
    public static boolean isShowStructMonument()      { return showStructMonument; }
    public static void setShowStructMonument(boolean v){ showStructMonument = v; save(); }
    public static boolean isShowStructStronghold()    { return showStructStronghold; }
    public static void setShowStructStronghold(boolean v){ showStructStronghold = v; save(); }
    public static boolean isShowStructDesert()        { return showStructDesert; }
    public static void setShowStructDesert(boolean v) { showStructDesert = v; save(); }
    public static boolean isShowStructFortress()      { return showStructFortress; }
    public static void setShowStructFortress(boolean v){ showStructFortress = v; save(); }
    public static boolean isShowStructBastion()       { return showStructBastion; }
    public static void setShowStructBastion(boolean v){ showStructBastion = v; save(); }

    public static int getCompassX()             { return compassX; }
    public static int getCompassY()             { return compassY; }
    public static void setCompassPos(int x, int y) { compassX = x; compassY = y; save(); }
    public static int getDurabilityX()          { return durabilityX; }
    public static int getDurabilityY()          { return durabilityY; }

    public static boolean isShowCoords()           { return showCoords; }
    public static void setShowCoords(boolean v)    { showCoords = v; save(); }
    public static boolean isShowDirection()        { return showDirection; }
    public static void setShowDirection(boolean v) { showDirection = v; save(); }
    public static boolean isShowBiome()            { return showBiome; }
    public static void setShowBiome(boolean v)     { showBiome = v; save(); }
    public static boolean isShowLight()            { return showLight; }
    public static void setShowLight(boolean v)     { showLight = v; save(); }
    public static boolean isShowLightOverlay()        { return showLightOverlay; }
    public static void setShowLightOverlay(boolean v) { showLightOverlay = v; save(); }
    public static boolean isShowFps()              { return showFps; }
    public static void setShowFps(boolean v)       { showFps = v; save(); }
    public static boolean isShowPing()             { return showPing; }
    public static void setShowPing(boolean v)      { showPing = v; save(); }
    public static boolean isShowTps()              { return showTps; }
    public static void setShowTps(boolean v)       { showTps = v; save(); }
    public static boolean isShowTime()             { return showTime; }
    public static void setShowTime(boolean v)      { showTime = v; save(); }
    public static boolean isShowSession()          { return showSession; }
    public static void setShowSession(boolean v)   { showSession = v; save(); }
    public static boolean isShowTargetBlock()          { return showTargetBlock; }
    public static void setShowTargetBlock(boolean v)   { showTargetBlock = v; save(); }
    public static boolean isShowEffectTimers()       { return showEffectTimers; }
    public static void setShowEffectTimers(boolean v){ showEffectTimers = v; save(); }
    public static boolean isShowPlayers()          { return showPlayers; }
    public static boolean isShowDurability()       { return showDurability; }
    public static void setShowDurability(boolean v)  { showDurability = v; save(); }
    public static boolean isShowSlimeChunks()        { return showSlimeChunks; }
    public static void setShowSlimeChunks(boolean v) { showSlimeChunks = v; save(); }
    public static void setShowPlayers(boolean v)   { showPlayers = v; save(); }
    public static int getWailaY()                       { return wailaY; }
    public static void setWailaY(int v)                 { wailaY = v; save(); }
    public static WailaPosition getWailaPosition()      { return wailaPosition; }
    public static void setWailaPosition(WailaPosition v){ wailaPosition = v; save(); }
    public static int getWailaBgAlpha()                 { return wailaBgAlpha; }
    public static void setWailaBgAlpha(int v)           { wailaBgAlpha = Math.max(0, Math.min(255, v)); save(); }

    public static int getColorCoords()              { return colorCoords; }
    public static void setColorCoords(int v)        { colorCoords = v; save(); }
    public static int getColorDirection()           { return colorDirection; }
    public static void setColorDirection(int v)     { colorDirection = v; save(); }
    public static int getColorBiome()               { return colorBiome; }
    public static void setColorBiome(int v)         { colorBiome = v; save(); }
    public static int getColorLightSafe()           { return colorLightSafe; }
    public static void setColorLightSafe(int v)     { colorLightSafe = v; save(); }
    public static int getColorLightWarn()           { return colorLightWarn; }
    public static void setColorLightWarn(int v)     { colorLightWarn = v; save(); }

    private static class Data {
        int bgAlpha = 120; float scale = 1.0f; String position = "TOP_LEFT";
        int panelX = -1; int panelY = -1;
        int compassX = -1; int compassY = -1;
        int durabilityX = -1; int durabilityY = -1;
        boolean showCompassBar = true;
        boolean showStructures = false;
        boolean showStructVillage = true, showStructOutpost = true, showStructMonument = true;
        boolean showStructStronghold = true, showStructDesert = true;
        boolean showStructFortress = true, showStructBastion = true;
        boolean showCoords = true, showDirection = true, showBiome = true;
        boolean showLight = true, showLightOverlay = false;
        boolean showFps = true, showPing = true, showTps = true, showTime = true, showSession = true;
        boolean showTargetBlock = true, showPlayers = true, showEffectTimers = true;
        boolean showDurability = true, showSlimeChunks = false;
        int wailaY = 6, wailaBgAlpha = 170;
        String wailaPosition = "TOP_CENTER";
        int colorCoords = 0xFFFFFF, colorDirection = 0x55FF55, colorBiome = 0x55FFFF;
        int colorLightSafe = 0x55FF55, colorLightWarn = 0xFF5555;
        int colorFps = 0xFFFF55, colorPing = 0xFF9900, colorTps = 0x55FF55, colorTime = 0xAAAAAA, colorSession = 0xAAAAAA;
        int colorTargetBlock = 0xFFFFFF, colorPlayers = 0x55FFFF;
        int colorDurability = 0x55FF55, colorDurabilityWarn = 0xFF5555;
    }
}
