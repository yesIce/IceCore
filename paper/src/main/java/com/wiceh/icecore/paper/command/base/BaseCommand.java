package com.wiceh.icecore.paper.command;

import com.wiceh.icecore.core.api.service.PlayerService;
import com.wiceh.icecore.paper.lang.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected final JavaPlugin plugin;
    protected final PlayerService playerService;

    private final String permission;
    private final boolean playerOnly;

    protected BaseCommand(JavaPlugin plugin, PlayerService playerService,
                          String permission, boolean playerOnly) {
        this.plugin = plugin;
        this.playerService = playerService;
        this.permission = permission;
        this.playerOnly = playerOnly;
    }

    @Override
    public final boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                   @NotNull String label, @NotNull String[] args) {
        if (playerOnly && !(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage(Lang.get(sender, "error.player-only"));
            return true;
        }

        if (permission != null && !sender.hasPermission(permission)) {
            sender.sendMessage(Lang.get(sender, "error.no-permission"));
            return true;
        }

        CommandContext context = new CommandContext(sender, label, args, playerService);

        try {
            execute(context);
        } catch (Exception e) {
            sender.sendMessage(Lang.get(sender, "error.unknown-error"));
            plugin.getLogger().warning(
                    "Error executing /" + label + " by " + sender.getName() + ": " + e.getMessage()
            );
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (permission != null && !sender.hasPermission(permission)) {
            return Collections.emptyList();
        }
        return tabComplete(sender, args);
    }

    /**
     * Override to provide tab completion. Default: no suggestions.
     */
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    /**
     * Implement the command logic here.
     */
    protected abstract void execute(CommandContext ctx);

    // === helpers per i comandi figli ===

    /**
     * Run something on the main thread (for sendMessage, world manipulation, etc.).
     */
    protected void runSync(Runnable runnable) {
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    /**
     * Run an async future and switch back to the main thread for the result.
     */
    protected <T> void handleAsync(java.util.concurrent.CompletableFuture<T> future,
                                   java.util.function.Consumer<T> onSuccess,
                                   CommandSender sender) {
        future.thenAccept(result -> runSync(() -> onSuccess.accept(result)))
                .exceptionally(throwable -> {
                    runSync(() -> sender.sendMessage(Lang.get(sender, "error.unknown-error")));
                    plugin.getLogger().warning("Async command failed: " + throwable.getMessage());
                    return null;
                });
    }
}