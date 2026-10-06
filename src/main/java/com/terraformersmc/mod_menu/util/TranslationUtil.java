package com.terraformersmc.mod_menu.util;

import com.terraformersmc.mod_menu.ModMenu;
import net.minecraft.client.resources.I18n;

import java.text.NumberFormat;
import java.util.Arrays;

public class TranslationUtil {
    public static String translateNumeric(String key, int[]... args) {
        Object[] realArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            NumberFormat nf = NumberFormat.getInstance();
            if (args[i].length == 1) {
                realArgs[i] = nf.format(args[i][0]);
            } else {
                realArgs[i] = nf.format(args[i][0]) + "/" + nf.format(args[i][1]);
            }
        }

        int[] override = new int[args.length];
        Arrays.fill(override, -1);
        for (int i = 0; i < args.length; i++) {
            int[] arg = args[i];
            if (arg == null) {
                throw new NullPointerException("args[" + i + "]");
            }
            if (arg.length == 1) {
                override[i] = arg[0];
            }
        }

        String lastKey = key;
        for (int flags = (1 << args.length) - 1; flags >= 0; flags--) {
            StringBuilder fullKey = new StringBuilder(key);
            for (int i = 0; i < args.length; i++) {
                fullKey.append('.');
                if (((flags & (1 << i)) != 0) && override[i] != -1) {
                    fullKey.append(override[i]);
                } else {
                    fullKey.append('n');
                }
            }
            lastKey = fullKey.toString();
            if (I18n.hasKey(lastKey)) {
                return I18n.format(lastKey, realArgs);
            }
        }
        return I18n.format(lastKey, realArgs);
    }

    public static String translationKeyOf(String type, String id) {
        return type + "." + ModMenu.MOD_ID + "." + id;
    }
}
