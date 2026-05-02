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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class ProfileCommand extends BaseCommand {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public ProfileCommand(JavaPlugin plugin, PlayerService playerService) {
        super(plugin, playerService, IcePermission.COMMAND_PROFILE, false);
    }

    @Override
    protected void execute(CommandContext ctx) {
        CompletableFuture<Optional<PlayerProfile>> future;
        String targetLabel;

        if (ctx.argsLength() == 0) {
            Optional<Player> selfOpt = ctx.asPlayer();
            if (selfOpt.isEmpty()) {
                ctx.reply(Lang.get(ctx.sender(), "error.console-needs-target",
                        "usage", "/profile <player>"));
                return;
            }
            Player self = selfOpt.get();
            future = playerService.loadProfile(self.getUniqueId());
            targetLabel = self.getName();
        } else {
            if (!IcePermission.COMMAND_PROFILE_OTHERS.has(ctx.sender())) {
                ctx.reply(Lang.get(ctx.sender(), "error.no-permission"));
                return;
            }
            String name = ctx.getString(0).orElseThrow();
            future = playerService.findByUsername(name);
            targetLabel = name;
        }

        handleAsync(future, profileOpt -> {
            if (profileOpt.isEmpty()) {
                ctx.reply(Lang.get(ctx.sender(), "error.player-not-found", "player", targetLabel));
                return;
            }
            sendProfile(ctx.sender(), profileOpt.get());
        }, ctx.sender());
    }

    private void sendProfile(CommandSender sender, PlayerProfile profile) {
        Instant now = Instant.now();
        Duration sinceFirst = Duration.between(profile.firstLogin(), now);

        sender.sendMessage(Lang.get(sender, "command.profile.header"));
        sender.sendMessage(Lang.get(sender, "command.profile.username", "username", profile.username()));
        sender.sendMessage(Lang.get(sender, "command.profile.uuid", "uuid", profile.uuid().toString()));
        sender.sendMessage(Lang.get(sender, "command.profile.first-seen",
                "date", DATE_FORMAT.format(profile.firstLogin()),
                "ago", DurationFormatter.formatGranular(sender, sinceFirst.toMillis())));
        sender.sendMessage(Lang.get(sender, "command.profile.last-seen",
                "date", DATE_FORMAT.format(profile.lastLogin())));
        long playtimeMillis = profile.playtimeMillis();
        Player online = Bukkit.getPlayer(profile.uuid());
        if (online != null && online.isOnline()) {
            playtimeMillis += Math.max(0L, Duration.between(profile.lastLogin(), now).toMillis());
        }
        sender.sendMessage(Lang.get(sender, "command.profile.playtime",
                "time", DurationFormatter.formatGranular(sender, playtimeMillis)));
        sender.sendMessage(Lang.get(sender, "command.profile.footer"));
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) return List.of();
        if (!IcePermission.COMMAND_PROFILE_OTHERS.has(sender)) return List.of();

        String partial = args[0].toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
    }
}
