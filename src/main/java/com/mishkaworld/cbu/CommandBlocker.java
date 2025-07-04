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
        
        // Разбираем команду
        CommandBlockerUltra.CommandInfo commandInfo = CommandBlockerUltra.parseCommand(fullCommand);
        
        // Проверяем права через PermissionChecker
        PermissionChecker.PermissionResult result = permissionChecker.checkPermission(player, commandInfo);
        
        // Если команда заблокирована, отменяем её выполнение
        if (!result.allowed) {
            event.setCancelled(true);
            String errorMessage = plugin.getConfig().getString("error-message", "&cКоманды отключены на этом сервере!");
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', errorMessage));
        }
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
        
        // Добавляем субкоманды как отдельные команды
        Player player = event.getPlayer();
        for (String mainCommand : permissionChecker.getCommandConfigs().keySet()) {
            PermissionChecker.CommandConfig commandConfig = permissionChecker.getCommandConfigs().get(mainCommand);
            
            // Добавляем все субкоманды, на которые есть права
            for (String subCommand : commandConfig.subcommands.keySet()) {
                PermissionChecker.SubCommandConfig subConfig = commandConfig.subcommands.get(subCommand);
                if (subConfig.permission == null || player.hasPermission(subConfig.permission)) {
                    event.getCommands().add(subCommand);
                }
            }
        }
    }

    private boolean isBlocked(String cmd) {
        return blockedCommands.contains("*") || blockedCommands.contains(cmd);
    }

    // Можно добавить методы для динамического управления списком блокировок
} 