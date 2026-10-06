package com.terraformersmc.mod_menu.gui.widget;

import com.terraformersmc.mod_menu.ModMenu;
import com.terraformersmc.mod_menu.gui.ModsScreen;
import com.terraformersmc.mod_menu.util.DrawingUtil;
import com.terraformersmc.mod_menu.util.mod.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;

import java.net.URI;
import java.util.*;

public class DescriptionListWidget extends GuiListExtended {
    private final ModsScreen parent;
    private final FontRenderer font;
    private Mod selectedMod = null;
    private final List<IGuiListEntry> entries = new ArrayList<IGuiListEntry>();

    public DescriptionListWidget(Minecraft client, int width, int height, int top, int bottom, int entryHeight, ModsScreen parent) {
        super(client, width, height, top, bottom, entryHeight);
        this.parent = parent;
        this.font = client.fontRenderer;
        this.setHasListHeader(false, 0);
    }

    @Override
    public int getSize() {
        return entries.size();
    }

    @Override
    public IGuiListEntry getListEntry(int index) {
        if (index >= 0 && index < entries.size()) {
            return entries.get(index);
        }
        return null;
    }

    @Override
    protected int getScrollBarX() {
        return this.right - 6;
    }

    @Override
    public int getListWidth() {
        return this.width - 12;
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= this.left && mouseX <= this.right && mouseY >= this.top && mouseY <= this.bottom;
    }

    @Override
    protected boolean isSelected(int slotIndex) {
        return false;
    }

    @Override
    protected void drawContainerBackground(Tessellator tessellator) {
        int color = (this.mc.world != null) ? 0xC0101010 : 0x80000000;
        Gui.drawRect(this.left, this.top, this.right, this.bottom, color);
    }

    @Override
    public void drawScreen(int mouseXIn, int mouseYIn, float partialTicks) {
        super.drawScreen(mouseXIn, mouseYIn, partialTicks);
        if (this.visible) {
            DrawingUtil.renderListSeparators(this.left, this.right, this.top, this.bottom);
        }
    }

    @Override
    protected void drawSelectionBox(int insideLeft, int insideTop, int mouseXIn, int mouseYIn, float partialTicks) {
        ScaledResolution res = new ScaledResolution(mc);
        double scaleW = mc.displayWidth / res.getScaledWidth_double();
        double scaleH = mc.displayHeight / res.getScaledHeight_double();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            (int) (this.left * scaleW),
            (int) (mc.displayHeight - (this.bottom * scaleH)),
            (int) (this.width * scaleW),
            (int) ((this.bottom - this.top) * scaleH)
        );
        try {
            super.drawSelectionBox(insideLeft, insideTop, mouseXIn, mouseYIn, partialTicks);
        } finally {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    @Override
    protected void overlayBackground(int startY, int endY, int startAlpha, int endAlpha) {
    }

    public void setSelectedMod(Mod mod) {
        this.selectedMod = mod;
        this.entries.clear();
        this.amountScrolled = 0;
        rebuildUI();
    }

    public void updateSelectedModIfRequired(Mod mod) {
        if (mod != selectedMod) {
            setSelectedMod(mod);
        }
    }

    private void rebuildUI() {
        if (selectedMod == null) {
            return;
        }

        int wrapWidth = this.width - 28;
        Mod mod = selectedMod;

        // 1. Description
        String desc = mod.getTranslatedDescription();
        if (desc != null && !desc.trim().isEmpty()) {
            desc = desc.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\r", "\n");
            String[] paragraphs = desc.split("\n", -1);
            for (String paragraph : paragraphs) {
                if (paragraph.isEmpty()) {
                    entries.add(new TextEntry("", 0, 0));
                } else {
                    List<String> descLines = font.listFormattedStringToWidth(paragraph, wrapWidth);
                    for (String line : descLines) {
                        entries.add(new TextEntry(line, 0, 0xFFFFFF));
                    }
                }
            }
        }

        // 2. Links (Website and Issues are action buttons, only show custom links and sources here)
        Map<String, String> links = mod.getLinks();
        String sourceLink = mod.getSource();
        boolean hasCustomLinks = false;
        if (links != null) {
            for (Map.Entry<String, String> entry : links.entrySet()) {
                if (!"modmenu.website".equals(entry.getKey()) && !"modmenu.issues".equals(entry.getKey())) {
                    hasCustomLinks = true;
                    break;
                }
            }
        }
        boolean hasAnyLinks = (sourceLink != null && !sourceLink.trim().isEmpty()) || hasCustomLinks;
        if (hasAnyLinks && !ModMenu.getConfig().hideModLinks) {
            entries.add(new TextEntry("", 0, 0));
            entries.add(new TextEntry(I18n.format("modmenu.links"), 0, 0xAAAAAA));

            if (sourceLink != null && !sourceLink.trim().isEmpty()) {
                entries.add(new LinkEntry(I18n.format("modmenu.source"), sourceLink, 8));
            }
            if (links != null) {
                for (Map.Entry<String, String> entry : links.entrySet()) {
                    if (!"modmenu.website".equals(entry.getKey()) && !"modmenu.issues".equals(entry.getKey())) {
                        String label = I18n.hasKey(entry.getKey()) ? I18n.format(entry.getKey()) : entry.getKey();
                        entries.add(new LinkEntry(label, entry.getValue(), 8));
                    }
                }
            }
        }

        // 3. License
        Set<String> licenses = mod.getLicense();
        if (!ModMenu.getConfig().hideModLicense && licenses != null && !licenses.isEmpty()) {
            entries.add(new TextEntry("", 0, 0));
            entries.add(new TextEntry(I18n.format("modmenu.license"), 0, 0xAAAAAA));
            for (String license : licenses) {
                if (license != null && !license.trim().isEmpty()) {
                    license = license.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\r", "\n");
                    for (String p : license.split("\n", -1)) {
                        List<String> wrapped = font.listFormattedStringToWidth(p, wrapWidth - 8);
                        for (String line : wrapped) {
                            entries.add(new TextEntry(line, 8, 0xDDDDDD));
                        }
                    }
                }
            }
        }

        // 4. Credits / Authors
        if (!ModMenu.getConfig().hideModCredits && !"java".equals(mod.getId())) {
            SortedMap<String, Set<String>> credits = mod.getCredits();
            if (credits != null && !credits.isEmpty()) {
                entries.add(new TextEntry("", 0, 0));
                entries.add(new TextEntry(I18n.format("modmenu.credits"), 0, 0xAAAAAA));

                for (Map.Entry<String, Set<String>> entry : credits.entrySet()) {
                    String roleKey = "modmenu.credits.role." + entry.getKey().toLowerCase(Locale.ROOT);
                    String roleName = I18n.hasKey(roleKey) ? I18n.format(roleKey) : entry.getKey();
                    entries.add(new TextEntry(roleName + ":", 8, 0xBBBBBB));
                    for (String person : entry.getValue()) {
                        if (person != null && !person.trim().isEmpty()) {
                            person = person.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\r", "\n");
                            for (String p : person.split("\n", -1)) {
                                List<String> wrapped = font.listFormattedStringToWidth(p, wrapWidth - 16);
                                for (String line : wrapped) {
                                    entries.add(new TextEntry(line, 16, 0xDDDDDD));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public class TextEntry implements IGuiListEntry {
        protected final String text;
        protected final int indent;
        protected final int color;

        public TextEntry(String text, int indent, int color) {
            this.text = text;
            this.indent = indent;
            this.color = color;
        }

        @Override
        public void updatePosition(int slotIndex, int x, int y, float partialTicks) {
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
            if (text != null && !text.isEmpty()) {
                font.drawStringWithShadow(text, x + indent, y + 1, color);
            }
        }

        @Override
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
            return false;
        }

        @Override
        public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
        }
    }

    public class LinkEntry extends TextEntry {
        private final String url;

        public LinkEntry(String text, String url, int indent) {
            super(TextFormatting.BLUE + "" + TextFormatting.UNDERLINE + text, indent, 0x5555FF);
            this.url = url;
        }

        @Override
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
            int width = font.getStringWidth(text);
            if (relativeX >= indent && relativeX <= indent + width) {
                parent.openLink(url);
                return true;
            }
            return false;
        }
    }
}
