package com.shanebeestudios.hg.plugin.managers;

import com.shanebeestudios.hg.api.data.MobData;
import com.shanebeestudios.hg.api.data.MobEntry;
import com.shanebeestudios.hg.api.game.Game;
import com.shanebeestudios.hg.api.parsers.ItemParser;
import com.shanebeestudios.hg.api.registry.Registries;
import com.shanebeestudios.hg.api.util.NBTApi;
import com.shanebeestudios.hg.api.util.Util;
import com.shanebeestudios.hg.plugin.HungerGames;
import com.shanebeestudios.hg.plugin.configs.Config;
import io.lumine.mythic.api.mobs.MythicMob;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;

/**
 * 游戏内生物生成管理类
 * <p>通过 {@link HungerGames#getMobManager()} 获取该类的实例</p>
 */
@SuppressWarnings("unused")
public class MobManager {

    private final HungerGames plugin;
    private final Random random = new Random();
    private MobData defaultMobData;

    public MobManager(HungerGames plugin) {
        this.plugin = plugin;
        loadDefaultMobs();
    }

    /**
     * 加载默认生物配置
     */
    private void loadDefaultMobs() {
        Util.log("正在加载生物配置:");
        File kitFile = new File(this.plugin.getDataFolder(), "mobs.yml");

        if (!kitFile.exists()) {
            this.plugin.saveResource("mobs.yml", false);
            Util.log("- 新的 mobs.yml 文件已<green>成功生成!");
        }
        YamlConfiguration mobConfig = YamlConfiguration.loadConfiguration(kitFile);
        ConfigurationSection mobsSection = mobConfig.getConfigurationSection("mobs");
        assert mobsSection != null;
        this.defaultMobData = createMobData(mobsSection, null);
        Util.log("- <aqua>%s <grey>个生物已<green>成功加载!", this.defaultMobData.getMobCount());
    }

    /**
     * 获取默认的生物数据
     * <p>数据来自 mobs.yml 文件</p>
     *
     * @return 默认的生物数据
     */
    public MobData getDefaultMobData() {
        return this.defaultMobData;
    }

    /**
     * 从竞技场配置中加载生物数据
     *
     * @param game        游戏实例，用于添加数据
     * @param arenaConfig 配置节，从中获取生物数据
     */
    public void loadGameMobs(Game game, ConfigurationSection arenaConfig) {
        ConfigurationSection mobsSection = arenaConfig.getConfigurationSection("mobs");
        if (mobsSection == null) return;

        MobData mobData = createMobData(mobsSection, game);
        Util.log("- 为竞技场 <white>'<aqua>%s<white>' <grey>加载了 <aqua>%s <grey>个自定义生物",
            game.getGameArenaData().getName(), mobData.getMobCount());
        game.getGameEntityData().setMobData(mobData);
    }

    /**
     * 创建生物数据
     *
     * @param mobsSection 配置节，包含生物数据
     * @param game        游戏实例，可为null
     * @return 创建好的生物数据
     */
    private MobData createMobData(ConfigurationSection mobsSection, @Nullable Game game) {
        MobData mobData = new MobData();
        int count = 0;
        String gameName = game != null ? game.getGameArenaData().getName() + ":" : "";
        for (String time : Arrays.asList("day", "night")) {
            if (!mobsSection.contains(time)) continue;
            ConfigurationSection timeSection = mobsSection.getConfigurationSection(time);
            assert timeSection != null;

            for (String sectionKey : timeSection.getKeys(false)) {
                String mobEntryKey = gameName + time + ":" + sectionKey;
                ConfigurationSection mobSection = timeSection.getConfigurationSection(sectionKey);
                assert mobSection != null;

                String typeString = mobSection.getString("type");
                if (typeString == null) continue;

                MobEntry mobEntry;
                // 神话生物
                if (typeString.startsWith("MM:")) {
                    if (this.plugin.getMythicMobManager() == null) continue;

                    String mythicMob = typeString.replace("MM:", "");
                    double level = mobSection.getDouble("level", 0);

                    Optional<MythicMob> mob = this.plugin.getMythicMobManager().getMythicMob(mythicMob);
                    if (mob.isPresent()) {
                        mobEntry = new MobEntry(mobEntryKey, mob.get(), level);
                    } else {
                        Util.warning("无效的神话生物: %s", mythicMob);
                        continue;
                    }

                }
                // 普通生物
                else {
                    // 生物类型
                    NamespacedKey namespacedKey = NamespacedKey.fromString(typeString);
                    if (namespacedKey == null) {
                        Util.log("<red>无效的生物类型 <white>'<yellow>%s<white>'", typeString);
                        continue;
                    }
                    EntityType entityType = Registries.ENTITY_TYPE_REGISTRY.get(namespacedKey);
                    if (entityType == null) {
                        Util.log("<red>无效的生物类型 <white>'<yellow>%s<white>'", namespacedKey);
                        continue;
                    }

                    mobEntry = new MobEntry(mobEntryKey, entityType);
                    if (mobSection.contains("name")) {
                        String name = mobSection.getString("name");
                        mobEntry.setName(Util.getMini(name));
                    }

                    // 装备
                    ConfigurationSection gearSection = mobSection.getConfigurationSection("gear");
                    if (gearSection != null) {
                        for (EquipmentSlot slot : EquipmentSlot.values()) {
                            String slotName = slot.name().toLowerCase(Locale.ROOT);
                            if (gearSection.contains(slotName)) {
                                ConfigurationSection itemSection = gearSection.getConfigurationSection(slotName);
                                ItemStack itemStack = ItemParser.parseItem(itemSection);
                                if (itemStack != null) {
                                    mobEntry.addGear(slot, itemStack);
                                }
                            }
                        }
                    }

                    // 药水效果
                    if (mobSection.contains("potion_effects")) {
                        List<PotionEffect> potionEffects = new ArrayList<>();
                        if (mobSection.isConfigurationSection("potion_effects")) {
                            ConfigurationSection potionEffectsSection = mobSection.getConfigurationSection("potion_effects");
                            assert potionEffectsSection != null;
                            for (String key : potionEffectsSection.getKeys(false)) {
                                PotionEffect potionEffect = ItemParser.parsePotionEffect(key, potionEffectsSection);
                                potionEffects.add(potionEffect);
                            }
                        }
                        mobEntry.addPotionEffects(potionEffects);
                    }
                }

                // 属性
                if (mobSection.isConfigurationSection("attributes")) {
                    ConfigurationSection attributesSection = mobSection.getConfigurationSection("attributes");
                    assert attributesSection != null;
                    for (String key : attributesSection.getKeys(false)) {
                        NamespacedKey attributeKey = NamespacedKey.fromString(key);
                        if (attributeKey == null) {
                            Util.warning("属性键 '%s' 对生物条目 '%s:%s' 无效",
                                key, time, sectionKey);
                            continue;
                        }
                        Attribute attribute = Registries.ATTRIBUTE_REGISTRY.get(attributeKey);
                        if (attribute == null) {
                            Util.warning("无效的属性 '%s' 对生物条目 '%s:%s'",
                                attributeKey.toString(), time, sectionKey);
                            continue;
                        }
                        double value = attributesSection.getDouble(key);
                        mobEntry.addAttribute(attribute, value);
                    }
                }

                // NBT数据
                if (mobSection.contains("nbt")) {
                    String nbtString = mobSection.getString("nbt");
                    if (nbtString != null) {
                        String validated = NBTApi.validateNBT(nbtString);
                        if (validated != null) {
                            Util.warning("无效的NBT数据 '%s' 对生物条目 '%s:%s'",
                                nbtString, time, sectionKey);
                        } else {
                            mobEntry.setNbt(nbtString);
                        }
                    }
                }

                // 死亡消息
                String deathMessage = mobSection.getString("death_message", null);
                if (deathMessage != null) {
                    mobEntry.setDeathMessage(deathMessage);
                }
                int weight = mobSection.getInt("weight", 1);
                count++;
                for (int i = 1; i <= weight; i++) {
                    if (time.equalsIgnoreCase("day")) {
                        mobData.addDayMob(mobEntry);
                    } else {
                        mobData.addNightMob(mobEntry);
                    }
                }
                if (Config.SETTINGS_DEBUG) {
                    Util.log("- 已加载生物条目 <white>'<aqua>%s<white>'", mobEntryKey);
                }
            }
        }
        mobData.setMobCount(count);
        return mobData;
    }


}
