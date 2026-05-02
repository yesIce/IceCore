package com.wiceh.icecore.paper.command;

import com.wiceh.icecore.common.model.Friendship;
import com.wiceh.icecore.core.api.service.FriendService;
import com.wiceh.icecore.core.api.service.PlayerService;
import com.wiceh.icecore.paper.command.base.BaseCommand;
import com.wiceh.icecore.paper.command.base.CommandContext;
import com.wiceh.icecore.paper.lang.Lang;
import com.wiceh.icecore.paper.permission.IcePermission;
import com.wiceh.icecore.paper.util.DurationFormatter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FriendCommand extends BaseCommand {

    private final FriendService friendService;

    public FriendCommand(JavaPlugin plugin, PlayerService playerService,
                         FriendService friendService) {
        super(plugin, playerService, IcePermission.COMMAND_FRIEND, true);
        this.friendService = Objects.requireNonNull(friendService);
    }

    @Override
    protected void execute(CommandContext ctx) {
        Player player = ctx.asPlayer().orElseThrow();

        if (ctx.argsLength() == 0) {
            ctx.reply(Lang.get(player, "command.friend.usage"));
            return;
        }

        String sub = ctx.getString(0).orElseThrow().toLowerCase();

        switch (sub) {
            case "add" -> handleAdd(ctx, player);
            case "accept" -> handleAccept(ctx, player);
            case "deny" -> handleDeny(ctx, player);
            case "remove" -> handleRemove(ctx, player);
            case "list" -> handleList(ctx, player);
            case "requests" -> handleRequests(ctx, player);
            default -> ctx.reply(Lang.get(player, "command.friend.usage"));
        }
    }

    private void handleAdd(CommandContext ctx, Player player) {
        if (ctx.argsLength() < 2) {
            ctx.reply(Lang.get(player, "command.friend.usage"));
            return;
        }
        String targetName = ctx.getString(1).orElseThrow();

        playerService.findByUsername(targetName).thenCompose(profileOpt -> {
            if (profileOpt.isEmpty()) {
                runSync(() -> ctx.reply(Lang.get(player, "error.player-not-found",
                        "player", targetName)));
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }

            UUID targetUuid = profileOpt.get().uuid();
            return friendService.sendRequest(player.getUniqueId(), targetUuid)
                    .thenAccept(result -> runSync(() -> {
                        String actualName = profileOpt.get().username();
                        switch (result) {
                            case REQUEST_SENT -> ctx.reply(Lang.get(player, "command.friend.add.sent",
                                    "player", actualName));
                            case AUTO_ACCEPTED -> {
                                ctx.reply(Lang.get(player, "command.friend.add.auto-accepted",
                                        "player", actualName));

                                Player other = Bukkit.getPlayer(targetUuid);
                                if (other != null) {
                                    other.sendMessage(Lang.get(other,
                                            "command.friend.accept.success",
                                            "player", player.getName()));
                                }
                            }
                            case ALREADY_FRIENDS -> ctx.reply(Lang.get(player, "command.friend.add.already-friends",
                                    "player", actualName));
                            case CANNOT_ADD_SELF -> ctx.reply(Lang.get(player, "command.friend.add.cannot-add-self"));
                            default -> ctx.reply(Lang.get(player, "error.unknown-error"));
                        }
                    }));
        }).exceptionally(throwable -> {
            runSync(() -> ctx.reply(Lang.get(player, "error.unknown-error")));
            plugin.getLogger().warning("Friend add failed: " + throwable.getMessage());
            return null;
        });
    }

    private void handleAccept(CommandContext ctx, Player player) {
        if (ctx.argsLength() < 2) {
            ctx.reply(Lang.get(player, "command.friend.usage"));
            return;
        }
        String targetName = ctx.getString(1).orElseThrow();

        playerService.findByUsername(targetName).thenCompose(profileOpt -> {
            if (profileOpt.isEmpty()) {
                runSync(() -> ctx.reply(Lang.get(player, "error.player-not-found",
                        "player", targetName)));
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }

            UUID senderUuid = profileOpt.get().uuid();
            return friendService.acceptRequest(player.getUniqueId(), senderUuid)
                    .thenAccept(result -> runSync(() -> {
                        String actualName = profileOpt.get().username();
                        switch (result) {
                            case REQUEST_ACCEPTED -> {
                                ctx.reply(Lang.get(player, "command.friend.accept.success",
                                        "player", actualName));
                                Player other = Bukkit.getPlayer(senderUuid);
                                if (other != null) {
                                    other.sendMessage(Lang.get(other,
                                            "command.friend.accept.success",
                                            "player", player.getName()));
                                }
                            }
                            case REQUEST_NOT_FOUND -> ctx.reply(Lang.get(player, "command.friend.accept.not-found",
                                    "player", actualName));
                            default -> ctx.reply(Lang.get(player, "error.unknown-error"));
                        }
                    }));
        }).exceptionally(throwable -> {
            runSync(() -> ctx.reply(Lang.get(player, "error.unknown-error")));
            plugin.getLogger().warning("Friend accept failed: " + throwable.getMessage());
            return null;
        });
    }

    private void handleDeny(CommandContext ctx, Player player) {
        if (ctx.argsLength() < 2) {
            ctx.reply(Lang.get(player, "command.friend.usage"));
            return;
        }
        String targetName = ctx.getString(1).orElseThrow();

        playerService.findByUsername(targetName).thenCompose(profileOpt -> {
            if (profileOpt.isEmpty()) {
                runSync(() -> ctx.reply(Lang.get(player, "error.player-not-found",
                        "player", targetName)));
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }

            UUID senderUuid = profileOpt.get().uuid();
            return friendService.denyRequest(player.getUniqueId(), senderUuid)
                    .thenAccept(result -> runSync(() -> {
                        String actualName = profileOpt.get().username();
                        switch (result) {
                            case REQUEST_DENIED -> ctx.reply(Lang.get(player, "command.friend.deny.success",
                                    "player", actualName));
                            case REQUEST_NOT_FOUND -> ctx.reply(Lang.get(player, "command.friend.deny.not-found",
                                    "player", actualName));
                            default -> ctx.reply(Lang.get(player, "error.unknown-error"));
                        }
                    }));
        }).exceptionally(throwable -> {
            runSync(() -> ctx.reply(Lang.get(player, "error.unknown-error")));
            plugin.getLogger().warning("Friend deny failed: " + throwable.getMessage());
            return null;
        });
    }

    private void handleRemove(CommandContext ctx, Player player) {
        if (ctx.argsLength() < 2) {
            ctx.reply(Lang.get(player, "command.friend.usage"));
            return;
        }
        String targetName = ctx.getString(1).orElseThrow();

        playerService.findByUsername(targetName).thenCompose(profileOpt -> {
            if (profileOpt.isEmpty()) {
                runSync(() -> ctx.reply(Lang.get(player, "error.player-not-found",
                        "player", targetName)));
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }

            UUID targetUuid = profileOpt.get().uuid();
            return friendService.removeFriend(player.getUniqueId(), targetUuid)
                    .thenAccept(result -> runSync(() -> {
                        String actualName = profileOpt.get().username();
                        switch (result) {
                            case FRIEND_REMOVED -> ctx.reply(Lang.get(player, "command.friend.remove.success",
                                    "player", actualName));
                            case NOT_FRIENDS -> ctx.reply(Lang.get(player, "command.friend.remove.not-friends",
                                    "player", actualName));
                            default -> ctx.reply(Lang.get(player, "error.unknown-error"));
                        }
                    }));
        }).exceptionally(throwable -> {
            runSync(() -> ctx.reply(Lang.get(player, "error.unknown-error")));
            plugin.getLogger().warning("Friend remove failed: " + throwable.getMessage());
            return null;
        });
    }

    private void handleList(CommandContext ctx, Player player) {
        handleAsync(
                friendService.getFriends(player.getUniqueId()),
                friends -> {
                    ctx.reply(Lang.get(player, "command.friend.list.header"));
                    if (friends.isEmpty()) {
                        ctx.reply(Lang.get(player, "command.friend.list.empty"));
                    } else {
                        for (Friendship f : friends) {
                            UUID friendUuid = f.otherThan(player.getUniqueId());
                            String friendName = resolveName(friendUuid);
                            boolean online = Bukkit.getPlayer(friendUuid) != null;
                            String since = DurationFormatter.formatLargest(player,
                                    Duration.between(f.since(), Instant.now()).toMillis());

                            String key = online
                                    ? "command.friend.list.entry-online"
                                    : "command.friend.list.entry-offline";
                            ctx.reply(Lang.get(player, key,
                                    "player", friendName,
                                    "since", since));
                        }
                    }
                    ctx.reply(Lang.get(player, "command.friend.list.footer"));
                },
                player
        );
    }

    private void handleRequests(CommandContext ctx, Player player) {
        handleAsync(
                friendService.getIncomingRequests(player.getUniqueId()),
                senders -> {
                    ctx.reply(Lang.get(player, "command.friend.requests.header"));
                    if (senders.isEmpty()) {
                        ctx.reply(Lang.get(player, "command.friend.requests.empty"));
                    } else {
                        for (UUID senderUuid : senders) {
                            String name = resolveName(senderUuid);
                            ctx.reply(Lang.get(player, "command.friend.requests.entry",
                                    "player", name));
                        }
                    }
                    ctx.reply(Lang.get(player, "command.friend.requests.footer"));
                },
                player
        );
    }

    private String resolveName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        var offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() != null ? offline.getName() : uuid.toString();
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Stream.of("add", "accept", "deny", "remove", "list", "requests")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && !args[0].equalsIgnoreCase("list") && !args[0].equalsIgnoreCase("requests")) {
            String partial = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
