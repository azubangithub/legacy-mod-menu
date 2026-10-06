package com.terraformersmc.mod_menu.gui.widget.entries;

import com.terraformersmc.mod_menu.ModMenu;
import com.terraformersmc.mod_menu.gui.widget.ModListWidget;
import com.terraformersmc.mod_menu.util.DrawingUtil;
import com.terraformersmc.mod_menu.util.mod.Mod;
import com.terraformersmc.mod_menu.util.mod.ModBadgeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;

import java.awt.Dimension;

public class ModListEntry implements GuiListExtended.IGuiListEntry {
    /** Vanilla unknown pack texture — guaranteed to exist in the MC jar (matches 1.20.1 version). */
    public static final ResourceLocation UNKNOWN_ICON = new ResourceLocation("textures/misc/unknown_pack.png");
    private static final ResourceLocation MOD_CONFIGURATION_ICON = new ResourceLocation(ModMenu.MOD_ID, "textures/gui/mod_configuration.png");

    protected final Minecraft client;
    public final Mod mod;
    protected final ModListWidget list;
    protected Tuple<ResourceLocation, Dimension> iconLocation;
    /** True when iconLocation points to the unknown/placeholder icon, so we retry for online fetches */
    protected boolean iconIsPlaceholder = false;
    /** Frame counter for throttling placeholder icon re-resolution (avoids re-resolving every frame) */
    private int placeholderRetryCounter = 0;
    private static final int PLACEHOLDER_RETRY_INTERVAL = 20; // ~1 second at 20 FPS
    public static final int FULL_ICON_SIZE = 32;
    public static final int COMPACT_ICON_SIZE = 19;
    protected long sinceLastClick;

    public ModListEntry(Mod mod, ModListWidget list) {
        this.mod = mod;
        this.list = list;
        this.client = Minecraft.getMinecraft();
    }

    @Override
    public void updatePosition(int slotIndex, int x, int y, float partialTicks) {
    }

    @Override
    public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
        x += getXOffset();
        listWidth -= getXOffset();
        int iconSize = ModMenu.getConfig().compactList ? COMPACT_ICON_SIZE : FULL_ICON_SIZE;
        String modId = mod.getId();

        if ("java".equals(modId)) {
            DrawingUtil.drawRandomVersionBackground(mod, x, y, iconSize, iconSize);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        Tuple<ResourceLocation, Dimension> tex = getIconTexture();
        if (tex != null && tex.getFirst() != null) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.enableTexture2D();
            client.getTextureManager().bindTexture(tex.getFirst());
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0f, 0f, iconSize, iconSize, iconSize, iconSize);
            GlStateManager.disableBlend();
        }

        // Reset color so subsequent text draws are unaffected
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        String name = mod.getTranslatedName();
        int maxNameWidth = listWidth - iconSize - 6;
        String trimmedName = client.fontRenderer.trimStringToWidth(name, maxNameWidth);
        client.fontRenderer.drawString(trimmedName, x + iconSize + 4, y + 1, 0xFFFFFF);

        if (!ModMenu.getConfig().hideBadges) {
            int badgeX = x + iconSize + 4 + client.fontRenderer.getStringWidth(trimmedName) + 4;
            new ModBadgeRenderer(badgeX, y, x + listWidth, mod, list.getParent()).draw();
        }

        if (!ModMenu.getConfig().compactList) {
            String summary = mod.getSummary();
            DrawingUtil.drawWrappedString(summary, x + iconSize + 4, y + client.fontRenderer.FONT_HEIGHT + 2, listWidth - iconSize - 7, 2, 0x808080);
        } else {
            DrawingUtil.drawWrappedString(mod.getPrefixedVersion(), x + iconSize + 4, y + client.fontRenderer.FONT_HEIGHT + 2, listWidth - iconSize - 7, 2, 0x808080);
        }

        // Quick configure button
        if (!(this instanceof ParentEntry) && !(this instanceof ChildParentEntry) && ModMenu.getConfig().quickConfigure &&
                list.getParent().getModHasConfigScreen(mod.getContainer())) {
            boolean hovered = mouseX >= x && mouseX <= x + listWidth && mouseY >= y && mouseY <= y + slotHeight;
            if (this.client.gameSettings.touchscreen || hovered) {
                Gui.drawRect(x, y, x + iconSize, y + iconSize, -1601138544);
                boolean hoveringIcon = mouseX >= x && mouseX < x + iconSize && mouseY >= y && mouseY < y + iconSize;
                int v = hoveringIcon ? iconSize : 0;
                final int textureSize = ModMenu.getConfig().compactList ? (int) (256 / (FULL_ICON_SIZE / (double) COMPACT_ICON_SIZE)) : 256;
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                client.getTextureManager().bindTexture(MOD_CONFIGURATION_ICON);
                Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, (float) v, iconSize, iconSize, (float) textureSize, (float) textureSize);
                GlStateManager.disableBlend();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    @Override
    public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
        list.select(this);
        int iconSize = ModMenu.getConfig().compactList ? COMPACT_ICON_SIZE : FULL_ICON_SIZE;

        if (ModMenu.getConfig().quickConfigure && list.getParent().getModHasConfigScreen(this.mod.getContainer())) {
            long now = Minecraft.getSystemTime();
            if (relativeX <= iconSize + getXOffset()) {
                this.openConfig();
                return true;
            } else if (now - this.sinceLastClick < 250) {
                this.openConfig();
                return true;
            }
            this.sinceLastClick = now;
        }
        return true;
    }

    @Override
    public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
    }

    public void openConfig() {
        if (mod.getContainer() != null) {
            this.list.getParent().safelyOpenConfigScreen(mod.getContainer());
        }
    }

    public Mod getMod() {
        return mod;
    }

    public Tuple<ResourceLocation, Dimension> getIconTexture() {
        // First call: try to resolve the icon
        if (this.iconLocation == null) {
            resolveIcon();
        }
        // Subsequent calls for placeholders: throttle re-resolution to avoid
        // calling getIcon() every single frame (only retry every PLACEHOLDER_RETRY_INTERVAL frames)
        else if (this.iconIsPlaceholder && ++placeholderRetryCounter >= PLACEHOLDER_RETRY_INTERVAL) {
            placeholderRetryCounter = 0;
            resolveIcon();
        }
        return iconLocation;
    }

    private void resolveIcon() {
        Tuple<DynamicTexture, Dimension> icon = mod.getIcon(list.getIconHandler(), 64, false);
        if (icon != null && icon.getFirst() != null) {
            ResourceLocation loc = new ResourceLocation(ModMenu.MOD_ID, mod.getId() + "_icon");
            client.getTextureManager().loadTexture(loc, icon.getFirst());
            this.iconLocation = new Tuple<ResourceLocation, Dimension>(loc, icon.getSecond());
            this.iconIsPlaceholder = false;
        } else {
            // Use the vanilla unknown pack texture — it's auto-loaded by the texture manager
            // on first bindTexture() call, so no manual DynamicTexture registration is needed.
            this.iconLocation = new Tuple<ResourceLocation, Dimension>(UNKNOWN_ICON, new Dimension(32, 32));
            this.iconIsPlaceholder = true;
        }
    }

    public int getXOffset() {
        return 0;
    }

    @Override
    public String toString() {
        return "ModListEntry{mod_id=\"" + getMod().getId() + "\"}";
    }
}
