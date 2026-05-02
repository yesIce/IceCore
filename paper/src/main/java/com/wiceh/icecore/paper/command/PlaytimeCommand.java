package com.wiceh.icecore.paper.command;

import com.wiceh.icecore.common.model.PlayerProfile;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class PlaytimeCommand extends BaseCommand {

    public PlaytimeCommand(JavaPlugin plugin, PlayerService playerService) {
        super(plugin, playerService, IcePermission.COMMAND_PLAYTIME, false);
    }

    @Override
    protected void execute(CommandContext ctx) {
        CompletableFuture<Optional<PlayerProfile>> future;
        String targetLabel;

        if (ctx.argsLength() == 0) {
            Optional<Player> selfOpt = ctx.asPlayer();
            if (selfOpt.isEmpty()) {
                ctx.reply(Lang.get(ctx.sender(), "error.console-needs-target",
                        "usage", "/playtime <player>"));
                return;
            }

            Player self = selfOpt.get();
            future = playerService.loadProfile(self.getUniqueId());
            targetLabel = self.getName();
        } else {
            if (!IcePermission.COMMAND_PLAYTIME_OTHERS.has(ctx.sender())) {
                ctx.reply(Lang.get(ctx.sender(), "error.no-permission"));
                return;
            }

            targetLabel = ctx.getString(0).orElseThrow();
            future = playerService.findByUsername(targetLabel);
        }

        handleAsync(future, profileOpt -> {
            if (profileOpt.isEmpty()) {
                ctx.reply(Lang.get(ctx.sender(), "error.player-not-found", "player", targetLabel));
                return;
            }

            PlayerProfile profile = profileOpt.get();
            Instant now = Instant.now();
            long playtimeMillis = profile.playtimeMillis();

            Player online = Bukkit.getPlayer(profile.uuid());
            if (online != null && online.isOnline())
                playtimeMillis += Math.max(0L, Duration.between(profile.lastLogin(), now).toMillis());

            ctx.reply(Lang.get(ctx.sender(), "command.playtime.result",
                    "player", profile.username(),
                    "time", DurationFormatter.formatGranular(ctx.sender(), playtimeMillis)));
        }, ctx.sender());
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) return List.of();
        if (!IcePermission.COMMAND_PLAYTIME_OTHERS.has(sender)) return List.of();

        String partial = args[0].toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
    }
}
