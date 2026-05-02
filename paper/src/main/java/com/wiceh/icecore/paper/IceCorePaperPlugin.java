package com.wiceh.icecore.paper;

import com.wiceh.icecore.core.api.service.FriendService;
import com.wiceh.icecore.core.api.service.PlayerService;
import com.wiceh.icecore.core.cache.FriendRequestCache;
import com.wiceh.icecore.core.cache.PlayerCache;
import com.wiceh.icecore.core.config.DatabaseConfig;
import com.wiceh.icecore.core.config.RedisConfig;
import com.wiceh.icecore.core.database.redis.RedisConnectionProvider;
import com.wiceh.icecore.core.database.sql.FlywayMigrator;
import com.wiceh.icecore.core.database.sql.SqlConnectionProvider;
import com.wiceh.icecore.core.repository.sql.FriendshipRepository;
import com.wiceh.icecore.core.repository.sql.PlayerRepository;
import com.wiceh.icecore.core.service.impl.FriendServiceImpl;
import com.wiceh.icecore.core.service.impl.PlayerServiceImpl;
import com.wiceh.icecore.paper.command.FriendCommand;
import com.wiceh.icecore.paper.command.LangCommand;
import com.wiceh.icecore.paper.command.PlaytimeCommand;
import com.wiceh.icecore.paper.command.ProfileCommand;
import com.wiceh.icecore.paper.config.PluginConfigLoader;
import com.wiceh.icecore.paper.lang.Lang;
import com.wiceh.icecore.paper.lang.LangService;
import com.wiceh.icecore.paper.lang.LocaleCache;
import com.wiceh.icecore.paper.listener.PlayerJoinListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class IceCorePaperPlugin extends JavaPlugin {

    private SqlConnectionProvider sqlProvider;
    private RedisConnectionProvider redisProvider;
    private ExecutorService databaseExecutor;
    private PlayerService playerService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        File langFolder = new File(getDataFolder(), "lang");
        String defaultLocale = getConfig().getString("lang.default", "en");
        List<String> bundled = getConfig().getStringList("lang.bundled");
        if (bundled.isEmpty())
            bundled = List.of("en");

        LangService langService = new LangService(langFolder, defaultLocale);
        langService.load(bundled);
        LocaleCache localeCache = new LocaleCache(defaultLocale);
        Lang.initialize(langService, localeCache);
        getLogger().info("Loaded " + langService.availableLocales().size() + " language(s)");

        try {
            PluginConfigLoader loader = new PluginConfigLoader(getConfig());
            DatabaseConfig dbConfig = loader.loadDatabaseConfig();
            RedisConfig redisConfig = loader.loadRedisConfig();

            getLogger().info("Initializing database connections...");
            this.sqlProvider = new SqlConnectionProvider(dbConfig);
            this.redisProvider = new RedisConnectionProvider(redisConfig);

            getLogger().info("Running database migrations...");
            new FlywayMigrator(sqlProvider.dataSource()).migrate();

            getLogger().info("Wiring services...");
            this.databaseExecutor = Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "IceCore-DB-Worker");
                t.setDaemon(true);
                return t;
            });

            PlayerRepository repository = new PlayerRepository(sqlProvider);
            PlayerCache cache = new PlayerCache(redisProvider);
            PlayerService service = new PlayerServiceImpl(repository, cache, databaseExecutor);
            this.playerService = service;

            FriendshipRepository friendshipRepository = new FriendshipRepository(sqlProvider);
            FriendRequestCache friendRequestCache = new FriendRequestCache(redisProvider);
            FriendService friendService = new FriendServiceImpl(friendshipRepository, friendRequestCache, databaseExecutor);

            getServer().getPluginManager().registerEvents(
                    new PlayerJoinListener(this, service),
                    this
            );

            ProfileCommand profileCommand = new ProfileCommand(this, service);
            PluginCommand profileCmd = Objects.requireNonNull(getCommand("profile"));
            profileCmd.setExecutor(profileCommand);
            profileCmd.setTabCompleter(profileCommand);

            PlaytimeCommand playtimeCommand = new PlaytimeCommand(this, service);
            PluginCommand playtimeCmd = Objects.requireNonNull(getCommand("playtime"));
            playtimeCmd.setExecutor(playtimeCommand);
            playtimeCmd.setTabCompleter(playtimeCommand);

            LangCommand langCommand = new LangCommand(this, service, langService);
            PluginCommand langCmd = Objects.requireNonNull(getCommand("lang"));
            langCmd.setExecutor(langCommand);
            langCmd.setTabCompleter(langCommand);

            FriendCommand friendCommand = new FriendCommand(this, service, friendService);
            PluginCommand friendCmd = Objects.requireNonNull(getCommand("friend"));
            friendCmd.setExecutor(friendCommand);
            friendCmd.setTabCompleter(friendCommand);

            getLogger().info("IceCore enabled successfully!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable IceCore", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down IceCore...");

        if (playerService != null && databaseExecutor != null && !databaseExecutor.isShutdown()) {
            Collection<? extends org.bukkit.entity.Player> onlinePlayers = getServer().getOnlinePlayers();
            if (!onlinePlayers.isEmpty()) {
                CompletableFuture<?>[] saves = onlinePlayers.stream()
                        .map(player -> playerService.handleQuit(player.getUniqueId()))
                        .toArray(CompletableFuture[]::new);

                try {
                    CompletableFuture.allOf(saves).get(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    getLogger().log(Level.WARNING, "Interrupted while saving online player playtime", e);
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Failed to save all online player playtime", e);
                }
            }
        }

        if (databaseExecutor != null) {
            databaseExecutor.shutdown();

            try {
                if (!databaseExecutor.awaitTermination(5, TimeUnit.SECONDS))
                    databaseExecutor.shutdownNow();
            } catch (InterruptedException e) {
                databaseExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (redisProvider != null) redisProvider.close();
        if (sqlProvider != null) sqlProvider.close();

        getLogger().info("IceCore disabled.");
    }
}
