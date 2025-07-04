package com.mishkaworld.cbu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;
import java.util.List;
import java.util.ArrayList;

public class TabCompleteManager implements Listener {
    private final PermissionChecker permissionChecker;

    public TabCompleteManager(PermissionChecker permissionChecker) {
        this.permissionChecker = permissionChecker;
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        if (event == null || event.getBuffer() == null || !event.getBuffer().startsWith("/")) return;
        
        String buffer = event.getBuffer();
        String[] parts = buffer.split(" ");
        boolean endsWithSpace = buffer.endsWith(" ");
        
        if (parts.length == 0) return;
        
        String command = parts[0].substring(1); // убираем /
        
        // Очищаем стандартные предложения
        event.getCompletions().clear();
        
        // Показываем автодополнение для команд из конфига
        handleTabCompleteFromConfig(event, parts, endsWithSpace);
    }

    private void handleTabCompleteFromConfig(TabCompleteEvent event, String[] parts, boolean endsWithSpace) {
        if (!(event.getSender() instanceof Player)) return;
        
        Player player = (Player) event.getSender();
        String command = parts[0].substring(1);
        
        // Если это первая команда (только /), показываем все доступные субкоманды
        if (parts.length == 1 && !endsWithSpace) {
            showAvailableSubCommands(event, player);
            return;
        }
        
        // Ищем команду в конфиге
        for (String mainCommand : permissionChecker.getCommandConfigs().keySet()) {
            if (mainCommand == null) continue;
            
            PermissionChecker.CommandConfig commandConfig = permissionChecker.getCommandConfigs().get(mainCommand);
            if (commandConfig == null) continue;
            
            for (String subCommand : commandConfig.subcommands.keySet()) {
                if (subCommand == null) continue;
                
                if (command.equals(subCommand)) {
                    PermissionChecker.SubCommandConfig subConfig = commandConfig.subcommands.get(subCommand);
                    if (subConfig != null) {
                        handleSubCommandTabComplete(event, parts, endsWithSpace, subConfig, player);
                    }
                    return;
                }
            }
        }
    }

    private void showAvailableSubCommands(TabCompleteEvent event, Player player) {
        String currentInput = event.getBuffer().substring(1).toLowerCase(); // убираем / и приводим к нижнему регистру
        for (String mainCommand : permissionChecker.getCommandConfigs().keySet()) {
            if (mainCommand == null) continue;
            
            PermissionChecker.CommandConfig commandConfig = permissionChecker.getCommandConfigs().get(mainCommand);
            if (commandConfig == null) continue;
            
            for (String subCommand : commandConfig.subcommands.keySet()) {
                if (subCommand == null) continue;
                
                PermissionChecker.SubCommandConfig subConfig = commandConfig.subcommands.get(subCommand);
                if (subConfig == null) continue;
                
                if (subConfig.permission == null || player.hasPermission(subConfig.permission)) {
                    if (currentInput.isEmpty() || subCommand.toLowerCase().startsWith(currentInput)) {
                        event.getCompletions().add(subCommand);
                    }
                }
            }
        }
    }

    private void handleSubCommandTabComplete(TabCompleteEvent event, String[] parts, boolean endsWithSpace,
                                          PermissionChecker.SubCommandConfig subConfig, Player player) {
        // parts[0] = /gm, parts[1] = первый аргумент, parts[2] = второй аргумент и т.д.
        int argIndex = parts.length - 1;
        if (endsWithSpace) {
            argIndex++; // если строка заканчивается пробелом, значит пользователь начал новый аргумент
        }

        if (argIndex == 1) { // Первый аргумент
            String currentInput = (parts.length > 1 && !endsWithSpace) ? parts[1].toLowerCase() : "";
            for (String argKey : subConfig.arguments.keySet()) {
                if (argKey == null || !argKey.equals("arg#1")) continue;
                
                PermissionChecker.ArgumentConfig argConfig = subConfig.arguments.get(argKey);
                if (argConfig == null) continue;
                
                if (!argConfig.lists.isEmpty()) {
                    for (String listKey : argConfig.lists.keySet()) {
                        if (listKey == null) continue;
                        
                        String permission = argConfig.lists.get(listKey);
                        if (permission == null || player.hasPermission(permission)) {
                            if (currentInput.isEmpty() || listKey.toLowerCase().startsWith(currentInput)) {
                                event.getCompletions().add(listKey);
                            }
                        }
                    }
                }
            }
        } else if (argIndex == 2) { // Второй аргумент
            String currentInput = (parts.length > 2 && !endsWithSpace) ? parts[2].toLowerCase() : "";
            String argKey = "arg#2";
            if (subConfig.arguments.containsKey(argKey)) {
                PermissionChecker.ArgumentConfig argConfig = subConfig.arguments.get(argKey);
                if (argConfig != null && (argConfig.permission == null || player.hasPermission(argConfig.permission))) {
                    List<String> players = getOnlinePlayers();
                    for (String playerName : players) {
                        if (playerName != null && (currentInput.isEmpty() || playerName.toLowerCase().startsWith(currentInput))) {
                            event.getCompletions().add(playerName);
                        }
                    }
                }
            }
        }
        // Если аргументов больше, ничего не подсказываем
    }

    private List<String> getOnlinePlayers() {
        List<String> players = new ArrayList<>();
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            players.add(player.getName());
        }
        return players;
    }
} 