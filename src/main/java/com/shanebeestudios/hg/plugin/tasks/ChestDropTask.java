package com.shanebeestudios.hg.plugin.tasks;

import com.shanebeestudios.hg.api.game.Game;
import com.shanebeestudios.hg.api.game.GameRegion;
import com.shanebeestudios.hg.api.util.Util;
import com.shanebeestudios.hg.plugin.HungerGames;
import com.shanebeestudios.hg.plugin.configs.Config;
import com.shanebeestudios.hg.plugin.configs.Language;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ChestDropTask implements Runnable {

    private final Game game;
    private final Language lang;
    private final int taskId;
    private final List<ChestDropChestTask> tasks = new ArrayList<>();

    public ChestDropTask(Game game) {
        this.game = game;
        HungerGames plugin = HungerGames.getPlugin();
        this.lang = plugin.getLang();
        int interval = Config.CHESTS_CHEST_DROP_INTERVAL * 20;
        this.taskId = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, interval, interval);
    }

    public void run() {
        GameRegion gameRegion = this.game.getGameArenaData().getGameRegion();
        Location randomLocation = gameRegion.getRandomLocation();

        for (Player player : this.game.getGamePlayerData().getPlayers()) {
            Util.sendMessage(player, this.lang.chest_drop_line);
            Util.sendMessage(player, this.lang.chest_drop_dropped_at_location
                .replace("<x>", "" + randomLocation.getBlockX())
                .replace("<y>", "" + randomLocation.getBlockY())
                .replace("<z>", "" + randomLocation.getBlockZ()));
            Util.sendMessage(player, this.lang.chest_drop_line);
            this.tasks.add(new ChestDropChestTask(randomLocation));
        }
    }

    public void stop() {
        this.tasks.forEach(ChestDropChestTask::stop);
        this.tasks.clear();
        Bukkit.getScheduler().cancelTask(this.taskId);
    }

}
