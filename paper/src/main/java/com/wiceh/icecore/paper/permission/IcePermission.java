package com.wiceh.icecore.paper.permission;

import org.bukkit.command.CommandSender;

import java.util.Objects;

public enum IcePermission {

    COMMAND_PROFILE("command.profile"),
    COMMAND_PROFILE_OTHERS("command.profile.others"),
    COMMAND_PLAYTIME("command.playtime"),
    COMMAND_PLAYTIME_OTHERS("command.playtime.others"),
    COMMAND_LANG("command.lang"),
    COMMAND_FRIEND("command.friend");

    private final String node;

    IcePermission(String node) {
        this.node = "icecore." + node;
    }

    public String node() {
        return node;
    }

    public boolean has(CommandSender sender) {
        return Objects.requireNonNull(sender, "sender must not be null").hasPermission(node);
    }
}
