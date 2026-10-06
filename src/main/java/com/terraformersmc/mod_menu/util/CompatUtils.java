package com.terraformersmc.mod_menu.util;

import net.minecraftforge.fml.common.Loader;

public class CompatUtils {
    public static boolean isCustomMenu() {
        return Loader.isModLoaded("custommainmenu");
    }
}
