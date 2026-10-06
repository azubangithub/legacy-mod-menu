package com.terraformersmc.mod_menu.util.mod.java;

import com.terraformersmc.mod_menu.util.VersionUtil;
import com.terraformersmc.mod_menu.util.mod.Mod;
import com.terraformersmc.mod_menu.util.mod.ModBadge;
import com.terraformersmc.mod_menu.util.mod.ModIconHandler;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.Tuple;
import net.minecraftforge.fml.common.ModContainer;

import java.awt.Dimension;
import java.util.*;

public class JavaDummyMod implements Mod {

    protected final ModMenuData modMenuData;
    private static final String MOD_ID = "java";

    protected final Map<String, String> links = new HashMap<String, String>();
    protected final Set<String> badgeNames = new LinkedHashSet<String>();
    protected final Set<ModBadge> badges = new LinkedHashSet<ModBadge>();
    protected boolean childHasUpdate = false;

    public JavaDummyMod() {
        badgeNames.add("library");
        this.modMenuData = new ModMenuData(null, null, MOD_ID);
        this.badges.add(ModBadge.LIBRARY);
        reCalculateBadge();
    }

    @Override
    public String getId() {
        return MOD_ID;
    }

    @Override
    public String getName() {
        String name = System.getProperty("java.vm.name");
        return name != null ? name : "Java";
    }

    @Override
    public Tuple<DynamicTexture, Dimension> getIcon(ModIconHandler iconHandler, int i, boolean isSmall) {
        return iconHandler.createIcon(null, "assets/mod_menu/java_icon.png");
    }

    @Override
    public String getDescription() {
        return MOD_ID;
    }

    @Override
    public String getTranslatedDescription() {
        String desc = Mod.super.getTranslatedDescription();
        if (I18n.hasKey("modmenu.javaDistributionName")) {
            desc = desc + "\n" + I18n.format("modmenu.javaDistributionName", getName());
        }
        return desc;
    }

    @Override
    public String getVersion() {
        String ver = System.getProperty("java.runtime.version");
        return ver != null ? ver : System.getProperty("java.version");
    }

    @Override
    public String getPrefixedVersion() {
        return VersionUtil.getPrefixedVersion(getVersion());
    }

    @Override
    public List<String> getAuthors() {
        String vendor = System.getProperty("java.vendor");
        return vendor != null ? Collections.singletonList(vendor) : Collections.<String>emptyList();
    }

    @Override
    public Map<String, Collection<String>> getContributors() {
        return Collections.emptyMap();
    }

    @Override
    public SortedMap<String, Set<String>> getCredits() {
        return new TreeMap<String, Set<String>>();
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
        return System.getProperty("java.vendor.url");
    }

    @Override
    public String getIssueTracker() {
        return null;
    }

    @Override
    public String getSource() {
        return null;
    }

    @Override
    public String getParent() {
        return null;
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

    @Override
    public ModContainer getContainer() {
        return null;
    }
}
