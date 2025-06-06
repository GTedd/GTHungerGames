package com.shanebeestudios.hg.plugin.commands;

import com.shanebeestudios.hg.api.util.NBTApi;
import com.shanebeestudios.hg.plugin.HungerGames;
import dev.jorel.commandapi.CommandTree;

public class MainCommand {

    CommandTree command;

    public MainCommand(HungerGames plugin) {
        this.command = new CommandTree("hungergames");
        this.command.withHelp("HungerGames 基础命令", "包含 HungerGames 游戏所需的所有命令！");
        this.command.withAliases("hg");

        // 注册子命令
        this.command.then(new CreateCommand(plugin).register());
        this.command.then(new DebugCommand(plugin).register());
        this.command.then(new DeleteArenaCommand(plugin).register());
        this.command.then(new EditCommand(plugin).register());
        this.command.then(new ForceStartCommand(plugin).register());
        this.command.then(new JoinCommand(plugin).register());
        this.command.then(new KitCommand(plugin).register());
        this.command.then(new KitsCommand(plugin).register());
        this.command.then(new LeaveCommand(plugin).register());
        this.command.then(new ListCommand(plugin).register());
        this.command.then(new ListGamesCommand(plugin).register());
        this.command.then(new PermissionsCommand(plugin).register());
        this.command.then(new RefillChestNowCommand(plugin).register());
        this.command.then(new ReloadCommand(plugin).register());
        this.command.then(new SessionCommand(plugin).register());
        this.command.then(new SetExitCommand(plugin).register());
        this.command.then(new SettingsCommand(plugin).register());
        this.command.then(new SpectateCommand(plugin).register());
        this.command.then(new StatusCommand(plugin).register());
        this.command.then(new StopCommand(plugin).register());
        this.command.then(new StopAllCommand(plugin).register());
        this.command.then(new TeamCommand(plugin).register());
        this.command.then(new ToggleCommand(plugin).register());

        if (NBTApi.isEnabled()) {
            this.command.then(new NBTCommand(plugin).register());
        }

        this.command.register();
    }

}
