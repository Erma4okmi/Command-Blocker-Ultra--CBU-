package com.mishkaworld.cbu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Map;

public class TabCompleteManager implements Listener {
    private final PermissionChecker permissionChecker;
    private final LogManager logManager;

    public TabCompleteManager(PermissionChecker permissionChecker, FileConfiguration config, Plugin plugin) {
        this.permissionChecker = permissionChecker;
        this.logManager = new LogManager(config, plugin);
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        if (!isValidTabCompleteEvent(event)) return;
        
        String[] parts = event.getBuffer().split(" ");
        boolean endsWithSpace = event.getBuffer().endsWith(" ");
        
        if (parts.length == 0) return;
        
        // Очищаем стандартные предложения
        event.getCompletions().clear();
        
        // Показываем автодополнение для команд из конфига
        handleTabCompleteFromConfig(event, parts, endsWithSpace);
    }

    private boolean isValidTabCompleteEvent(TabCompleteEvent event) {
        return event != null && event.getBuffer() != null && event.getBuffer().startsWith("/");
    }

    private void handleTabCompleteFromConfig(TabCompleteEvent event, String[] parts, boolean endsWithSpace) {
        if (!(event.getSender() instanceof Player)) return;
        
        Player player = (Player) event.getSender();
        String command = parts[0].substring(1);
        
        logManager.log("Обработка команды: '" + command + "' для игрока " + player.getName());
        
        // Если это первая команда (только /), показываем все доступные команды и субкоманды
        if (parts.length == 1 && !endsWithSpace) {
            showAvailableCommands(event, player);
            return;
        }
        
        // Проверяем команды
        if (handleRegularCommands(event, parts, endsWithSpace, command, player)) return;
        if (handleSuperCommands(event, parts, endsWithSpace, command, player)) return;
        
        logManager.log("Команда '" + command + "' не найдена в конфиге");
    }

    private boolean handleRegularCommands(TabCompleteEvent event, String[] parts, boolean endsWithSpace, 
                                       String command, Player player) {
        // Прямая проверка
        PermissionChecker.CommandConfig commandConfig = permissionChecker.getCommandConfigs().get(command);
        if (commandConfig != null) {
            logManager.log("Найдена обычная команда: " + command);
            handleRegularCommandTabComplete(event, parts, endsWithSpace, commandConfig, player);
            return true;
        }

        // Проверка алиасов
        for (PermissionChecker.CommandConfig config : permissionChecker.getCommandConfigs().values()) {
            if (config.hasAlias(command)) {
                logManager.log("Найден алиас '" + command + "' для команды");
                handleRegularCommandTabComplete(event, parts, endsWithSpace, config, player);
                return true;
            }
        }

        return false;
    }

    private boolean handleSuperCommands(TabCompleteEvent event, String[] parts, boolean endsWithSpace, 
                                     String command, Player player) {
        // Прямая проверка
        for (PermissionChecker.SuperCommandConfig superConfig : permissionChecker.getSuperCommandConfigs().values()) {
            if (superConfig.getSubCommand(command) != null) {
                logManager.log("Найдена субкоманда '" + command + "'");
                handleSubCommandTabComplete(event, parts, endsWithSpace, superConfig.getSubCommand(command), player);
                return true;
            }
            
            // Проверка алиасов
            if (superConfig.hasAlias(command)) {
                logManager.log("Найден алиас '" + command + "' для супер-команды");
                showAvailableSubCommands(event, player, superConfig);
                return true;
            }
        }

        return false;
    }

    private void showAvailableCommands(TabCompleteEvent event, Player player) {
        String currentInput = event.getBuffer().substring(1).toLowerCase();
        
        // Показываем обычные команды
        for (PermissionChecker.CommandConfig config : permissionChecker.getCommandConfigs().values()) {
            if (hasPermission(player, config.getPermission())) {
                addCompletionIfMatches(event, currentInput, config.getMainCommand());
                addAliasCompletions(event, currentInput, config);
            }
        }
        
        // Показываем субкоманды из супер-команд
        for (PermissionChecker.SuperCommandConfig superConfig : permissionChecker.getSuperCommandConfigs().values()) {
            for (PermissionChecker.SubCommandConfig subConfig : superConfig.getSubCommands().values()) {
                if (hasPermission(player, subConfig.getPermission())) {
                    addCompletionIfMatches(event, currentInput, subConfig.getSubCommandName());
                }
            }
        }
    }

    private void showAvailableSubCommands(TabCompleteEvent event, Player player, PermissionChecker.SuperCommandConfig superCommandConfig) {
        String currentInput = event.getBuffer().substring(1).toLowerCase();
        
        for (PermissionChecker.SubCommandConfig subConfig : superCommandConfig.getSubCommands().values()) {
            if (hasPermission(player, subConfig.getPermission())) {
                addCompletionIfMatches(event, currentInput, subConfig.getSubCommandName());
            }
        }
    }

    private boolean hasPermission(Player player, String permission) {
        return permission == null || permission.equalsIgnoreCase("none") || player.hasPermission(permission);
    }

    private void addCompletionIfMatches(TabCompleteEvent event, String currentInput, String completion) {
        if (currentInput.isEmpty() || completion.toLowerCase().startsWith(currentInput)) {
            event.getCompletions().add(completion);
        }
    }

    private void addAliasCompletions(TabCompleteEvent event, String currentInput, PermissionChecker.CommandConfig config) {
        if (config.getAliases() == null) return;
        
        for (String alias : config.getAliases()) {
            if (alias != null && hasPermission((Player) event.getSender(), config.getPermission())) {
                addCompletionIfMatches(event, currentInput, alias);
            }
        }
    }

    private void handleRegularCommandTabComplete(TabCompleteEvent event, String[] parts, boolean endsWithSpace,
                                              PermissionChecker.CommandConfig commandConfig, Player player) {
        int argIndex = parts.length - 1;
        if (endsWithSpace) {
            argIndex++;
        }
        
        String argKey = "arg#" + argIndex;
        if (argIndex < 1) return;
        
        PermissionChecker.ArgumentConfig argConfig = commandConfig.getArguments().get(argKey);
        if (argConfig == null) return;

        String currentInput = (parts.length > argIndex && !endsWithSpace) ? parts[argIndex].toLowerCase() : "";

        handleArgumentCompletion(event, player, argConfig, currentInput);
    }

    private void handleSubCommandTabComplete(TabCompleteEvent event, String[] parts, boolean endsWithSpace,
                                          PermissionChecker.SubCommandConfig subConfig, Player player) {
        int argIndex = parts.length - 1;
        if (endsWithSpace) {
            argIndex++;
        }
        
        String argKey = "arg#" + argIndex;
        if (argIndex < 1) return;
        
        PermissionChecker.ArgumentConfig argConfig = subConfig.getArguments().get(argKey);
        if (argConfig == null) return;

        String currentInput = (parts.length > argIndex && !endsWithSpace) ? parts[argIndex].toLowerCase() : "";

        handleArgumentCompletion(event, player, argConfig, currentInput);
    }

    private void handleArgumentCompletion(TabCompleteEvent event, Player player, 
                                       PermissionChecker.ArgumentConfig argConfig, String currentInput) {
        if (!argConfig.getLists().isEmpty()) {
            handleListCompletion(event, player, argConfig, currentInput);
        } else {
            handlePlayerCompletion(event, player, argConfig, currentInput);
        }
    }

    private void handleListCompletion(TabCompleteEvent event, Player player, 
                                    PermissionChecker.ArgumentConfig argConfig, String currentInput) {
        for (Map.Entry<String, String> entry : argConfig.getLists().entrySet()) {
            String listKey = entry.getKey();
            String permission = entry.getValue();
            
            if (listKey == null) continue;
            
            boolean hasPermissionForValue = hasPermission(player, permission);
            logManager.log("Проверка прав для значения '" + listKey + "': permission=" + permission + 
                         ", hasPermission=" + hasPermissionForValue + ", player=" + player.getName());
            
            if (hasPermissionForValue) {
                if (currentInput.isEmpty() || listKey.toLowerCase().startsWith(currentInput)) {
                    event.getCompletions().add(listKey);
                    logManager.log("Добавлено значение '" + listKey + "' для игрока " + player.getName());
                }
            } else {
                logManager.log("Игрок " + player.getName() + " не имеет прав на значение '" + listKey + 
                             "' (permission: " + permission + ")");
            }
        }
    }

    private void handlePlayerCompletion(TabCompleteEvent event, Player player, 
                                     PermissionChecker.ArgumentConfig argConfig, String currentInput) {
        boolean shouldShowPlayers = hasPermission(player, argConfig.getPermission());
        
        if (shouldShowPlayers) {
            List<String> players = getOnlinePlayers();
            for (String playerName : players) {
                if (playerName != null && (currentInput.isEmpty() || playerName.toLowerCase().startsWith(currentInput))) {
                    event.getCompletions().add(playerName);
                }
            }
        }
    }

    private List<String> getOnlinePlayers() {
        List<String> players = new ArrayList<>();
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            players.add(player.getName());
        }
        return players;
    }
} 