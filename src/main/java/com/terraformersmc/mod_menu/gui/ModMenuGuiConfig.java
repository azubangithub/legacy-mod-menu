package com.terraformersmc.mod_menu.gui;

import com.terraformersmc.mod_menu.ModMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.ArrayList;
import java.util.List;

public class ModMenuGuiConfig extends GuiConfig {
    public ModMenuGuiConfig(GuiScreen parent) {
        super(parent,
                getConfigElements(),
                ModMenu.MOD_ID,
                false,
                false,
                "Legacy Mod Menu");
    }

    private static List<IConfigElement> getConfigElements() {
        List<IConfigElement> list = new ArrayList<IConfigElement>();
        Configuration cfg = ModMenu.getConfig().getConfiguration();
        for (String cat : cfg.getCategoryNames()) {
            list.add(new ConfigElement(cfg.getCategory(cat)));
        }
        return list;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Configuration cfg = ModMenu.getConfig().getConfiguration();
        if (cfg.hasChanged()) {
            cfg.save();
        }
        ModMenu.getConfig().readConfig();
        if (!ModMenu.MODS.isEmpty()) {
            ModMenu.initModsList();
        }
    }
}
