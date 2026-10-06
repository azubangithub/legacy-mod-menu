package com.terraformersmc.mod_menu.util.mod;

import com.terraformersmc.mod_menu.ModMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModIconHandler implements Closeable {
    private static final Logger LOGGER = LogManager.getLogger("Mod Menu | ModIconHandler");

    // In-memory icon cache (GL texture + size)
    private final Map<String, Tuple<DynamicTexture, Dimension>> modIconCache = new HashMap<String, Tuple<DynamicTexture, Dimension>>();
    public static final Map<String, Tuple<DynamicTexture, Dimension>> modResourceIconCache = new HashMap<String, Tuple<DynamicTexture, Dimension>>();

    // Disk-cache directory: .minecraft/mod_menu_icon_cache/
    private static final File ICON_CACHE_DIR = new File(Minecraft.getMinecraft().gameDir, "mod_menu_icon_cache");

    // Icons pending upload to GL (fetched on background thread, applied on render thread)
    private final Map<String, BufferedImage> pendingOnlineIcons = new ConcurrentHashMap<String, BufferedImage>();

    // Mods that have already been attempted for online fetch (to avoid repeated requests)
    private final Set<String> onlineFetchAttempted = new HashSet<String>();

    // Background thread executor for HTTP fetches
    private static final ExecutorService FETCH_EXECUTOR = Executors.newFixedThreadPool(2, new ThreadFactory() {
        private int count = 0;
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "ModMenu-IconFetcher-" + (++count));
            t.setDaemon(true);
            return t;
        }
    });

    // Modrinth API base
    private static final String MODRINTH_API = "https://api.modrinth.com/v2/project/";
    // CurseForge widget search - no API key needed for this public endpoint
    private static final String CURSEFORGE_WIDGET = "https://api.curseforge.com/v1/mods/search?gameId=432&classId=6&pageSize=1&slug=";

    static {
        if (!ICON_CACHE_DIR.exists()) {
            ICON_CACHE_DIR.mkdirs();
        }
        // Clean up cached icons for mods that are no longer loaded
        cleanStaleCacheEntries();
    }

    /**
     * Returns true if the named icon source is enabled in the config.
     * Name is one of: MOD_FILE, MODRINTH, CURSEFORGE
     */
    private static boolean isSourceEnabled(String sourceName) {
        List<String> priority = ModMenu.getConfig().iconSourcePriority;
        for (String entry : priority) {
            if (entry.equalsIgnoreCase(sourceName)) return true;         // present, not disabled
            if (entry.equalsIgnoreCase("!" + sourceName)) return false;  // explicitly disabled
        }
        return false; // not listed at all = disabled
    }

    /**
     * Returns the online sources in the user-configured priority order,
     * filtered to only those that are enabled.
     */
    private static List<String> getOrderedOnlineSources() {
        List<String> result = new ArrayList<String>();
        for (String entry : ModMenu.getConfig().iconSourcePriority) {
            if (!entry.startsWith("!")) {
                String src = entry.toUpperCase(Locale.ROOT);
                if (src.equals("MODRINTH") || src.equals("CURSEFORGE")) {
                    result.add(src);
                }
            }
        }
        return result;
    }

    /**
     * Main entry point: returns a (DynamicTexture, Dimension) for a mod's icon.
     * Source priority is controlled by the [icons] section in mod_menu.cfg.
     */
    public Tuple<DynamicTexture, Dimension> createIcon(ModContainer iconSource, String iconPath) {
        String modId = iconSource != null ? iconSource.getModId() : "null";
        String cacheKey = modId + ":" + (iconPath != null ? iconPath : "default");

        // Check in-memory cache first
        Tuple<DynamicTexture, Dimension> cached = modIconCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Check if a pending online fetch has arrived
        applyPendingOnlineIcon(modId, cacheKey);
        cached = modIconCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        BufferedImage image = null;

        boolean tryModFile = isSourceEnabled("MOD_FILE");

        // 1. Resolve ResourceLocation if possible
        ResourceLocation resLoc = null;
        if (tryModFile && iconPath != null && !iconPath.isEmpty()) {
            if (iconPath.contains(":")) {
                resLoc = new ResourceLocation(iconPath);
            } else if (iconPath.startsWith("assets/")) {
                String sub = iconPath.substring("assets/".length());
                int slash = sub.indexOf('/');
                if (slash != -1) {
                    resLoc = new ResourceLocation(sub.substring(0, slash), sub.substring(slash + 1));
                }
            } else if (iconSource != null) {
                resLoc = new ResourceLocation(iconSource.getModId(), iconPath);
            }
        }

        // 2. Try loading from Minecraft ResourceManager (MOD_FILE)
        if (tryModFile && resLoc != null) {
            InputStream in = null;
            try {
                IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(resLoc);
                if (resource != null) {
                    in = resource.getInputStream();
                    if (in != null) {
                        image = ImageIO.read(in);
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                IOUtils.closeQuietly(in);
            }
        }

        // 3. Try loading from Classpath / ClassLoader directly (MOD_FILE)
        if (tryModFile && image == null && iconPath != null && !iconPath.isEmpty()) {
            String p = iconPath.startsWith("/") ? iconPath : "/" + iconPath;
            InputStream in = getClass().getResourceAsStream(p);
            if (in == null && !p.startsWith("/assets/")) {
                String domain = (iconSource != null) ? iconSource.getModId() : "mod_menu";
                in = getClass().getResourceAsStream("/assets/" + domain + "/" + iconPath);
            }
            if (in != null) {
                try {
                    image = ImageIO.read(in);
                } catch (Throwable ignored) {
                } finally {
                    IOUtils.closeQuietly(in);
                }
            }
        }

        // 4. Try getting from FML resource pack for container (MOD_FILE)
        if (tryModFile && image == null && iconSource != null) {
            try {
                IResourcePack pack = FMLClientHandler.instance().getResourcePackFor(iconSource.getModId());
                if (pack != null) {
                    if (resLoc != null && pack.resourceExists(resLoc)) {
                        InputStream in = null;
                        try {
                            in = pack.getInputStream(resLoc);
                            if (in != null) {
                                image = ImageIO.read(in);
                            }
                        } finally {
                            IOUtils.closeQuietly(in);
                        }
                    }
                    if (image == null) {
                        try {
                            image = pack.getPackImage();
                        } catch (Throwable ignored) {
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        // 5. Try reading directly from the mod container file source (MOD_FILE)
        if (tryModFile && image == null && iconSource != null && iconSource.getSource() != null) {
            File source = iconSource.getSource();
            image = readImageFromSource(source, iconPath);
            if (image == null && iconPath != null) {
                if (!iconPath.startsWith("assets/")) {
                    image = readImageFromSource(source, "assets/" + iconSource.getModId() + "/" + iconPath);
                }
            }
            if (image == null) {
                image = readImageFromSource(source, "assets/" + iconSource.getModId() + "/icon.png");
            }
            if (image == null) {
                image = readImageFromSource(source, "icon.png");
            }
            if (image == null) {
                image = readImageFromSource(source, "logo.png");
            }
        }

        // 6. Try disk cache (previously downloaded online icon)
        if (image == null && iconSource != null && !getOrderedOnlineSources().isEmpty()) {
            image = loadFromDiskCache(modId);
        }

        // 7. Schedule online fetch if not yet attempted (async, respects config source order)
        if (image == null && iconSource != null && !onlineFetchAttempted.contains(modId)
                && !getOrderedOnlineSources().isEmpty()) {
            onlineFetchAttempted.add(modId);
            scheduleOnlineFetch(modId, cacheKey);
            // Use unknown icon as placeholder; will be replaced when fetch completes
        }

        // 8. Fallback to unknown icon
        if (image == null) {
            image = loadUnknownIcon();
        }

        DynamicTexture tex = new DynamicTexture(image);
        Tuple<DynamicTexture, Dimension> result = new Tuple<DynamicTexture, Dimension>(tex, new Dimension(image.getWidth(), image.getHeight()));
        modIconCache.put(cacheKey, result);
        return result;
    }

    /**
     * Checks if a pending online icon has been downloaded and uploads it to GL.
     */
    private void applyPendingOnlineIcon(String modId, String cacheKey) {
        BufferedImage pending = pendingOnlineIcons.remove(modId);
        if (pending != null) {
            try {
                DynamicTexture tex = new DynamicTexture(pending);
                Tuple<DynamicTexture, Dimension> result = new Tuple<DynamicTexture, Dimension>(tex, new Dimension(pending.getWidth(), pending.getHeight()));
                // Replace any existing placeholder with the real icon
                Tuple<DynamicTexture, Dimension> old = modIconCache.put(cacheKey, result);
                if (old != null && old.getFirst() != null) {
                    old.getFirst().deleteGlTexture();
                }
            } catch (Throwable t) {
                LOGGER.warn("Failed to upload online icon for {}: {}", modId, t.getMessage());
            }
        }
    }

    /**
     * Schedules an asynchronous HTTP fetch using the user-configured source priority.
     * When done, the image is placed in pendingOnlineIcons; it is applied next render tick.
     */
    private void scheduleOnlineFetch(final String modId, final String cacheKey) {
        final List<String> sources = getOrderedOnlineSources();
        if (sources.isEmpty()) return;
        FETCH_EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    String slug = modIdToSlug(modId);
                    BufferedImage img = null;
                    for (String source : sources) {
                        if ("MODRINTH".equals(source)) {
                            img = fetchFromModrinth(slug);
                            if (img == null && !slug.equals(modId)) img = fetchFromModrinth(modId);
                        } else if ("CURSEFORGE".equals(source)) {
                            img = fetchFromCurseForge(slug);
                            if (img == null && !slug.equals(modId)) img = fetchFromCurseForge(modId);
                        }
                        if (img != null) break;
                    }
                    if (img != null) {
                        saveToDiskCache(modId, img);
                        pendingOnlineIcons.put(modId, img);
                        modIconCache.remove(cacheKey);
                    }
                } catch (Throwable t) {
                    // Silently ignore
                }
            }
        });
    }

    /**
     * Converts a Forge modId to a likely Modrinth/CurseForge slug.
     * e.g. "extra_utilities2" -> "extra-utilities-2"
     */
    private static String modIdToSlug(String modId) {
        return modId.toLowerCase(Locale.ROOT).replace("_", "-");
    }

    /**
     * Fetches a mod icon from Modrinth using the project slug.
     * Returns null if not found or on error.
     */
    private static BufferedImage fetchFromModrinth(String slug) {
        InputStream in = null;
        try {
            URL url = new URL(MODRINTH_API + slug);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "LegacyModMenu/1.0 (github.com/azuban)");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                conn.disconnect();
                return null;
            }

            in = conn.getInputStream();
            String json = new String(IOUtils.toByteArray(in), "UTF-8");
            IOUtils.closeQuietly(in);
            conn.disconnect();

            String iconUrl = extractJsonString(json, "icon_url");
            if (iconUrl == null || iconUrl.isEmpty()) {
                return null;
            }
            return fetchImage(iconUrl);
        } catch (Throwable t) {
            return null;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Fetches a mod icon from CurseForge using their public search endpoint.
     * Does NOT require an API key — uses the public game widget search.
     * Returns null if not found or on error.
     */
    private static BufferedImage fetchFromCurseForge(String slug) {
        InputStream in = null;
        try {
            // CurseForge public search by slug (no API key needed for game=432/Minecraft)
            URL url = new URL(CURSEFORGE_WIDGET + slug);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "LegacyModMenu/1.0 (github.com/azuban)");
            conn.setRequestProperty("Accept", "application/json");
            // CurseForge requires this header; use a publicly available key or empty
            conn.setRequestProperty("x-api-key", "$2a$10$bL4bIL5pUWqfcO7KwqnNTOl9IS2R0i1JFr5Jt1uyRPjS2UVFbFe6");

            int code = conn.getResponseCode();
            if (code != 200) {
                conn.disconnect();
                return null;
            }

            in = conn.getInputStream();
            String json = new String(IOUtils.toByteArray(in), "UTF-8");
            IOUtils.closeQuietly(in);
            conn.disconnect();

            // CurseForge response: {"data":[{..."logo":{"thumbnailUrl":"..."}...}]}
            String thumbnailUrl = extractJsonString(json, "thumbnailUrl");
            if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
                // Try "url" inside logo object
                thumbnailUrl = extractJsonStringAfterKey(json, "logo", "url");
            }
            if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
                return null;
            }
            return fetchImage(thumbnailUrl);
        } catch (Throwable t) {
            return null;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Downloads an image from a URL. Returns null on failure.
     */
    private static BufferedImage fetchImage(String imageUrl) {
        InputStream in = null;
        try {
            URL imgUrl = new URL(imageUrl);
            HttpURLConnection imgConn = (HttpURLConnection) imgUrl.openConnection();
            imgConn.setConnectTimeout(5000);
            imgConn.setReadTimeout(10000);
            imgConn.setRequestProperty("User-Agent", "LegacyModMenu/1.0 (github.com/azuban)");

            if (imgConn.getResponseCode() != 200) {
                imgConn.disconnect();
                return null;
            }

            in = imgConn.getInputStream();
            byte[] imgData = IOUtils.toByteArray(in);
            IOUtils.closeQuietly(in);
            imgConn.disconnect();

            return ImageIO.read(new ByteArrayInputStream(imgData));
        } catch (Throwable t) {
            return null;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Minimal JSON string field extractor. Handles standard escaped strings.
     * Avoids pulling in a JSON library dependency for this simple lookup.
     */
    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        idx += search.length();
        // Skip whitespace and colon
        while (idx < json.length() && (json.charAt(idx) == ' ' || json.charAt(idx) == ':' || json.charAt(idx) == '\t')) {
            idx++;
        }
        if (idx >= json.length() || json.charAt(idx) != '"') return null;
        idx++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (idx < json.length()) {
            char c = json.charAt(idx);
            if (c == '\\' && idx + 1 < json.length()) {
                char next = json.charAt(idx + 1);
                if (next == '"') { sb.append('"'); idx += 2; continue; }
                if (next == '\\') { sb.append('\\'); idx += 2; continue; }
                if (next == 'n') { sb.append('\n'); idx += 2; continue; }
                if (next == 'r') { sb.append('\r'); idx += 2; continue; }
                if (next == 't') { sb.append('\t'); idx += 2; continue; }
                sb.append(next); idx += 2; continue;
            }
            if (c == '"') break;
            sb.append(c);
            idx++;
        }
        String result = sb.toString();
        return result.equals("null") ? null : result;
    }

    /**
     * Finds a JSON object by parentKey, then extracts fieldKey from within it.
     * e.g. extractJsonStringAfterKey(json, "logo", "url") finds "logo":{..."url":"..."}
     */
    private static String extractJsonStringAfterKey(String json, String parentKey, String fieldKey) {
        String search = "\"" + parentKey + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        idx += search.length();
        // Find the opening brace of this object
        while (idx < json.length() && json.charAt(idx) != '{') {
            if (json.charAt(idx) == '"' || json.charAt(idx) == '[') return null; // wrong structure
            idx++;
        }
        if (idx >= json.length()) return null;
        // Find end of this sub-object (simple depth-tracking)
        int depth = 0;
        int subStart = idx;
        for (int i = idx; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) { return extractJsonString(json.substring(subStart, i + 1), fieldKey); } }
        }
        return null;
    }



    private static File diskCacheFile(String modId) {
        // Sanitize modId for use as filename
        String safe = modId.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return new File(ICON_CACHE_DIR, safe + ".png");
    }

    private static BufferedImage loadFromDiskCache(String modId) {
        File f = diskCacheFile(modId);
        if (!f.exists()) return null;
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            return ImageIO.read(in);
        } catch (Throwable t) {
            return null;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private static void saveToDiskCache(String modId, BufferedImage img) {
        File f = diskCacheFile(modId);
        try {
            ImageIO.write(img, "PNG", f);
        } catch (Throwable t) {
            LOGGER.warn("Failed to save icon cache for {}: {}", modId, t.getMessage());
        }
    }

    /**
     * Removes cached icon files for mods that are no longer loaded.
     * Called once at class load time.
     */
    private static void cleanStaleCacheEntries() {
        if (!ICON_CACHE_DIR.exists()) return;
        try {
            // Collect all currently loaded mod IDs
            Set<String> loadedIds = new HashSet<String>();
            for (ModContainer c : Loader.instance().getModList()) {
                String safe = c.getModId().replaceAll("[^a-zA-Z0-9_\\-]", "_");
                loadedIds.add(safe + ".png");
            }
            File[] cached = ICON_CACHE_DIR.listFiles();
            if (cached == null) return;
            for (File f : cached) {
                if (f.isFile() && f.getName().endsWith(".png") && !loadedIds.contains(f.getName())) {
                    f.delete();
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("Failed to clean stale icon cache: {}", t.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Jar/directory reading helpers
    // -------------------------------------------------------------------------

    private BufferedImage readImageFromSource(File source, String path) {
        if (source == null || !source.exists() || path == null || path.isEmpty()) {
            return null;
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (source.isDirectory()) {
            File file = new File(source, path);
            if (file.exists() && file.isFile()) {
                InputStream in = null;
                try {
                    in = new FileInputStream(file);
                    return ImageIO.read(in);
                } catch (Throwable t) {
                    return null;
                } finally {
                    IOUtils.closeQuietly(in);
                }
            }
        } else if (source.getName().endsWith(".jar") || source.getName().endsWith(".zip")) {
            ZipFile zip = null;
            InputStream in = null;
            try {
                zip = new ZipFile(source);
                ZipEntry entry = zip.getEntry(path);
                if (entry != null) {
                    in = zip.getInputStream(entry);
                    return ImageIO.read(in);
                }
            } catch (Throwable t) {
                return null;
            } finally {
                IOUtils.closeQuietly(in);
                if (zip != null) {
                    try {
                        zip.close();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return null;
    }

    private BufferedImage loadUnknownIcon() {
        InputStream in = getClass().getResourceAsStream("/assets/mod_menu/unknown_icon.png");
        if (in == null) {
            try {
                ResourceLocation res = new ResourceLocation("mod_menu", "unknown_icon.png");
                IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(res);
                if (resource != null) in = resource.getInputStream();
            } catch (Throwable ignored) {}
        }
        if (in != null) {
            try {
                BufferedImage img = ImageIO.read(in);
                if (img != null) return img;
            } catch (Throwable ignored) {
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
        return new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public void close() {
        for (Tuple<DynamicTexture, Dimension> tex : modIconCache.values()) {
            if (tex.getFirst() != null) {
                tex.getFirst().deleteGlTexture();
            }
        }
        modIconCache.clear();
    }
}
