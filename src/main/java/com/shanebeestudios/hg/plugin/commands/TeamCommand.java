package com.shanebeestudios.hg.plugin.commands;

import com.shanebeestudios.hg.api.command.CustomArg;
import com.shanebeestudios.hg.api.data.PlayerData;
import com.shanebeestudios.hg.api.game.Game;
import com.shanebeestudios.hg.api.game.GameTeam;
import com.shanebeestudios.hg.api.util.Util;
import com.shanebeestudios.hg.plugin.HungerGames;
import com.shanebeestudios.hg.plugin.configs.Config;
import com.shanebeestudios.hg.plugin.permission.Permissions;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

public class TeamCommand extends SubCommand {

    public TeamCommand(HungerGames plugin) {
        super(plugin);
    }

    @Override
    protected Argument<?> register() {
        return LiteralArgument.literal("team")
            .withPermission(Permissions.COMMAND_TEAM.permission())
            .then(LiteralArgument.literal("create")
                .then(new StringArgument("name")
                    .executesPlayer(info -> {
                        Player player = info.sender();
                        PlayerData playerData = this.playerManager.getPlayerData(player);
                        if (playerData == null) {
                            Util.sendPrefixedMessage(player, this.lang.command_base_not_in_valid_game);
                            return;
                        }
                        GameTeam gameTeam = playerData.getTeam();
                        if (gameTeam != null) {
                            String exists = this.lang.command_team_already_have.replace("<name>", gameTeam.getTeamName());
                            Util.sendMessage(player, exists);
                        } else {
                            String teamName = info.args().getByClass("name", String.class);
                            assert teamName != null;

                            Game game = playerData.getGame();
                            if (game.getGameScoreboard().hasGameTeam(teamName)) {
                                String exists = this.lang.command_team_already_exists.replace("<name>", teamName);
                                Util.sendMessage(player, exists);
                                return;
                            }
                            game.getGameScoreboard().createGameTeam(player, teamName);
                            String created = this.lang.command_team_created.replace("<name>", teamName);
                            Util.sendMessage(player, created);
                        }
                    })))
            .then(LiteralArgument.literal("invite")
                .then(CustomArg.GAME_PLAYER_FOR_TEAM.get("player")
                    .executesPlayer(info -> {
                        Player teamLeader = info.sender();
                        PlayerData playerData = this.playerManager.getPlayerData(teamLeader);
                        if (playerData == null) {
                            // 不在有效的游戏中
                            Util.sendPrefixedMessage(teamLeader, this.lang.command_base_not_in_valid_game);
                            return;
                        }

                        GameTeam gameTeam = playerData.getTeam();
                        if (gameTeam == null) {
                            // 当前没有队伍
                            Util.sendMessage(teamLeader, this.lang.command_team_none);
                            return;
                        }

                        Game game = playerData.getGame();
                        Player invitee = info.args().getByClass("player", Player.class);
                        assert invitee != null;
                        if (!game.getGamePlayerData().getPlayers().contains(invitee)) {
                            // 被邀请者不在游戏中
                            Util.sendMessage(teamLeader, this.lang.command_team_player_not_available.replace("<player>", invitee.getName()));
                            return;
                        }

                        if (invitee == teamLeader) {
                            // 不能邀请自己加入队伍
                            Util.sendMessage(teamLeader, this.lang.command_team_self);
                            return;
                        }
                        if (gameTeam.getLeader() != teamLeader) {
                            // 只有队长可以邀请他人
                            Util.sendMessage(teamLeader, this.lang.command_team_only_leader);
                            return;
                        }
                        if (gameTeam.isOnTeam(invitee)) {
                            // 被邀请者已经在队伍中
                            Util.sendMessage(teamLeader, this.lang.command_team_on_team.replace("<player>", invitee.getName()));
                            return;
                        }
                        if ((gameTeam.getPlayers().size() + gameTeam.getPendingPlayers().size()) >= Config.TEAM_MAX_TEAM_SIZE) {
                            // 队伍人数已达上限
                            Util.sendMessage(teamLeader, this.lang.command_team_max);
                            return;
                        }
                        // 最终邀请玩家加入队伍
                        gameTeam.invite(invitee);
                        Util.sendMessage(teamLeader, this.lang.command_team_invited.replace("<player>", invitee.getName()));
                    })))
            .then(LiteralArgument.literal("accept")
                .executesPlayer(info -> {
                    Player player = info.sender();
                    PlayerData playerData = this.playerManager.getPlayerData(player);
                    if (playerData == null) {
                        // 不在有效的游戏中
                        Util.sendPrefixedMessage(player, this.lang.command_base_not_in_valid_game);
                        return;
                    }
                    GameTeam gameTeam = playerData.getPendingTeam();
                    if (gameTeam == null || !gameTeam.isPending(player)) {
                        // 当前没有待处理的邀请
                        Util.sendMessage(player, this.lang.command_team_no_pend);
                        return;
                    }
                    gameTeam.acceptInvite(player);
                    gameTeam.messageMembers("<gold>*<aqua><strikethrough>                                                                             <gold>*");
                    gameTeam.messageMembers(this.lang.command_team_joined.replace("<player>", player.getName()));
                    gameTeam.messageMembers("<gold>*<aqua><strikethrough>                                                                             <gold>*");
                }))
            .then(LiteralArgument.literal("deny")
                .executesPlayer(info -> {
                    Player player = info.sender();
                    PlayerData playerData = this.playerManager.getPlayerData(player);
                    if (playerData == null) {
                        Util.sendPrefixedMessage(player, this.lang.command_base_not_in_valid_game);
                        return;
                    }
                    GameTeam gameTeam = playerData.getPendingTeam();
                    if (gameTeam == null || !gameTeam.isPending(player)) {
                        // 当前没有待处理的邀请
                        Util.sendMessage(player, this.lang.command_team_no_pend);
                        return;
                    }
                    gameTeam.declineInvite(player);
                }))
            .then(LiteralArgument.literal("teleport")
                .withPermission(Permissions.COMMAND_TEAM_TELEPORT.permission())
                .then(CustomArg.GAME_PLAYER_ON_TEAM.get("player")
                    .executesPlayer(info -> {
                        Player player = info.sender();
                        PlayerData playerData = this.playerManager.getPlayerData(player);
                        if (playerData == null) {
                            Util.sendPrefixedMessage(player, this.lang.command_base_not_in_valid_game);
                            return;
                        }

                        GameTeam gameTeam = playerData.getTeam();
                        if (gameTeam == null) {
                            Util.sendMessage(player, this.lang.command_team_none);
                            return;
                        }
                        Player tpTo = info.args().getByClass("player", Player.class);
                        assert tpTo != null;
                        if (gameTeam.isOnTeam(tpTo)) {
                            player.teleport(tpTo);
                            Util.sendMessage(player, this.lang.command_team_tp.replace("<player>", tpTo.getName()));
                        } else {
                            Util.sendMessage(player, this.lang.command_team_not_on_team.replace("<player>", tpTo.getName()));
                        }
                    })));
    }

}
