package com.terraformersmc.mod_menu.util.mod.forge;

import com.terraformersmc.mod_menu.ModMenu;
import com.terraformersmc.mod_menu.util.VersionUtil;
import com.terraformersmc.mod_menu.util.mod.Mod;
import com.terraformersmc.mod_menu.util.mod.ModBadge;
import com.terraformersmc.mod_menu.util.mod.ModIconHandler;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Tuple;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ModMetadata;

import java.awt.Dimension;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ForgeMod implements Mod {

    private static final Pattern GITHUB_ISSUES_PATTERN = Pattern.compile(
        "(?:https?://)?(?:www\\.)?github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/issues(?:/[^\\s,.)\\]>\"']*)?",
        Pattern.CASE_INSENSITIVE
    );

    protected final ModContainer container;
    protected final ModMetadata metadata;
    protected final ModMenuData modMenuData;

    protected final Set<ModBadge> badges = new LinkedHashSet<ModBadge>();
    protected final Set<String> badgeNames = new LinkedHashSet<String>();

    protected final Map<String, String> links = new HashMap<String, String>();
    protected final List<String> contributors = new ArrayList<String>();
    protected final List<String> authors = new ArrayList<String>();

    protected boolean childHasUpdate = false;
    protected String sources;
    protected String issueTrackerUrl;
    protected String website;
    protected String license;

    public ForgeMod(ModContainer container) {
        this.container = container;
        this.metadata = container != null ? container.getMetadata() : null;

        String id = getId();

        if ("minecraft".equals(id)) {
            badgeNames.add("minecraft");
        } else {
            badgeNames.add("forge");
        }

        // Cleanroom detection
        if (id.contains("cleanroom") || (metadata != null && metadata.name != null && metadata.name.toLowerCase(Locale.ROOT).contains("cleanroom"))) {
            badgeNames.add("cleanroom");
        }

        // Client-side only detection
        if (detectClientSideOnly(container)) {
            badgeNames.add("clientside");
            badges.add(ModBadge.CLIENT);
        }

        String parentId = null;
        if (metadata != null) {
            parentId = metadata.parent;
            if (parentId != null && parentId.trim().isEmpty()) {
                parentId = null;
            }
            if (parentId == null && metadata.parentMod != null && metadata.parentMod.getModId() != null) {
                parentId = metadata.parentMod.getModId();
            }

            if (metadata.url != null && !metadata.url.trim().isEmpty()) {
                website = metadata.url;
            }
            if (metadata.updateUrl != null && !metadata.updateUrl.trim().isEmpty()) {
                links.put("modmenu.hasUpdate", metadata.updateUrl);
            }

            if (metadata.authorList != null) {
                authors.addAll(metadata.authorList);
            }

            if (metadata.credits != null && !metadata.credits.trim().isEmpty()) {
                contributors.add(metadata.credits.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\r", "\n"));
            }
        }

        if (container != null && container.getCustomModProperties() != null) {
            String lic = container.getCustomModProperties().get("license");
            if (lic != null && !lic.trim().isEmpty()) {
                this.license = lic.trim();
            }
            String issues = container.getCustomModProperties().get("issueTracker");
            if (issues == null) issues = container.getCustomModProperties().get("issues");
            if (issues == null) issues = container.getCustomModProperties().get("issueTrackerUrl");
            if (issues != null && !issues.trim().isEmpty()) {
                this.issueTrackerUrl = issues.trim();
            }
        }

        // Only detect genuine existing issues link in description or metadata URL (do not generate automatically)
        if (issueTrackerUrl == null || issueTrackerUrl.trim().isEmpty()) {
            if (metadata != null) {
                if (metadata.description != null) {
                    Matcher m = GITHUB_ISSUES_PATTERN.matcher(metadata.description);
                    if (m.find()) {
                        String matched = m.group().replaceAll("[/.,)\\]>\"']+$", "");
                        if (!matched.startsWith("http://") && !matched.startsWith("https://")) {
                            matched = "https://" + matched;
                        }
                        this.issueTrackerUrl = matched;
                    }
                }
                if (issueTrackerUrl == null && metadata.url != null) {
                    Matcher m = GITHUB_ISSUES_PATTERN.matcher(metadata.url);
                    if (m.find()) {
                        String matched = m.group().replaceAll("[/.,)\\]>\"']+$", "");
                        if (!matched.startsWith("http://") && !matched.startsWith("https://")) {
                            matched = "https://" + matched;
                        }
                        this.issueTrackerUrl = matched;
                    }
                }
            }
        }

        if (authors.isEmpty()) {
            if ("minecraft".equals(id)) {
                authors.add("Mojang Studios");
            } else if ("forge".equals(id)) {
                authors.add("Forge Development LLC");
            }
        }

        this.modMenuData = new ModMenuData(parentId, null, id);
        this.badges.addAll(modMenuData.getBadges());
        reCalculateBadge();
    }

    private boolean detectClientSideOnly(ModContainer container) {
        if (container == null) return false;
        try {
            if (container instanceof net.minecraftforge.fml.common.FMLModContainer) {
                java.lang.reflect.Field descField = net.minecraftforge.fml.common.FMLModContainer.class.getDeclaredField("descriptor");
                descField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Object> desc = (Map<String, Object>) descField.get(container);
                if (desc != null) {
                    Object val = desc.get("clientSideOnly");
                    if (val instanceof Boolean && (Boolean) val) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Object modInst = container.getMod();
            if (modInst != null) {
                net.minecraftforge.fml.common.Mod anno = modInst.getClass().getAnnotation(net.minecraftforge.fml.common.Mod.class);
                if (anno != null && anno.clientSideOnly()) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @Override
    public ModContainer getContainer() {
        return container;
    }

    @Override
    public String getId() {
        try {
            if (container != null && container.getModId() != null) {
                return container.getModId();
            }
        } catch (Throwable ignored) {
        }
        if (metadata != null && metadata.modId != null) {
            return metadata.modId;
        }
        return "unknown";
    }

    @Override
    public String getName() {
        if ("minecraft".equals(getId())) {
            return "Minecraft";
        }
        try {
            if (container != null && container.getName() != null) {
                return container.getName();
            }
        } catch (Throwable ignored) {
        }
        if (metadata != null && metadata.name != null) {
            return metadata.name;
        }
        return "Unknown";
    }

    @Override
    public Tuple<DynamicTexture, Dimension> getIcon(ModIconHandler iconHandler, int i, boolean isSmall) {
        String iconSourceId = getId();
        String iconResourceId = iconSourceId + (isSmall ? "_small" : "");

        if (ModIconHandler.modResourceIconCache.containsKey(iconResourceId)) {
            return ModIconHandler.modResourceIconCache.get(iconResourceId);
        }

        String iconPath = null;
        if (metadata != null && metadata.logoFile != null && !metadata.logoFile.trim().isEmpty()) {
            iconPath = metadata.logoFile;
        }

        if ("minecraft".equals(iconSourceId)) {
            iconPath = "assets/mod_menu/minecraft_icon.png";
        } else if ("mcp".equals(iconSourceId) && (iconPath == null || iconPath.isEmpty())) {
            iconPath = "mcplogo.png";
        } else if ("fml".equals(iconSourceId) && (iconPath == null || iconPath.isEmpty())) {
            iconPath = "forge_logo.png";
        } else if ("mod_menu".equals(iconSourceId)) {
            iconPath = "assets/mod_menu/icon.png";
        }

        return iconHandler.createIcon(container, iconPath);
    }

    @Override
    public String getDescription() {
        if (metadata != null && metadata.description != null) {
            return metadata.description.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\r", "\n");
        }
        return "";
    }

    @Override
    public String getVersion() {
        if (container != null) {
            try {
                String ver = container.getDisplayVersion();
                if (ver != null && !ver.isEmpty()) {
                    return ver;
                }
            } catch (Throwable ignored) {
            }
            try {
                String ver = container.getVersion();
                if (ver != null && !ver.isEmpty()) {
                    return ver;
                }
            } catch (Throwable ignored) {
            }
        }
        if (metadata != null && metadata.version != null && !metadata.version.isEmpty()) {
            return metadata.version;
        }
        return "";
    }

    @Override
    public String getPrefixedVersion() {
        return VersionUtil.getPrefixedVersion(getVersion());
    }

    @Override
    public List<String> getAuthors() {
        return authors;
    }

    @Override
    public Map<String, Collection<String>> getContributors() {
        Map<String, Collection<String>> result = new LinkedHashMap<String, Collection<String>>();
        for (String c : contributors) {
            result.put(c, Collections.singletonList("Contributor"));
        }
        return result;
    }

    @Override
    public SortedMap<String, Set<String>> getCredits() {
        SortedMap<String, Set<String>> credits = new TreeMap<String, Set<String>>();
        for (String author : authors) {
            if (!credits.containsKey("Author")) {
                credits.put("Author", new LinkedHashSet<String>());
            }
            credits.get("Author").add(author);
        }
        for (String contributor : contributors) {
            if (!credits.containsKey("Contributor")) {
                credits.put("Contributor", new LinkedHashSet<String>());
            }
            credits.get("Contributor").add(contributor);
        }
        return credits;
    }

    @Override
    public Set<ModBadge> getBadges() {
        return badges;
    }

    @Override
    public Set<String> getBadgeNames() {
        return badgeNames;
    }

    @Override
    public String getWebsite() {
        if ("minecraft".equals(getId())) {
            return "https://www.minecraft.net/";
        }
        return website;
    }

    @Override
    public String getIssueTracker() {
        return issueTrackerUrl;
    }

    @Override
    public String getSource() {
        return sources;
    }

    @Override
    public String getParent() {
        return modMenuData.getParent();
    }

    @Override
    public Set<String> getLicense() {
        Set<String> set = new HashSet<String>();
        if ("minecraft".equals(getId())) {
            set.add("Minecraft EULA");
        } else if ("forge".equals(getId()) || "fml".equals(getId())) {
            set.add("LGPL-2.1");
        } else if ("mcp".equals(getId())) {
            set.add("Custom");
        } else if ("mod_menu".equals(getId())) {
            set.add("MIT");
        }
        if (this.license != null && !this.license.trim().isEmpty()) {
            set.add(this.license);
        }
        return set;
    }

    @Override
    public Map<String, String> getLinks() {
        return links;
    }

    @Override
    public boolean isReal() {
        return true;
    }

    @Override
    public boolean getChildHasUpdate() {
        return childHasUpdate;
    }

    @Override
    public void setChildHasUpdate() {
        this.childHasUpdate = true;
    }

    @Override
    public ModMenuData getModMenuData() {
        return modMenuData;
    }
}
