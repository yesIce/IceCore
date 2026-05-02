package com.wiceh.icecore.paper.lang;

import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class LangService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LangService.class);

    private final File langFolder;
    private final String defaultLocale;
    private final Map<String, YamlConfiguration> loadedLocales = new HashMap<>();

    public LangService(File langFolder, String defaultLocale) {
        this.langFolder = Objects.requireNonNull(langFolder, "langFolder must not be null");
        this.defaultLocale = Objects.requireNonNull(defaultLocale, "defaultLocale must not be null");
    }

    public void load(List<String> bundledLocales) {
        if (!langFolder.exists()) {
            if (!langFolder.mkdirs()) {
                throw new IllegalStateException("Could not create lang folder: " + langFolder);
            }
        }

        for (String locale : bundledLocales) {
            extractIfMissing(locale);
        }

        loadedLocales.clear();

        File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            String locale = file.getName().replace(".yml", "");
            try {
                loadedLocales.put(locale, YamlConfiguration.loadConfiguration(file));
                LOGGER.info("Loaded locale: {}", locale);
            } catch (Exception e) {
                LOGGER.warn("Failed to load locale {}: {}", locale, e.getMessage());
            }
        }

        if (!loadedLocales.containsKey(defaultLocale)) {
            throw new IllegalStateException(
                    "Default locale '" + defaultLocale + "' is not loaded"
            );
        }
    }

    public String get(String locale, String key, Object... placeholders) {
        String resolvedLocale = loadedLocales.containsKey(locale) ? locale : defaultLocale;
        YamlConfiguration config = loadedLocales.get(resolvedLocale);

        String value = config.getString(key);

        if (value == null && !resolvedLocale.equals(defaultLocale)) {
            value = loadedLocales.get(defaultLocale).getString(key);
        }

        if (value == null) {
            LOGGER.warn("Missing translation key: {} (locale: {})", key, resolvedLocale);
            return "§c[" + key + "]";
        }

        String result = applyPlaceholders(value, placeholders);
        return result.replace('&', '§');
    }

    public String defaultLocale() {
        return defaultLocale;
    }

    public boolean hasLocale(String locale) {
        return loadedLocales.containsKey(locale);
    }

    public List<String> availableLocales() {
        return List.copyOf(loadedLocales.keySet());
    }

    private void extractIfMissing(String locale) {
        File target = new File(langFolder, locale + ".yml");
        if (target.exists()) {
            return;
        }

        try (InputStream in = getClass().getResourceAsStream("/lang/" + locale + ".yml")) {
            if (in == null) {
                LOGGER.warn("Bundled locale not found in JAR: {}", locale);
                return;
            }
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Extracted bundled locale: {}", locale);
        } catch (IOException e) {
            LOGGER.warn("Failed to extract locale {}: {}", locale, e.getMessage());
        }
    }

    private String applyPlaceholders(String value, Object... placeholders) {
        if (placeholders.length == 0) {
            return value;
        }
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders must come in key-value pairs");
        }

        String result = value;
        for (int i = 0; i < placeholders.length; i += 2) {
            String key = String.valueOf(placeholders[i]);
            String val = String.valueOf(placeholders[i + 1]);
            result = result.replace("{" + key + "}", val);
        }
        return result;
    }
}