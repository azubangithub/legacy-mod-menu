package com.terraformersmc.mod_menu.util.mod.parent;

import com.terraformersmc.mod_menu.util.mod.Mod;
import com.terraformersmc.mod_menu.util.mod.ModBadge;
import com.terraformersmc.mod_menu.util.mod.ModIconHandler;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Tuple;
import net.minecraftforge.fml.common.ModContainer;

import java.awt.Dimension;
import java.util.*;

public class DummyParentMod implements Mod {
    private final String id;
    private final Mod host;
    private boolean childHasUpdate;
    private final Set<String> badgeNames = new LinkedHashSet<String>();
    private final Set<ModBadge> badges = new LinkedHashSet<ModBadge>();

    public DummyParentMod(Mod host, String id) {
        this.host = host;
        this.id = id;

        if (host != null && host.getModMenuData() != null && host.getModMenuData().getDummyParentData() != null) {
            ModMenuData.DummyParentData parentData = host.getModMenuData().getDummyParentData();
            if (parentData.getBadges() != null) {
                badgeNames.addAll(parentData.getBadges());
            }
        }
        reCalculateBadge();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        if (host != null && host.getModMenuData() != null && host.getModMenuData().getDummyParentData() != null) {
            String name = host.getModMenuData().getDummyParentData().getName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        return id;
    }

    @Override
    public Tuple<DynamicTexture, Dimension> getIcon(ModIconHandler iconHandler, int i, boolean isSmall) {
        String iconPath = null;
        if (host != null && host.getModMenuData() != null && host.getModMenuData().getDummyParentData() != null) {
            iconPath = host.getModMenuData().getDummyParentData().getIcon();
        }
        if ("inherit".equals(iconPath) && host != null) {
            return host.getIcon(iconHandler, i, isSmall);
        }
        if (iconPath == null) {
            iconPath = "assets/mod_menu/textures/gui/parent_mod.png";
        }
        return iconHandler.createIcon(host != null ? host.getContainer() : null, iconPath);
    }

    @Override
    public String getDescription() {
        if (host != null && host.getModMenuData() != null && host.getModMenuData().getDummyParentData() != null) {
            String desc = host.getModMenuData().getDummyParentData().getDescription();
            if (desc != null) {
                return desc;
            }
        }
        return "";
    }

    @Override
    public String getVersion() {
        return "";
    }

    @Override
    public String getPrefixedVersion() {
        return "";
    }

    @Override
    public List<String> getAuthors() {
        return Collections.emptyList();
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
        return null;
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
        return Collections.emptyMap();
    }

    @Override
    public boolean isReal() {
        return false;
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
        return host != null ? host.getModMenuData() : null;
    }

    @Override
    public ModContainer getContainer() {
        return null;
    }
}
