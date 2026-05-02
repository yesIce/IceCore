package com.wiceh.icecore.paper.command;

import com.wiceh.icecore.common.model.PlayerProfile;
import com.wiceh.icecore.core.api.service.PlayerService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class CommandContext {

    private final CommandSender sender;
    private final String label;
    private final String[] args;
    private final PlayerService playerService;

    public CommandContext(CommandSender sender, String label, String[] args, PlayerService playerService) {
        this.sender = sender;
        this.label = label;
        this.args = args;
        this.playerService = playerService;
    }

    public CommandSender sender() {
        return sender;
    }

    public String label() {
        return label;
    }

    public String[] args() {
        return args;
    }

    public int argsLength() {
        return args.length;
    }

    public boolean isPlayer() {
        return sender instanceof Player;
    }

    public Optional<Player> asPlayer() {
        return sender instanceof Player p ? Optional.of(p) : Optional.empty();
    }

    public Optional<String> getString(int index) {
        return index < args.length ? Optional.of(args[index]) : Optional.empty();
    }

    public Optional<Integer> getInt(int index) {
        if (index >= args.length) return Optional.empty();
        try {
            return Optional.of(Integer.parseInt(args[index]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Resolves a player profile by username (only players seen by the server).
     */
    public CompletableFuture<Optional<PlayerProfile>> getProfileByUsername(int index) {
        Optional<String> name = getString(index);
        if (name.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return playerService.findByUsername(name.get());
    }

    public void reply(String message) {
        sender.sendMessage(message);
    }
}