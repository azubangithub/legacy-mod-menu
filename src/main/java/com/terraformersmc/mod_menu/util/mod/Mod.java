package com.terraformersmc.mod_menu.util.mod;

import com.terraformersmc.mod_menu.ModMenu;
import com.terraformersmc.mod_menu.config.ModMenuConfig;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.Tuple;
import net.minecraftforge.fml.common.ModContainer;

import java.awt.Dimension;
import java.util.*;

public interface Mod {

    String getId();

    String getName();

    default String getTranslatedName() {
        String translationKey = "modmenu.nameTranslation." + getId();
        if (!I18n.hasKey(translationKey)) {
            translationKey = "mod_menu.nameTranslation." + getId();
        }
        if (!I18n.hasKey(translationKey)) {
            translationKey = "modmenu.nameTranslation." + getId().replace("_", "-");
        }
        if ((getId().equals("minecraft") || getId().equals("java") || ModMenu.getConfig().translateNames) && I18n.hasKey(translationKey)) {
            return I18n.format(translationKey);
        }
        return getName();
    }

    Tuple<DynamicTexture, Dimension> getIcon(ModIconHandler iconHandler, int i, boolean isSmall);

    default String getSummary() {
        return getTranslatedSummary();
    }

    default String getTranslatedSummary() {
        String translationKey = "modmenu.summaryTranslation." + getId();
        if (!I18n.hasKey(translationKey)) {
            translationKey = "mod_menu.summaryTranslation." + getId();
        }
        if (!I18n.hasKey(translationKey)) {
            translationKey = "modmenu.summaryTranslation." + getId().replace("_", "-");
        }
        if ((getId().equals("minecraft") || getId().equals("java") || ModMenu.getConfig().translateDescriptions) && I18n.hasKey(translationKey)) {
            return I18n.format(translationKey);
        }
        return getTranslatedDescription();
    }

    String getDescription();

    default String getTranslatedDescription() {
        String translatableDescriptionKey = "modmenu.descriptionTranslation." + getId();
        if (!I18n.hasKey(translatableDescriptionKey)) {
            translatableDescriptionKey = "mod_menu.descriptionTranslation." + getId();
        }
        if (!I18n.hasKey(translatableDescriptionKey)) {
            translatableDescriptionKey = "modmenu.descriptionTranslation." + getId().replace("_", "-");
        }
        if ((getId().equals("minecraft") || getId().equals("java") || ModMenu.getConfig().translateDescriptions) && I18n.hasKey(translatableDescriptionKey)) {
            return I18n.format(translatableDescriptionKey).replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\r", "\n");
        }
        String desc = getDescription();
        if (desc != null) {
            return desc.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\r", "\n");
        }
        return "";
    }

    default void reCalculateBadge() {
        ModMenuConfig config = ModMenu.getConfig();
        config.modBadges.putIfAbsent(getId(), new LinkedHashSet<String>());

        Set<String> defaultBadges = new LinkedHashSet<String>(getBadgeNames());

        if (config.disabledModBadges.containsKey(getId())) {
            defaultBadges.removeAll(config.disabledModBadges.get(getId()));
        }

        this.getBadges().clear();
        if (!config.disableDefaultBadgesAll && !config.disableDefaultBadges.contains(getId())) {
            this.getBadges().addAll(ModBadge.convert(defaultBadges, this.getId()));
        }
        Set<String> badgelist = config.modBadges.get(this.getId());
        if (badgelist != null) {
            this.getBadges().addAll(ModBadge.convert(badgelist, this.getId()));
        }
    }

    String getVersion();

    String getPrefixedVersion();

    List<String> getAuthors();

    Map<String, Collection<String>> getContributors();

    SortedMap<String, Set<String>> getCredits();

    Set<ModBadge> getBadges();

    Set<String> getBadgeNames();

    String getWebsite();

    String getIssueTracker();

    String getSource();

    String getParent();

    Set<String> getLicense();

    Map<String, String> getLinks();

    boolean isReal();

    void setChildHasUpdate();

    boolean getChildHasUpdate();

    default boolean isHidden() {
        for (String string : ModMenu.getConfig().hiddenMods) {
            if (getId().matches(string)) {
                return true;
            }
        }
        return false;
    }

    ModMenuData getModMenuData();

    ModContainer getContainer();

    class ModMenuData {
        private final Set<ModBadge> badges = new LinkedHashSet<ModBadge>();
        private String parent;
        private DummyParentData dummyParentData;

        public ModMenuData(String parent, DummyParentData dummyParentData, String id) {
            this.parent = parent;
            this.dummyParentData = dummyParentData;
            badges.add(ModBadge.FORGE);
        }

        public Set<ModBadge> getBadges() {
            return badges;
        }

        public String getParent() {
            return parent;
        }

        public void setParent(String parent) {
            this.parent = parent;
        }

        public DummyParentData getDummyParentData() {
            return dummyParentData;
        }

        public void fillParentIfEmpty(String parent) {
            if (this.parent == null) {
                this.parent = parent;
            }
        }

        public static class DummyParentData {
            private final String id;
            private final String name;
            private final String description;
            private final String icon;
            private final Set<String> badges;

            public DummyParentData(String id, String name, String description, String icon, Set<String> badges) {
                this.id = id;
                this.name = name;
                this.description = description;
                this.icon = icon;
                this.badges = badges;
            }

            public String getId() {
                return id;
            }

            public String getName() {
                return name;
            }

            public String getDescription() {
                return description;
            }

            public String getIcon() {
                return icon;
            }

            public Set<String> getBadges() {
                return badges;
            }
        }
    }
}
