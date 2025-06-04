package com.shanebeestudios.hg.plugin.permission;

import com.shanebeestudios.hg.api.util.Util;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.permissions.DefaultPermissions;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 权限管理类
 * <p>定义和管理所有插件相关的权限节点</p>
 */
public class Permissions {

    /**
     * 权限记录类
     * @param permission 权限节点字符串
     * @param bukkitPermission Bukkit权限对象
     */
    public record Permission(String permission, org.bukkit.permissions.Permission bukkitPermission) {
        /**
         * 检查发送者是否拥有此权限
         * @param sender 命令发送者
         * @return 是否拥有权限
         */
        public boolean has(CommandSender sender) {
            return sender.hasPermission(this.bukkitPermission.getName());
        }
    }

    // 权限节点存储Map
    static final Map<String, org.bukkit.permissions.Permission> PERMISSIONS = new LinkedHashMap<>();

    // =============== 命令权限 ===============
    public static final Permission COMMAND_CREATE = getCommand("create", "创建新的游戏竞技场", PermissionDefault.OP);
    public static final Permission COMMAND_DEBUG = getCommand("debug", "调试竞技场", PermissionDefault.OP);
    public static final Permission COMMAND_DELETE = getCommand("delete", "删除游戏竞技场", PermissionDefault.OP);
    public static final Permission COMMAND_EDIT = getCommand("edit", "编辑游戏竞技场", PermissionDefault.OP);
    public static final Permission COMMAND_FORCE_START = getCommand("forcestart", "强制开始游戏", PermissionDefault.OP);
    public static final Permission COMMAND_JOIN = getCommand("join", "加入游戏", PermissionDefault.TRUE);
    public static final Permission COMMAND_JOIN_OTHERS = getCommand("join_others", "将其他玩家加入游戏", PermissionDefault.OP);
    public static final Permission COMMAND_KIT = getCommand("kit", "在游戏中获取装备包", PermissionDefault.TRUE);
    public static final Permission COMMAND_LEAVE = getCommand("leave", "离开游戏", PermissionDefault.TRUE);
    public static final Permission COMMAND_LIST = getCommand("list", "列出当前游戏中的所有玩家", PermissionDefault.TRUE);
    public static final Permission COMMAND_LIST_GAME = getCommand("list_game", "列出指定游戏中的所有玩家", PermissionDefault.TRUE);
    public static final Permission COMMAND_NBT = getCommand("nbt", "从配置中获取物品的NBT数据", PermissionDefault.OP);
    public static final Permission COMMAND_PERMISSIONS = getCommand("permissions", "查看权限列表", PermissionDefault.OP);
    public static final Permission COMMAND_REFILL_CHESTS = getCommand("refill_chests", "重新填充游戏中的箱子", PermissionDefault.OP);
    public static final Permission COMMAND_RELOAD = getCommand("reload", "重新加载配置/竞技场", PermissionDefault.OP);
    public static final Permission COMMAND_SESSION = getCommand("session", "竞技场创建会话选项", PermissionDefault.OP);
    public static final Permission COMMAND_SET_EXIT = getCommand("setexit", "设置游戏出口(单个/全部/全局)", PermissionDefault.OP);
    public static final Permission COMMAND_SETTINGS = getCommand("settings", "临时修改配置设置(不保存到文件)", PermissionDefault.OP);
    public static final Permission COMMAND_SPECTATE = getCommand("spectate", "旁观游戏", PermissionDefault.TRUE);
    public static final Permission COMMAND_STATUS = getCommand("status", "查看游戏状态", PermissionDefault.TRUE);
    public static final Permission COMMAND_STOP = getCommand("stop", "停止游戏", PermissionDefault.OP);
    public static final Permission COMMAND_STOP_ALL = getCommand("stopallgames", "停止所有游戏", PermissionDefault.OP);
    public static final Permission COMMAND_TEAM = getCommand("team", "在游戏中创建/加入队伍", PermissionDefault.TRUE);
    public static final Permission COMMAND_TEAM_TELEPORT = getCommand("team.teleport", "传送到队伍成员", PermissionDefault.TRUE);
    public static final Permission COMMAND_TOGGLE = getCommand("toggle", "切换游戏状态", PermissionDefault.OP);

    // =============== 其他权限 ===============
    public static final Permission BYPASS_COMMAND_RESTRICTION = getBase("bypass.command.restriction", "绕过游戏中的命令限制", PermissionDefault.OP);

    /**
     * 获取命令权限
     * @param perm 权限后缀
     * @param description 权限描述
     * @param defaultPermission 默认权限级别
     * @return 权限对象
     */
    private static Permission getCommand(String perm, String description, PermissionDefault defaultPermission) {
        return getBase("command." + perm, description, defaultPermission);
    }

    /**
     * 注册装备包权限
     * @param arenaName 竞技场名称(可为null)
     * @param kitName 装备包名称
     * @param kitPermission 装备包权限后缀
     * @return 权限对象
     */
    public static Permission registerKitPermission(@Nullable String arenaName, String kitName, String kitPermission) {
        String message;
        if (arenaName != null) {
            message = "竞技场 '" + arenaName + "' 中装备包 '" + kitName + "' 的权限";
        } else {
            arenaName = "kits";
            message = "kits.yml 中装备包 '" + kitName + "' 的权限";
        }
        return getBase("kit." + arenaName + "." + kitPermission, message, PermissionDefault.OP);
    }

    /**
     * 获取基础权限
     * @param perm 权限后缀
     * @param description 权限描述
     * @param defaultPermission 默认权限级别
     * @return 权限对象
     */
    private static Permission getBase(String perm, String description, PermissionDefault defaultPermission) {
        String stringPerm = "hungergames." + perm;
        org.bukkit.permissions.Permission bukkitPermission = DefaultPermissions.registerPermission(stringPerm, description, defaultPermission);
        PERMISSIONS.put(stringPerm, bukkitPermission);
        return new Permission(stringPerm, bukkitPermission);
    }

    /**
     * 调试输出所有权限信息
     */
    public static void debug() {
        Util.log("权限列表:");
        for (Map.Entry<String, org.bukkit.permissions.Permission> entry : PERMISSIONS.entrySet()) {
            String color = switch (entry.getValue().getDefault()) {
                case OP -> "yellow";
                case TRUE, NOT_OP -> "green";
                case FALSE -> "red";
            };
            Util.log("  <white>'<#F09616>%s<white>':", entry.getKey());
            Util.log("    <grey> 描述: <aqua>%s", entry.getValue().getDescription());
            Util.log("    <grey> 默认权限: <%s>%s", color, entry.getValue().getDefault().toString());
        }
    }


}
