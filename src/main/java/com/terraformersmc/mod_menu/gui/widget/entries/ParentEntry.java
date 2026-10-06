package com.terraformersmc.mod_menu.gui.widget.entries;

import com.terraformersmc.mod_menu.ModMenu;
import com.terraformersmc.mod_menu.gui.widget.ModListWidget;
import com.terraformersmc.mod_menu.util.mod.Mod;
import com.terraformersmc.mod_menu.util.mod.ModBadge;
import com.terraformersmc.mod_menu.util.mod.ModSearch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParentEntry extends ModListEntry {
    private static final ResourceLocation PARENT_MOD_TEXTURE = new ResourceLocation(ModMenu.MOD_ID, "textures/gui/parent_mod.png");
    protected List<Mod> children;
    protected boolean hoveringIcon = false;

    public ParentEntry(Mod parent, List<Mod> children, ModListWidget list) {
        super(parent, list);
        this.children = children;
    }

    @Override
    public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
        super.drawEntry(slotIndex, x, y, listWidth, slotHeight, mouseX, mouseY, isSelected, partialTicks);

        FontRenderer font = client.fontRenderer;
        int childrenBadgeHeight = font.FONT_HEIGHT;
        int childrenBadgeWidth = font.FONT_HEIGHT;

        int shownChildren = ModSearch.search(list.getParent(), list.getParent().getSearchInput(), getChildren()).size();
        int allChildren = 0;
        for (Mod child : children) {
            if (!child.isHidden() && (ModMenu.getConfig().showLibraries || !child.getBadges().contains(ModBadge.LIBRARY))) {
                allChildren++;
            }
        }

        String str = (shownChildren == allChildren) ? String.valueOf(shownChildren) : (shownChildren + "/" + allChildren);
        int childrenWidth = font.getStringWidth(str) - 1;
        if (childrenBadgeWidth < childrenWidth + 4) {
            childrenBadgeWidth = childrenWidth + 4;
        }

        int iconSize = ModMenu.getConfig().compactList ? COMPACT_ICON_SIZE : FULL_ICON_SIZE;
        int childrenBadgeX = x + iconSize - childrenBadgeWidth;
        int childrenBadgeY = y + iconSize - childrenBadgeHeight;
        int childrenOutlineColor = 0xff107454;
        int childrenFillColor = 0xff093929;

        Gui.drawRect(childrenBadgeX + 1, childrenBadgeY, childrenBadgeX + childrenBadgeWidth - 1, childrenBadgeY + 1, childrenOutlineColor);
        Gui.drawRect(childrenBadgeX, childrenBadgeY + 1, childrenBadgeX + 1, childrenBadgeY + childrenBadgeHeight - 1, childrenOutlineColor);
        Gui.drawRect(childrenBadgeX + childrenBadgeWidth - 1, childrenBadgeY + 1, childrenBadgeX + childrenBadgeWidth, childrenBadgeY + childrenBadgeHeight - 1, childrenOutlineColor);
        Gui.drawRect(childrenBadgeX + 1, childrenBadgeY + 1, childrenBadgeX + childrenBadgeWidth - 1, childrenBadgeY + childrenBadgeHeight - 1, childrenFillColor);
        Gui.drawRect(childrenBadgeX + 1, childrenBadgeY + childrenBadgeHeight - 1, childrenBadgeX + childrenBadgeWidth - 1, childrenBadgeY + childrenBadgeHeight, childrenOutlineColor);

        font.drawString(str, (int) (childrenBadgeX + (float) childrenBadgeWidth / 2 - (float) childrenWidth / 2), childrenBadgeY + 1, 0xCACACA, false);

        this.hoveringIcon = mouseX >= x && mouseX <= x + iconSize && mouseY >= y && mouseY <= y + iconSize;
        if (this.hoveringIcon) {
            Gui.drawRect(x, y, x + iconSize, y + iconSize, 0xA0909090);
            int xOffset = list.getParent().showModChildren.contains(getMod().getId()) ? 32 : 0;
            int yOffset = hoveringIcon ? 32 : 0;
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableBlend();
            client.getTextureManager().bindTexture(PARENT_MOD_TEXTURE);
            Gui.drawScaledCustomSizeModalRect(x, y, (float) xOffset, (float) yOffset, 32, 32, iconSize, iconSize, 256.0F, 256.0F);
            GlStateManager.disableBlend();
        }
    }

    @Override
    public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
        int iconSize = ModMenu.getConfig().compactList ? COMPACT_ICON_SIZE : FULL_ICON_SIZE;
        boolean quickConfigure = ModMenu.getConfig().quickConfigure;

        if (relativeX <= iconSize) {
            this.toggleChildren();
            return true;
        } else if (!quickConfigure && Minecraft.getSystemTime() - this.sinceLastClick < 250) {
            this.toggleChildren();
            return true;
        } else {
            return super.mousePressed(slotIndex, mouseX, mouseY, mouseEvent, relativeX, relativeY);
        }
    }

    public void toggleChildren() {
        String id = getMod().getId();
        if (list.getParent().showModChildren.contains(id)) {
            list.getParent().showModChildren.remove(id);
        } else {
            list.getParent().showModChildren.add(id);
        }
        list.filter(list.getParent().getSearchInput(), false, false);
    }

    public void setChildren(List<Mod> children) {
        this.children = children;
    }

    public void addChildren(List<Mod> children) {
        this.children.addAll(children);
    }

    public void addChildren(Mod... children) {
        this.children.addAll(Arrays.asList(children));
    }

    public List<Mod> getChildren() {
        return children != null ? children : new ArrayList<Mod>();
    }
}
