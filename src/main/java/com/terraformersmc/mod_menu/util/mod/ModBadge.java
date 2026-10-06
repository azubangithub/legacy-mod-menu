package com.terraformersmc.mod_menu.util.mod;

import com.terraformersmc.mod_menu.ModMenu;
import net.minecraft.client.resources.I18n;

import java.util.*;

public class ModBadge {
    private final String translationKey;
    private final int fillColor;
    private final int outlineColor;
    private final int textColor;

    public static final ModBadge LIBRARY = new ModBadge("modmenu.badge.library", 0xff107454, 0xff093929);
    public static final ModBadge CLIENT = new ModBadge("modmenu.badge.clientsideOnly", 0xff2b4b7c, 0xff0e2a55);
    public static final ModBadge DEPRECATED = new ModBadge("modmenu.badge.deprecated", 0xff841426, 0xff530C17);
    public static final ModBadge FORGE = new ModBadge("modmenu.badge.forge", 0xff3c4d63, 0xff26303d);
    public static final ModBadge LITELOADER = new ModBadge("modmenu.badge.liteloader", 0xff5a72a0, 0xff384869);
    public static final ModBadge CLEANROOM = new ModBadge("modmenu.badge.cleanroom", 0xff2d7d46, 0xff184a28);
    public static final ModBadge MODPACK = new ModBadge("modmenu.badge.modpack", 0xff7a2b7c, 0xff510d54);
    public static final ModBadge MINECRAFT = new ModBadge("modmenu.badge.minecraft", 0xff6f6c6a, 0xff31302f);

    public static final Map<String, ModBadge> DEFAULT_BADGES = new LinkedHashMap<String, ModBadge>();
    static {
        DEFAULT_BADGES.put("library", LIBRARY);
        DEFAULT_BADGES.put("client", CLIENT);
        DEFAULT_BADGES.put("deprecated", DEPRECATED);
        DEFAULT_BADGES.put("forge", FORGE);
        DEFAULT_BADGES.put("liteloader", LITELOADER);
        DEFAULT_BADGES.put("cleanroom", CLEANROOM);
        DEFAULT_BADGES.put("modpack", MODPACK);
        DEFAULT_BADGES.put("minecraft", MINECRAFT);
    }

    public static final Map<String, ModBadge> CUSTOM_BADGES = new LinkedHashMap<String, ModBadge>();
    public static final List<Map<String, ModBadge>> BADGES = Arrays.asList(DEFAULT_BADGES, CUSTOM_BADGES);

    public ModBadge(String translationKey, int outlineColor, int fillColor) {
        this(translationKey, outlineColor, fillColor, 0xCACACA);
    }

    public ModBadge(String translationKey, int outlineColor, int fillColor, int textColor) {
        this.translationKey = translationKey;
        this.fillColor = fillColor;
        this.outlineColor = outlineColor;
        this.textColor = textColor;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public String getDisplayName() {
        return I18n.format(this.translationKey);
    }

    public int getOutlineColor() {
        return this.outlineColor;
    }

    public int getFillColor() {
        return this.fillColor;
    }

    public int getTextColor() {
        return this.textColor;
    }

    public static Set<ModBadge> convert(Set<String> badgeKeys, String modId) {
        Set<ModBadge> result = new LinkedHashSet<ModBadge>();
        if (badgeKeys == null) {
            return result;
        }
        for (String key : badgeKeys) {
            if (DEFAULT_BADGES.containsKey(key)) {
                result.add(DEFAULT_BADGES.get(key));
            } else if (CUSTOM_BADGES.containsKey(key)) {
                result.add(CUSTOM_BADGES.get(key));
            } else {
                ModMenu.LOGGER.warn("Skipping unknown badge key '{}' specified by mod '{}'", key, modId);
            }
        }
        return result;
    }
}
