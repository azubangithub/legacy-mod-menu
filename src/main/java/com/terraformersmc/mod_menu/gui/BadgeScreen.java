package com.terraformersmc.mod_menu.gui;

import com.terraformersmc.mod_menu.ModMenu;
import com.terraformersmc.mod_menu.config.ModMenuConfig;
import com.terraformersmc.mod_menu.gui.widget.BadgeToogleButton;
import com.terraformersmc.mod_menu.util.DrawingUtil;
import com.terraformersmc.mod_menu.util.mod.Mod;
import com.terraformersmc.mod_menu.util.mod.ModBadge;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

import java.io.IOException;
import java.util.*;

public class BadgeScreen extends GuiScreen {
    private final Mod mod;
    private final int posX;
    private final GuiScreen parent;
    private final Map<Integer, Map.Entry<String, ModBadge>> buttonToBadge = new HashMap<Integer, Map.Entry<String, ModBadge>>();

    public BadgeScreen(Mod mod, int paneWidth, int searchBoxWidth, GuiScreen parent) {
        this(mod, paneWidth / 2 + searchBoxWidth / 2 - 20 / 2 + 26, parent);
    }

    public BadgeScreen(Mod mod, int posX, GuiScreen parent) {
        this.mod = mod;
        this.parent = parent;
        this.posX = posX;
    }

    @Override
    public void onGuiClosed() {
        ModMenu.getConfig().save();
        super.onGuiClosed();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.buttonToBadge.clear();

        // Close button (id 0)
        this.buttonList.add(new GuiButton(0, posX, 22, 20, 20, "X"));

        int i = 0;
        final int buttonX = posX - 14;
        int buttonId = 100;

        for (Map<String, ModBadge> badgeMap : ModBadge.BADGES) {
            for (Map.Entry<String, ModBadge> badgeEntry : badgeMap.entrySet()) {
                final ModBadge badge = badgeEntry.getValue();
                final String badgeKey = badgeEntry.getKey();
                boolean hasBadge = mod.getBadges().contains(badge);

                final int currentId = buttonId++;
                buttonToBadge.put(currentId, badgeEntry);

                BadgeToogleButton toggleBtn = new BadgeToogleButton(currentId, buttonX, 43 + 12 * i, 11, 11, hasBadge, new Runnable() {
                    @Override
                    public void run() {
                        ModMenuConfig config = ModMenu.getConfig();
                        if (mod.getBadges().contains(badge)) {
                            mod.getBadges().remove(badge);
                            Set<String> set = config.modBadges.get(mod.getId());
                            if (set != null) {
                                set.remove(badgeKey);
                            }
                            if (mod.getBadgeNames().contains(badgeKey)) {
                                if (!config.disabledModBadges.containsKey(mod.getId())) {
                                    config.disabledModBadges.put(mod.getId(), new LinkedHashSet<String>());
                                }
                                config.disabledModBadges.get(mod.getId()).add(badgeKey);
                            }
                        } else {
                            mod.getBadges().add(badge);
                            Set<String> disabled = config.disabledModBadges.get(mod.getId());
                            if (disabled != null && disabled.contains(badgeKey)) {
                                disabled.remove(badgeKey);
                            } else {
                                if (!config.modBadges.containsKey(mod.getId())) {
                                    config.modBadges.put(mod.getId(), new LinkedHashSet<String>());
                                }
                                config.modBadges.get(mod.getId()).add(badgeKey);
                            }
                        }
                    }
                });

                this.buttonList.add(toggleBtn);
                i++;
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            this.mc.displayGuiScreen(parent);
            return;
        }

        if (button instanceof BadgeToogleButton) {
            BadgeToogleButton toggleBtn = (BadgeToogleButton) button;
            toggleBtn.onClick();
            toggleBtn.toggle();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        int i = 0;
        for (Map<String, ModBadge> badges : ModBadge.BADGES) {
            for (Map.Entry<String, ModBadge> mapEntry : badges.entrySet()) {
                ModBadge badge = mapEntry.getValue();
                String text = badge.getDisplayName();
                int badgeWidth = fontRenderer.getStringWidth(text) + 6;
                DrawingUtil.drawBadge(posX, 43 + 12 * i, badgeWidth, text,
                        badge.getOutlineColor(), badge.getFillColor(), badge.getTextColor());
                i++;
            }
        }
    }
}
