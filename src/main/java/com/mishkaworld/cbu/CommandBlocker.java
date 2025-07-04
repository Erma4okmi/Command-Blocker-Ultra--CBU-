package com.mishkaworld.cbu;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public class CommandBlocker implements Listener {
    private final Plugin plugin;
    private final PermissionChecker permissionChecker;
    private final Set<String> blockedCommands = new HashSet<>();

    public CommandBlocker(Plugin plugin, PermissionChecker permissionChecker) {
        this.plugin = plugin;
        this.permissionChecker = permissionChecker;
        // Убираем блокировку всех команд по умолчанию
        // blockedCommands.add("*");
    }

    // Блокировка команд игроков
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String fullCommand = event.getMessage();
        
        // Используем LogManager для логирования
        if (plugin instanceof CommandBlockerUltra) {
            LogManager logManager = ((CommandBlockerUltra) plugin).getLogManager();
            logManager.log("DEBUG: CommandBlocker получил команду: '" + fullCommand + "' от " + player.getName());
        }
        
        CommandBlockerUltra.CommandInfo commandInfo = CommandBlockerUltra.parseCommand(fullCommand);
        PermissionChecker.PermissionResult result = permissionChecker.checkPermission(player, commandInfo);
        
        if (plugin instanceof CommandBlockerUltra) {
            LogManager logManager = ((CommandBlockerUltra) plugin).getLogManager();
            logManager.log("DEBUG: Проверка прав для '" + fullCommand + "' -> allowed=" + result.allowed + ", message='" + result.message + "'");
        }
        
        if (!result.allowed) {
            if (plugin instanceof CommandBlockerUltra) {
                LogManager logManager = ((CommandBlockerUltra) plugin).getLogManager();
                logManager.log("DEBUG: Блокируем команду '" + fullCommand + "' для " + player.getName());
            }
            blockCommand(event, player);
        } else {
            if (plugin instanceof CommandBlockerUltra) {
                LogManager logManager = ((CommandBlockerUltra) plugin).getLogManager();
                logManager.log("DEBUG: Разрешаем команду '" + fullCommand + "' для " + player.getName());
            }
        }
    }

    private void blockCommand(PlayerCommandPreprocessEvent event, Player player) {
        event.setCancelled(true);
        String errorMessage = plugin.getConfig().getString("error-message", "&cКоманды отключены на этом сервере!");
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', errorMessage));
    }

    // Блокировка команд из консоли (теперь не блокируем)
    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        // Не блокируем команды из консоли
    }

    // Скрываем все команды, показываем только разрешенные
    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent event) {
        // Очищаем все команды
        event.getCommands().clear();
        
        Player player = event.getPlayer();
        
        addRegularCommands(event, player);
        addSuperCommands(event, player);
    }

    private void addRegularCommands(PlayerCommandSendEvent event, Player player) {
        for (PermissionChecker.CommandConfig config : permissionChecker.getCommandConfigs().values()) {
            if (hasPermission(player, config.getPermission())) {
                event.getCommands().add(config.getMainCommand());
                addAliases(event, player, config);
            }
        }
    }

    private void addSuperCommands(PlayerCommandSendEvent event, Player player) {
        for (PermissionChecker.SuperCommandConfig superConfig : permissionChecker.getSuperCommandConfigs().values()) {
            for (PermissionChecker.SubCommandConfig subConfig : superConfig.getSubCommands().values()) {
                if (hasPermission(player, subConfig.getPermission())) {
                    event.getCommands().add(subConfig.getSubCommandName());
                }
            }
        }
    }

    private void addAliases(PlayerCommandSendEvent event, Player player, PermissionChecker.CommandConfig config) {
        if (config.getAliases() == null) return;
        
        for (String alias : config.getAliases()) {
            if (alias != null && hasPermission(player, config.getPermission())) {
                event.getCommands().add(alias);
            }
        }
    }

    private boolean hasPermission(Player player, String permission) {
        return permission == null || permission.equalsIgnoreCase("none") || player.hasPermission(permission);
    }

    private boolean isBlocked(String cmd) {
        return blockedCommands.contains("*") || blockedCommands.contains(cmd);
    }

    // Можно добавить методы для динамического управления списком блокировок
} 