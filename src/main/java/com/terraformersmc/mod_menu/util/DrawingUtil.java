package com.terraformersmc.mod_menu.util;

import com.terraformersmc.mod_menu.ModMenu;
import com.terraformersmc.mod_menu.util.mod.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Random;

@SideOnly(Side.CLIENT)
public class DrawingUtil {
    private static final Minecraft CLIENT = Minecraft.getMinecraft();
    private static final ResourceLocation HEADER_SEPARATOR = new ResourceLocation("mod_menu", "textures/gui/header_separator.png");
    private static final ResourceLocation FOOTER_SEPARATOR = new ResourceLocation("mod_menu", "textures/gui/footer_separator.png");

    public static void drawRandomVersionBackground(
            Mod mod,
            int x,
            int y,
            int width,
            int height
    ) {
        int seed = mod.getName().hashCode() + mod.getVersion().hashCode();
        Random random = new Random(seed);
        int color = 0xFF000000 | MathHelper.hsvToRGB(random.nextFloat(), 0.7f + random.nextFloat() * 0.1f, 0.9f);
        if (!ModMenu.getConfig().randomJavaColors) {
            color = 0xFFDD5656;
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
        Gui.drawRect(x, y, x + width, y + height, color);
    }

    public static void drawWrappedString(
            String string,
            int x,
            int y,
            int wrapWidth,
            int lines,
            int color
    ) {
        if (string == null || string.isEmpty()) {
            return;
        }
        string = string.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\r", "\n");
        while (string.endsWith("\n")) {
            string = string.substring(0, string.length() - 1);
        }
        List<String> strings = CLIENT.fontRenderer.listFormattedStringToWidth(string, wrapWidth);
        for (int i = 0; i < strings.size(); i++) {
            if (i >= lines) {
                break;
            }
            String line = strings.get(i);
            if (i == lines - 1 && strings.size() > lines) {
                line = line + "...";
            }
            CLIENT.fontRenderer.drawStringWithShadow(line, x, y + i * CLIENT.fontRenderer.FONT_HEIGHT, color);
        }
    }

    public static void drawBadge(
            int x,
            int y,
            int tagWidth,
            String text,
            int outlineColor,
            int fillColor,
            int textColor
    ) {
        int fontHeight = CLIENT.fontRenderer.FONT_HEIGHT;
        Gui.drawRect(x + 1, y - 1, x + tagWidth, y, outlineColor);
        Gui.drawRect(x, y, x + 1, y + fontHeight, outlineColor);
        Gui.drawRect(x + 1, y + fontHeight, x + tagWidth, y + fontHeight + 1, outlineColor);
        Gui.drawRect(x + tagWidth, y, x + tagWidth + 1, y + fontHeight, outlineColor);
        Gui.drawRect(x + 1, y, x + tagWidth, y + fontHeight, fillColor);

        int textX = (int) (x + 1 + (tagWidth - CLIENT.fontRenderer.getStringWidth(text)) / 2.0F);
        CLIENT.fontRenderer.drawString(text, textX, y + 1, textColor, false);
    }

    public static void renderListSeparators(int left, int right, int top, int bottom) {
        int width = right - left;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE
        );
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        float uMax = (float) width / 32.0F;

        // Header separator (top - 2 to top)
        CLIENT.getTextureManager().bindTexture(HEADER_SEPARATOR);
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(left, top, 0.0D).tex(0.0D, 1.0D).endVertex();
        buffer.pos(right, top, 0.0D).tex((double) uMax, 1.0D).endVertex();
        buffer.pos(right, top - 2, 0.0D).tex((double) uMax, 0.0D).endVertex();
        buffer.pos(left, top - 2, 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();

        // Footer separator (bottom to bottom + 2)
        CLIENT.getTextureManager().bindTexture(FOOTER_SEPARATOR);
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(left, bottom + 2, 0.0D).tex(0.0D, 1.0D).endVertex();
        buffer.pos(right, bottom + 2, 0.0D).tex((double) uMax, 1.0D).endVertex();
        buffer.pos(right, bottom, 0.0D).tex((double) uMax, 0.0D).endVertex();
        buffer.pos(left, bottom, 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.disableBlend();
    }
}
