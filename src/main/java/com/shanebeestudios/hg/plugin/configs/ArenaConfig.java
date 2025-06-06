package com.shanebeestudios.hg.plugin.configs;

import com.shanebeestudios.hg.api.game.Game;
import com.shanebeestudios.hg.api.game.GameArenaData;
import com.shanebeestudios.hg.api.game.GameBorderData;
import com.shanebeestudios.hg.api.game.GameRegion;
import com.shanebeestudios.hg.api.parsers.LocationParser;
import com.shanebeestudios.hg.api.status.Status;
import com.shanebeestudios.hg.api.util.Pair;
import com.shanebeestudios.hg.api.util.Util;
import com.shanebeestudios.hg.plugin.HungerGames;
import com.shanebeestudios.hg.plugin.managers.GameManager;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件的数据处理器
 */
public class ArenaConfig {

    private final HungerGames plugin;
    private final GameManager gameManager;
    private File arenaDirectory;
    private final Map<String, Pair<File, FileConfiguration>> fileConfigMap = new HashMap<>();

    /**
     * @hidden 内部使用
     */
    @ApiStatus.Internal
    public ArenaConfig(HungerGames plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
        loadAllArenas();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private Pair<File, FileConfiguration> getOrCreateConfig(String name) {
        if (this.fileConfigMap.containsKey(name)) {
            return this.fileConfigMap.get(name);
        }
        File file = new File(this.arenaDirectory, name + ".yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Pair<File, FileConfiguration> fileConfig = Pair.of(file, config);
        this.fileConfigMap.put(name, fileConfig);
        return fileConfig;
    }

    private void saveArenaConfig(String name) {
        Pair<File, FileConfiguration> fileConfig = this.fileConfigMap.get(name);
        try {
            fileConfig.second().save(fileConfig.first());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void loadAllArenas() {
        Util.log("正在加载竞技场:");

        this.arenaDirectory = new File(this.plugin.getDataFolder(), "arenas");
        if (!this.arenaDirectory.exists()) {
            if (!this.arenaDirectory.mkdirs()) {
                Util.warning("无法创建竞技场目录!");
            }
        }
        int readyCount = 0;
        int brokenCount = 0;
        for (File arenaFile : this.arenaDirectory.listFiles()) {
            String name = arenaFile.getName();
            if (name.endsWith(".yml")) {
                YamlConfiguration arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);
                name = name.replace(".yml", "");
                boolean isReady = loadArena(arenaConfig, name);
                this.fileConfigMap.put(name, Pair.of(arenaFile, arenaConfig));
                if (isReady) {
                    readyCount++;
                } else {
                    brokenCount++;
                }
            }
        }
        if (readyCount == 0 && brokenCount == 0) {
            Util.log("- <red>未找到任何竞技场 <grey>是时候创建一些了!");
        }
        if (readyCount > 0) {
            Util.log("- <aqua>%s <grey>个竞技场已<green>成功加载!", readyCount);
        }
        if (brokenCount > 0) {
            Util.log("- <aqua>%s <grey>个竞技场<red>加载失败!", brokenCount);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public boolean loadArena(FileConfiguration arenaConfig, String arenaName) {
        boolean isReady = true;
        List<Location> spawns = new ArrayList<>();
        Location lobbysign = null;
        int timer = 0;
        int cost = 0;
        int minPlayers = 0;
        int maxPlayers = 0;
        int freeRoamTime = 0;
        GameRegion gameRegion = null;
        List<String> commands;

        // INFO
        ConfigurationSection infoSection = arenaConfig.getConfigurationSection("info");
        try {
            timer = infoSection.getInt("timer");
            minPlayers = infoSection.getInt("min_players");
            maxPlayers = infoSection.getInt("max_players");
            freeRoamTime = infoSection.getInt("free_roam_time");
        } catch (Exception e) {
            Util.warning("无法加载竞技场 '" + arenaName + "' 的信息!");
            isReady = false;
        }
        try {
            cost = infoSection.getInt("cost");
        } catch (Exception ignore) {
        }

        // LOCATIONS
        ConfigurationSection locationsSection = arenaConfig.getConfigurationSection("locations");
        try {
            lobbysign = LocationParser.getBlockLocFromString(locationsSection.getString("lobby_sign"));
        } catch (Exception e) {
            Util.warning("无法加载竞技场 '" + arenaName + "' 的大厅标志!");
            Util.debug(e);
            isReady = false;
        }

        try {
            for (String location : locationsSection.getStringList("spawns")) {
                spawns.add(LocationParser.getLocFromString(location));
            }
        } catch (Exception e) {
            Util.warning("无法加载竞技场 '" + arenaName + "' 的随机出生点!");
            isReady = false;
        }

        // REGION
        try {
            ConfigurationSection regionSection = arenaConfig.getConfigurationSection("region");
            String world = regionSection.getString("world");
            BoundingBox boundingBox = regionSection.getObject("bounding_box", BoundingBox.class);
            gameRegion = GameRegion.loadFromConfig(world, boundingBox);
        } catch (Exception e) {
            Util.warning("无法加载竞技场 " + arenaName + " 的区域边界!");
            isReady = false;
        }

        Game game = new Game(arenaName, gameRegion, spawns, lobbysign, timer, minPlayers, maxPlayers, freeRoamTime, isReady, cost);
        this.gameManager.loadGameFromConfig(arenaName, game);
        GameArenaData gameArenaData = game.getGameArenaData();

        World world = gameRegion.getWorld();
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            Util.warning("世界 '%s' 中竞技场 '%s' 的难度设置为和平模式...", world.getName(), arenaName);
            Util.warning("这可能会对游戏产生负面影响，请考虑提高难度");
        }

        // KITS
        this.plugin.getKitManager().loadGameKits(game, arenaConfig);
        // MOBS
        this.plugin.getMobManager().loadGameMobs(game, arenaConfig);
        // ITEMS
        this.plugin.getItemManager().loadGameItems(game, arenaConfig);

        // BORDER
        if (arenaConfig.isSet("game_border")) {
            ConfigurationSection borderSection = arenaConfig.getConfigurationSection("game_border");
            GameBorderData gameBorderData = game.getGameBorderData();
            if (borderSection.isSet("center_location")) {
                Location borderCenter = LocationParser.getBlockLocFromString(borderSection.getString("center_location"));
                gameBorderData.setCenterLocation(borderCenter);
            }
            if (borderSection.isSet("final_size")) {
                int borderSize = borderSection.getInt("final_size");
                gameBorderData.setFinalBorderSize(borderSize);
            }
            if (borderSection.isSet("countdown_start") && borderSection.isSet("countdown_end")) {
                int countdownStart = borderSection.getInt("countdown_start");
                int countdownEnd = borderSection.getInt("countdown_end");
                gameBorderData.setBorderCountdownStart(countdownStart);
                gameBorderData.setBorderCountdownEnd(countdownEnd);
            }
        }

        // COMMANDS
        if (arenaConfig.isSet("commands")) {
            commands = arenaConfig.getStringList("commands");
        } else {
            commands = Collections.singletonList("none");
        }
        game.getGameCommandData().setCommands(commands);

        // CHEST REFILL
        if (arenaConfig.isConfigurationSection("chest_refill")) {
            ConfigurationSection chestRefillSection = arenaConfig.getConfigurationSection("chest_refill");
            if (chestRefillSection.isSet("time")) {
                int chestRefill = chestRefillSection.getInt("time");
                gameArenaData.setChestRefillTime(chestRefill);
            }
            if (chestRefillSection.isSet("repeat")) {
                int chestRefillRepeat = chestRefillSection.getInt("repeat");
                gameArenaData.setChestRefillRepeat(chestRefillRepeat);
            }
        }
        try {
            if (locationsSection.isSet("exit")) {
                Location exitLocation = LocationParser.getLocFromString(locationsSection.getString("exit"));
                gameArenaData.setExitLocation(exitLocation);
            }

        } catch (Exception exception) {
            Util.log("- <yellow>无法为竞技场 '%s' 设置退出位置，将使用世界 '%s' 的出生点代替",
                arenaName, world.getName());
            Util.debug(exception);
        }
        if (gameArenaData.getStatus() == Status.BROKEN) {
            Util.warning("<red>无法正确初始化竞技场 <white>'<aqua>%s<white>'<grey>", arenaName);
            Util.warning("运行 <white>'<aqua>/hg debug %s<white>' <yellow>命令来检查竞技场状态", arenaName);
            isReady = false;
        } else {
            Util.log("- 已加载竞技场 <white>'<aqua>%s<white>'<grey>", arenaName);
        }
        return isReady;
    }

    /**
     * 将游戏保存到配置
     *
     * @param game 要保存的游戏
     */
    public void saveGameToConfig(Game game) {
        String arenaName = game.getGameArenaData().getName();
        Pair<File, FileConfiguration> fileConfig = getOrCreateConfig(arenaName);
        FileConfiguration gameSection = fileConfig.second();

        GameArenaData gameArenaData = game.getGameArenaData();

        // CHEST REFILL
        ConfigurationSection chestRefillSection = gameSection.createSection("chest_refill");
        int chestRefillTime = gameArenaData.getChestRefillTime();
        if (chestRefillTime > 0) chestRefillSection.set("time", chestRefillTime);
        int chestRefillRepeat = gameArenaData.getChestRefillRepeat();
        if (chestRefillRepeat > 0) chestRefillSection.set("repeat", chestRefillRepeat);

        // INFO
        ConfigurationSection infoSection = gameSection.createSection("info");
        infoSection.set("cost", gameArenaData.getCost());
        infoSection.set("timer", gameArenaData.getTimer());
        infoSection.set("min_players", gameArenaData.getMinPlayers());
        infoSection.set("max_players", gameArenaData.getMaxPlayers());
        infoSection.set("free_roam_time", gameArenaData.getFreeRoamTime());

        // REGION
        ConfigurationSection regionSection = gameSection.createSection("region");
        regionSection.set("world", gameArenaData.getGameRegion().getWorld().getName());
        regionSection.set("bounding_box", gameArenaData.getGameRegion().getBoundingBox());

        // COMMANDS
        gameSection.set("commands", game.getGameCommandData().getCommands());

        // LOCATIONS
        ConfigurationSection locationsSection = gameSection.createSection("locations");
        List<String> spawns = new ArrayList<>();
        gameArenaData.getSpawns().forEach(spawn -> spawns.add(LocationParser.locToString(spawn)));
        locationsSection.set("spawns", spawns);

        Location exit = gameArenaData.getExitLocation();
        if (exit != null) {
            locationsSection.set("exit", LocationParser.blockLocToString(exit));
        }
        locationsSection.set("lobby_sign", LocationParser.blockLocToString(game.getLobbyLocation()));

        // BORDER
        GameBorderData borderData = game.getGameBorderData();
        if (!borderData.isDefault()) {
            ConfigurationSection borderSection = gameSection.createSection("game_border");
            Location centerLocation = borderData.getCenterLocation();
            if (centerLocation != null) {
                String locString = LocationParser.blockLocToString(centerLocation);
                borderSection.set("center_location", locString);
            }
            borderSection.set("final_size", borderData.getFinalBorderSize());
            borderSection.set("countdown_start", borderData.getBorderCountdownStart());
            borderSection.set("countdown_end", borderData.getBorderCountdownEnd());
        }

        saveArenaConfig(arenaName);
    }

    /**
     * 移除一个竞技场
     *
     * @param game 要移除的游戏
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void removeArena(Game game) {
        String name = game.getGameArenaData().getName();
        Pair<File, FileConfiguration> fileFileConfigurationPair = this.fileConfigMap.get(name);
        fileFileConfigurationPair.first().delete();
        this.fileConfigMap.remove(name);
    }

}
