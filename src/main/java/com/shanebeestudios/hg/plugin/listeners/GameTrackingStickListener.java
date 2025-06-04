package com.shanebeestudios.hg.plugin.listeners;

import com.shanebeestudios.hg.api.data.PlayerData;
import com.shanebeestudios.hg.api.game.Game;
import com.shanebeestudios.hg.api.util.ItemUtils;
import com.shanebeestudios.hg.api.util.Util;
import com.shanebeestudios.hg.plugin.HungerGames;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * 追踪棒监听器
 * <p>处理玩家使用追踪棒的相关事件</p>
 */
public class GameTrackingStickListener extends GameListenerBase {

    public GameTrackingStickListener(HungerGames plugin) {
        super(plugin);
    }

    /**
     * 玩家使用追踪棒事件
     * @param event 玩家交互事件
     */
    @EventHandler
    private void onClickWithStick(PlayerInteractEvent event) {
        Action action = event.getAction();
        // 只处理左键点击事件
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = event.getItem();
            // 检查玩家是否在游戏中且手持追踪棒
            if (this.playerManager.hasPlayerData(player) && ItemUtils.isTrackingStick(item)) {
                event.setCancelled(true);
                useTrackStick(player, item);
            }
        }
    }

    /**
     * 使用追踪棒追踪最近玩家
     * @param player 使用追踪棒的玩家
     * @param itemStack 追踪棒物品
     */
    private void useTrackStick(Player player, ItemStack itemStack) {
        PlayerData playerData = this.playerManager.getPlayerData(player);
        assert playerData != null;
        final Game game = playerData.getGame();
        // 获取游戏区域边界框
        BoundingBox box = game.getGameArenaData().getGameRegion().getBoundingBox();

        // 计算搜索范围
        int distance = (int) Math.min(120, Math.max(box.getWidthX() / 2, box.getWidthZ() / 2));
        // 搜索附近玩家
        for (Entity nearbyEntity : player.getNearbyEntities(distance, 50, distance)) {
            if (nearbyEntity instanceof Player nearbyPlayer) {
                // 检查是否为同一游戏中的玩家
                if (!game.getGamePlayerData().getPlayers().contains(nearbyPlayer)) continue;

                Location location = nearbyEntity.getLocation();
                int range = (int) player.getLocation().distance(location);
                // 发送追踪结果消息
                Util.sendMessage(player, this.lang.item_tracking_stick_nearest
                    .replace("<player>", nearbyEntity.getName())
                    .replace("<range>", "" + range)
                    .replace("<location>", getDirection(player.getLocation().getBlock(), location.getBlock())));
                // 消耗追踪棒耐久度
                itemStack.damage(1, player);
                player.updateInventory();
                return;
            }
        }
        // 未找到附近玩家
        Util.sendMessage(player, this.lang.item_tracking_stick_no_near);
    }

    /**
     * 获取两个方块之间的方向
     * @param block 起始方块
     * @param block1 目标方块
     * @return 方向字符串
     */
    private String getDirection(Block block, Block block1) {
        Vector bv = block.getLocation().toVector();
        Vector bv2 = block1.getLocation().toVector();
        float y = (float) angle(bv.getX(), bv.getZ(), bv2.getX(), bv2.getZ());
        float cal = (y * 10);
        int c = (int) cal;
        // 根据角度计算方向
        if (c <= 1 && c >= -1) {
            return "南";
        } else if (c > -14 && c < -1) {
            return "西南";
        } else if (c >= -17 && c <= -14) {
            return "西";
        } else if (c > -29 && c < -17) {
            return "西北";
        } else if (c > 17 && c < 29) {
            return "东北";
        } else if (c <= 17 && c >= 14) {
            return "东";
        } else if (c > 1 && c < 14) {
            return "东南";
        } else if (c <= 29 && c >= -29) {
            return "北";
        } else {
            return "未知";
        }
    }

    /**
     * 计算两点之间的角度
     * @param d 起始点X坐标
     * @param e 起始点Z坐标
     * @param f 目标点X坐标
     * @param g 目标点Z坐标
     * @return 角度值
     */
    private double angle(double d, double e, double f, double g) {
        // 计算向量差
        int x = (int) (f - d);
        int z = (int) (g - e);

        return Math.atan2(x, z);
    }

}
