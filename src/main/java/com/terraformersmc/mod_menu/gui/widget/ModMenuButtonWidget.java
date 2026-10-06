package com.terraformersmc.mod_menu.gui.widget;

import com.terraformersmc.mod_menu.gui.ModsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class ModMenuButtonWidget extends GuiButton {
    private final GuiScreen screen;

    public ModMenuButtonWidget(int buttonId, int x, int y, int width, int height, String text, GuiScreen screen) {
        super(buttonId, x, y, width, height, text);
        this.screen = screen;
    }

    public void onClick() {
        Minecraft.getMinecraft().displayGuiScreen(new ModsScreen(screen));
    }
}
