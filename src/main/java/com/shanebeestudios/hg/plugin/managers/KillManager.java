package com.shanebeestudios.hg.plugin.managers;

import com.shanebeestudios.hg.api.data.Leaderboard;
import com.shanebeestudios.hg.api.events.PlayerDeathGameEvent;
import com.shanebeestudios.hg.api.game.Game;
import com.shanebeestudios.hg.api.game.GameCommandData;
import com.shanebeestudios.hg.api.game.GamePlayerData;
import com.shanebeestudios.hg.api.registry.Registries;
import com.shanebeestudios.hg.api.util.ItemUtils;
import com.shanebeestudios.hg.api.util.Util;
import com.shanebeestudios.hg.plugin.HungerGames;
import com.shanebeestudios.hg.plugin.configs.Config;
import com.shanebeestudios.hg.plugin.configs.Language;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏内死亡事件管理器
 */
@SuppressWarnings("UnstableApiUsage")
public class KillManager {

    private final HungerGames plugin;
    private final Language lang;
    private final Leaderboard leaderboard;
    private final Map<EntityType, String> entityTypeMessages = new HashMap<>();
    private final Map<DamageType, String> damageTypeMessages = new HashMap<>();

    public KillManager(HungerGames plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLang();
        this.leaderboard = plugin.getLeaderboard();

        this.lang.death_message_entity_types.forEach((entityTypeString, message) -> {
            NamespacedKey key = NamespacedKey.fromString(entityTypeString);
            if (key != null) {
                EntityType entityType = Registries.ENTITY_TYPE_REGISTRY.get(key);
                if (entityType != null) {
                    this.entityTypeMessages.put(entityType, message);
                } else {
                    Util.warning("Invalid EntityType '%s', check lang file", key);
                }
            } else {
                Util.warning("Invalid key for EntityType '%s', check lang file", entityTypeString);
            }
        });
        this.lang.death_message_damage_types.forEach((damageTypeString, message) -> {
            NamespacedKey key = NamespacedKey.fromString(damageTypeString);
            if (key != null) {
                DamageType damageType = Registries.DAMAGE_TYPE_REGISTRY.get(key);
                if (damageType != null) {
                    this.damageTypeMessages.put(damageType, message);
                } else {
                    Util.warning("Invalid DamageType '%s', check lang file", key);
                }
            } else {
                Util.warning("Invalid key for DamageType '%s', check lang file", damageTypeString);
            }
        });
    }

    /**
     * 获取玩家自然死亡(非实体导致的死亡)时的死亡消息
     *
     * @param damageSource 伤害来源
     * @param victim       死亡的玩家
     * @return 玩家死亡时将发送的消息
     */
    @SuppressWarnings("UnstableApiUsage")
    public String getNaturalDeathString(DamageSource damageSource, Player victim) {
        String damageType = this.damageTypeMessages.get(damageSource.getDamageType());
        if (damageType != null) {
            return damageType.replace("<player>", victim.getName());
        } else {
            TranslatableComponent translatable = Component.translatable(damageSource.getDamageType().getTranslationKey());
            String cause = Util.unMini(translatable);
            return this.lang.death_messages_other
                .replace("<player>", victim.getName())
                .replace("<cause>", cause);
        }
    }

    /**
     * 获取玩家被实体杀死时的死亡消息
     *
     * @param victimName 死亡的玩家名称
     * @param killer     杀死玩家的实体
     * @return 包含受害者名称和凶手信息的死亡字符串
     */
    public String getKillString(String victimName, DamageSource damageSource, Entity killer) {
        // Custom death message from mobs.yml
        if (killer.hasMetadata("death-message")) {
            return killer.getMetadata("death-message").getFirst().asString().replace("<player>", victimName);
        }

        // Projectile shooter
        if (killer instanceof Projectile projectile && projectile.getShooter() instanceof Player playerShooter) {
            return getPlayerKillString(victimName, playerShooter, projectile);
        }

        if (killer instanceof Player playerKiller) {
            return getPlayerKillString(victimName, playerKiller, null);
        }

        if (this.entityTypeMessages.containsKey(killer.getType())) {
            return this.entityTypeMessages.get(killer.getType()).replace("<player>", victimName);
        }

        TranslatableComponent translatable = Component.translatable(damageSource.getDamageType().getTranslationKey());
        String cause = Util.unMini(translatable);
        return this.lang.death_messages_other
            .replace("<player>", victimName)
            .replace("<cause>", cause);
    }

    @SuppressWarnings("ReplaceNullCheck")
    private String getPlayerKillString(String victimName, Player killer, Projectile projectile) {
        String weaponName;
        if (projectile != null) {
            if (projectile instanceof AbstractArrow arrow) {
                ItemStack weapon = arrow.getWeapon();
                if (weapon != null) {
                    weaponName = Util.unMini(weapon.getData(DataComponentTypes.ITEM_NAME));
                } else {
                    weaponName = Util.unMini(arrow.getItemStack().getData(DataComponentTypes.ITEM_NAME));
                }
            } else {
                // Fallback, just in case
                weaponName = "bow and arrow";
            }
        } else if (killer.getInventory().getItemInMainHand().getType() == Material.AIR) {
            weaponName = "fist";
        } else {
            ItemStack itemStack = killer.getInventory().getItemInMainHand();
            Component data = itemStack.getData(DataComponentTypes.ITEM_NAME);
            weaponName = Util.unMini(data);
        }
        return this.entityTypeMessages.get(EntityType.PLAYER)
            .replace("<player>", victimName)
            .replace("<killer>", killer.getName())
            .replace("<weapon>", weaponName);
    }

    /**
     * 检查射击者是否为玩家
     *
     * @param projectile 击中玩家的箭
     * @return 如果箭是由玩家射出的则返回true
     */
    public boolean isShotByPlayer(Entity projectile) {
        return projectile instanceof Projectile p && p.getShooter() instanceof Player;
    }

    /**
     * 获取射出此箭的玩家
     *
     * @param projectile 要查询的箭
     * @return 射出箭的玩家
     */
    public @Nullable Player getPlayerShooter(Projectile projectile) {
        if (projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    @SuppressWarnings("UnstableApiUsage")
    public void processDeath(Player player, Game game, Entity attacker, DamageSource damageSource) {
        List<ItemStack> drops = dropInventoryOfPlayer(player);
        player.setHealth(20);
        player.getInventory().clear();
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            GamePlayerData gamePlayerData = game.getGamePlayerData();
            String deathString;

            DamageType damageType = damageSource.getDamageType();

            // Player attack
            if (attacker instanceof Player attackerPlayer) {
                gamePlayerData.addKill(attackerPlayer);
                this.leaderboard.addStat(attackerPlayer, Leaderboard.Stats.KILLS);
                deathString = getKillString(player.getName(), damageSource, attacker);
            }
            // Projectile attack
            else if (attacker instanceof Projectile projectile) {
                deathString = getKillString(player.getName(), damageSource, attacker);
                if (isShotByPlayer(attacker)) {
                    Player shooter = getPlayerShooter(projectile);
                    if (shooter != null && shooter != player) {
                        gamePlayerData.addKill(shooter);
                        this.leaderboard.addStat(shooter, Leaderboard.Stats.KILLS);
                    }
                }
            }
            // Mob attack
            else if (damageType == DamageType.MOB_ATTACK || damageType == DamageType.MOB_ATTACK_NO_AGGRO) {
                deathString = getKillString(player.getName(), damageSource, attacker);
            }
            // Natural death
            else {
                deathString = getNaturalDeathString(damageSource, player);
            }

            // 向游戏中所有玩家发送死亡消息
            gamePlayerData.messageAllActivePlayers(this.lang.death_messages_prefix + " <light_purple>" + deathString);

            this.leaderboard.addStat(player, Leaderboard.Stats.DEATHS);
            this.leaderboard.addStat(player, Leaderboard.Stats.GAMES);

            for (Player alive : game.getGamePlayerData().getPlayers()) {
                if (alive != null && player != alive) {
                    alive.playSound(alive.getLocation(), Config.SOUNDS_DEATH, 5, 1);
                }
            }

            gamePlayerData.leaveGame(player, true);
            game.getGameCommandData().runCommands(GameCommandData.CommandType.DEATH, player);

            // 调用我们的死亡事件以便其他插件可以获取这个假死亡
            PlayerDeathGameEvent event = new PlayerDeathGameEvent(player, damageSource, drops, deathString, game);
            event.callEvent();
            // 调用bukkit玩家死亡事件以便其他插件也可以获取
            PlayerDeathEvent playerDeathEvent = new PlayerDeathEvent(player, damageSource,
                drops, 0, 0, 0, 0,
                Util.getMini(deathString), false);
            playerDeathEvent.callEvent();

            Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> checkStick(game), 40L);
        }, 1);
    }

    private List<ItemStack> dropInventoryOfPlayer(Player player) {
        List<ItemStack> drops = new ArrayList<>();
        PlayerInventory inventory = player.getInventory();
        Location location = player.getLocation();
        World world = player.getWorld();

        for (ItemStack itemStack : inventory.getStorageContents()) {
            if (itemStack != null && itemStack.getType() != Material.AIR && !ItemUtils.isCursed(itemStack)) {
                drops.add(itemStack);
                world.dropItemNaturally(location, itemStack).setPersistent(false);
            }
        }
        for (ItemStack itemStack : inventory.getArmorContents()) {
            if (itemStack != null && itemStack.getType() != Material.AIR && !ItemUtils.isCursed(itemStack)) {
                drops.add(itemStack);
                world.dropItemNaturally(location, itemStack).setPersistent(false);
            }
        }
        return drops;
    }

    private void checkStick(Game game) {
        if (Config.SETTINGS_PLAYERS_FOR_TRACKING_STICK == game.getGamePlayerData().getPlayers().size()) {
            for (Player player : game.getGamePlayerData().getPlayers()) {
                Util.sendMessage(player, this.lang.item_tracking_stick_bar);
                Util.sendMessage(player, this.lang.item_tracking_stick_new1);
                Util.sendMessage(player, this.lang.item_tracking_stick_new2);
                Util.sendMessage(player, this.lang.item_tracking_stick_bar);
                player.getInventory().addItem(ItemUtils.getTrackingStick());
            }
        }
    }

}
