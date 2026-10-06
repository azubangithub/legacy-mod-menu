package com.terraformersmc.mod_menu.util.mod.liteloader;

import com.terraformersmc.mod_menu.util.VersionUtil;
import com.terraformersmc.mod_menu.util.mod.Mod;
import com.terraformersmc.mod_menu.util.mod.ModBadge;
import com.terraformersmc.mod_menu.util.mod.ModIconHandler;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Tuple;
import net.minecraftforge.fml.common.ModContainer;

import java.awt.Dimension;
import java.lang.reflect.Method;
import java.util.*;

public class LiteLoaderMod implements Mod {

    protected final String id;
    protected final String name;
    protected final String version;
    protected final String description;
    protected final String author;
    protected final String url;

    protected final ModMenuData modMenuData;
    protected final Set<ModBadge> badges = new LinkedHashSet<ModBadge>();
    protected final Set<String> badgeNames = new LinkedHashSet<String>();
    protected final Map<String, String> links = new HashMap<String, String>();
    private static final java.util.regex.Pattern GITHUB_ISSUES_PATTERN = java.util.regex.Pattern.compile(
        "(?:https?://)?(?:www\\.)?github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/issues(?:/[^\\s,.)\\]>\"']*)?",
        java.util.regex.Pattern.CASE_INSENSITIVE
    );

    protected String issueTrackerUrl;
    protected boolean childHasUpdate = false;

    public LiteLoaderMod(String id, String name, String version, String description, String author, String url) {
        this.id = id != null ? id : "unknown_litemod";
        this.name = name != null ? name : this.id;
        this.version = version != null ? version : "1.0";
        this.description = description != null ? description : "";
        this.author = author != null ? author : "";
        this.url = url != null ? url : "";

        badgeNames.add("liteloader");
        this.modMenuData = new ModMenuData(null, null, this.id);
        this.badges.add(ModBadge.LITELOADER);
        if (this.url != null && !this.url.isEmpty()) {
            links.put("modmenu.website", this.url);
        }

        if (this.description != null) {
            java.util.regex.Matcher m = GITHUB_ISSUES_PATTERN.matcher(this.description);
            if (m.find()) {
                String matched = m.group().replaceAll("[/.,)\\]>\"']+$", "");
                if (!matched.startsWith("http://") && !matched.startsWith("https://")) {
                    matched = "https://" + matched;
                }
                this.issueTrackerUrl = matched;
            }
        }

        reCalculateBadge();
    }

    public static List<LiteLoaderMod> getLoadedMods() {
        List<LiteLoaderMod> list = new ArrayList<LiteLoaderMod>();
        try {
            Class<?> liteLoaderClass = Class.forName("com.mumfrey.liteloader.core.LiteLoader");
            Method getInstance = liteLoaderClass.getMethod("getInstance");
            Object liteLoader = getInstance.invoke(null);
            if (liteLoader == null) return list;

            Method getLoadedMods = liteLoaderClass.getMethod("getLoadedMods");
            List<?> loadedMods = (List<?>) getLoadedMods.invoke(liteLoader);
            if (loadedMods == null) return list;

            for (Object modObj : loadedMods) {
                if (modObj == null) continue;
                String modId = invokeString(modObj, "getIdentifier", "getName");
                String modName = invokeString(modObj, "getName", "getIdentifier");
                String modVersion = invokeString(modObj, "getVersion");
                String modDesc = invokeString(modObj, "getDescription");
                String modAuthor = invokeString(modObj, "getAuthor");
                String modUrl = invokeString(modObj, "getMetaURL", "getURL");

                list.add(new LiteLoaderMod(modId, modName, modVersion, modDesc, modAuthor, modUrl));
            }
        } catch (Throwable ignored) {
        }
        return list;
    }

    private static String invokeString(Object obj, String... methodNames) {
        for (String mName : methodNames) {
            try {
                Method m = obj.getClass().getMethod(mName);
                Object res = m.invoke(obj);
                if (res != null) return res.toString();
            } catch (Throwable ignored) {
            }
        }
        return "";
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Tuple<DynamicTexture, Dimension> getIcon(ModIconHandler iconHandler, int i, boolean isSmall) {
        return iconHandler.createIcon(null, "assets/mod_menu/unknown_icon.png");
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getPrefixedVersion() {
        return VersionUtil.getPrefixedVersion(getVersion());
    }

    @Override
    public List<String> getAuthors() {
        if (author != null && !author.isEmpty()) {
            return Collections.singletonList(author);
        }
        return Collections.emptyList();
    }

    @Override
    public Map<String, Collection<String>> getContributors() {
        return Collections.emptyMap();
    }

    @Override
    public SortedMap<String, Set<String>> getCredits() {
        SortedMap<String, Set<String>> credits = new TreeMap<String, Set<String>>();
        if (author != null && !author.isEmpty()) {
            credits.put("Author", Collections.singleton(author));
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
        return url;
    }

    @Override
    public String getIssueTracker() {
        return issueTrackerUrl;
    }

    @Override
    public String getSource() {
        return null;
    }

    @Override
    public String getParent() {
        return modMenuData.getParent();
    }

    @Override
    public Set<String> getLicense() {
        return Collections.emptySet();
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
    public void setChildHasUpdate() {
        childHasUpdate = true;
    }

    @Override
    public boolean getChildHasUpdate() {
        return childHasUpdate;
    }

    @Override
    public ModMenuData getModMenuData() {
        return modMenuData;
    }

    @Override
    public ModContainer getContainer() {
        return null;
    }
}
