package com.shanebeestudios.hg.plugin.managers;

import com.shanebeestudios.hg.api.data.PlayerData;
import com.shanebeestudios.hg.api.game.Game;
import com.shanebeestudios.hg.api.status.PlayerStatus;
import com.shanebeestudios.hg.plugin.HungerGames;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家管理器
 * <p>可以通过<b>{@link HungerGames#getPlayerManager()}</b>获取此类的实例</p>
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class PlayerManager {

    private final Map<UUID, PlayerData> playerMap;
    private final Map<UUID, PlayerData> spectatorMap;

    public PlayerManager() {
        this.playerMap = new HashMap<>();
        this.spectatorMap = new HashMap<>();
    }

    /**
     * 检查玩家是否在游戏中且拥有玩家数据
     *
     * @param player 要检查的玩家
     * @return 如果玩家在游戏中且有数据则返回true
     */
    public boolean hasPlayerData(Player player) {
        return this.playerMap.containsKey(player.getUniqueId());
    }

    /**
     * 检查玩家是否在观战且拥有玩家数据
     *
     * @param player 要检查的玩家
     * @return 如果玩家在观战且有数据则返回true
     */
    public boolean hasSpectatorData(Player player) {
        return this.spectatorMap.containsKey(player.getUniqueId());
    }

    /**
     * 获取游戏中玩家的数据实例
     *
     * @param player 要获取数据的玩家
     * @return 玩家的PlayerData，如果玩家不在游戏中则返回null
     */
    @Nullable
    public PlayerData getPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        if (this.playerMap.containsKey(uuid)) {
            return this.playerMap.get(uuid);
        }
        return null;
    }

    /**
     * 获取观战玩家的数据实例
     *
     * @param player 要获取数据的玩家
     * @return 玩家的PlayerData，如果玩家不在观战则返回null
     */
    @Nullable
    public PlayerData getSpectatorData(Player player) {
        UUID uuid = player.getUniqueId();
        if (this.spectatorMap.containsKey(uuid)) {
            return spectatorMap.get(uuid);
        }
        return null;
    }

    /**
     * 获取游戏中玩家的数据实例
     * <p>首先检查玩家是否在游戏中，然后检查是否在观战
     * <br>如果需要特定数据，请使用{@link #getPlayerData(Player)}或{@link #getSpectatorData(Player)}</p>
     *
     * @param player 要获取数据的玩家
     * @return 玩家的PlayerData，如果玩家不在游戏中则返回null
     */
    @Nullable
    public PlayerData getData(Player player) {
        if (hasPlayerData(player))
            return getPlayerData(player);
        else if (hasSpectatorData(player))
            return getSpectatorData(player);
        return null;
    }

    /**
     * 为玩家创建{@link PlayerData}
     *
     * @param player 要创建数据的玩家
     * @param game   玩家正在进入的游戏
     */
    public void createPlayerData(Player player, Game game) {
        PlayerData playerData = new PlayerData(player, game);
        this.playerMap.put(player.getUniqueId(), playerData);
    }

    /**
     * 为观战者创建{@link PlayerData}
     *
     * @param spectator 要创建数据的观战者
     * @param game      观战者正在进入的游戏
     */
    public void createSpectatorData(Player spectator, Game game) {
        PlayerData playerData = new PlayerData(spectator, game);
        this.spectatorMap.put(spectator.getUniqueId(), playerData);
    }

    /**
     * 从PlayerData映射中移除玩家数据
     *
     * @param player 要移除PlayerData的持有者
     */
    public void removePlayerData(Player player) {
        this.playerMap.remove(player.getUniqueId());
    }

    /**
     * 从PlayerData映射中移除玩家数据
     *
     * @param uuid 要移除PlayerData的持有者的UUID
     */
    public void removePlayerData(UUID uuid) {
        this.playerMap.remove(uuid);
    }

    /**
     * 从SpectatorData映射中移除玩家数据
     *
     * @param player 要移除PlayerData的持有者
     */
    public void removeSpectatorData(Player player) {
        this.spectatorMap.remove(player.getUniqueId());
    }

    /**
     * 从SpectatorData映射中移除玩家数据
     *
     * @param uuid 要移除PlayerData的持有者的UUID
     */
    public void removeSpectatorData(UUID uuid) {
        this.spectatorMap.remove(uuid);
    }

    /**
     * 将{@link PlayerData}从玩家转移到观战者
     *
     * @param player 要转移的玩家
     */
    public void transferPlayerDataToSpectator(Player player) {
        UUID uuid = player.getUniqueId();
        if (this.playerMap.containsKey(uuid)) {
            PlayerData clone = this.playerMap.get(uuid).clone();
            if (clone != null) {
                this.spectatorMap.put(uuid, clone);
                this.playerMap.remove(uuid);
            }
        }
    }

    /**
     * 获取玩家当前所在的游戏
     *
     * @param player 要获取游戏的玩家
     * @return 玩家所在的游戏，如果玩家不在游戏中则返回null
     */
    @SuppressWarnings("DataFlowIssue")
    public @Nullable Game getGame(Player player) {
        if (hasPlayerData(player))
            return getPlayerData(player).getGame();
        else if (hasSpectatorData(player))
            return getSpectatorData(player).getGame();
        else
            return null;
    }

    /**
     * 检查玩家是否已在游戏中
     *
     * @param player 要检查的玩家
     * @return 如果在游戏中则返回true，否则返回false
     */
    public boolean isInGame(Player player) {
        return getPlayerStatus(player) != PlayerStatus.NOT_IN_GAME;
    }

    /**
     * 获取玩家的状态
     *
     * @param player 要获取状态的玩家
     * @return 玩家的状态
     */
    public PlayerStatus getPlayerStatus(Player player) {
        if (hasPlayerData(player)) return PlayerStatus.IN_GAME;
        else if (hasSpectatorData(player)) return PlayerStatus.SPECTATOR;
        else return PlayerStatus.NOT_IN_GAME;
    }

}
