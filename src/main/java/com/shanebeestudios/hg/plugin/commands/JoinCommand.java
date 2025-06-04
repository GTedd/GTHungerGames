package com.shanebeestudios.hg.plugin.commands;

import com.shanebeestudios.hg.api.command.CustomArg;
import com.shanebeestudios.hg.api.game.Game;
import com.shanebeestudios.hg.api.util.Util;
import com.shanebeestudios.hg.plugin.HungerGames;
import com.shanebeestudios.hg.plugin.permission.Permissions;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * 加入游戏命令
 * <p>处理玩家加入游戏的相关命令逻辑</p>
 */
public class JoinCommand extends SubCommand {

    public JoinCommand(HungerGames plugin) {
        super(plugin);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Argument<?> register() {
        return LiteralArgument.literal("join")
            .withPermission(Permissions.COMMAND_JOIN.permission()) // 基础加入权限
            .then(CustomArg.GAME.get("game") // 游戏参数
                .then(new EntitySelectorArgument.ManyPlayers("players") // 可选玩家参数
                    .setOptional(true)
                    .withPermission(Permissions.COMMAND_JOIN_OTHERS.permission()) // 加入其他玩家权限
                    .executesPlayer(info -> {
                        Player sender = info.sender();
                        CommandArguments args = info.args();
                        Game game = args.getByClass("game", Game.class);
                        if (game != null) {
                            Collection<Player> players = (Collection<Player>) args.getByClass("players", Collection.class);
                            if (players != null) {
                                // 为每个指定玩家执行加入游戏
                                players.forEach(player -> joinGame(sender, player, game));
                            } else {
                                // 没有指定玩家则加入自己
                                joinGame(sender, sender, game);
                            }
                        } else {
                            Util.sendPrefixedMessage(sender, "无效的游戏: %s", args.getRaw("game"));
                        }
                    })));
    }

    /**
     * 处理玩家加入游戏的逻辑
     * @param sender 命令发送者
     * @param player 要加入游戏的玩家
     * @param game 要加入的游戏
     */
    private void joinGame(CommandSender sender, Player player, Game game) {
        // 检查玩家是否已在游戏中
        if (this.playerManager.isInGame(player)) {
            if (sender == player) {
                Util.sendPrefixedMessage(sender, this.lang.command_join_already_in_game);
            } else {
                Util.sendPrefixedMessage(sender, 
                    this.lang.command_join_already_in_game_other.replace("<player>", player.getName()));
            }
            return;
        }
        // 执行加入游戏
        game.joinGame(player, true);
    }

}
