package com.terraformersmc.mod_menu.gui.widget;

import com.terraformersmc.mod_menu.ModMenu;
import com.terraformersmc.mod_menu.gui.ModsScreen;
import com.terraformersmc.mod_menu.gui.widget.entries.*;
import com.terraformersmc.mod_menu.util.DrawingUtil;
import com.terraformersmc.mod_menu.util.mod.Mod;
import com.terraformersmc.mod_menu.util.mod.ModBadge;
import com.terraformersmc.mod_menu.util.mod.ModIconHandler;
import com.terraformersmc.mod_menu.util.mod.ModSearch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import java.io.Closeable;
import java.util.*;

public class ModListWidget extends GuiListExtended implements Closeable {
    private final ModsScreen parent;
    private final List<ModListEntry> entries = new ArrayList<ModListEntry>();
    private List<Mod> mods = null;
    private final Set<Mod> addedMods = new HashSet<Mod>();
    private ModListEntry selected = null;
    private String selectedModId = null;
    private final ModIconHandler iconHandler = new ModIconHandler();

    public ModListWidget(Minecraft client, int width, int height, int top, int bottom, int entryHeight, ModsScreen parent) {
        super(client, width, height, top, bottom, entryHeight);
        this.parent = parent;
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
    protected boolean isSelected(int slotIndex) {
        ModListEntry entry = (ModListEntry) getListEntry(slotIndex);
        return entry != null && selected != null && entry.getMod().getId().equals(selected.getMod().getId());
    }

    @Override
    protected int getScrollBarX() {
        return this.width - 6;
    }

    @Override
    public int getListWidth() {
        return this.width - 12;
    }

    public void select(ModListEntry entry) {
        boolean changed = (this.selected != entry);
        this.selected = entry;
        if (entry != null) {
            this.selectedModId = entry.getMod().getId();
        }
        // Play click sound when selection changes (matches vanilla button feedback)
        if (changed && entry != null) {
            Minecraft.getMinecraft().getSoundHandler().playSound(
                PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F)
            );
        }
        parent.updateSelectedEntry(entry);
    }

    public void setSelected(ModListEntry entry) {
        select(entry);
    }

    public ModListEntry getSelected() {
        return selected;
    }

    public void setSelectedModId(String id) {
        this.selectedModId = id;
    }

    public String getSelectedModId() {
        return this.selectedModId;
    }

    public List<ModListEntry> getEntries() {
        return entries;
    }

    public void addEntry(ModListEntry entry) {
        if (entry != null && !addedMods.contains(entry.getMod())) {
            addedMods.add(entry.getMod());
            entries.add(entry);
            if (selectedModId != null && entry.getMod().getId().equals(selectedModId)) {
                select(entry);
            }
        }
    }

    public void clearEntries() {
        entries.clear();
        addedMods.clear();
    }

    public void reloadFilters() {
        filter(parent.getSearchInput(), true, false);
    }

    public void filter(String searchTerm, boolean refresh) {
        filter(searchTerm, refresh, true);
    }

    private boolean hasVisibleChildMods(Mod parent) {
        List<Mod> children = ModMenu.PARENT_MAP.get(parent);
        boolean hideLibraries = !ModMenu.getConfig().showLibraries;
        for (Mod child : children) {
            if (!child.isHidden() && (!hideLibraries || !child.getBadges().contains(ModBadge.LIBRARY))) {
                return true;
            }
        }
        return false;
    }

    public void filter(String searchTerm, boolean refresh, boolean reposition) {
        this.clearEntries();
        Collection<Mod> allMods = new HashSet<Mod>();
        for (Mod mod : ModMenu.MODS.values()) {
            if (ModMenu.getConfig().configMode) {
                if (!parent.getModHasConfigScreen(mod.getContainer())) {
                    continue;
                }
            }
            if (!mod.isHidden()) {
                allMods.add(mod);
            }
        }

        if (this.mods == null || refresh) {
            this.mods = new ArrayList<Mod>(allMods);
            this.mods.sort(ModMenu.getConfig().sorting.getComparator());
        }

        List<Mod> matched = ModSearch.search(parent, searchTerm, this.mods);

        for (Mod mod : matched) {
            String modId = mod.getId();

            if (mod.getBadges().contains(ModBadge.LIBRARY) && !ModMenu.getConfig().showLibraries) {
                continue;
            }

            if (!ModMenu.PARENT_MAP.values().contains(mod)) {
                if (ModMenu.PARENT_MAP.containsKey(mod) && hasVisibleChildMods(mod)) {
                    List<Mod> children = new ArrayList<Mod>(ModMenu.PARENT_MAP.get(mod));
                    children.sort(ModMenu.getConfig().sorting.getComparator());
                    ParentEntry parentEntry = new ParentEntry(mod, children, this);
                    this.addEntry(parentEntry);

                    if (this.parent.showModChildren.contains(modId)) {
                        List<Mod> validChildren = ModSearch.search(this.parent, searchTerm, children);
                        for (Mod child : validChildren) {
                            addChildMod(child, validChildren, parentEntry, Collections.singletonList((ModListEntry) parentEntry), searchTerm, 1);
                        }
                    }
                } else {
                    this.addEntry(new IndependentEntry(mod, this));
                }
            }
        }

        if (!reposition) {
            return;
        }

        if (parent.getSelectedEntry() != null && !entries.isEmpty()) {
            for (ModListEntry entry : entries) {
                if (entry.getMod().getId().equals(parent.getSelectedEntry().getMod().getId())) {
                    select(entry);
                    break;
                }
            }
        }
        if (selected == null && !entries.isEmpty()) {
            select(entries.get(0));
        }
    }

    public void addChildMod(Mod child, List<Mod> validChildren, ParentEntry parent, List<ModListEntry> parents, String searchTerm, int parentCount) {
        if (ModMenu.PARENT_MAP.containsKey(child) && hasVisibleChildMods(child)) {
            List<Mod> childChildren = new ArrayList<Mod>(ModMenu.PARENT_MAP.get(child));
            childChildren.sort(ModMenu.getConfig().sorting.getComparator());
            ChildParentEntry childParentEntry = new ChildParentEntry(
                    child,
                    parent,
                    parents,
                    childChildren,
                    this,
                    validChildren.indexOf(child) == validChildren.size() - 1
            );
            this.addEntry(childParentEntry);

            if (this.parent.showModChildren.contains(child.getId())) {
                List<Mod> validChildChildren = ModSearch.search(this.parent, searchTerm, childChildren);
                for (Mod childChild : validChildChildren) {
                    List<ModListEntry> nextParents = new ArrayList<ModListEntry>(parents);
                    nextParents.add(childParentEntry);
                    addChildMod(childChild, validChildChildren, parent, nextParents, searchTerm, parentCount + 1);
                }
            }
        } else {
            this.addEntry(new ChildEntry(
                    child,
                    parent,
                    parents,
                    this,
                    validChildren.indexOf(child) == validChildren.size() - 1
            ));
        }
    }

    public ModsScreen getParent() {
        return parent;
    }

    public ModIconHandler getIconHandler() {
        return iconHandler;
    }

    public int getRowLeft() {
        return this.left;
    }

    public int getRowTop(int index) {
        return this.top + 4 - (int) this.amountScrolled + index * this.slotHeight;
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= this.left && mouseX <= this.right && mouseY >= this.top && mouseY <= this.bottom;
    }

    public int getDisplayedCountFor(Set<String> set) {
        int count = 0;
        for (ModListEntry c : entries) {
            if (c.getMod() != null && set.contains(c.getMod().getId())) {
                count++;
            }
        }
        return count;
    }

    @Override
    protected void drawContainerBackground(Tessellator tessellator) {
        int color = (this.mc.world != null) ? 0xC0101010 : 0x80000000;
        Gui.drawRect(this.left, this.top, this.right, this.bottom, color);
    }

    @Override
    public void drawScreen(int mouseXIn, int mouseYIn, float partialTicks) {
        if (this.visible) {
            this.mouseX = mouseXIn;
            this.mouseY = mouseYIn;
            this.drawBackground();
            int scrollBarX = this.getScrollBarX();
            this.bindAmountScrolled();
            GlStateManager.disableLighting();
            GlStateManager.disableFog();
            Tessellator tessellator = Tessellator.getInstance();
            this.drawContainerBackground(tessellator);
            int k = this.left + this.width / 2 - this.getListWidth() / 2 + 2;
            int l = this.top + 4 - (int) this.amountScrolled;

            if (this.hasListHeader) {
                this.drawListHeader(k, l, tessellator);
            }

            this.drawSelectionBox(k, l, mouseXIn, mouseYIn, partialTicks);
            GlStateManager.disableDepth();

            // Render 1.21.1 list separators (header_separator at top, footer_separator at bottom)
            DrawingUtil.renderListSeparators(this.left, this.right, this.top, this.bottom);

            int maxScroll = this.getMaxScroll();
            if (maxScroll > 0) {
                int k1 = (this.bottom - this.top) * (this.bottom - this.top) / this.getContentHeight();
                k1 = MathHelper.clamp(k1, 32, this.bottom - this.top - 8);
                int l1 = (int) this.amountScrolled * (this.bottom - this.top - k1) / maxScroll + this.top;

                if (l1 < this.top) {
                    l1 = this.top;
                }

                BufferBuilder bufferbuilder = tessellator.getBuffer();
                GlStateManager.disableTexture2D();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                bufferbuilder.pos((double) scrollBarX, (double) this.bottom, 0.0D).tex(0.0D, 1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos((double) (scrollBarX + 6), (double) this.bottom, 0.0D).tex(1.0D, 1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos((double) (scrollBarX + 6), (double) this.top, 0.0D).tex(1.0D, 0.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos((double) scrollBarX, (double) this.top, 0.0D).tex(0.0D, 0.0D).color(0, 0, 0, 255).endVertex();
                tessellator.draw();
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                bufferbuilder.pos((double) scrollBarX, (double) (l1 + k1), 0.0D).tex(0.0D, 1.0D).color(128, 128, 128, 255).endVertex();
                bufferbuilder.pos((double) (scrollBarX + 6), (double) (l1 + k1), 0.0D).tex(1.0D, 1.0D).color(128, 128, 128, 255).endVertex();
                bufferbuilder.pos((double) (scrollBarX + 6), (double) l1, 0.0D).tex(1.0D, 0.0D).color(128, 128, 128, 255).endVertex();
                bufferbuilder.pos((double) scrollBarX, (double) l1, 0.0D).tex(0.0D, 0.0D).color(128, 128, 128, 255).endVertex();
                tessellator.draw();
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                bufferbuilder.pos((double) scrollBarX, (double) (l1 + k1 - 1), 0.0D).tex(0.0D, 1.0D).color(192, 192, 192, 255).endVertex();
                bufferbuilder.pos((double) (scrollBarX + 5), (double) (l1 + k1 - 1), 0.0D).tex(1.0D, 1.0D).color(192, 192, 192, 255).endVertex();
                bufferbuilder.pos((double) (scrollBarX + 5), (double) l1, 0.0D).tex(1.0D, 0.0D).color(192, 192, 192, 255).endVertex();
                bufferbuilder.pos((double) scrollBarX, (double) l1, 0.0D).tex(0.0D, 0.0D).color(192, 192, 192, 255).endVertex();
                tessellator.draw();
            }

            this.renderDecorations(mouseXIn, mouseYIn);
            GlStateManager.enableTexture2D();
            GlStateManager.shadeModel(7424);
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
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

    @Override
    public void close() {
        iconHandler.close();
    }
}
