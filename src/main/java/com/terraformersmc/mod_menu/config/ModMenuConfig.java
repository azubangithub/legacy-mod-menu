package com.terraformersmc.mod_menu.config;

import com.terraformersmc.mod_menu.ModMenu;
import com.terraformersmc.mod_menu.util.mod.Mod;
import com.terraformersmc.mod_menu.util.mod.parent.DummyParentMod;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.util.*;

public class ModMenuConfig {
    public static final String CATEGORY_MAIN = "main";
    public static final String CATEGORY_HIDE = "hide";
    public static final String CATEGORY_COUNT = "count";
    public static final String CATEGORY_ICONS = "icons";

    private final Configuration config;

    public Sorting sorting = Sorting.ASCENDING;
    public boolean compactList = false;
    public TitleMenuButtonStyle modsButtonStyle = TitleMenuButtonStyle.CLASSIC;
    public GameMenuButtonStyle gameMenuStyle = GameMenuButtonStyle.REPLACE;
    public ModCountLocation modCountLocation = ModCountLocation.TITLE_SCREEN;
    public boolean easterEggs = true;
    public boolean randomJavaColors = false;
    public boolean translateNames = true;
    public boolean translateDescriptions = true;
    public boolean quickConfigure = true;
    public boolean modifyTitleScreen = true;
    public boolean modifyGameMenu = true;
    public boolean configMode = false;
    public boolean disableDragAndDrop = false;
    public boolean useCatalogueIcon = true;

    public boolean showLibraries = false;
    public boolean hideModLinks = false;
    public boolean hideModLicense = false;
    public boolean hideBadges = false;
    public List<String> hideBadge = new ArrayList<String>();
    public boolean hideModCredits = false;
    public boolean hideConfigButtons = false;
    public boolean hideBadgeButtons = true;
    public boolean hideScreenTop = false;
    public List<String> hiddenMods = new ArrayList<String>();
    public List<String> hiddenConfigs = new ArrayList<String>();
    public boolean disableDefaultBadgesAll = false;
    public List<String> disableDefaultBadges = new ArrayList<String>();

    public boolean countHiddenMods = true;
    public boolean countChildren = true;
    public boolean countLibraries = true;

    /**
     * Ordered list of icon sources. Each entry is an IconSource name, optionally prefixed with '!' to disable.
     * Default order: MOD_FILE first, then MODRINTH, then CURSEFORGE.
     * Example: ["MOD_FILE", "MODRINTH", "CURSEFORGE"]
     * To disable a source: ["MOD_FILE", "!MODRINTH", "CURSEFORGE"]
     */
    public List<String> iconSourcePriority = new ArrayList<String>(Arrays.asList("MOD_FILE", "MODRINTH", "CURSEFORGE"));

    public final Map<String, Set<String>> modBadges = new HashMap<String, Set<String>>();
    public final Map<String, Set<String>> disabledModBadges = new HashMap<String, Set<String>>();
    public List<String> modParents = new ArrayList<String>();
    public List<String> rawModBadges = new ArrayList<String>();

    public ModMenuConfig(File configFile) {
        this.config = new Configuration(configFile);
        load();
    }

    public Configuration getConfiguration() {
        return config;
    }

    public void load() {
        config.load();
        readConfig();
    }

    public void readConfig() {
        Property propSorting = config.get(CATEGORY_MAIN, "sorting", sorting.name(), "Sorting order: ASCENDING or DESCENDING");
        propSorting.setValidValues(new String[]{"ASCENDING", "DESCENDING"});
        try {
            sorting = Sorting.valueOf(propSorting.getString().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            sorting = Sorting.ASCENDING;
        }

        compactList = config.getBoolean("compact_list", CATEGORY_MAIN, compactList, "Makes list more compacted");

        Property propModsBtn = config.get(CATEGORY_MAIN, "mods_button_style", modsButtonStyle.name(), "Title screen mods button style: CLASSIC, REPLACE_REALMS, SHRINK, SHRINK_LEFT, ICON");
        propModsBtn.setValidValues(new String[]{"CLASSIC", "REPLACE_REALMS", "SHRINK", "SHRINK_LEFT", "ICON"});
        try {
            modsButtonStyle = TitleMenuButtonStyle.valueOf(propModsBtn.getString().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            modsButtonStyle = TitleMenuButtonStyle.CLASSIC;
        }

        Property propGameBtn = config.get(CATEGORY_MAIN, "game_menu_button_style", gameMenuStyle.name(), "Pause menu mods button style: INSERT, REPLACE, ICON");
        propGameBtn.setValidValues(new String[]{"REPLACE", "INSERT", "ICON"});
        try {
            gameMenuStyle = GameMenuButtonStyle.valueOf(propGameBtn.getString().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            gameMenuStyle = GameMenuButtonStyle.REPLACE;
        }

        Property propCountLoc = config.get(CATEGORY_MAIN, "mod_count_location", modCountLocation.name(), "Location of mod count: TITLE_SCREEN, MODS_BUTTON, TITLE_SCREEN_AND_MODS_BUTTON, NONE");
        propCountLoc.setValidValues(new String[]{"TITLE_SCREEN", "MODS_BUTTON", "TITLE_SCREEN_AND_MODS_BUTTON", "NONE"});
        try {
            modCountLocation = ModCountLocation.valueOf(propCountLoc.getString().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            modCountLocation = ModCountLocation.TITLE_SCREEN;
        }

        easterEggs = config.getBoolean("easter_eggs", CATEGORY_MAIN, easterEggs, "Shows secret mod count translations");
        randomJavaColors = config.getBoolean("random_java_colors", CATEGORY_MAIN, randomJavaColors, "Makes java mod have random colors");
        translateNames = config.getBoolean("translate_names", CATEGORY_MAIN, translateNames, "Make mod names translatable");
        translateDescriptions = config.getBoolean("translate_descriptions", CATEGORY_MAIN, translateDescriptions, "Make mod descriptions translatable");
        quickConfigure = config.getBoolean("quick_configure", CATEGORY_MAIN, quickConfigure, "Shows config button above mod icon on the left");
        modifyTitleScreen = config.getBoolean("modify_title_screen", CATEGORY_MAIN, modifyTitleScreen, "Modifies title screen with custom mods button");
        modifyGameMenu = config.getBoolean("modify_game_menu", CATEGORY_MAIN, modifyGameMenu, "Modifies pause screen with custom mods button");
        configMode = config.getBoolean("config_mode", CATEGORY_MAIN, configMode, "Will only show mods with config available");
        disableDragAndDrop = config.getBoolean("disable_drag_and_drop", CATEGORY_MAIN, disableDragAndDrop, "Disables drag and drop mods adding");
        useCatalogueIcon = config.getBoolean("use_catalogue_icon", CATEGORY_MAIN, useCatalogueIcon, "Will use catalogue icon if present");

        showLibraries = config.getBoolean("show_libraries", CATEGORY_HIDE, showLibraries, "Shows mods with library badge");
        hideModLinks = config.getBoolean("hide_mod_links", CATEGORY_HIDE, hideModLinks, "Hides mod links");
        hideModLicense = config.getBoolean("hide_mod_license", CATEGORY_HIDE, hideModLicense, "Hides mod license");
        hideBadges = config.getBoolean("hide_badges", CATEGORY_HIDE, hideBadges, "Hides mod badges");
        hideModCredits = config.getBoolean("hide_mod_credits", CATEGORY_HIDE, hideModCredits, "Hides mod credits");
        hideConfigButtons = config.getBoolean("hide_config_buttons", CATEGORY_HIDE, hideConfigButtons, "Hides config buttons");
        hideBadgeButtons = config.getBoolean("hide_badge_buttons", CATEGORY_HIDE, hideBadgeButtons, "Hides button which allows changing mod badge");
        hideScreenTop = config.getBoolean("hide_screen_top", CATEGORY_HIDE, hideScreenTop, "Hides search bar and moves mod icon up");

        hiddenMods = new ArrayList<String>(Arrays.asList(config.getStringList("hidden_mods", CATEGORY_HIDE, new String[0], "Add modid of the mod to hide it from the modlist")));
        hiddenConfigs = new ArrayList<String>(Arrays.asList(config.getStringList("hidden_configs", CATEGORY_HIDE, new String[0], "Add modid of the mod to hide its config")));
        hideBadge = new ArrayList<String>(Arrays.asList(config.getStringList("hide_badge", CATEGORY_HIDE, new String[0], "Add id of the badge to hide it")));
        disableDefaultBadgesAll = config.getBoolean("disable_default_badges_all", CATEGORY_HIDE, disableDefaultBadgesAll, "Disable all default badges");
        disableDefaultBadges = new ArrayList<String>(Arrays.asList(config.getStringList("disable_default_badges", CATEGORY_HIDE, new String[0], "Disable default badges for specific mods")));

        countHiddenMods = config.getBoolean("count_hidden_mods", CATEGORY_COUNT, countHiddenMods, "Count hidden mods in total count");
        countChildren = config.getBoolean("count_children", CATEGORY_COUNT, countChildren, "Count children mods in total count");
        countLibraries = config.getBoolean("count_libraries", CATEGORY_COUNT, countLibraries, "Count library mods in total count");

        iconSourcePriority = new ArrayList<String>(Arrays.asList(config.getStringList(
            "icon_source_priority", CATEGORY_ICONS,
            new String[]{"MOD_FILE", "MODRINTH", "CURSEFORGE"},
            "Order and toggle of icon sources. Prefix with '!' to disable a source.\n" +
            "Available sources: MOD_FILE, MODRINTH, CURSEFORGE\n" +
            "Example: MOD_FILE, !MODRINTH, CURSEFORGE  (disables Modrinth)")));

        rawModBadges = new ArrayList<String>(Arrays.asList(config.getStringList("mod_badges", CATEGORY_MAIN, new String[0], "Adds badge to mod (format: modid=badge1, badge2)")));
        modParents = new ArrayList<String>(Arrays.asList(config.getStringList("mod_parents", CATEGORY_MAIN, new String[0], "Make mods appear under another mod (format: parentModId=childId1, childId2)")));

        if (config.hasChanged()) {
            config.save();
        }

        onLoad();
    }

    public void onLoad() {
        modBadges.clear();
        disabledModBadges.clear();
        for (String badge : rawModBadges) {
            String[] badgeKeyValue = badge.split("=");
            if (badgeKeyValue.length == 2) {
                Set<String> badges = new LinkedHashSet<String>();
                Set<String> disabledBadges = new LinkedHashSet<String>();
                for (String badgeId : badgeKeyValue[1].split(",\\s*")) {
                    if (badgeId.startsWith("!")) {
                        disabledBadges.add(badgeId.substring(1));
                    } else {
                        badges.add(badgeId);
                    }
                }
                modBadges.put(badgeKeyValue[0], badges);
                disabledModBadges.put(badgeKeyValue[0], disabledBadges);
            }
        }
    }

    public void processModParents() {
        Map<String, Mod> dummyParents = new HashMap<String, Mod>();
        Map<String, List<String>> parentsMap = new HashMap<String, List<String>>();

        for (String parentToMods : modParents) {
            if (parentToMods.isEmpty()) continue;
            String[] split = parentToMods.split("=");
            if (split.length == 2) {
                parentsMap.put(split[0], Arrays.asList(split[1].split(",\\s*")));
            }
        }

        HashSet<String> modParentSet = new HashSet<String>();
        for (Map.Entry<String, List<String>> entry : parentsMap.entrySet()) {
            String parentId = entry.getKey();
            for (String childId : entry.getValue()) {
                Mod mod = ModMenu.MODS.get(childId);
                if (mod == null) {
                    mod = dummyParents.get(childId);
                }
                if (mod == null) continue;

                modParentSet.clear();
                Mod parent = ModMenu.MODS.get(parentId);
                if (parent == null) {
                    parent = dummyParents.get(parentId);
                }
                if (parent == null) {
                    parent = new DummyParentMod(mod, parentId);
                    dummyParents.put(parentId, parent);
                }

                ModMenu.ROOT_MODS.remove(mod.getId());
                ModMenu.PARENT_MAP.put(parent, mod);
            }
        }
        ModMenu.MODS.putAll(dummyParents);
    }

    public void save() {
        List<String> list = new ArrayList<String>();
        for (Map.Entry<String, Set<String>> entry : modBadges.entrySet()) {
            String key = entry.getKey();
            Set<String> values = entry.getValue();
            Set<String> disabledBadges = disabledModBadges.get(key);
            if (values.isEmpty() && (disabledBadges == null || disabledBadges.isEmpty())) {
                continue;
            }

            StringBuilder sb = new StringBuilder();
            for (String value : values) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(value);
            }
            if (disabledBadges != null) {
                for (String value : disabledBadges) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append("!").append(value);
                }
            }
            if (sb.length() > 0) {
                list.add(key + "=" + sb.toString());
            }
        }
        this.rawModBadges = list;

        // CATEGORY_MAIN
        config.get(CATEGORY_MAIN, "sorting", sorting.name()).set(sorting.name());
        config.get(CATEGORY_MAIN, "compact_list", compactList).set(compactList);
        config.get(CATEGORY_MAIN, "mods_button_style", modsButtonStyle.name()).set(modsButtonStyle.name());
        config.get(CATEGORY_MAIN, "game_menu_button_style", gameMenuStyle.name()).set(gameMenuStyle.name());
        config.get(CATEGORY_MAIN, "mod_count_location", modCountLocation.name()).set(modCountLocation.name());
        config.get(CATEGORY_MAIN, "easter_eggs", easterEggs).set(easterEggs);
        config.get(CATEGORY_MAIN, "random_java_colors", randomJavaColors).set(randomJavaColors);
        config.get(CATEGORY_MAIN, "translate_names", translateNames).set(translateNames);
        config.get(CATEGORY_MAIN, "translate_descriptions", translateDescriptions).set(translateDescriptions);
        config.get(CATEGORY_MAIN, "quick_configure", quickConfigure).set(quickConfigure);
        config.get(CATEGORY_MAIN, "modify_title_screen", modifyTitleScreen).set(modifyTitleScreen);
        config.get(CATEGORY_MAIN, "modify_game_menu", modifyGameMenu).set(modifyGameMenu);
        config.get(CATEGORY_MAIN, "config_mode", configMode).set(configMode);
        config.get(CATEGORY_MAIN, "disable_drag_and_drop", disableDragAndDrop).set(disableDragAndDrop);
        config.get(CATEGORY_MAIN, "use_catalogue_icon", useCatalogueIcon).set(useCatalogueIcon);
        config.get(CATEGORY_MAIN, "mod_badges", new String[0]).set(rawModBadges.toArray(new String[0]));
        config.get(CATEGORY_MAIN, "mod_parents", new String[0]).set(modParents.toArray(new String[0]));

        // CATEGORY_HIDE
        config.get(CATEGORY_HIDE, "show_libraries", showLibraries).set(showLibraries);
        config.get(CATEGORY_HIDE, "hide_mod_links", hideModLinks).set(hideModLinks);
        config.get(CATEGORY_HIDE, "hide_mod_license", hideModLicense).set(hideModLicense);
        config.get(CATEGORY_HIDE, "hide_badges", hideBadges).set(hideBadges);
        config.get(CATEGORY_HIDE, "hide_badge", new String[0]).set(hideBadge.toArray(new String[0]));
        config.get(CATEGORY_HIDE, "hide_mod_credits", hideModCredits).set(hideModCredits);
        config.get(CATEGORY_HIDE, "hide_config_buttons", hideConfigButtons).set(hideConfigButtons);
        config.get(CATEGORY_HIDE, "hide_badge_buttons", hideBadgeButtons).set(hideBadgeButtons);
        config.get(CATEGORY_HIDE, "hide_screen_top", hideScreenTop).set(hideScreenTop);
        config.get(CATEGORY_HIDE, "hidden_mods", new String[0]).set(hiddenMods.toArray(new String[0]));
        config.get(CATEGORY_HIDE, "hidden_configs", new String[0]).set(hiddenConfigs.toArray(new String[0]));
        config.get(CATEGORY_HIDE, "disable_default_badges_all", disableDefaultBadgesAll).set(disableDefaultBadgesAll);
        config.get(CATEGORY_HIDE, "disable_default_badges", new String[0]).set(disableDefaultBadges.toArray(new String[0]));

        // CATEGORY_COUNT
        config.get(CATEGORY_COUNT, "count_hidden_mods", countHiddenMods).set(countHiddenMods);
        config.get(CATEGORY_COUNT, "count_children", countChildren).set(countChildren);
        config.get(CATEGORY_COUNT, "count_libraries", countLibraries).set(countLibraries);

        // CATEGORY_ICONS
        config.get(CATEGORY_ICONS, "icon_source_priority", new String[]{"MOD_FILE", "MODRINTH", "CURSEFORGE"}).set(iconSourcePriority.toArray(new String[0]));

        config.save();
    }

    public enum Sorting {
        ASCENDING(new Comparator<Mod>() {
            @Override
            public int compare(Mod a, Mod b) {
                return a.getTranslatedName().toLowerCase(Locale.ROOT).compareTo(b.getTranslatedName().toLowerCase(Locale.ROOT));
            }
        }),
        DESCENDING(new Comparator<Mod>() {
            @Override
            public int compare(Mod a, Mod b) {
                return b.getTranslatedName().toLowerCase(Locale.ROOT).compareTo(a.getTranslatedName().toLowerCase(Locale.ROOT));
            }
        });

        private final Comparator<Mod> comparator;

        Sorting(Comparator<Mod> comparator) {
            this.comparator = comparator;
        }

        public Comparator<Mod> getComparator() {
            return comparator;
        }

        public void cycleValue() {
            int next = ordinal() + 1;
            if (next >= values().length) next = 0;
            ModMenu.getConfig().sorting = values()[next];
            ModMenu.getConfig().save();
        }
    }

    public enum ModCountLocation {
        TITLE_SCREEN(true, false),
        MODS_BUTTON(false, true),
        TITLE_SCREEN_AND_MODS_BUTTON(true, true),
        NONE(false, false);

        private final boolean titleScreen;
        private final boolean modsButton;

        ModCountLocation(boolean titleScreen, boolean modsButton) {
            this.titleScreen = titleScreen;
            this.modsButton = modsButton;
        }

        public boolean isOnTitleScreen() {
            return titleScreen;
        }

        public boolean isOnModsButton() {
            return modsButton;
        }
    }

    public enum TitleMenuButtonStyle {
        CLASSIC, REPLACE_REALMS, SHRINK, SHRINK_LEFT, ICON
    }

    public enum GameMenuButtonStyle {
        REPLACE, INSERT, ICON
    }
}
