package com.shanebeestudios.hg.plugin.commands;

import com.shanebeestudios.hg.api.util.Util;
import com.shanebeestudios.hg.plugin.HungerGames;
import com.shanebeestudios.hg.plugin.permission.Permissions;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;

/**
 * 重载命令类 - 用于重新加载插件配置
 */
public class ReloadCommand extends SubCommand {

    public ReloadCommand(HungerGames plugin) {
        super(plugin);
    }

    @Override
    protected Argument<?> register() {
        return LiteralArgument.literal("reload")  // 重载命令
            .withPermission(Permissions.COMMAND_RELOAD.permission())  // 所需权限
            .then(LiteralArgument.literal("confirm")  // 确认子命令
                .executes(info -> {
                    reload(info.sender());  // 执行重载
                }))
            .then(LiteralArgument.literal("cancel")  // 取消子命令
                .executes(info -> {
                    Util.sendPrefixedMessage(info.sender(), "<red>已取消重载");
                }))
            .executes(info -> {
                CommandSender sender = info.sender();
                if (gamesNotRunning(sender)) {  // 检查是否有游戏运行
                    reload(sender);
                }
            });
    }

    /**
     * 执行重载操作
     * @param sender 命令发送者
     */
    private void reload(CommandSender sender) {
        long start = System.currentTimeMillis();
        Util.sendPrefixedMessage(sender, "<gold>正在重载插件... 请查看控制台是否有错误！");
        this.plugin.reloadPlugin();
        Util.sendPrefixedMessage(sender, "<grey>重载<green>成功<grey>，耗时 <aqua>" +
            (System.currentTimeMillis() - start) + "<grey> 毫秒");
    }

    /**
     * 检查是否有游戏正在运行
     * @param sender 命令发送者
     * @return 是否没有游戏运行
     */
    public boolean gamesNotRunning(CommandSender sender) {
        int running = this.gameManager.gamesRunning();
        if (running > 0) {
            Util.sendPrefixedMessage(sender, "<gold>当前仍有 <aqua>" + running + "<gold> 个游戏正在进行中");
            Util.sendMessage(sender, "<gold>是否要停止所有游戏并重载？");

            if (sender instanceof Player) {
                // 创建可点击的确认按钮
                Component yes = clickableCommand("<green>确认", "/hg reload confirm", lines -> {
                    lines.add("<grey>点击 <green>确认");
                    lines.add("<grey>将停止所有游戏");
                    lines.add("<grey>并重载插件");
                });

                Component space = Util.getMini(" <grey>或 ");
                // 创建可点击的取消按钮
                Component no = clickableCommand("<red>取消", "/hg reload cancel", lines -> {
                    lines.add("<grey>点击 <red>取消");
                    lines.add("<grey>取消重载操作");
                });
                TextComponent msg = Component.text().append(yes).append(space).append(no).build();
                sender.sendMessage(msg);
            } else {
                Util.log("<gold>请输入 <aqua>hg reload confirm <gold>强制重载");
            }
            return false;
        }
        return true;
    }

    /**
     * 创建可点击的命令组件
     * @param message 显示文本
     * @param command 执行的命令
     * @param hover 悬停提示内容
     * @return 可点击的文本组件
     */
    private Component clickableCommand(@NotNull String message, @NotNull String command, Consumer<List<String>> hover) {
        Component msg = Util.getMini(message);

        msg = msg.clickEvent(ClickEvent.runCommand(command));  // 设置点击事件
        if (hover != null) {
            List<String> hovers = new ArrayList<>();
            hover.accept(hovers);
            StringJoiner joiner = new StringJoiner("<newline>");  // 用换行符连接多行文本
            hovers.forEach(joiner::add);
            Component mini = Util.getMini(joiner.toString());

            msg = msg.hoverEvent(HoverEvent.showText(mini));  // 设置悬停提示
        }
        return msg;
    }

}
