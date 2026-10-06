package com.terraformersmc.mod_menu.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class UpdateAvailableBadge {
    private static final ResourceLocation UPDATE_ICON = new ResourceLocation("realms", "textures/gui/realms/trial_icon.png");

    public static void renderBadge(int x, int y) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(UPDATE_ICON);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        int animOffset = 0;
        if ((Minecraft.getSystemTime() / 800L & 1L) == 1L) {
            animOffset = 8;
        }
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0f, animOffset, 8, 8, 8, 16);
        GlStateManager.disableBlend();
    }
}
