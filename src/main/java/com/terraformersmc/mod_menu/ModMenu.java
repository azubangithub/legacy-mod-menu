package com.terraformersmc.mod_menu;

import com.google.common.collect.LinkedListMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.terraformersmc.mod_menu.config.ModMenuConfig;
import com.terraformersmc.mod_menu.event.ModMenuEventHandler;
import com.terraformersmc.mod_menu.gui.ModsScreen;
import com.terraformersmc.mod_menu.util.ModMenuScreenTexts;
import com.terraformersmc.mod_menu.util.mod.Mod;
import com.terraformersmc.mod_menu.util.mod.ModBadge;
import com.terraformersmc.mod_menu.util.mod.forge.ForgeMod;
import com.terraformersmc.mod_menu.util.mod.java.JavaDummyMod;
import com.terraformersmc.mod_menu.util.mod.liteloader.LiteLoaderMod;
import com.terraformersmc.mod_menu.util.mod.parent.DummyParentMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.text.NumberFormat;
import java.util.*;

@net.minecraftforge.fml.common.Mod(
        modid = ModMenu.MOD_ID,
        name = ModMenu.NAME,
        version = ModMenu.VERSION,
        guiFactory = "com.terraformersmc.mod_menu.config.ModMenuGuiFactory",
        clientSideOnly = true
)
public class ModMenu {
    public static final String MOD_ID = "mod_menu";
    public static final String NAME = "Legacy Mod Menu";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LogManager.getLogger("Legacy Mod Menu");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @net.minecraftforge.fml.common.Mod.Instance(MOD_ID)
    public static ModMenu INSTANCE;

    private static ModMenuConfig config;
    public static final Map<String, Mod> MODS = new HashMap<String, Mod>();
    public static final Map<String, Mod> ROOT_MODS = new HashMap<String, Mod>();
    public static final LinkedListMultimap<Mod, Mod> PARENT_MAP = LinkedListMultimap.create();

    private static int cachedDisplayedModCount = -1;

    public static ModMenuConfig getConfig() {
        return config;
    }

    @net.minecraftforge.fml.common.Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File configFile = new File(event.getModConfigurationDirectory(), "mod_menu.cfg");
        config = new ModMenuConfig(configFile);
        MinecraftForge.EVENT_BUS.register(new ModMenuEventHandler());
    }

    @net.minecraftforge.fml.common.Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModMenuEventHandler.initKeyBinding();
    }

    @net.minecraftforge.fml.common.Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        initModsList();
    }

    public static void initModsList() {
        MODS.clear();
        ROOT_MODS.clear();
        PARENT_MAP.clear();

        // 1. Forge mods
        for (ModContainer container : Loader.instance().getModList()) {
            Mod mod = new ForgeMod(container);
            MODS.put(mod.getId(), mod);
        }

        // 2. LiteLoader mods
        for (LiteLoaderMod liteMod : LiteLoaderMod.getLoadedMods()) {
            if (!MODS.containsKey(liteMod.getId())) {
                MODS.put(liteMod.getId(), liteMod);
            }
        }

        // 3. Java dummy mod
        Mod java = new JavaDummyMod();
        MODS.put("java", java);

        // 4. Build parent relationships
        Map<String, Mod> dummyParents = new HashMap<String, Mod>();
        HashSet<String> modParentSet = new HashSet<String>();

        for (Mod mod : MODS.values()) {
            String parentId = mod.getParent();
            if (parentId == null) {
                ROOT_MODS.put(mod.getId(), mod);
                continue;
            }

            Mod parent = null;
            modParentSet.clear();
            while (true) {
                parent = MODS.containsKey(parentId) ? MODS.get(parentId) : dummyParents.get(parentId);
                if (parent == null) {
                    parent = new DummyParentMod(mod, parentId);
                    dummyParents.put(parentId, parent);
                }

                parentId = parent != null ? parent.getParent() : null;
                if (parentId == null) {
                    break;
                }

                if (modParentSet.contains(parentId)) {
                    LOGGER.warn("Mods contain each other as parents: {}", modParentSet);
                    parent = null;
                    break;
                }
                modParentSet.add(parentId);
            }

            if (parent == null) {
                ROOT_MODS.put(mod.getId(), mod);
                continue;
            }
            PARENT_MAP.put(parent, mod);
        }

        for (Mod mod : new ArrayList<Mod>(MODS.values())) {
            if (mod.getContainer() != null && mod.getContainer().getMetadata() != null) {
                List<net.minecraftforge.fml.common.ModContainer> children = mod.getContainer().getMetadata().childMods;
                if (children != null && !children.isEmpty()) {
                    for (net.minecraftforge.fml.common.ModContainer childCont : children) {
                        Mod childMod = MODS.get(childCont.getModId());
                        if (childMod != null && childMod != mod) {
                            PARENT_MAP.put(mod, childMod);
                            ROOT_MODS.remove(childMod.getId());
                        }
                    }
                }
            }
        }

        MODS.putAll(dummyParents);

        // 5. Config custom parents
        config.processModParents();

        clearModCountCache();
    }

    public static void clearModCountCache() {
        cachedDisplayedModCount = -1;
    }

    public static String getDisplayedModCount() {
        if (cachedDisplayedModCount == -1) {
            boolean includeChildren = config.countChildren;
            boolean includeLibraries = config.countLibraries;
            boolean includeHidden = config.countHiddenMods;

            int count = 0;
            for (Mod mod : MODS.values()) {
                boolean isChild = mod.getParent() != null;
                if (!includeChildren && isChild) {
                    continue;
                }
                boolean isLibrary = mod.getBadges().contains(ModBadge.LIBRARY);
                if (!includeLibraries && isLibrary) {
                    continue;
                }
                if (!includeHidden && mod.isHidden()) {
                    continue;
                }
                count++;
            }
            cachedDisplayedModCount = count;
        }
        return NumberFormat.getInstance().format(cachedDisplayedModCount);
    }

    public static String createModsButtonText(boolean title) {
        ModMenuConfig.TitleMenuButtonStyle titleStyle = config.modsButtonStyle;
        ModMenuConfig.GameMenuButtonStyle gameMenuStyle = config.gameMenuStyle;

        boolean isIcon = title ?
                titleStyle == ModMenuConfig.TitleMenuButtonStyle.ICON :
                gameMenuStyle == ModMenuConfig.GameMenuButtonStyle.ICON;

        boolean isShort = title ?
                (titleStyle == ModMenuConfig.TitleMenuButtonStyle.SHRINK || titleStyle == ModMenuConfig.TitleMenuButtonStyle.SHRINK_LEFT) :
                gameMenuStyle == ModMenuConfig.GameMenuButtonStyle.REPLACE;

        StringBuilder text = new StringBuilder(I18n.format("modmenu.title"));
        if (config.modCountLocation.isOnModsButton() && !isIcon) {
            String count = getDisplayedModCount();
            if (isShort) {
                text.append(" ").append(I18n.format("modmenu.loaded.short", count));
            } else {
                String specificKey = "modmenu.loaded." + count;
                String key = I18n.hasKey(specificKey) ? specificKey : "modmenu.loaded";
                if (config.easterEggs && I18n.hasKey(specificKey + ".secret")) {
                    key = specificKey + ".secret";
                }
                text.append(" ").append(I18n.format(key, count));
            }
        }
        return text.toString();
    }

    public static boolean hasConfigScreen(ModContainer container) {
        if (container == null) return false;
        if (config.hiddenConfigs.contains(container.getModId()) || "java".equals(container.getModId())) {
            return false;
        }
        if ("minecraft".equals(container.getModId()) || MOD_ID.equals(container.getModId())) {
            return true;
        }
        return container.getGuiClassName() != null && !container.getGuiClassName().isEmpty();
    }

    public static GuiScreen getConfigScreen(ModContainer container, GuiScreen parent) {
        if (container == null) return null;
        if ("minecraft".equals(container.getModId())) {
            return new GuiOptions(parent, Minecraft.getMinecraft().gameSettings);
        }
        if (MOD_ID.equals(container.getModId())) {
            return new com.terraformersmc.mod_menu.gui.ModMenuGuiConfig(parent);
        }
        try {
            String guiClassName = container.getGuiClassName();
            if (guiClassName != null && !guiClassName.isEmpty()) {
                Class<?> guiClass = Class.forName(guiClassName, true, Loader.instance().getModClassLoader());
                Object factory = guiClass.newInstance();
                if (factory instanceof IModGuiFactory) {
                    IModGuiFactory guiFactory = (IModGuiFactory) factory;
                    guiFactory.initialize(Minecraft.getMinecraft());
                    if (guiFactory.hasConfigGui()) {
                        return guiFactory.createConfigGui(parent);
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to load config screen for " + container.getModId(), t);
        }
        return null;
    }
}
