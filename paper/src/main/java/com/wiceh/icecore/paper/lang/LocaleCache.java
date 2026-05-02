package com.wiceh.icecore.paper.lang;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LocaleCache {

    private final Map<UUID, String> locales = new ConcurrentHashMap<>();
    private final String defaultLocale;

    public LocaleCache(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public void set(UUID uuid, String locale) {
        if (locale != null) {
            locales.put(uuid, locale);
        } else {
            locales.remove(uuid);
        }
    }

    public void remove(UUID uuid) {
        locales.remove(uuid);
    }

    public String get(UUID uuid) {
        return locales.getOrDefault(uuid, defaultLocale);
    }
}