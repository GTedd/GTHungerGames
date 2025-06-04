package com.shanebeestudios.hg.plugin.managers;

import com.shanebeestudios.hg.api.data.Leaderboard;
import com.shanebeestudios.hg.api.game.Game;
import com.shanebeestudios.hg.plugin.HungerGames;
import com.shanebeestudios.hg.plugin.configs.Language;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 内部占位符处理类
 */
@SuppressWarnings("UnstableApiUsage")
public class Placeholders extends PlaceholderExpansion {

    private final HungerGames plugin;
    private final GameManager gameManager;
    private final Leaderboard leaderboard;
    private final Language lang;

    public Placeholders(HungerGames plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
        this.leaderboard = plugin.getLeaderboard();
        this.lang = plugin.getLang();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "hungergames";
    }

    @NotNull
    @Override
    public String getAuthor() {
        return this.plugin.getPluginMeta().getAuthors().toString();
    }

    @NotNull
    @Override
    public String getVersion() {
        return this.plugin.getPluginMeta().getVersion();
    }


    @Nullable
    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String identifier) {
        String[] id = identifier.split("_");
        switch (id[0]) {
            case "lb": // 排行榜相关
                switch (id[1]) {
                    case "wins":   // 胜利次数
                    case "kills": // 击杀数
                    case "deaths": // 死亡数
                    case "games": // 游戏次数
                        if (id[2].equalsIgnoreCase("p")) // 玩家名
                            return getStatPlayers(identifier);
                        else if (id[2].equalsIgnoreCase("s")) // 分数
                            return getStatScores(identifier);
                        else if (id[2].equalsIgnoreCase("c")) // 组合格式(玩家名+分数)
                            return getStatPlayers(identifier) + this.lang.leaderboard_combined_separator + getStatScores(identifier);
                        else if (id[2].equalsIgnoreCase("player")) // 指定玩家数据
                            return getStatsPlayer(identifier, offlinePlayer);
                }
            case "status": // 游戏状态
                if (getGame(id[1]) == null) return "无效游戏";
                return getGame(id[1]).getGameArenaData().getStatus().getStringName();
            case "playerstatus": // 玩家状态
                if (offlinePlayer instanceof Player player)
                    return this.plugin.getPlayerManager().getPlayerStatus(player).getStringName();
                return "离线";
            case "cost": // 游戏费用
                if (getGame(id[1]) == null) return "无效游戏";
                return String.valueOf(getGame(id[1]).getGameArenaData().getCost());
            case "playerscurrent": // 当前玩家数
                if (getGame(id[1]) == null) return "无效游戏";
                return String.valueOf(getGame(id[1]).getGamePlayerData().getPlayers().size());
            case "playersmax": // 最大玩家数
                if (getGame(id[1]) == null) return "无效游戏";
                return String.valueOf(getGame(id[1]).getGameArenaData().getMaxPlayers());
            case "playersmin": // 最小玩家数
                if (getGame(id[1]) == null) return "无效游戏";
                return String.valueOf(getGame(id[1]).getGameArenaData().getMinPlayers());
        }
        return null;
    }

    /**
     * 获取指定名称的游戏实例
     * @param name 游戏名称
     * @return 游戏实例，未找到返回null
     */
    private Game getGame(String name) {
        return this.gameManager.getGame(name);
    }

    /**
     * 获取指定玩家的统计数据
     * @param identifier 占位符标识
     * @param player 玩家对象
     * @return 统计值字符串
     */
    private String getStatsPlayer(String identifier, OfflinePlayer player) {
        String[] ind = identifier.split("_");
        Leaderboard.Stats stat = Leaderboard.Stats.valueOf(ind[1].toUpperCase());
        return String.valueOf(this.leaderboard.getStat(player.getUniqueId(), stat));
    }

    /**
     * 获取排行榜中的玩家名称
     * @param identifier 占位符标识
     * @return 玩家名称字符串
     */
    private String getStatPlayers(String identifier) {
        String[] ind = identifier.split("_");
        Leaderboard.Stats stat = Leaderboard.Stats.valueOf(ind[1].toUpperCase());
        int leader = (Integer.parseInt(ind[3]));
        if (this.leaderboard.getStatsPlayers(stat).size() >= leader + 1) {
            return this.leaderboard.getStatsPlayers(stat).get(leader);
        } else {
            return this.lang.leaderboard_blank_space;
        }
    }

    /**
     * 获取排行榜中的分数
     * @param identifier 占位符标识
     * @return 分数字符串
     */
    private String getStatScores(String identifier) {
        String[] ind = identifier.split("_");
        Leaderboard.Stats stat = Leaderboard.Stats.valueOf(ind[1].toUpperCase());
        int leader = (Integer.parseInt(ind[3]));
        if (this.leaderboard.getStatsScores(stat).size() >= leader + 1) {
            return "" + this.leaderboard.getStatsScores(stat).get(leader);
        } else {
            return this.lang.leaderboard_blank_space;
        }
    }

}
