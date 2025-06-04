package com.shanebeestudios.hg.plugin.listeners;

import com.shanebeestudios.hg.api.util.Util;
import com.shanebeestudios.hg.plugin.HungerGames;
import com.shanebeestudios.hg.plugin.permission.Permissions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * 游戏命令监听器
 * <p>处理游戏中玩家的命令执行限制</p>
 */
public class GameCommandListener extends GameListenerBase {

    public GameCommandListener(HungerGames instance) {
        super(instance);
    }

    /**
     * 玩家命令预处理事件
     * @param event 玩家命令预处理事件
     */
    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        // 检查玩家是否有绕过命令限制的权限
        if (Permissions.BYPASS_COMMAND_RESTRICTION.has(player)) return;

        String[] st = event.getMessage().split(" ");
        
        // 防止游戏中的玩家执行非饥饿游戏命令
        if (this.playerManager.isInGame(player) && !st[0].equalsIgnoreCase("/login")) {
            // 允许执行饥饿游戏相关命令
            if (st[0].equalsIgnoreCase("/hg") || st[0].equalsIgnoreCase("/hungergames")) {
                return;
            }
            // 阻止命令执行并发送提示消息
            event.setMessage("/");
            event.setCancelled(true);
            Util.sendMessage(player, this.lang.listener_command_handler_no_command);
        }
        // 防止将玩家传送出竞技场
        else if (("/tp".equalsIgnoreCase(st[0]) || "/teleport".equalsIgnoreCase(st[0])) && st.length >= 2) {
            Player targetPlayer = Bukkit.getServer().getPlayer(st[1]);
            if (targetPlayer != null) {
                // 检查目标玩家是否在游戏中
                if (this.playerManager.hasPlayerData(targetPlayer)) {
                    Util.sendMessage(player, this.lang.listener_command_handler_playing);
                    event.setMessage("/");
                    event.setCancelled(true);
                }
            }
        }
    }

}
