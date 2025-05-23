package com.shanebeestudios.hg.plugin.managers;

import com.shanebeestudios.hg.api.data.KitData;
import com.shanebeestudios.hg.api.data.KitEntry;
import com.shanebeestudios.hg.api.game.Game;
import com.shanebeestudios.hg.api.parsers.ItemParser;
import com.shanebeestudios.hg.api.util.Util;
import com.shanebeestudios.hg.plugin.HungerGames;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * General manager for kits
 */
public class KitManager {

    private final HungerGames plugin;
    private final ItemManager itemManager;
    private KitData defaultKitData;

    public KitManager(HungerGames plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
        loadDefaultKits();
    }

    private void loadDefaultKits() {
        Util.log("Loading kits:");
        File kitFile = new File(this.plugin.getDataFolder(), "kits.yml");

        if (!kitFile.exists()) {
            this.plugin.saveResource("kits.yml", false);
            Util.log("- New kits.yml file has been <green>successfully generated!");
        }
        YamlConfiguration kitConfig = YamlConfiguration.loadConfiguration(kitFile);
        ConfigurationSection kitsSection = kitConfig.getConfigurationSection("kits");
        assert kitsSection != null;
        this.defaultKitData = kitCreator(kitsSection, null);
        Util.log("- Kits have been <green>successfully loaded!");
    }

    /**
     * Get the default KitData
     *
     * @return Default KitData
     */
    public KitData getDefaultKitData() {
        return this.defaultKitData;
    }

    /**
     * Set the kits for a game from a config
     *
     * @param game        The game to set the kits for
     * @param arenaConfig Config the kit is pulled from
     */
    public void loadGameKits(Game game, ConfigurationSection arenaConfig) {
        ConfigurationSection kitsSection = arenaConfig.getConfigurationSection("kits");
        if (kitsSection == null) return;

        KitData kitData = kitCreator(kitsSection, game);
        Util.log("- Loaded custom kits for arena <white>'<aqua>%s<white>'", game.getGameArenaData().getName());
        game.getGameItemData().setKitData(kitData);
    }

    private KitData kitCreator(ConfigurationSection kitsSection, @Nullable Game game) {
        KitData kit = new KitData();
        String gameName = game != null ? game.getGameArenaData().getName() + ":" : "";
        for (String kitName : kitsSection.getKeys(false)) {
            try {
                ConfigurationSection kitSection = kitsSection.getConfigurationSection(kitName);
                assert kitSection != null;
                // ITEMS
                List<ItemStack> inventoryContent = this.itemManager.loadItems(kitSection);

                // GEAR
                ItemStack helmet = ItemParser.parseItem(kitSection.getConfigurationSection("helmet"));
                ItemStack chestplate = ItemParser.parseItem(kitSection.getConfigurationSection("chestplate"));
                ItemStack leggings = ItemParser.parseItem(kitSection.getConfigurationSection("leggings"));
                ItemStack boots = ItemParser.parseItem(kitSection.getConfigurationSection("boots"));

                // POTION_EFFECTS
                List<PotionEffect> potionEffects = new ArrayList<>();
                if (kitSection.isConfigurationSection("potion_effects")) {
                    ConfigurationSection potionEffectsSection = kitSection.getConfigurationSection("potion_effects");
                    assert potionEffectsSection != null;
                    for (String key : potionEffectsSection.getKeys(false)) {
                        PotionEffect potionEffect = ItemParser.parsePotionEffect(key, potionEffectsSection);
                        potionEffects.add(potionEffect);
                    }
                }

                // PERMISSION
                String permission = null;
                if (kitSection.contains("permission") && !kitSection.getString("permission", "none").equalsIgnoreCase("none"))
                    permission = kitSection.getString("permission");

                KitEntry kitEntry = new KitEntry(game, kitName, inventoryContent, helmet, chestplate, leggings, boots, permission, potionEffects);
                kit.addKitEntry(kitName, kitEntry);
                Util.log("- Loaded kit <white>'<aqua>%s<white>'", gameName + kitName);
            } catch (Exception e) {
                Util.log("-------------------------------------------");
                Util.log("<yellow>Unable to load kit " + gameName + kitName + "! (for a more detailed message enable 'debug' in config and reload)");
                Util.log("-------------------------------------------");
                Util.debug(e);
            }
        }
        return kit;
    }

}
