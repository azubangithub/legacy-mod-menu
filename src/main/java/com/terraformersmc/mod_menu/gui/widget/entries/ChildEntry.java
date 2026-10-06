package com.terraformersmc.mod_menu.gui.widget.entries;

import com.terraformersmc.mod_menu.gui.widget.ModListWidget;
import com.terraformersmc.mod_menu.util.mod.Mod;
import net.minecraft.client.gui.Gui;

import java.util.List;

public class ChildEntry extends ModListEntry {
    protected boolean bottomChild;
    protected ParentEntry parent;
    protected final List<ModListEntry> parents;

    public ChildEntry(Mod mod, ParentEntry parent, List<ModListEntry> parents, ModListWidget list, boolean bottomChild) {
        super(mod, list);
        this.bottomChild = bottomChild;
        this.parent = parent;
        this.parents = parents;
    }

    @Override
    public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
        super.drawEntry(slotIndex, x, y, listWidth, slotHeight, mouseX, mouseY, isSelected, partialTicks);
        x -= 9;
        int color = 0xFFA0A0A0;
        for (int i = 1; i < parents.size(); i++) {
            if (parents.get(i) instanceof ChildParentEntry) {
                ChildParentEntry childParent = (ChildParentEntry) parents.get(i);
                if (!childParent.bottomChild) {
                    Gui.drawRect(x + childParent.getXOffset(), y - 2, x + 1 + childParent.getXOffset(), y + slotHeight + 2, color);
                }
            }
        }
        x += getXOffset();
        Gui.drawRect(x, y - 2, x + 1, y + (bottomChild ? slotHeight / 2 : slotHeight + 2), color);
        Gui.drawRect(x, y + slotHeight / 2, x + 7, y + slotHeight / 2 + 1, color);
    }

    @Override
    public int getXOffset() {
        return 13 * parents.size();
    }
}
