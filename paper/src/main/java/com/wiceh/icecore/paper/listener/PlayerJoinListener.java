package com.wiceh.icecore.paper.listener;

import com.wiceh.icecore.core.api.service.PlayerService;
import com.wiceh.icecore.paper.lang.Lang;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.UUID;

public final class PlayerJoinListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerService playerService;

    public PlayerJoinListener(JavaPlugin plugin, PlayerService playerService) {
        this.plugin = Objects.requireNonNull(plugin);
        this.playerService = Objects.requireNonNull(playerService);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String username = player.getName();

        playerService.handleJoin(uuid, username)
                .thenAccept(profile -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Lang.setLocale(uuid, profile.locale());

                    if (player.isOnline()) {
                        player.sendMessage(Lang.get(player, "join.welcome",
                                "player", profile.username()));
                    }
                }))
                .exceptionally(throwable -> {
                    plugin.getLogger().warning(
                            "Failed to load profile for " + username + ": " + throwable.getMessage()
                    );
                    return null;
                });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Lang.removeLocale(uuid);

        playerService.handleQuit(uuid)
                .exceptionally(throwable -> {
                    plugin.getLogger().warning(
                            "Failed to save quit data for " + player.getName() +
                            ": " + throwable.getMessage()
                    );
                    return null;
                });
    }
}
