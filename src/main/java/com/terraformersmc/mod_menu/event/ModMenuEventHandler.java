package com.terraformersmc.mod_menu.event;

import com.terraformersmc.mod_menu.ModMenu;
import com.terraformersmc.mod_menu.config.ModMenuConfig;
import com.terraformersmc.mod_menu.gui.ModsScreen;
import com.terraformersmc.mod_menu.gui.widget.UpdateCheckerTexturedButtonWidget;
import com.terraformersmc.mod_menu.util.CompatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.List;

public class ModMenuEventHandler {
    public static final ResourceLocation MODS_BUTTON_TEXTURE = new ResourceLocation(ModMenu.MOD_ID, "textures/gui/mods_button.png");
    private static KeyBinding MENU_KEY_BIND;

    public static void initKeyBinding() {
        MENU_KEY_BIND = new KeyBinding("key.modmenu.open_menu", Keyboard.KEY_NONE, "key.categories.misc");
        ClientRegistry.registerKeyBinding(MENU_KEY_BIND);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && MENU_KEY_BIND != null) {
            while (MENU_KEY_BIND.isPressed()) {
                Minecraft mc = Minecraft.getMinecraft();
                mc.displayGuiScreen(new ModsScreen(mc.currentScreen));
            }
        }
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen screen = event.getGui();

        if (screen instanceof GuiMainMenu && ModMenu.getConfig().modifyTitleScreen && !CompatUtils.isCustomMenu()) {
            handleTitleScreen(screen, event.getButtonList());
        } else if (screen instanceof GuiIngameMenu && ModMenu.getConfig().modifyGameMenu) {
            handlePauseScreen(screen, event.getButtonList());
        }
    }

    private void handleTitleScreen(GuiScreen screen, List<GuiButton> buttons) {
        GuiButton modsButton = null;
        GuiButton realmsButton = null;

        for (GuiButton btn : buttons) {
            if (btn.id == 6) { // Forge Mods button
                modsButton = btn;
            } else if (btn.id == 14) { // Realms button
                realmsButton = btn;
            }
        }

        if (modsButton != null) {
            modsButton.displayString = ModMenu.createModsButtonText(true);
            ModMenuConfig.TitleMenuButtonStyle style = ModMenu.getConfig().modsButtonStyle;

            if (style == ModMenuConfig.TitleMenuButtonStyle.CLASSIC) {
                // Below Realms: Realms is full-width (200), Mods is full-width (200) below it
                if (realmsButton != null) {
                    realmsButton.x = screen.width / 2 - 100;
                    realmsButton.width = 200;
                    realmsButton.y -= 12;

                    modsButton.x = screen.width / 2 - 100;
                    modsButton.width = 200;
                    modsButton.y = realmsButton.y + 24;

                    for (GuiButton btn : buttons) {
                        if (btn.id == 1 || btn.id == 2) { // Singleplayer, Multiplayer
                            btn.y -= 12;
                        } else if (btn.id == 0 || btn.id == 4 || btn.id == 5) { // Options, Quit, Language
                            btn.y += 12;
                        }
                    }
                } else {
                    modsButton.x = screen.width / 2 - 100;
                    modsButton.width = 200;
                }
            } else if (style == ModMenuConfig.TitleMenuButtonStyle.SHRINK) {
                // Adjacent: Realms on left (98), Mods on right (98)
                if (realmsButton != null) {
                    realmsButton.width = 98;
                    realmsButton.x = screen.width / 2 - 100;
                    modsButton.width = 98;
                    modsButton.x = screen.width / 2 + 2;
                    modsButton.y = realmsButton.y;
                } else {
                    modsButton.x = screen.width / 2 - 100;
                    modsButton.width = 200;
                }
            } else if (style == ModMenuConfig.TitleMenuButtonStyle.SHRINK_LEFT) {
                // Adjacent on left: Mods on left (98), Realms on right (98)
                if (realmsButton != null) {
                    modsButton.width = 98;
                    modsButton.x = screen.width / 2 - 100;
                    realmsButton.width = 98;
                    realmsButton.x = screen.width / 2 + 2;
                    modsButton.y = realmsButton.y;
                } else {
                    modsButton.x = screen.width / 2 - 100;
                    modsButton.width = 200;
                }
            } else if (style == ModMenuConfig.TitleMenuButtonStyle.REPLACE_REALMS) {
                // Replace Realms: Mods takes Realms position with full width (200), Realms is removed
                if (realmsButton != null) {
                    modsButton.y = realmsButton.y;
                    buttons.remove(realmsButton);
                }
                modsButton.x = screen.width / 2 - 100;
                modsButton.width = 200;
            } else if (style == ModMenuConfig.TitleMenuButtonStyle.ICON) {
                // Icon: Realms is full-width (200), centered; Mods is 20x20 icon next to Realms at +104
                if (realmsButton != null) {
                    realmsButton.x = screen.width / 2 - 100;
                    realmsButton.width = 200;
                }
                modsButton.visible = false;
                buttons.remove(modsButton);
                int buttonY = (realmsButton != null) ? realmsButton.y : (screen.height / 4 + 48 + 48);
                buttons.add(new UpdateCheckerTexturedButtonWidget(6, screen.width / 2 + 104, buttonY, 20, 20,
                        0, 0, 20, MODS_BUTTON_TEXTURE, 32, 64, screen));
            }
        }
    }

    private void handlePauseScreen(GuiScreen screen, List<GuiButton> buttons) {
        GuiButton modsButton = null;
        GuiButton optionsButton = null;
        for (GuiButton btn : buttons) {
            if (btn.id == 12) { // Forge Mod Options in pause menu
                modsButton = btn;
            } else if (btn.id == 0) { // Options button
                optionsButton = btn;
            }
        }

        ModMenuConfig.GameMenuButtonStyle style = ModMenu.getConfig().gameMenuStyle;

        if (modsButton != null) {
            modsButton.displayString = ModMenu.createModsButtonText(false);

            if (style == ModMenuConfig.GameMenuButtonStyle.ICON) {
                if (optionsButton != null) {
                    optionsButton.width = 200;
                }
                modsButton.visible = false;
                buttons.remove(modsButton);
                int buttonY = (optionsButton != null) ? optionsButton.y : (screen.height / 4 + 96 + -16);
                buttons.add(new UpdateCheckerTexturedButtonWidget(12, screen.width / 2 + 104, buttonY, 20, 20,
                        0, 0, 20, MODS_BUTTON_TEXTURE, 32, 64, screen));
            } else if (style == ModMenuConfig.GameMenuButtonStyle.INSERT) {
                if (optionsButton != null) {
                    optionsButton.width = 200;
                }
                modsButton.x = screen.width / 2 - 100;
                modsButton.width = 200;
            }
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        GuiButton btn = event.getButton();
        if (btn == null) return;

        if (event.getGui() instanceof GuiMainMenu) {
            if (btn.id == 6) { // Mods button on Main Menu
                event.setCanceled(true);
                Minecraft.getMinecraft().displayGuiScreen(new ModsScreen(event.getGui()));
            }
        } else if (event.getGui() instanceof GuiIngameMenu) {
            if (btn.id == 12) { // Mod Options in Pause Menu
                event.setCanceled(true);
                Minecraft.getMinecraft().displayGuiScreen(new ModsScreen(event.getGui()));
            }
        }
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (ModMenu.MOD_ID.equals(event.getModID())) {
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
}
