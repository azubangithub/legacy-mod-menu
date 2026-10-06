package com.terraformersmc.mod_menu.gui.widget;

import com.terraformersmc.mod_menu.gui.ModsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class UpdateCheckerTexturedButtonWidget extends GuiButton {
    private final ResourceLocation texture;
    private final int u;
    private final int v;
    private final int hoveredVOffset;
    private final int texWidth;
    private final int texHeight;
    private final GuiScreen parentScreen;

    public UpdateCheckerTexturedButtonWidget(int buttonId, int x, int y, int width, int height,
                                            int u, int v, int hoveredVOffset,
                                            ResourceLocation texture, int texWidth, int texHeight,
                                            GuiScreen parentScreen) {
        super(buttonId, x, y, width, height, "");
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.hoveredVOffset = hoveredVOffset;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.parentScreen = parentScreen;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;
        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

        mc.getTextureManager().bindTexture(texture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();

        int currentV = this.v;
        if (this.hovered) {
            currentV += this.hoveredVOffset;
        }

        drawModalRectWithCustomSizedTexture(this.x, this.y, this.u, currentV, this.width, this.height, this.texWidth, this.texHeight);
        GlStateManager.disableBlend();
    }

    public void onClick() {
        Minecraft.getMinecraft().displayGuiScreen(new ModsScreen(parentScreen));
    }
}
