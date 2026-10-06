package com.terraformersmc.mod_menu.gui.widget;

import com.terraformersmc.mod_menu.ModMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class BadgeToogleButton extends GuiButton {
    private static final ResourceLocation BADGE_TOGGLE_TEXTURE =
            new ResourceLocation(ModMenu.MOD_ID, "textures/gui/badge_toggle_button.png");
    private boolean hasBadge;
    private final Runnable onPress;

    public BadgeToogleButton(int buttonId, int x, int y, int width, int height, boolean hasBadge, Runnable onPress) {
        super(buttonId, x, y, width, height, "");
        this.hasBadge = hasBadge;
        this.onPress = onPress;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;
        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

        mc.getTextureManager().bindTexture(BADGE_TOGGLE_TEXTURE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();

        int u = hasBadge ? 11 : 0;
        int v = 0;
        if (!this.enabled) {
            v = 22;
        } else if (this.hovered) {
            v = 11;
        }

        drawModalRectWithCustomSizedTexture(this.x, this.y, u, v, this.width, this.height, 22, 22);
        GlStateManager.disableBlend();
    }

    public void onClick() {
        if (onPress != null) {
            onPress.run();
        }
    }

    public void toggle() {
        this.hasBadge = !this.hasBadge;
    }

    public boolean hasBadge() {
        return this.hasBadge;
    }
}
