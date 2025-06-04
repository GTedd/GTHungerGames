package com.shanebeestudios.hg.plugin.managers;

import com.google.common.collect.ImmutableList;
import com.shanebeestudios.hg.api.command.CustomArg;
import com.shanebeestudios.hg.api.data.ItemData.ChestType;
import com.shanebeestudios.hg.api.game.Game;
import com.shanebeestudios.hg.api.game.GameArenaData;
import com.shanebeestudios.hg.api.game.GameRegion;
import com.shanebeestudios.hg.api.status.Status;
import com.shanebeestudios.hg.api.util.Util;
import com.shanebeestudios.hg.plugin.HungerGames;
import com.shanebeestudios.hg.plugin.configs.Config;
import com.shanebeestudios.hg.plugin.configs.Language;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 游戏管理器 - 负责管理所有游戏实例
 */
public class GameManager {

    private final HungerGames plugin;
    private final Map<String, Game> games = new HashMap<>(); // 存储所有游戏的映射表
    private final Language lang; // 语言配置文件
    private final Random random = new Random(); // 随机数生成器
    private Location globalExitLocation; // 全局退出位置

    public GameManager(HungerGames plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLang();
        CustomArg.init(plugin, this);
        this.globalExitLocation = Config.SETTINGS_GLOBAL_EXIT_LOCATION;
    }

    /**
     * 获取所有游戏
     * @return 所有游戏的不可变列表
     */
    public ImmutableList<Game> getGames() {
        return ImmutableList.copyOf(this.games.values());
    }

    /**
     * 获取所有游戏名称
     * @return 按字母排序的游戏名称列表
     */
    public List<String> getGameNames() {
        return this.games.keySet().stream().sorted().collect(Collectors.toList());
    }

    /**
     * 获取全局退出位置
     * <p>如果未设置全局位置，则尝试返回玩家的重生位置，
     * 如果失败则返回主世界的出生点</p>
     * @param player 要检查的玩家
     * @return 退出位置
     */
    public Location getGlobalExitLocation(@Nullable Player player) {
        if (this.globalExitLocation != null) return this.globalExitLocation;

        if (player != null) {
            Location respawnLocation = player.getRespawnLocation();
            if (respawnLocation != null) return respawnLocation;
        }

        return Bukkit.getWorlds().getFirst().getSpawnLocation();
    }

    /**
     * 设置全局退出位置
     * @param location 要设置的退出位置
     */
    public void setGlobalExitLocation(Location location) {
        this.globalExitLocation = location;
        this.plugin.getHGConfig().setGlobalExitLocation(location);
    }

    /**
     * 停止所有正在运行的游戏
     */
    @SuppressWarnings("DataFlowIssue")
    public void stopAllGames() {
        PlayerManager playerManager = this.plugin.getPlayerManager();
        List<Player> players = new ArrayList<>();
        for (Game game : this.games.values()) {
            game.cancelTasks(); // 取消所有任务
            game.getGameBlockData().forceRollback(); // 强制回滚
            players.addAll(game.getGamePlayerData().getPlayers()); // 获取所有玩家
            players.addAll(game.getGamePlayerData().getSpectators()); // 获取所有观战者
        }
        for (Player player : players) {
            player.closeInventory(); // 关闭玩家背包
            if (playerManager.hasPlayerData(player)) {
                playerManager.getPlayerData(player).getGame().getGamePlayerData().leaveGame(player, false);
                playerManager.removePlayerData(player);
            }
            if (playerManager.hasSpectatorData(player)) {
                playerManager.getSpectatorData(player).getGame().getGamePlayerData().leaveSpectate(player);
                playerManager.removePlayerData(player);
            }
        }
        this.games.clear(); // 清空游戏列表
    }

    /**
     * 创建新游戏
     * @param name 游戏名称
     * @param corner1 区域角点1
     * @param corner2 区域角点2
     * @param spawns 出生点列表
     * @param sign 关联的告示牌
     * @param timer 游戏时长
     * @param minPlayers 最小玩家数
     * @param maxPlayers 最大玩家数
     * @param cost 参与费用
     * @return 创建的游戏实例
     */
    @SuppressWarnings("UnusedReturnValue")
    public Game createGame(String name, Block corner1, Block corner2, List<Location> spawns, Location sign,
                           int timer, int minPlayers, int maxPlayers, int cost) {
        GameRegion gameRegion = GameRegion.createNew(corner1, corner2);
        Game game = new Game(name, gameRegion, spawns, sign, timer, minPlayers, maxPlayers, 
                            Config.SETTINGS_FREE_ROAM_TIME, true, cost);
        this.games.put(name, game);
        this.plugin.getArenaConfig().saveGameToConfig(game);
        return game;
    }

    /**
     * 从配置加载游戏
     * @param name 游戏名称
     * @param game 游戏实例
     */
    public void loadGameFromConfig(String name, Game game) {
        this.games.put(name, game);
    }

    /**
     * 检查游戏设置状态
     * @param game 要检查的游戏
     * @param sender 命令发送者
     * @return 游戏是否准备就绪
     */
    public boolean checkGame(Game game, CommandSender sender) {
        GameArenaData gameArenaData = game.getGameArenaData();
        int minPlayers = gameArenaData.getMinPlayers();
        int maxPlayers = gameArenaData.getMaxPlayers();
        String name = gameArenaData.getName();

        boolean isReady = true;

        // 检查出生点数量
        if (gameArenaData.getSpawns().size() < maxPlayers) {
            Util.sendPrefixedMessage(sender, this.lang.arena_debug_need_more_spawns.replace("<number>",
                "" + (maxPlayers - gameArenaData.getSpawns().size())));
            isReady = false;
        }
        // 检查玩家数量设置
        if (maxPlayers < minPlayers) {
            Util.sendPrefixedMessage(sender, this.lang.arena_debug_min_max_players
                .replace("<min>", "" + minPlayers)
                .replace("<max>", "" + maxPlayers));
            isReady = false;
        }
        // 检查大厅墙
        if (!game.getGameBlockData().isLobbyValid()) {
            Util.sendPrefixedMessage(sender, this.lang.arena_debug_invalid_lobby);
            Util.sendPrefixedMessage(sender, this.lang.arena_debug_set_lobby.replace("<arena>", name));
            isReady = false;
        }
        // 检查区域重叠
        Game overlap = gameArenaData.checkOverlap();
        if (overlap != null) {
            String message = this.lang.arena_debug_arena_overlap
                .replace("<arena1>", name)
                .replace("<arena2>", overlap.getGameArenaData().getName());
            Util.sendPrefixedMessage(sender, message);
            isReady = false;
        }
        // 检查通过
        if (isReady) {
            Util.sendPrefixedMessage(sender, this.lang.arena_debug_ready_run.replace("<arena>", name));
            // 只有运行调试命令时才更新状态
            if (sender != null) gameArenaData.setStatus(Status.READY);
        }
        return isReady;
    }

    /**
     * 填充箱子物品
     * @param game 所属游戏
     * @param block 箱子方块
     * @param chestType 箱子类型
     */
    public void fillChests(Game game, Block block, ChestType chestType) {
        Inventory inventory = ((InventoryHolder) block.getState()).getInventory();
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot <= 26; slot++) {
            slots.add(slot);
        }
        Collections.shuffle(slots); // 随机打乱槽位
        inventory.clear(); // 清空箱子
        
        // 根据箱子类型获取最小/最大物品数
        int min = switch (chestType) {
            case REGULAR -> Config.CHESTS_REGULAR_MIN_CONTENT;
            case BONUS -> Config.CHESTS_BONUS_MIN_CONTENT;
            case PLAYER_PLACED -> 0;
            case CHEST_DROP -> Config.CHESTS_CHEST_DROP_MIN_CONTENT;
        };
        int max = switch (chestType) {
            case REGULAR -> Config.CHESTS_REGULAR_MAX_CONTENT;
            case BONUS -> Config.CHESTS_BONUS_MAX_CONTENT;
            case PLAYER_PLACED -> 0;
            case CHEST_DROP -> Config.CHESTS_CHEST_DROP_MAX_CONTENT;
        };

        int c = this.random.nextInt(max) + 1;
        c = Math.max(c, min);
        while (c != 0) {
            ItemStack it = randomItem(game, chestType);
            int slot = slots.getFirst();
            slots.removeFirst();
            inventory.setItem(slot, it);
            c--;
        }
    }

    /**
     * 获取随机物品
     * @param game 所属游戏
     * @param chestType 箱子类型
     * @return 随机物品堆
     */
    public ItemStack randomItem(Game game, ChestType chestType) {
        List<ItemStack> items = game.getGameItemData().getItemData().getItems(chestType);
        int r = items.size();
        if (r == 0) return new ItemStack(Material.AIR);
        int i = this.random.nextInt(r);
        return items.get(i);

    }

    /**
     * 检查位置是否在游戏区域内
     * @param location 要检查的位置
     * @return 是否在游戏区域内
     */
    public boolean isInRegion(Location location) {
        for (Game g : this.games.values()) {
            if (g.getGameArenaData().isInRegion(location))
                return true;
        }
        return false;
    }

    /**
     * 获取位置所在的游戏
     * @param location 要检查的位置
     * @return 游戏实例，如果不存在则返回null
     */
    public Game getGame(Location location) {
        for (Game g : this.games.values()) {
            if (g.getGameArenaData().isInRegion(location))
                return g;
        }
        return null;
    }

    /**
     * 通过名称获取游戏
     * @param name 游戏名称
     * @return 游戏实例
     */
    public Game getGame(String name) {
        return this.games.get(name);
    }

    /**
     * 删除游戏
     * @param game 要删除的游戏
     */
    public void deleteGame(Game game) {
        String name = game.getGameArenaData().getName();
        this.games.remove(name);
        this.plugin.getArenaConfig().removeArena(game);
    }

    /**
     * 获取正在运行的游戏数量
     * @return 运行中的游戏数量
     */
    public int gamesRunning() {
        int i = 0;
        for (Game game : this.games.values()) {
            switch (game.getGameArenaData().getStatus()) {
                case RUNNING:    // 运行中
                case COUNTDOWN: // 倒计时
                case FREE_ROAM: // 自由活动
                case ROLLBACK:   // 回滚中
                    i++;
            }
        }
        return i;
    }

}
