package com.shanebeestudios.hg.plugin.commands;

import com.shanebeestudios.hg.api.util.Util;
import com.shanebeestudios.hg.plugin.HungerGames;
import com.shanebeestudios.hg.plugin.configs.Config;
import com.shanebeestudios.hg.plugin.permission.Permissions;
import dev.jorel.commandapi.BukkitStringTooltip;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Optional;

public class SettingsCommand extends SubCommand {

    public SettingsCommand(HungerGames plugin) {
        super(plugin);
    }

    @Override
    protected Argument<?> register() {
        return LiteralArgument.literal("settings")
            .withPermission(Permissions.COMMAND_SETTINGS.permission())
            .then(debug());
    }

    private Argument<?> debug() {
        return LiteralArgument.literal("debug")
            .then(new StringArgument("开关")
                .setOptional(true)
                .includeSuggestions(ArgumentSuggestions.stringsWithTooltips(
                    List.of(
                        BukkitStringTooltip.ofString("enable", "启用调试设置"),
                        BukkitStringTooltip.ofString("disable", "禁用调试设置"))))
                .executes(info -> {
                    CommandSender sender = info.sender();
                    Optional<String> enable = info.args().getOptionalByClass("开关", String.class);
                    if (enable.isPresent()) {
                        String s = enable.get();
                        if (s.equalsIgnoreCase("enable")) {
                            Config.SETTINGS_DEBUG = true;
                            Util.sendPrefixedMessage(sender, "调试设置已<green>启用<grey>！");
                        } else if (s.equalsIgnoreCase("disable")) {
                            Config.SETTINGS_DEBUG = false;
                            Util.sendPrefixedMessage(sender, "调试设置已<red>禁用<grey>！");
                        } else {
                            Util.sendPrefixedMessage(sender, "无效选项 '%s'", s);
                        }
                    } else {
                        String setting = Config.SETTINGS_DEBUG ? "<green>已启用" : "<red>已禁用";
                        Util.sendPrefixedMessage(sender, "调试设置当前状态：%s", setting);
                    }
                }));
    }

}
