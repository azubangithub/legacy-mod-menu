package com.terraformersmc.mod_menu.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class GuiImageButton extends GuiButton {
    private final ResourceLocation texture;
    private final int textureWidth;
    private final int textureHeight;
    private boolean active = false;

    public GuiImageButton(int buttonId, int x, int y, int width, int height, ResourceLocation texture, int textureWidth, int textureHeight) {
        super(buttonId, x, y, width, height, "");
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    public GuiImageButton(int buttonId, int x, int y, int width, int height, ResourceLocation texture) {
        this(buttonId, x, y, width, height, texture, 32, 64);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return this.active;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;
        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

        mc.getTextureManager().bindTexture(this.texture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        int v = 0;
        if (!this.enabled) {
            v = this.height * 2;
        } else if (this.hovered) {
            v = this.height;
        } else if (this.active) {
            v = this.height * 2;
        } else {
            v = 0;
        }

        drawModalRectWithCustomSizedTexture(this.x, this.y, 0.0F, (float) v, this.width, this.height, (float) this.textureWidth, (float) this.textureHeight);
        GlStateManager.disableBlend();
    }
}
