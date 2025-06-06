package com.shanebeestudios.hg.plugin.listeners;

import com.shanebeestudios.hg.api.data.ItemData;
import com.shanebeestudios.hg.api.data.PlayerData;
import com.shanebeestudios.hg.api.game.Game;
import com.shanebeestudios.hg.api.game.GameArenaData;
import com.shanebeestudios.hg.api.game.GameBlockData;
import com.shanebeestudios.hg.api.status.Status;
import com.shanebeestudios.hg.api.util.BlockUtils;
import com.shanebeestudios.hg.api.util.Util;
import com.shanebeestudios.hg.plugin.HungerGames;
import com.shanebeestudios.hg.plugin.configs.Config;
import com.shanebeestudios.hg.plugin.permission.Permissions;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class GameBlockListener extends GameListenerBase {

    public GameBlockListener(HungerGames plugin) {
        super(plugin);
        BlockUtils.setupBuilder();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onAttack(EntityDamageByEntityEvent event) {
        // 阻止玩家从道具框架中移除道具
        if (event.getEntity() instanceof Hanging hanging) {
            handleItemFrame(hanging, event, !Config.ROLLBACK_ALLOW_ITEMFRAME_TAKE);
        }
    }

    @EventHandler
    private void hangingPopOff(HangingBreakEvent event) {
        handleItemFrame(event.getEntity(), event, event instanceof HangingBreakByEntityEvent);
    }

    private void handleItemFrame(Hanging hanging, Event event, boolean cancel) {
        if (this.gameManager.isInRegion(hanging.getLocation())) {
            Game game = this.gameManager.getGame(hanging.getLocation());
            switch (game.getGameArenaData().getStatus()) {
                case RUNNING:
                case FREE_ROAM:
                case COUNTDOWN:
                    if (cancel && event instanceof Cancellable cancellable) {
                        cancellable.setCancelled(true);
                    } else if (hanging instanceof ItemFrame itemFrame) {
                        game.getGameBlockData().recordItemFrame(itemFrame);
                    }
            }
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @EventHandler
    private void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (this.playerManager.hasSpectatorData(player)) {
            event.setCancelled(true);
            return;
        }
        if (!this.playerManager.hasPlayerData(player)) {
            if (this.gameManager.isInRegion(block.getLocation()) && !Permissions.COMMAND_CREATE.has(player)) {
                // 防止非游戏玩家在竞技场放置方块
                event.setCancelled(true);
            }
            return;
        }
        PlayerData playerData = this.playerManager.getPlayerData(player);
        Game game = playerData.getGame();
        GameArenaData gameArenaData = game.getGameArenaData();

        if (gameArenaData.getGameRegion().isInRegion(block.getLocation())) {
            if (Config.ROLLBACK_ALLOW_BREAK_BLOCKS) {
                GameBlockData gameBlockData = game.getGameBlockData();
                Status status = gameArenaData.getStatus();
                if (status == Status.RUNNING || (status == Status.ROLLBACK && !Config.ROLLBACK_PROTECT_DURING_FREE_ROAM)) {
                    if (!BlockUtils.isEditableBlock(block.getType())) {
                        Util.sendMessage(player, this.lang.listener_no_edit_block);
                        event.setCancelled(true);
                    } else if (isChest(block)) {
                        gameBlockData.logChest(ItemData.ChestType.PLAYER_PLACED, block.getLocation());
                    }
                } else {
                    Util.sendMessage(player, this.lang.listener_not_running);
                    event.setCancelled(true);
                }
            } else if (!Permissions.COMMAND_CREATE.has(player)) {
                event.setCancelled(true);

            }
        } else {
            if (!playerData.hasGameStared()) return;
            // 防止在赛场外放置方块
            event.setCancelled(true);
        }

    }

    @SuppressWarnings("DataFlowIssue")
    @EventHandler
    private void blockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (this.playerManager.hasSpectatorData(player)) {
            event.setCancelled(true);
            return;
        }
        if (!this.playerManager.hasPlayerData(player)) {
            if (this.gameManager.isInRegion(block.getLocation()) && !Permissions.COMMAND_CREATE.has(player)) {
                // 防止非游戏玩家在竞技场打破方块
                event.setCancelled(true);
            }
            return;
        }
        PlayerData playerData = this.playerManager.getPlayerData(player);
        Game game = playerData.getGame();
        GameArenaData gameArenaData = game.getGameArenaData();

        if (gameArenaData.getGameRegion().isInRegion(block.getLocation())) {
            if (Config.ROLLBACK_ALLOW_BREAK_BLOCKS) {
                Status status = gameArenaData.getStatus();
                if (status == Status.RUNNING || (status == Status.ROLLBACK && !Config.ROLLBACK_PROTECT_DURING_FREE_ROAM)) {
                    if (!BlockUtils.isEditableBlock(block.getType())) {
                        Util.sendMessage(player, this.lang.listener_no_edit_block);
                        event.setCancelled(true);
                    } else if (isChest(block)) {
                        GameBlockData gameBlockData = game.getGameBlockData();
                        gameBlockData.removeChest(block.getLocation());
                    }
                } else {
                    Util.sendMessage(player, this.lang.listener_not_running);
                    event.setCancelled(true);
                }
            } else {
                if (Permissions.COMMAND_CREATE.has(player)) {
                    Status status = gameArenaData.getStatus();
                    switch (status) {
                        case FREE_ROAM:
                        case RUNNING:
                            game.getGameBlockData().removeChest(block.getLocation());
                        default:
                            return;
                    }
                }
                event.setCancelled(true);
            }
        } else {
            if (!playerData.hasGameStared()) return;
            // 防止在赛场外打破方块
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onBucketEmpty(PlayerBucketEmptyEvent event) {
        handleBucketEvent(event, false);
    }

    @EventHandler
    private void onBucketFill(PlayerBucketFillEvent event) {
        handleBucketEvent(event, true);
    }

    @SuppressWarnings("DataFlowIssue")
    private void handleBucketEvent(PlayerBucketEvent event, boolean fill) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        final boolean WATER = event.getBucket() == Material.WATER_BUCKET && BlockUtils.isEditableBlock(Material.WATER);
        final boolean LAVA = event.getBucket() == Material.LAVA_BUCKET && BlockUtils.isEditableBlock(Material.LAVA);

        if (this.gameManager.isInRegion(block.getLocation())) {
            if (Config.ROLLBACK_ALLOW_BREAK_BLOCKS && this.playerManager.hasPlayerData(player)) {
                Game game = this.playerManager.getPlayerData(player).getGame();
                if (game.getGameArenaData().getStatus() == Status.RUNNING || !Config.ROLLBACK_PROTECT_DURING_FREE_ROAM) {
                    if ((fill && !BlockUtils.isEditableBlock(block.getType())) ||
                        (!fill && !(WATER || LAVA))) {
                        Util.sendMessage(player, this.lang.listener_no_edit_block);
                        event.setCancelled(true);
                    }
                } else {
                    Util.sendMessage(player, this.lang.listener_not_running);
                    event.setCancelled(true);
                }
            } else {
                if (this.playerManager.hasPlayerData(player) || !Permissions.COMMAND_CREATE.has(player)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    private void onTrample(PlayerInteractEvent event) {
        if (!Config.ROLLBACK_PREVENT_TRAMPLING) return;
        Player player = event.getPlayer();
        if (this.playerManager.hasSpectatorData(player)) {
            event.setCancelled(true);
            return;
        }
        if (this.gameManager.isInRegion(player.getLocation())) {
            if (event.getAction() == Action.PHYSICAL) {
                assert event.getClickedBlock() != null;
                Material block = event.getClickedBlock().getType();
                if (block == Material.FARMLAND) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // UTIL
    private boolean isChest(Block block) {
        return block.getType() == Material.CHEST || BlockUtils.isBonusBlock(block);
    }

}
