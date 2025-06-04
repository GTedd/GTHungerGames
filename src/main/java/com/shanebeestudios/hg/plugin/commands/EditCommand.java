package com.shanebeestudios.hg.plugin.commands;

import com.shanebeestudios.hg.api.command.CustomArg;
import com.shanebeestudios.hg.api.game.Game;
import com.shanebeestudios.hg.api.game.GameArenaData;
import com.shanebeestudios.hg.api.game.GameBorderData;
import com.shanebeestudios.hg.api.util.Util;
import com.shanebeestudios.hg.plugin.HungerGames;
import com.shanebeestudios.hg.plugin.permission.Permissions;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.Location2DArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.wrappers.Location2D;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 编辑命令类 - 用于管理游戏竞技场的各种设置
 */
public class EditCommand extends SubCommand {

    public EditCommand(HungerGames plugin) {
        super(plugin);
    }

    @Override
    protected Argument<?> register() {
        return LiteralArgument.literal("edit")  // 主命令
            .withPermission(Permissions.COMMAND_EDIT.permission())  // 所需权限
            .then(CustomArg.GAME.get("game")  // 游戏参数
                .then(border())      // 边界设置子命令
                .then(chestRefill()) // 箱子刷新设置子命令
                .then(info())       // 游戏信息设置子命令
                .then(locations())  // 位置设置子命令
            );
    }

    // 边界设置相关命令
    private Argument<?> border() {
        return LiteralArgument.literal("border")  // 边界子命令
            .then(LiteralArgument.literal("final_size")  // 最终大小设置
                .then(new IntegerArgument("final_size", 5)  // 参数：最终大小（最小5）
                    .executes(info -> {
                        Game game = info.args().getByClass("game", Game.class);
                        int finalSize = info.args().getByClass("final_size", Integer.class);
                        GameBorderData gameBorderData = game.getGameBorderData();
                        gameBorderData.setFinalBorderSize(finalSize);
                        Util.sendPrefixedMessage(info.sender(), "边界最终大小已设置为 %s", finalSize);
                        saveGame(game);
                    })))
            .then(LiteralArgument.literal("countdown_start")  // 倒计时开始时间设置
                .then(new IntegerArgument("countdown_start", 5)  // 参数：开始时间（最小5）
                    .executes(info -> {
                        Game game = info.args().getByClass("game", Game.class);
                        int countdownStart = info.args().getByClass("countdown_start", Integer.class);
                        GameBorderData gameBorderData = game.getGameBorderData();
                        gameBorderData.setBorderCountdownStart(countdownStart);
                        Util.sendPrefixedMessage(info.sender(), "边界倒计时开始时间已设置为 %s", countdownStart);
                        saveGame(game);
                    })))
            .then(LiteralArgument.literal("countdown_end")  // 倒计时结束时间设置
                .then(new IntegerArgument("countdown_end", 5)  // 参数：结束时间（最小5）
                    .executes(info -> {
                        Game game = info.args().getByClass("game", Game.class);
                        int countdownEnd = info.args().getByClass("countdown_end", Integer.class);
                        GameBorderData gameBorderData = game.getGameBorderData();
                        gameBorderData.setBorderCountdownEnd(countdownEnd);
                        Util.sendPrefixedMessage(info.sender(), "边界倒计时结束时间已设置为 %s", countdownEnd);
                        saveGame(game);
                    })))
            .then(LiteralArgument.literal("center_location")  // 中心位置设置
                .then(new Location2DArgument("center_location", LocationType.BLOCK_POSITION)  // 参数：位置坐标
                    .executes(info -> {
                        Game game = info.args().getByClass("game", Game.class);
                        Location2D centerLocation = info.args().getByClass("center_location", Location2D.class);
                        GameBorderData gameBorderData = game.getGameBorderData();
                        gameBorderData.setCenterLocation(convert(centerLocation));
                        Util.sendPrefixedMessage(info.sender(), "边界中心位置已设置为 %s", centerLocation);
                        saveGame(game);
                    })));
    }

    // 箱子刷新设置相关命令
    private Argument<?> chestRefill() {
        return LiteralArgument.literal("chest-refill")  // 箱子刷新子命令
            .then(LiteralArgument.literal("time")  // 刷新时间设置
                .then(new IntegerArgument("seconds", 30)  // 参数：秒数（最小30）
                    .executes(info -> {
                        Game game = info.args().getByClass("game", Game.class);
                        CommandSender sender = info.sender();
                        String name = game.getGameArenaData().getName();
                        int time = info.args().getByClass("seconds", Integer.class);
                        if (time % 30 != 0) {
                            Util.sendPrefixedMessage(sender, "<yellow><time> <red>必须是30的倍数");
                            return;
                        }
                        game.getGameArenaData().setChestRefillTime(time);
                        saveGame(game);
                        Util.sendPrefixedMessage(sender, this.lang.command_edit_chest_refill_time_set
                            .replace("<arena>", name)
                            .replace("<sec>", String.valueOf(time)));

                    })))
            .then(LiteralArgument.literal("repeat")  // 重复刷新间隔设置
                .then(new IntegerArgument("seconds", 30)  // 参数：秒数（最小30）
                    .executes(info -> {
                        Game game = info.args().getByClass("game", Game.class);
                        CommandSender sender = info.sender();
                        String name = game.getGameArenaData().getName();
                        int time = info.args().getByClass("seconds", Integer.class);
                        if (time % 30 != 0) {
                            Util.sendPrefixedMessage(sender, "<yellow><time> <red>必须是30的倍数");
                            return;
                        }
                        game.getGameArenaData().setChestRefillRepeat(time);
                        saveGame(game);
                        Util.sendPrefixedMessage(sender, this.lang.command_edit_chest_refill_repeat_set
                            .replace("<arena>", name)
                            .replace("<sec>", String.valueOf(time)));
                    })));
    }

    // 游戏信息设置相关命令
    private Argument<?> info() {
        return LiteralArgument.literal("info")  // 信息子命令
            .then(LiteralArgument.literal("free_roam_time")  // 自由活动时间设置
                .then(new IntegerArgument("seconds", -1)  // 参数：秒数
                    .executes(info -> {
                        Game game = info.args().getByClass("game", Game.class);
                        int freeRoamTime = info.args().getByClass("seconds", Integer.class);
                        game.getGameArenaData().setFreeRoamTime(freeRoamTime);
                        Util.sendPrefixedMessage(info.sender(), "自由活动时间已设置为 %s", freeRoamTime);
                        saveGame(game);
                    })))
            .then(LiteralArgument.literal("cost")  // 游戏费用设置
                .then(new IntegerArgument("dollars", 0)  // 参数：金额（最小0）
                    .executes(info -> {
                        Game game = info.args().getByClass("game", Game.class);
                        int cost = info.args().getByClass("dollars", Integer.class);
                        game.getGameArenaData().setCost(cost);
                        Util.sendPrefixedMessage(info.sender(), "游戏费用已设置为 %s", cost);
                        saveGame(game);
                    })))
            .then(LiteralArgument.literal("timer")  // 游戏计时器设置
                .then(new IntegerArgument("seconds", 30)  // 参数：秒数（最小30）
                    .executes(info -> {
                        Game game = info.args().getByClass("game", Game.class);
                        int timerSeconds = info.args().getByClass("seconds", Integer.class);
                        game.getGameArenaData().setTimer(timerSeconds);
                        Util.sendPrefixedMessage(info.sender(), "游戏计时器已设置为 %s", timerSeconds);
                        saveGame(game);
                    })))
            .then(LiteralArgument.literal("min_players")  // 最小玩家数设置
                .then(new IntegerArgument("min", 2)  // 参数：最小玩家数（最小2）
                    .executes(info -> {
                        Game game = info.args().getByClass("game", Game.class);
                        int minPlayers = info.args().getByClass("min", Integer.class);
                        if (minPlayers > game.getGameArenaData().getMaxPlayers()) {
                            throw CommandAPI.failWithString("最小玩家数不能大于最大玩家数");
                        }
                        game.getGameArenaData().setMinPlayers(minPlayers);
                        Util.sendPrefixedMessage(info.sender(), "最小玩家数已设置为 %s", minPlayers);
                        saveGame(game);
                    })))
            .then(LiteralArgument.literal("max_players")  // 最大玩家数设置
                .then(new IntegerArgument("max", 2)  // 参数：最大玩家数（最小2）
                    .executes(info -> {
                        Game game = info.args().getByClass("game", Game.class);
                        int maxPlayers = info.args().getByClass("max", Integer.class);
                        if (maxPlayers < game.getGameArenaData().getMinPlayers()) {
                            throw CommandAPI.failWithString("最大玩家数不能小于最小玩家数");
                        }
                        game.getGameArenaData().setMaxPlayers(maxPlayers);
                        Util.sendPrefixedMessage(info.sender(), "最大玩家数已设置为 %s", maxPlayers);
                        saveGame(game);
                    })));
    }

    // 位置设置相关命令
    private Argument<?> locations() {
        return LiteralArgument.literal("locations")  // 位置子命令
            .then(LiteralArgument.literal("lobby_wall")  // 大厅墙设置
                .executesPlayer(info -> {
                    Game game = info.args().getByClass("game", Game.class);
                    assert game != null;
                    Player player = info.sender();
                    Block targetBlock = player.getTargetBlockExact(10);
                    if (targetBlock != null && Tag.ALL_SIGNS.isTagged(targetBlock.getType()) && game.getGameBlockData().setLobbyBlock(targetBlock.getLocation())) {
                        Util.sendPrefixedMessage(player, this.lang.command_edit_lobbywall_set);
                        saveGame(game);
                    } else {
                        Util.sendPrefixedMessage(player, this.lang.command_edit_lobbywall_incorrect);
                        Util.sendMessage(player, this.lang.command_edit_lobbywall_format);
                    }
                }))
            .then(LiteralArgument.literal("clear_spawns")  // 清除出生点
                .executes(info -> {
                    Game game = info.args().getByClass("game", Game.class);
                    assert game != null;
                    game.getGameArenaData().clearSpawns();
                    Util.sendPrefixedMessage(info.sender(), "已清除所有出生点 <yellow>竞技场 <white>'<aqua>%s<white>'<yellow> 最多支持 <red>%s<yellow> 名玩家，请确保添加足够的出生点",
                        game.getGameArenaData().getName(), game.getGameArenaData().getMaxPlayers());
                    saveGame(game);
                }))
            .then(LiteralArgument.literal("add_spawn")  // 添加出生点
                .then(new LocationArgument("location", LocationType.BLOCK_POSITION)  // 参数：位置坐标
                    .executesPlayer(info -> {
                        Game game = info.args().getByClass("game", Game.class);
                        assert game != null;
                        GameArenaData gameArenaData = game.getGameArenaData();
                        Location location = info.args().getByClass("location", Location.class);
                        assert location != null;
                        location.add(0.5, 0, 0.5);  // 调整到方块中心
                        location.setPitch(0);  // 重置俯仰角
                        location.setYaw(info.sender().getLocation().getYaw());  // 设置与玩家相同的偏航角
                        gameArenaData.addSpawn(location);

                        int spawnCount = gameArenaData.getSpawns().size();
                        int maxPlayers = gameArenaData.getMaxPlayers();
                        if (spawnCount < maxPlayers) {
                            Util.sendPrefixedMessage(info.sender(),
                                "<yellow>当前有 <aqua>%s <yellow>个出生点，但最多支持 <red>%s <yellow>名玩家，建议再添加 <green>%s<yellow> 个出生点",
                                spawnCount, maxPlayers, maxPlayers - spawnCount);
                        } else {
                            Util.sendPrefixedMessage(info.sender(), "<green>出生点设置完成！");
                        }
                        saveGame(game);
                    })));
    }

    // 将Location2D转换为Location
    private Location convert(Location2D location2D) {
        return new Location(location2D.getWorld(), location2D.getBlockX(), 0, location2D.getBlockZ());
    }

}
