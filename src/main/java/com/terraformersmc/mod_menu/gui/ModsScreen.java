package com.terraformersmc.mod_menu.gui;

import com.terraformersmc.mod_menu.ModMenu;
import com.terraformersmc.mod_menu.config.ModMenuConfig;
import com.terraformersmc.mod_menu.gui.widget.DescriptionListWidget;
import com.terraformersmc.mod_menu.gui.widget.GuiImageButton;
import com.terraformersmc.mod_menu.gui.widget.ModListWidget;
import com.terraformersmc.mod_menu.gui.widget.entries.ModListEntry;
import com.terraformersmc.mod_menu.util.DrawingUtil;
import com.terraformersmc.mod_menu.util.ModMenuScreenTexts;
import com.terraformersmc.mod_menu.util.TranslationUtil;
import com.terraformersmc.mod_menu.util.mod.Mod;
import com.terraformersmc.mod_menu.util.mod.ModBadge;
import com.terraformersmc.mod_menu.util.mod.ModBadgeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraftforge.fml.client.GuiModList;
import net.minecraftforge.fml.common.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Desktop;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class ModsScreen extends GuiScreen {
    private static final Logger LOGGER = LogManager.getLogger("Mod Menu | ModsScreen");
    private static final ResourceLocation FILTERS_BUTTON_LOCATION = new ResourceLocation(ModMenu.MOD_ID, "textures/gui/filters_button.png");
    private static final ResourceLocation CONFIGURE_BUTTON_LOCATION = new ResourceLocation(ModMenu.MOD_ID, "textures/gui/configure_button.png");
    public static final ResourceLocation BADGE_BUTTON_LOCATION = new ResourceLocation(ModMenu.MOD_ID, "textures/gui/badge_button.png");

    private final GuiScreen previousScreen;
    private GuiTextField searchBox;
    private ModListWidget modList;
    private DescriptionListWidget descriptionListWidget;
    private ModListEntry selected;

    private int paneWidth;
    private int rightPaneX;
    private int searchBoxX;
    private int searchBoxWidth;
    private boolean filterOptionsShown = false;

    private GuiImageButton configureButton;
    private GuiButton websiteButton;
    private GuiButton issuesButton;
    private GuiImageButton filtersButton;
    private GuiButton sortingButton;
    private GuiButton librariesButton;
    private GuiImageButton badgeButton;

    public final Set<String> showModChildren = new HashSet<String>();

    public ModsScreen(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        String prevSelectedId = (this.selected != null && this.selected.getMod() != null) ? this.selected.getMod().getId() : null;
        int prevScroll = (this.modList != null) ? this.modList.getAmountScrolled() : 0;

        boolean hideTop = ModMenu.getConfig().hideScreenTop;
        int paneY = hideTop ? 30 : (ModMenu.getConfig().configMode ? 48 : 48 + 19);
        int rightPaneY = hideTop ? 10 : 48;

        paneWidth = this.width / 2 - 8;
        rightPaneX = width - paneWidth;

        int filtersButtonSize = (ModMenu.getConfig().configMode ? 0 : 22) + (!ModMenu.getConfig().hideBadgeButtons ? 22 : 0);
        int searchWidthMax = paneWidth - 32 - filtersButtonSize;
        searchBoxWidth = ModMenu.getConfig().configMode ? Math.min(200, searchWidthMax) : searchWidthMax;
        searchBoxX = paneWidth / 2 - searchBoxWidth / 2 - filtersButtonSize / 2;

        String prevText = (this.searchBox != null) ? this.searchBox.getText() : "";
        this.searchBox = new GuiTextField(10, this.fontRenderer, searchBoxX, 22, searchBoxWidth, 20);
        this.searchBox.setText(prevText);
        this.searchBox.setVisible(!hideTop);

        this.modList = new ModListWidget(this.mc, paneWidth, this.height, paneY, this.height - 36, ModMenu.getConfig().compactList ? 23 : 36, this);
        this.modList.setSlotXBoundsFromLeft(0);
        if (prevSelectedId != null) {
            this.modList.setSelectedModId(prevSelectedId);
        }

        this.descriptionListWidget = new DescriptionListWidget(this.mc, paneWidth, this.height, rightPaneY + 60, this.height - 36, fontRenderer.FONT_HEIGHT + 2, this);
        this.descriptionListWidget.setSlotXBoundsFromLeft(rightPaneX);

        // Buttons
        int bottomButtonY = this.height - 28;
        this.buttonList.add(new GuiButton(0, this.width / 2 + 4, bottomButtonY, 150, 20, I18n.format("gui.done")));
        this.buttonList.add(new GuiButton(1, this.width / 2 - 154, bottomButtonY, 150, 20, I18n.format("modmenu.modsFolder")));

        // Configure button (ID 2) — placed at far right of right panel
        configureButton = new GuiImageButton(2, this.width - 24, rightPaneY, 20, 20, CONFIGURE_BUTTON_LOCATION);
        configureButton.visible = false;
        this.buttonList.add(configureButton);

        // Website button (ID 3)
        int urlButtonWidth = Math.min(paneWidth / 2 - 2, 200);
        websiteButton = new GuiButton(3, rightPaneX + (paneWidth / 2 - urlButtonWidth) / 2, rightPaneY + 36, urlButtonWidth, 20, I18n.format("modmenu.website"));
        websiteButton.visible = false;
        this.buttonList.add(websiteButton);

        // Issues button (ID 4)
        issuesButton = new GuiButton(4, rightPaneX + paneWidth / 2 + (paneWidth / 2 - urlButtonWidth) / 2, rightPaneY + 36, urlButtonWidth, 20, I18n.format("modmenu.issues"));
        issuesButton.visible = false;
        this.buttonList.add(issuesButton);

        // Filters button (ID 5)
        filtersButton = new GuiImageButton(5, searchBoxX + searchBoxWidth + 2, 22, 20, 20, FILTERS_BUTTON_LOCATION);
        filtersButton.visible = !hideTop && !ModMenu.getConfig().configMode;
        filtersButton.setActive(filterOptionsShown);
        this.buttonList.add(filtersButton);

        // Badge button (ID 8)
        badgeButton = new GuiImageButton(8, searchBoxX + searchBoxWidth + 24, 22, 20, 20, BADGE_BUTTON_LOCATION);
        badgeButton.visible = !hideTop && !ModMenu.getConfig().hideBadgeButtons;
        this.buttonList.add(badgeButton);

        // Filter popup buttons (ID 6 Sorting, ID 7 Libraries)
        int sortingWidth = fontRenderer.getStringWidth(ModMenuScreenTexts.getSortingText()) + 20;
        int librariesWidth = fontRenderer.getStringWidth(ModMenuScreenTexts.getLibrariesText()) + 20;
        int filtersX = Math.max(0, searchBoxX);

        sortingButton = new GuiButton(6, filtersX, 45, sortingWidth, 20, ModMenuScreenTexts.getSortingText());
        sortingButton.visible = filterOptionsShown;
        this.buttonList.add(sortingButton);

        librariesButton = new GuiButton(7, filtersX + sortingWidth + 2, 45, librariesWidth, 20, ModMenuScreenTexts.getLibrariesText());
        librariesButton.visible = filterOptionsShown;
        this.buttonList.add(librariesButton);

        this.modList.reloadFilters();
        if (prevScroll != 0) {
            this.modList.scrollBy(prevScroll);
        }

        if (this.modList.getSelected() != null) {
            this.updateSelectedEntry(this.modList.getSelected());
        } else if (this.selected != null) {
            this.updateSelectedEntry(this.selected);
        } else if (!this.modList.getEntries().isEmpty()) {
            this.modList.select(this.modList.getEntries().get(0));
        }
    }

    @Override
    public void updateScreen() {
        this.searchBox.updateCursorCounter();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        if (this.modList != null && this.modList.isMouseOver(mouseX, mouseY)) {
            this.modList.handleMouseInput();
        } else if (this.descriptionListWidget != null && this.descriptionListWidget.isMouseOver(mouseX, mouseY)) {
            this.descriptionListWidget.handleMouseInput();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.searchBox != null) {
            this.searchBox.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (this.modList != null) {
            this.modList.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (this.descriptionListWidget != null) {
            this.descriptionListWidget.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (this.modList != null) {
            this.modList.mouseReleased(mouseX, mouseY, state);
        }
        if (this.descriptionListWidget != null) {
            this.descriptionListWidget.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.searchBox != null && this.searchBox.textboxKeyTyped(typedChar, keyCode)) {
            this.modList.filter(this.searchBox.getText(), false);
        } else if (keyCode == 1) { // ESC key
            this.mc.displayGuiScreen(this.previousScreen);
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) { // Done
            this.mc.displayGuiScreen(this.previousScreen);
        } else if (button.id == 1) { // Open mods folder
            File modsDir = new File(this.mc.gameDir, "mods");
            try {
                Desktop.getDesktop().open(modsDir);
            } catch (Throwable t) {
                LOGGER.error("Couldn't open mods directory", t);
            }
        } else if (button.id == 2) { // Configure
            if (selected != null && selected.getMod().getContainer() != null) {
                safelyOpenConfigScreen(selected.getMod().getContainer());
            }
        } else if (button.id == 3) { // Website
            if (selected != null) {
                if ("minecraft".equals(selected.getMod().getId())) {
                    openLink("https://aka.ms/snapshotfeedback");
                } else if (selected.getMod().getWebsite() != null) {
                    openLink(selected.getMod().getWebsite());
                }
            }
        } else if (button.id == 4) { // Issues
            if (selected != null) {
                if ("minecraft".equals(selected.getMod().getId())) {
                    openLink("https://aka.ms/snapshotbugs");
                } else if (selected.getMod().getIssueTracker() != null) {
                    openLink(selected.getMod().getIssueTracker());
                }
            }
        } else if (button.id == 5) { // Filter options toggle
            filterOptionsShown = !filterOptionsShown;
            if (filtersButton != null) {
                filtersButton.setActive(filterOptionsShown);
            }
            sortingButton.visible = filterOptionsShown;
            librariesButton.visible = filterOptionsShown;
        } else if (button.id == 6) { // Sorting toggle
            ModMenu.getConfig().sorting.cycleValue();
            sortingButton.displayString = ModMenuScreenTexts.getSortingText();
            this.modList.reloadFilters();
        } else if (button.id == 7) { // Libraries toggle
            ModMenu.getConfig().showLibraries = !ModMenu.getConfig().showLibraries;
            ModMenu.getConfig().save();
            librariesButton.displayString = ModMenuScreenTexts.getLibrariesText();
            this.modList.reloadFilters();
        } else if (button.id == 8) { // Badge editor
            if (selected != null) {
                this.mc.displayGuiScreen(new BadgeScreen(selected.getMod(), badgeButton.x, this));
            }
        }
    }

    public void updateSelectedEntry(ModListEntry entry) {
        this.selected = entry;
        if (entry != null) {
            this.descriptionListWidget.updateSelectedModIfRequired(entry.getMod());
            boolean hasConfig = getModHasConfigScreen(entry.getMod().getContainer());
            configureButton.visible = hasConfig;
            configureButton.enabled = hasConfig;

            boolean isMinecraft = "minecraft".equals(entry.getMod().getId());
            String feedback = I18n.hasKey("menu.sendFeedback") ? I18n.format("menu.sendFeedback") :
                    (I18n.hasKey("modmenu.sendFeedback") ? I18n.format("modmenu.sendFeedback") : "Send Feedback");
            String bugs = I18n.hasKey("menu.reportBugs") ? I18n.format("menu.reportBugs") :
                    (I18n.hasKey("modmenu.reportBugs") ? I18n.format("modmenu.reportBugs") : "Report Bugs");
            websiteButton.displayString = isMinecraft ? feedback : I18n.format("modmenu.website");
            issuesButton.displayString = isMinecraft ? bugs : I18n.format("modmenu.issues");

            websiteButton.visible = true;
            websiteButton.enabled = isMinecraft || (entry.getMod().getWebsite() != null && !entry.getMod().getWebsite().isEmpty());

            issuesButton.visible = true;
            issuesButton.enabled = isMinecraft || (entry.getMod().getIssueTracker() != null && !entry.getMod().getIssueTracker().isEmpty());
        } else {
            configureButton.visible = false;
            websiteButton.visible = false;
            issuesButton.visible = false;
        }
    }

    public ModListEntry getSelectedEntry() {
        return selected;
    }

    public String getSearchInput() {
        return this.searchBox != null ? this.searchBox.getText() : "";
    }

    public boolean getModHasConfigScreen(ModContainer container) {
        return ModMenu.hasConfigScreen(container);
    }

    public void safelyOpenConfigScreen(ModContainer container) {
        GuiScreen configScreen = ModMenu.getConfigScreen(container, this);
        if (configScreen != null) {
            this.mc.displayGuiScreen(configScreen);
        }
    }

    public void openLink(final String url) {
        this.mc.displayGuiScreen(new GuiConfirmOpenLink(new GuiYesNoCallback() {
            @Override
            public void confirmClicked(boolean result, int id) {
                if (result) {
                    try {
                        Desktop.getDesktop().browse(new URI(url));
                    } catch (Throwable t) {
                        LOGGER.error("Failed to browse to URI " + url, t);
                    }
                }
                mc.displayGuiScreen(ModsScreen.this);
            }
        }, url, 31102009, true));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        if (this.modList != null) {
            this.modList.drawScreen(mouseX, mouseY, partialTicks);
        }
        if (this.descriptionListWidget != null && this.selected != null) {
            this.descriptionListWidget.drawScreen(mouseX, mouseY, partialTicks);
        }

        // Draw title
        if (!ModMenu.getConfig().hideScreenTop) {
            String title = I18n.format("modmenu.title");
            drawCenteredString(fontRenderer, title, this.paneWidth / 2, 8, 0xFFFFFF);
            this.searchBox.drawTextBox();

            if (!ModMenu.getConfig().configMode && !filterOptionsShown) {
                int showingModTextY = 46;
                String fullModCount = computeModCountText(true);
                if (!ModMenu.getConfig().showLibraries ||
                        fontRenderer.getStringWidth(fullModCount) <= paneWidth - 5) {
                    fontRenderer.drawStringWithShadow(fullModCount, this.searchBoxX, showingModTextY + 6, 0xFFFFFF);
                } else {
                    fontRenderer.drawStringWithShadow(computeModCountText(false), this.searchBoxX, showingModTextY, 0xFFFFFF);
                    fontRenderer.drawStringWithShadow(computeLibraryCountText(), this.searchBoxX, showingModTextY + 11, 0xFFFFFF);
                }
            }
        }

        // Right pane header (selected mod icon, name, version, badges)
        if (selected != null) {
            int rightPaneY = ModMenu.getConfig().hideScreenTop ? 10 : 48;
            Mod mod = selected.getMod();

            if ("java".equals(mod.getId())) {
                DrawingUtil.drawRandomVersionBackground(mod, rightPaneX, rightPaneY, 32, 32);
            }

            // Mod icon / wide logo support
            int imageOffset = 32;
            int imageHeight = 32;
            Tuple<ResourceLocation, Dimension> iconTex = selected.getIconTexture();
            if (iconTex != null && iconTex.getFirst() != null) {
                if (iconTex.getSecond() != null) {
                    int w = iconTex.getSecond().width;
                    int h = iconTex.getSecond().height;
                    if (w > 0 && h > 0 && w > h) {
                        imageHeight = 32;
                        imageOffset = (int) (32.0 * w / h);
                        if (imageOffset > paneWidth / 2) {
                            imageOffset = paneWidth / 2;
                        }
                    }
                }
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.enableBlend();
                GlStateManager.enableTexture2D();
                GlStateManager.tryBlendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                mc.getTextureManager().bindTexture(iconTex.getFirst());
                Gui.drawModalRectWithCustomSizedTexture(rightPaneX, rightPaneY, 0f, 0f, imageOffset, imageHeight, imageOffset, imageHeight);
                GlStateManager.disableBlend();
            }

            imageOffset += 4;

            // Name and version
            String modName = mod.getTranslatedName();
            int nameWidth = fontRenderer.getStringWidth(modName);
            fontRenderer.drawStringWithShadow(modName, rightPaneX + imageOffset, rightPaneY + 1, 0xFFFFFF);

            String version = mod.getPrefixedVersion();
            fontRenderer.drawStringWithShadow(version, rightPaneX + imageOffset, rightPaneY + 12, 0x888888);

            // Authors line ("By ...")
            List<String> names = mod.getAuthors();
            if (!names.isEmpty()) {
                String authors = String.join(", ", names);
                String authorText = I18n.format("modmenu.authorPrefix", authors);
                fontRenderer.drawStringWithShadow(authorText, rightPaneX + imageOffset, rightPaneY + 23, 0x888888);
            }

            // Badges
            new ModBadgeRenderer(rightPaneX + imageOffset + nameWidth + 6, rightPaneY + 1, width - 28, mod, this).draw();
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String computeModCountText(boolean includeLibs) {
        int[] rootMods = formatModCount(ModMenu.ROOT_MODS.values().stream()
                .filter(mod -> !mod.isHidden() && !mod.getBadges().contains(ModBadge.LIBRARY))
                .map(Mod::getId)
                .collect(Collectors.toSet()));

        if (includeLibs && ModMenu.getConfig().showLibraries) {
            int[] rootLibs = formatModCount(ModMenu.ROOT_MODS.values().stream()
                    .filter(mod -> !mod.isHidden() && mod.getBadges().contains(ModBadge.LIBRARY))
                    .map(Mod::getId)
                    .collect(Collectors.toSet()));
            return TranslationUtil.translateNumeric("modmenu.showingModsLibraries", rootMods, rootLibs);
        } else {
            return TranslationUtil.translateNumeric("modmenu.showingMods", rootMods);
        }
    }

    private String computeLibraryCountText() {
        if (ModMenu.getConfig().showLibraries) {
            int[] rootLibs = formatModCount(ModMenu.ROOT_MODS.values().stream()
                    .filter(mod -> !mod.isHidden() && mod.getBadges().contains(ModBadge.LIBRARY))
                    .map(Mod::getId)
                    .collect(Collectors.toSet()));
            return TranslationUtil.translateNumeric("modmenu.showingLibraries", rootLibs);
        } else {
            return "";
        }
    }

    private int[] formatModCount(Set<String> set) {
        int visible = modList != null ? modList.getDisplayedCountFor(set) : set.size();
        int total = set.size();
        if (visible == total) {
            return new int[]{total};
        }
        return new int[]{visible, total};
    }

    @Override
    public void onGuiClosed() {
        if (this.modList != null) {
            this.modList.close();
        }
        super.onGuiClosed();
    }
}
