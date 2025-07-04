package com.mishkaworld.cbu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import java.util.List;
import java.util.ArrayList;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TabCompleteManager implements Listener {
    private final PermissionChecker permissionChecker;
    private final LogManager logManager;

    public TabCompleteManager(PermissionChecker permissionChecker, FileConfiguration config, Plugin plugin) {
        this.permissionChecker = permissionChecker;
        this.logManager = new LogManager(config, plugin);
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
        
        // Добавляем отладочную информацию только если включен debug-mode
        logManager.log("Обработка команды: '" + command + "' для игрока " + player.getName());
        
        // Если это первая команда (только /), показываем все доступные команды и субкоманды
        if (parts.length == 1 && !endsWithSpace) {
            showAvailableCommands(event, player);
            return;
        }
        
        // Сначала проверяем обычные команды
        for (String mainCommand : permissionChecker.getCommandConfigs().keySet()) {
            if (mainCommand == null) continue;
            
            PermissionChecker.CommandConfig commandConfig = permissionChecker.getCommandConfigs().get(mainCommand);
            if (commandConfig == null) continue;
            
            // Проверяем основную команду
            if (command.equals(mainCommand)) {
                logManager.log("Найдена обычная команда: " + mainCommand);
                handleRegularCommandTabComplete(event, parts, endsWithSpace, commandConfig, player);
                return;
            }
            
            // Проверяем алиасы
            if (commandConfig.aliases != null && commandConfig.aliases.contains(command)) {
                logManager.log("Найден алиас '" + command + "' для команды: " + mainCommand);
                handleRegularCommandTabComplete(event, parts, endsWithSpace, commandConfig, player);
                return;
            }
        }
        
        // Проверяем супер-команды (с субкомандами)
        for (String mainCommand : permissionChecker.getSuperCommandConfigs().keySet()) {
            if (mainCommand == null) continue;
            
            PermissionChecker.SuperCommandConfig superCommandConfig = permissionChecker.getSuperCommandConfigs().get(mainCommand);
            if (superCommandConfig == null) continue;
            
            for (String subCommand : superCommandConfig.subcommands.keySet()) {
                if (subCommand == null) continue;
                
                if (command.equals(subCommand)) {
                    logManager.log("Найдена субкоманда '" + subCommand + "' для команды: " + mainCommand);
                    PermissionChecker.SubCommandConfig subConfig = superCommandConfig.subcommands.get(subCommand);
                    if (subConfig != null) {
                        handleSubCommandTabComplete(event, parts, endsWithSpace, subConfig, player);
                    }
                    return;
                }
            }
            
            // Проверяем алиасы супер-команд
            if (superCommandConfig.aliases != null && superCommandConfig.aliases.contains(command)) {
                logManager.log("Найден алиас '" + command + "' для супер-команды: " + mainCommand);
                // Если это алиас супер-команды, показываем её субкоманды
                showAvailableSubCommands(event, player, superCommandConfig);
                return;
            }
        }
        
        logManager.log("Команда '" + command + "' не найдена в конфиге");
    }

    private void showAvailableCommands(TabCompleteEvent event, Player player) {
        String currentInput = event.getBuffer().substring(1).toLowerCase(); // убираем / и приводим к нижнему регистру
        
        // Показываем обычные команды
        for (String mainCommand : permissionChecker.getCommandConfigs().keySet()) {
            if (mainCommand == null) continue;
            
            PermissionChecker.CommandConfig commandConfig = permissionChecker.getCommandConfigs().get(mainCommand);
            if (commandConfig == null) continue;
            
            // Проверяем права на команду
            if (commandConfig.permission == null || commandConfig.permission.equalsIgnoreCase("none") || player.hasPermission(commandConfig.permission)) {
                if (currentInput.isEmpty() || mainCommand.toLowerCase().startsWith(currentInput)) {
                    event.getCompletions().add(mainCommand);
                }
            }
            
            // Показываем алиасы
            if (commandConfig.aliases != null) {
                for (String alias : commandConfig.aliases) {
                    if (alias != null && (commandConfig.permission == null || commandConfig.permission.equalsIgnoreCase("none") || player.hasPermission(commandConfig.permission))) {
                        if (currentInput.isEmpty() || alias.toLowerCase().startsWith(currentInput)) {
                            event.getCompletions().add(alias);
                        }
                    }
                }
            }
        }
        
        // Показываем субкоманды из супер-команд
        for (String mainCommand : permissionChecker.getSuperCommandConfigs().keySet()) {
            if (mainCommand == null) continue;
            
            PermissionChecker.SuperCommandConfig superCommandConfig = permissionChecker.getSuperCommandConfigs().get(mainCommand);
            if (superCommandConfig == null) continue;
            
            for (String subCommand : superCommandConfig.subcommands.keySet()) {
                if (subCommand == null) continue;
                
                PermissionChecker.SubCommandConfig subConfig = superCommandConfig.subcommands.get(subCommand);
                if (subConfig == null) continue;
                
                if (subConfig.permission == null || subConfig.permission.equalsIgnoreCase("none") || player.hasPermission(subConfig.permission)) {
                    if (currentInput.isEmpty() || subCommand.toLowerCase().startsWith(currentInput)) {
                        event.getCompletions().add(subCommand);
                    }
                }
            }
        }
    }

    private void showAvailableSubCommands(TabCompleteEvent event, Player player, PermissionChecker.SuperCommandConfig superCommandConfig) {
        String currentInput = event.getBuffer().substring(1).toLowerCase();
        
        for (String subCommand : superCommandConfig.subcommands.keySet()) {
            if (subCommand == null) continue;
            
            PermissionChecker.SubCommandConfig subConfig = superCommandConfig.subcommands.get(subCommand);
            if (subConfig == null) continue;
            
            if (subConfig.permission == null || subConfig.permission.equalsIgnoreCase("none") || player.hasPermission(subConfig.permission)) {
                if (currentInput.isEmpty() || subCommand.toLowerCase().startsWith(currentInput)) {
                    event.getCompletions().add(subCommand);
                }
            }
        }
    }

    private void handleRegularCommandTabComplete(TabCompleteEvent event, String[] parts, boolean endsWithSpace,
                                              PermissionChecker.CommandConfig commandConfig, Player player) {
        // parts[0] = /ref, parts[1] = первый аргумент, parts[2] = второй аргумент и т.д.
        int argIndex = parts.length - 1;
        if (endsWithSpace) {
            argIndex++; // если строка заканчивается пробелом, значит пользователь начал новый аргумент
        }
        // Аргументы начинаются с arg#1
        String argKey = "arg#" + argIndex;
        if (argIndex < 1) return;
        if (!commandConfig.arguments.containsKey(argKey)) return;
        PermissionChecker.ArgumentConfig argConfig = commandConfig.arguments.get(argKey);
        if (argConfig == null) return;

        String currentInput = (parts.length > argIndex && !endsWithSpace) ? parts[argIndex].toLowerCase() : "";

        // Если есть списки значений — подсказываем их
        if (!argConfig.lists.isEmpty()) {
            for (String listKey : argConfig.lists.keySet()) {
                if (listKey == null) continue;
                String permission = argConfig.lists.get(listKey);
                
                // Показываем только если у игрока есть права на это значение
                if (permission == null || permission.equalsIgnoreCase("none") || player.hasPermission(permission)) {
                    if (currentInput.isEmpty() || listKey.toLowerCase().startsWith(currentInput)) {
                        event.getCompletions().add(listKey);
                    }
                }
            }
        } else {
            // Если нет списков, проверяем permission аргумента
            boolean shouldShowPlayers = false;
            
            if (argConfig.permission == null || argConfig.permission.equalsIgnoreCase("none")) {
                // Если permission: none или не указан — показываем игроков всем
                shouldShowPlayers = true;
            } else if (player.hasPermission(argConfig.permission)) {
                // Если есть permission и у игрока есть права — показываем игроков
                shouldShowPlayers = true;
            }
            
            if (shouldShowPlayers) {
                List<String> players = getOnlinePlayers();
                for (String playerName : players) {
                    if (playerName != null && (currentInput.isEmpty() || playerName.toLowerCase().startsWith(currentInput))) {
                        event.getCompletions().add(playerName);
                    }
                }
            }
        }
        // Если аргументов больше, всё работает динамически
    }

    private void handleSubCommandTabComplete(TabCompleteEvent event, String[] parts, boolean endsWithSpace,
                                          PermissionChecker.SubCommandConfig subConfig, Player player) {
        // parts[0] = /gm, parts[1] = первый аргумент, parts[2] = второй аргумент и т.д.
        int argIndex = parts.length - 1;
        if (endsWithSpace) {
            argIndex++; // если строка заканчивается пробелом, значит пользователь начал новый аргумент
        }
        // Аргументы начинаются с arg#1
        String argKey = "arg#" + argIndex;
        if (argIndex < 1) return;
        if (!subConfig.arguments.containsKey(argKey)) return;
        PermissionChecker.ArgumentConfig argConfig = subConfig.arguments.get(argKey);
        if (argConfig == null) return;

        String currentInput = (parts.length > argIndex && !endsWithSpace) ? parts[argIndex].toLowerCase() : "";

        // Если есть списки значений — подсказываем их
        if (!argConfig.lists.isEmpty()) {
            for (String listKey : argConfig.lists.keySet()) {
                if (listKey == null) continue;
                String permission = argConfig.lists.get(listKey);
                
                // Добавляем отладочную информацию только если включен debug-mode
                boolean hasPermission = permission == null || permission.equalsIgnoreCase("none") || player.hasPermission(permission);
                logManager.log("Проверка прав для значения '" + listKey + "': permission=" + permission + ", hasPermission=" + hasPermission + ", player=" + player.getName());
                
                // Показываем только если у игрока есть права на это значение
                if (hasPermission) {
                    if (currentInput.isEmpty() || listKey.toLowerCase().startsWith(currentInput)) {
                        event.getCompletions().add(listKey);
                        logManager.log("Добавлено значение '" + listKey + "' для игрока " + player.getName());
                    }
                } else {
                    logManager.log("Игрок " + player.getName() + " не имеет прав на значение '" + listKey + "' (permission: " + permission + ")");
                }
            }
        } else {
            // Если нет списков, проверяем permission аргумента
            boolean shouldShowPlayers = false;
            
            if (argConfig.permission == null || argConfig.permission.equalsIgnoreCase("none")) {
                // Если permission: none или не указан — показываем игроков всем
                shouldShowPlayers = true;
            } else if (player.hasPermission(argConfig.permission)) {
                // Если есть permission и у игрока есть права — показываем игроков
                shouldShowPlayers = true;
            }
            
            if (shouldShowPlayers) {
                List<String> players = getOnlinePlayers();
                for (String playerName : players) {
                    if (playerName != null && (currentInput.isEmpty() || playerName.toLowerCase().startsWith(currentInput))) {
                        event.getCompletions().add(playerName);
                    }
                }
            }
        }
        // Если аргументов больше, всё работает динамически
    }

    private List<String> getOnlinePlayers() {
        List<String> players = new ArrayList<>();
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            players.add(player.getName());
        }
        return players;
    }
} 