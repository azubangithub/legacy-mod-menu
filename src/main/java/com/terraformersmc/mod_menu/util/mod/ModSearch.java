package com.terraformersmc.mod_menu.util.mod;

import com.terraformersmc.mod_menu.ModMenu;
import com.terraformersmc.mod_menu.gui.ModsScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;

public class ModSearch {

    public static boolean validSearchQuery(String query) {
        return query != null && !query.trim().isEmpty();
    }

    public static List<Mod> search(ModsScreen screen, String query, List<Mod> candidates) {
        if (!validSearchQuery(query)) {
            List<Mod> result = new ArrayList<Mod>();
            for (Mod child : candidates) {
                if (!child.isHidden() && (ModMenu.getConfig().showLibraries || !child.getBadges().contains(ModBadge.LIBRARY))) {
                    result.add(child);
                }
            }
            return result;
        }

        final String cleanQuery = query.toLowerCase(Locale.ROOT).trim();
        List<Tuple<Mod, Integer>> matches = new ArrayList<Tuple<Mod, Integer>>();

        for (Mod candidate : candidates) {
            int score = passesFilters(screen, candidate, cleanQuery);
            if (score > 0) {
                matches.add(new Tuple<Mod, Integer>(candidate, score));
            }
        }

        Collections.sort(matches, new Comparator<Tuple<Mod, Integer>>() {
            @Override
            public int compare(Tuple<Mod, Integer> a, Tuple<Mod, Integer> b) {
                return b.getSecond() - a.getSecond();
            }
        });

        List<Mod> sortedMods = new ArrayList<Mod>();
        for (Tuple<Mod, Integer> tuple : matches) {
            sortedMods.add(tuple.getFirst());
        }
        return sortedMods;
    }

    private static int passesFilters(ModsScreen screen, Mod mod, String query) {
        String modId = mod.getId();
        String modName = mod.getName();
        String modTranslatedName = mod.getTranslatedName();
        String modDescription = mod.getDescription();
        String modSummary = mod.getSummary();

        String library = I18n.hasKey("modmenu.searchTerms.library") ? I18n.format("modmenu.searchTerms.library") : "library";
        String modpack = I18n.hasKey("modmenu.searchTerms.modpack") ? I18n.format("modmenu.searchTerms.modpack") : "modpack";
        String deprecated = I18n.hasKey("modmenu.searchTerms.deprecated") ? I18n.format("modmenu.searchTerms.deprecated") : "deprecated";
        String clientside = I18n.hasKey("modmenu.searchTerms.clientside") ? I18n.format("modmenu.searchTerms.clientside") : "client";
        String forge = I18n.hasKey("modmenu.searchTerms.forge") ? I18n.format("modmenu.searchTerms.forge") : "forge";
        String liteloader = I18n.hasKey("modmenu.searchTerms.liteloader") ? I18n.format("modmenu.searchTerms.liteloader") : "liteloader";
        String cleanroom = I18n.hasKey("modmenu.searchTerms.cleanroom") ? I18n.format("modmenu.searchTerms.cleanroom") : "cleanroom";
        String configurable = I18n.hasKey("modmenu.searchTerms.configurable") ? I18n.format("modmenu.searchTerms.configurable") : "config";

        // Libraries are currently hidden, ignore them entirely
        if (mod.isHidden() || (!ModMenu.getConfig().showLibraries && mod.getBadges().contains(ModBadge.LIBRARY))) {
            return 0;
        }

        if (modName.toLowerCase(Locale.ROOT).contains(query)
                || modTranslatedName.toLowerCase(Locale.ROOT).contains(query)
                || modId.toLowerCase(Locale.ROOT).contains(query)) {
            return query.length() >= 3 ? 2 : 1;
        }

        boolean hasCustomBadge = false;
        for (Map.Entry<String, ModBadge> badgeEntry : ModBadge.CUSTOM_BADGES.entrySet()) {
            String searchTerms = badgeEntry.getValue().getDisplayName();
            String termKey = "modmenu.searchTerms." + badgeEntry.getKey();
            if (I18n.hasKey(termKey)) {
                searchTerms = I18n.format(termKey);
            }
            if (searchTerms.toLowerCase(Locale.ROOT).contains(query) && mod.getBadges().contains(badgeEntry.getValue())) {
                hasCustomBadge = true;
                break;
            }
        }

        if (modDescription.toLowerCase(Locale.ROOT).contains(query)
                || modSummary.toLowerCase(Locale.ROOT).contains(query)
                || authorMatches(mod, query)
                || (library.contains(query) && mod.getBadges().contains(ModBadge.LIBRARY))
                || (forge.contains(query) && mod.getBadges().contains(ModBadge.FORGE))
                || (liteloader.contains(query) && mod.getBadges().contains(ModBadge.LITELOADER))
                || (cleanroom.contains(query) && mod.getBadges().contains(ModBadge.CLEANROOM))
                || (modpack.contains(query) && mod.getBadges().contains(ModBadge.MODPACK))
                || (deprecated.contains(query) && mod.getBadges().contains(ModBadge.DEPRECATED))
                || (clientside.contains(query) && mod.getBadges().contains(ModBadge.CLIENT))
                || hasCustomBadge
                || (configurable.contains(query) && screen.getModHasConfigScreen(mod.getContainer()))
        ) {
            return 1;
        }

        // Allow parent to pass filter if a child passes
        if (ModMenu.PARENT_MAP.containsKey(mod)) {
            for (Mod child : ModMenu.PARENT_MAP.get(mod)) {
                int result = passesFilters(screen, child, query);
                if (result > 0) {
                    return result;
                }
            }
        }
        return 0;
    }

    private static boolean authorMatches(Mod mod, String query) {
        for (String author : mod.getAuthors()) {
            if (author.toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }
}
