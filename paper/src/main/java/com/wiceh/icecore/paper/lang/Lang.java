package com.wiceh.icecore.paper.lang;

import com.wiceh.icecore.common.model.PlayerProfile;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class Lang {

    private static LangService instance;
    private static LocaleCache localeCache;

    private Lang() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void initialize(LangService service, LocaleCache cache) {
        if (instance != null) {
            throw new IllegalStateException("Lang already initialized");
        }
        instance = service;
        localeCache = cache;
    }

    public static String get(String locale, String key, Object... placeholders) {
        ensureInitialized();
        return instance.get(locale, key, placeholders);
    }

    public static String get(CommandSender sender, String key, Object... placeholders) {
        ensureInitialized();
        return instance.get(resolveLocale(sender), key, placeholders);
    }

    public static String getForProfile(PlayerProfile profile, String key, Object... placeholders) {
        ensureInitialized();
        String locale = profile.locale() != null ? profile.locale() : instance.defaultLocale();
        return instance.get(locale, key, placeholders);
    }

    public static String getLocale(java.util.UUID uuid) {
        ensureInitialized();
        return localeCache.get(uuid);
    }

    public static void setLocale(java.util.UUID uuid, String locale) {
        ensureInitialized();
        localeCache.set(uuid, locale);
    }

    public static void removeLocale(java.util.UUID uuid) {
        ensureInitialized();
        localeCache.remove(uuid);
    }

    private static String resolveLocale(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return instance.defaultLocale();
        }
        return localeCache.get(player.getUniqueId());
    }

    private static void ensureInitialized() {
        if (instance == null) {
            throw new IllegalStateException("Lang not initialized");
        }
    }
}