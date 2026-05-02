package com.wiceh.icecore.paper.util;

import com.wiceh.icecore.paper.lang.Lang;
import org.bukkit.command.CommandSender;

import java.time.Duration;

public final class DurationFormatter {

    private DurationFormatter() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String formatLargest(CommandSender sender, long millis) {
        if (millis < 0) millis = 0;
        Duration d = Duration.ofMillis(millis);

        long days = d.toDays();
        if (days > 0) return unit(sender, "day", days);

        long hours = d.toHours();
        if (hours > 0) return unit(sender, "hour", hours);

        long minutes = d.toMinutes();
        if (minutes > 0) return unit(sender, "minute", minutes);

        return unit(sender, "second", d.toSeconds());
    }

    public static String formatGranular(CommandSender sender, long millis) {
        if (millis < 0) millis = 0;
        Duration d = Duration.ofMillis(millis);

        long days = d.toDaysPart();
        long hours = d.toHoursPart();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();

        StringBuilder sb = new StringBuilder();
        String separator = Lang.get(sender, "time.separator");

        if (days > 0) appendShort(sb, sender, "day", days, separator);
        if (hours > 0) appendShort(sb, sender, "hour", hours, separator);
        if (minutes > 0) appendShort(sb, sender, "minute", minutes, separator);
        if (seconds > 0) appendShort(sb, sender, "second", seconds, separator);

        if (sb.isEmpty())
            appendShort(sb, sender, "second", 0, separator);

        return sb.toString();
    }

    private static String unit(CommandSender sender, String unitName, long n) {
        String form = (n == 1) ? "one" : "many";
        return Lang.get(sender, "time." + unitName + "." + form, "n", String.valueOf(n));
    }

    private static void appendShort(StringBuilder sb, CommandSender sender,
                                    String unitName, long n, String separator) {
        if (!sb.isEmpty()) sb.append(separator);
        sb.append(Lang.get(sender, "time.short." + unitName, "n", String.valueOf(n)));
    }
}