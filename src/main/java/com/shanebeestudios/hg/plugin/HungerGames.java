package com.shanebeestudios.hg.plugin;

import com.shanebeestudios.hg.api.data.Leaderboard;
import com.shanebeestudios.hg.api.region.TaskUtils;
import com.shanebeestudios.hg.api.util.NBTApi;
import com.shanebeestudios.hg.api.util.Util;
import com.shanebeestudios.hg.plugin.commands.MainCommand;
import com.shanebeestudios.hg.plugin.configs.ArenaConfig;
import com.shanebeestudios.hg.plugin.configs.Config;
import com.shanebeestudios.hg.plugin.configs.Language;
import com.shanebeestudios.hg.plugin.listeners.GameBlockListener;
import com.shanebeestudios.hg.plugin.listeners.GameChestListener;
import com.shanebeestudios.hg.plugin.listeners.GameCommandListener;
import com.shanebeestudios.hg.plugin.listeners.GameCompassListener;
import com.shanebeestudios.hg.plugin.listeners.GameDamageListenerBase;
import com.shanebeestudios.hg.plugin.listeners.GameEntityListener;
import com.shanebeestudios.hg.plugin.listeners.GameKitGuiListener;
import com.shanebeestudios.hg.plugin.listeners.GameLobbyListener;
import com.shanebeestudios.hg.plugin.listeners.GamePlayerListener;
import com.shanebeestudios.hg.plugin.listeners.GameTrackingStickListener;
import com.shanebeestudios.hg.plugin.listeners.SessionWandListener;
import com.shanebeestudios.hg.plugin.managers.GameManager;
import com.shanebeestudios.hg.plugin.managers.ItemManager;
import com.shanebeestudios.hg.plugin.managers.KillManager;
import com.shanebeestudios.hg.plugin.managers.KitManager;
import com.shanebeestudios.hg.plugin.managers.MobManager;
import com.shanebeestudios.hg.plugin.managers.Placeholders;
import com.shanebeestudios.hg.plugin.managers.PlayerManager;
import com.shanebeestudios.hg.plugin.managers.SessionManager;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.exceptions.UnsupportedVersionException;
import io.lumine.mythic.api.MythicProvider;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * <b>饥饿游戏插件主类</b>
 * <p>负责插件生命周期管理和核心功能初始化</p>
 */
public class HungerGames extends JavaPlugin {

    // 插件单例实例
    private static HungerGames PLUGIN_INSTANCE;

    private Leaderboard leaderboard;
    private Metrics metrics;

    // 配置文件实例
    private Config config;
    private Language lang;
    private ArenaConfig arenaConfig;

    // 管理器实例
    private ItemManager itemManager;
    private PlayerManager playerManager;
    private KitManager kitManager;
    private GameManager gameManager;
    private KillManager killManager;
    private SessionManager sessionManager;
    private MobManager mobManager;
    private io.lumine.mythic.api.mobs.MobManager mythicMobManager;

    /**
     * 插件加载时执行
     */
    @Override
    public void onLoad() {
        try {
            CommandAPI.onLoad(new CommandAPIBukkitConfig(this)
                .setNamespace("hungergames")
                .verboseOutput(false)
                .silentLogs(true)
                .skipReloadDatapacks(true));
        } catch (UnsupportedVersionException ignore) {
            Util.log("CommandAPI 不支持此版本的 Minecraft，请等待更新");
        }
    }

    /**
     * 插件启用时执行
     */
    @Override
    public void onEnable() {
        if (!Util.isRunningMinecraft(1, 21, 4)) {
            Util.warning("饥饿游戏不支持您的服务器版本！");
            Util.warning("仅支持 1.21.4 及以上版本");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        NBTApi.initializeNBTApi();
        TaskUtils.initialize(this);
        loadPlugin(true);
    }

    /**
     * 加载插件核心功能
     * @param load 是否为初次加载
     */
    @SuppressWarnings("deprecation")
    public void loadPlugin(boolean load) {
        long start = System.currentTimeMillis();
        PLUGIN_INSTANCE = this;

        this.config = new Config(this);

        // 检查MythicMobs插件
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") != null) {
            this.mythicMobManager = MythicProvider.get().getMobManager();
            Util.log("<grey>检测到MythicMobs，MythicMobs挂钩 <green>已启用");
        } else {
            Util.log("<grey>未检测到MythicMobs，MythicMobs挂钩 <red>已禁用");
        }
        this.lang = new Language(this);
        this.itemManager = new ItemManager(this);
        this.playerManager = new PlayerManager();
        this.kitManager = new KitManager(this);
        this.sessionManager = new SessionManager();
        this.mobManager = new MobManager(this);
        this.gameManager = new GameManager(this);
        this.arenaConfig = new ArenaConfig(this);
        this.leaderboard = new Leaderboard(this);
        this.killManager = new KillManager(this);

        // 检查PlaceholderAPI插件
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Placeholders(this).register();
            Util.log("<grey>检测到PlaceholderAPI，占位符 <green>已启用");
        } else {
            Util.log("<grey>未检测到PlaceholderAPI，占位符 <red>已禁用");
        }

        loadCommands();
        loadListeners();

        if (this.getDescription().getVersion().contains("beta")) {
            Util.warning("您正在使用测试版，请谨慎使用");
            Util.warning("问题反馈: <aqua>https://github.com/ShaneBeeStudios/HungerGames/issues");
        }

        setupMetrics();

        Util.log("饥饿游戏 <green>已启用<grey>，耗时 <aqua>%.2f 秒<grey>!", (float) (System.currentTimeMillis() - start) / 1000);
    }

    /**
     * 重载插件
     */
    public void reloadPlugin() {
        unloadPlugin(true);
    }

    /**
     * 卸载插件核心功能
     * @param reload 是否为重载操作
     */
    private void unloadPlugin(boolean reload) {
        this.gameManager.stopAllGames();
        PLUGIN_INSTANCE = null;
        this.config = null;
        this.metrics = null;
        this.mobManager = null;
        this.mythicMobManager = null;
        this.lang = null;
        this.itemManager = null;
        this.kitManager = null;
        this.playerManager = null;
        this.arenaConfig = null;
        this.killManager = null;
        this.gameManager = null;
        this.leaderboard.saveLeaderboard();
        this.leaderboard = null;
        HandlerList.unregisterAll(this);
        if (reload) {
            loadPlugin(false);
        }
    }

    /**
     * 插件禁用时执行
     */
    @Override
    public void onDisable() {
        // 我知道这看起来很奇怪，但这种方法只是
        // 全部为空，以防止内存泄漏
        unloadPlugin(false);
        Util.log("饥饿游戏已禁用！");
    }

    /**
     * 设置统计指标
     */
    private void setupMetrics() {
        this.metrics = new Metrics(this, 25144);
        // 配置统计
        this.metrics.addCustomChart(new DrilldownPie("config", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            map.put("worldborder-enabled", Map.of("" + Config.WORLD_BORDER_ENABLED, 1));
            map.put("chestdrop-enabled", Map.of("" + Config.CHESTS_CHEST_DROP_ENABLED, 1));
            map.put("reward-enabled", Map.of("" + Config.REWARD_ENABLED, 1));
            map.put("spectate-enabled", Map.of("" + Config.SPECTATE_ENABLED, 1));
            return map;
        }));

        // 竞技场数量统计
        this.metrics.addCustomChart(new SimplePie("arenas-count", () ->
            "" + this.gameManager.getGames().size()));
    }

    /**
     * 加载事件监听器
     */
    private void loadListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new GameBlockListener(this), this);
        pluginManager.registerEvents(new GameChestListener(this), this);
        pluginManager.registerEvents(new GameCommandListener(this), this);
        pluginManager.registerEvents(new GameCompassListener(this), this);
        pluginManager.registerEvents(new GameDamageListenerBase(this), this);
        pluginManager.registerEvents(new GameEntityListener(this), this);
        pluginManager.registerEvents(new GameKitGuiListener(this), this);
        pluginManager.registerEvents(new GameLobbyListener(this), this);
        pluginManager.registerEvents(new GamePlayerListener(this), this);
        pluginManager.registerEvents(new GameTrackingStickListener(this), this);
        pluginManager.registerEvents(new SessionWandListener(this), this);
    }

    /**
     * 加载命令
     */
    private void loadCommands() {
        if (CommandAPI.isLoaded()) {
            CommandAPI.onEnable();
            new MainCommand(this);
        }
    }

    // =============== 获取器方法 ===============

    /**
     * 获取插件实例
     * @return 插件实例
     */
    public static HungerGames getPlugin() {
        return PLUGIN_INSTANCE;
    }

    /**
     * 获取击杀管理器
     * @return 击杀管理器实例
     */
    public KillManager getKillManager() {
        return this.killManager;
    }

    /**
     * 获取物品管理器
     * @return 物品管理器实例
     */
    public ItemManager getItemManager() {
        return this.itemManager;
    }

    /**
     * 获取装备包管理器
     * @return 装备包管理器实例
     */
    public KitManager getKitManager() {
        return this.kitManager;
    }

    /**
     * 获取游戏管理器
     * @return 游戏管理器实例
     */
    public GameManager getGameManager() {
        return this.gameManager;
    }

    /**
     * 获取玩家管理器
     * @return 玩家管理器实例
     */
    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    /**
     * 获取竞技场配置
     * @return 竞技场配置实例
     */
    public ArenaConfig getArenaConfig() {
        return this.arenaConfig;
    }

    /**
     * 获取排行榜
     * @return 排行榜实例
     */
    public Leaderboard getLeaderboard() {
        return this.leaderboard;
    }

    /**
     * 获取语言文件
     * @return 语言文件实例
     */
    public Language getLang() {
        return this.lang;
    }

    /**
     * 获取主配置文件
     * @return 主配置实例
     */
    public Config getHGConfig() {
        return config;
    }

    /**
     * 获取生物管理器
     * @return 生物管理器实例
     */
    public MobManager getMobManager() {
        return this.mobManager;
    }

    /**
     * 获取统计指标
     * @return 统计指标实例
     */
    public Metrics getMetrics() {
        return this.metrics;
    }

    /**
     * 获取MythicMobs管理器
     * @return MythicMobs管理器实例
     */
    public io.lumine.mythic.api.mobs.MobManager getMythicMobManager() {
        return this.mythicMobManager;
    }

    /**
     * 获取会话管理器
     * @return 会话管理器实例
     */
    public SessionManager getSessionManager() {
        return this.sessionManager;
    }

}
