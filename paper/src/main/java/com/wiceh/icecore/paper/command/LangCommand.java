package com.wiceh.icecore.paper.command;

import com.wiceh.icecore.core.api.service.PlayerService;
import com.wiceh.icecore.paper.command.base.BaseCommand;
import com.wiceh.icecore.paper.command.base.CommandContext;
import com.wiceh.icecore.paper.lang.Lang;
import com.wiceh.icecore.paper.lang.LangService;
import com.wiceh.icecore.paper.permission.IcePermission;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class LangCommand extends BaseCommand {

    private final LangService langService;

    public LangCommand(JavaPlugin plugin, PlayerService playerService, LangService langService) {
        super(plugin, playerService, IcePermission.COMMAND_LANG, true);
        this.langService = Objects.requireNonNull(langService);
    }

    @Override
    protected void execute(CommandContext ctx) {
        Player player = ctx.asPlayer().orElseThrow();
        String available = String.join(", ", langService.availableLocales());

        if (ctx.argsLength() == 0) {
            ctx.reply(Lang.get(player, "command.lang.list", "available", available));
            return;
        }

        String requested = ctx.getString(0).orElseThrow().toLowerCase();

        if (!langService.hasLocale(requested)) {
            ctx.reply(Lang.get(player, "command.lang.invalid",
                    "locale", requested,
                    "available", available));
            return;
        }

        String current = Lang.getLocale(player.getUniqueId());
        if (requested.equals(current)) {
            ctx.reply(Lang.get(player, "command.lang.already-set", "locale", requested));
            return;
        }

        handleAsyncVoid(
                playerService.updateLocale(player.getUniqueId(), requested),
                () -> {
                    Lang.setLocale(player.getUniqueId(), requested);
                    player.sendMessage(Lang.get(requested, "command.lang.changed",
                            "locale", requested));
                },
                player
        );
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) return List.of();
        String partial = args[0].toLowerCase();
        return langService.availableLocales().stream()
                .filter(l -> l.startsWith(partial))
                .collect(Collectors.toList());
    }
}
