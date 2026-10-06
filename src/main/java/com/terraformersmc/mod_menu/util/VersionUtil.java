package com.terraformersmc.mod_menu.util;

import java.util.Arrays;
import java.util.List;

public final class VersionUtil {
    private static final List<String> PREFIXES = Arrays.asList("version", "ver", "v");

    private VersionUtil() {
    }

    public static String stripPrefix(String version) {
        if (version == null) return "";
        version = version.trim();

        for (String prefix : PREFIXES) {
            if (version.startsWith(prefix)) {
                return version.substring(prefix.length());
            }
        }

        return version;
    }

    public static String getPrefixedVersion(String version) {
        if (version == null) return "";
        return "v" + stripPrefix(version);
    }

    public static String removeBuildMetadata(String version) {
        if (version == null) return "";
        return version.split("\\+")[0];
    }
}
