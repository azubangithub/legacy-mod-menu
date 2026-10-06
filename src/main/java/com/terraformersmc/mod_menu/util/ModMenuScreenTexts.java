package com.terraformersmc.mod_menu.util;

import com.terraformersmc.mod_menu.ModMenu;
import net.minecraft.client.resources.I18n;

import java.util.Locale;

public final class ModMenuScreenTexts {
    public static final String TITLE = "modmenu.title";
    public static final String LIBRARIES = "option.modmenu.show_libraries";
    public static final String SORTING = "option.modmenu.sorting";

    private ModMenuScreenTexts() {
    }

    public static String getLibrariesText() {
        return I18n.format(LIBRARIES) + ": " + I18n.format(LIBRARIES + "." + String.valueOf(ModMenu.getConfig().showLibraries).toLowerCase(Locale.ROOT));
    }

    public static String getSortingText() {
        return I18n.format(SORTING) + ": " + I18n.format(SORTING + "." + ModMenu.getConfig().sorting.name().toLowerCase(Locale.ROOT));
    }
}
