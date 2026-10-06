package com.terraformersmc.mod_menu.util.mod;

import com.terraformersmc.mod_menu.gui.ModsScreen;
import com.terraformersmc.mod_menu.util.DrawingUtil;
import net.minecraft.client.Minecraft;

import java.util.Set;

public class ModBadgeRenderer {
    protected int startX, startY, badgeX, badgeY, badgeMax;
    protected Mod mod;
    protected Minecraft client;
    protected final ModsScreen screen;

    public ModBadgeRenderer(int startX, int startY, int endX, Mod mod, ModsScreen screen) {
        this.startX = startX;
        this.startY = startY;
        this.badgeMax = endX;
        this.mod = mod;
        this.screen = screen;
        this.client = Minecraft.getMinecraft();
    }

    public void draw() {
        this.badgeX = startX;
        this.badgeY = startY;
        Set<ModBadge> badges = mod.getBadges();
        for (ModBadge badge : badges) {
            drawBadge(badge);
        }
    }

    public void drawBadge(ModBadge badge) {
        String text = badge.getDisplayName();
        int width = client.fontRenderer.getStringWidth(text) + 6;
        if (badgeX + width < badgeMax) {
            DrawingUtil.drawBadge(badgeX, badgeY, width, text, badge.getOutlineColor(), badge.getFillColor(), badge.getTextColor());
            badgeX += width + 3;
        }
    }

    public Mod getMod() {
        return mod;
    }
}
