package com.mishkaworld.cbu;

import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class PermissionChecker {
    private final FileConfiguration config;
    private final Map<String, CommandConfig> commandConfigs = new HashMap<>();

    public PermissionChecker(FileConfiguration config) {
        this.config = config;
        loadCommandConfigs();
    }

    private void loadCommandConfigs() {
        ConfigurationSection commandsSection = config.getConfigurationSection("commands");
        if (commandsSection == null) return;

        for (String commandName : commandsSection.getKeys(false)) {
            ConfigurationSection commandSection = commandsSection.getConfigurationSection(commandName);
            if (commandSection == null) continue;

            CommandConfig commandConfig = new CommandConfig();
            commandConfig.permission = commandSection.getString("permission");

            // Загружаем субкоманды
            ConfigurationSection subcommandsSection = commandSection.getConfigurationSection("subcommands");
            if (subcommandsSection != null) {
                for (String subcommandName : subcommandsSection.getKeys(false)) {
                    ConfigurationSection subcommandSection = subcommandsSection.getConfigurationSection(subcommandName);
                    if (subcommandSection == null) continue;

                    SubCommandConfig subCommandConfig = new SubCommandConfig();
                    subCommandConfig.permission = subcommandSection.getString("permission");

                    // Загружаем аргументы
                    ConfigurationSection argumentsSection = subcommandSection.getConfigurationSection("arguments");
                    if (argumentsSection != null) {
                        for (String argName : argumentsSection.getKeys(false)) {
                            ConfigurationSection argSection = argumentsSection.getConfigurationSection(argName);
                            if (argSection == null) continue;

                            ArgumentConfig argConfig = new ArgumentConfig();
                            argConfig.permission = argSection.getString("permission");
                            
                            // Загружаем списки значений
                            ConfigurationSection listsSection = argSection.getConfigurationSection("lists");
                            if (listsSection != null) {
                                for (String listKey : listsSection.getKeys(false)) {
                                    argConfig.lists.put(listKey, listsSection.getString(listKey));
                                }
                            }

                            subCommandConfig.arguments.put(argName, argConfig);
                        }
                    }

                    commandConfig.subcommands.put(subcommandName, subCommandConfig);
                }
            }

            commandConfigs.put(commandName, commandConfig);
        }
    }

    public PermissionResult checkPermission(Player player, CommandBlockerUltra.CommandInfo commandInfo) {
        String mainCommand = commandInfo.getMainCommand();
        String subCommand = commandInfo.getSubCommand();
        
        // Отладочная информация
        player.getServer().getLogger().info("[DEBUG] Проверка прав для команды: " + mainCommand + " субкоманда: " + subCommand);
        
        // Если команда является субкомандой, обрабатываем её
        for (String configMainCommand : commandConfigs.keySet()) {
            CommandConfig config = commandConfigs.get(configMainCommand);
            if (config.subcommands.containsKey(mainCommand)) {
                return checkSubCommandPermission(player, commandInfo, configMainCommand, mainCommand);
            }
        }
        
        // Проверяем основную команду
        CommandConfig commandConfig = commandConfigs.get(mainCommand);
        if (commandConfig == null) {
            return new PermissionResult(false, "Команда не найдена в конфиге");
        }

        // Проверяем права на основную команду
        if (commandConfig.permission != null && !player.hasPermission(commandConfig.permission)) {
            return new PermissionResult(false, "Нет прав на команду: " + commandConfig.permission);
        }

        // Если есть субкоманда, проверяем её
        if (subCommand != null && commandConfig.subcommands.containsKey(subCommand)) {
            SubCommandConfig subCommandConfig = commandConfig.subcommands.get(subCommand);
            
            // Отладочная информация
            player.getServer().getLogger().info("[DEBUG] Проверка прав на субкоманду: " + subCommandConfig.permission);
            
            // Проверяем права на субкоманду
            if (subCommandConfig.permission != null && !player.hasPermission(subCommandConfig.permission)) {
                return new PermissionResult(false, "Нет прав на субкоманду: " + subCommandConfig.permission);
            }

            // Проверяем аргументы
            String[] subArgs = commandInfo.getSubCommandArguments();
            for (int i = 0; i < subArgs.length; i++) {
                String argKey = "arg#" + (i + 1);
                if (subCommandConfig.arguments.containsKey(argKey)) {
                    ArgumentConfig argConfig = subCommandConfig.arguments.get(argKey);
                    
                    // Проверяем права на аргумент
                    if (argConfig.permission != null) {
                        player.getServer().getLogger().info("[DEBUG] Проверка прав на аргумент: " + argConfig.permission);
                        if (!player.hasPermission(argConfig.permission)) {
                            return new PermissionResult(false, "Нет прав на аргумент: " + argConfig.permission);
                        }
                    }

                    // Проверяем списки значений
                    if (!argConfig.lists.isEmpty()) {
                        String argValue = subArgs[i];
                        boolean foundInList = false;
                        boolean hasPermissionForValue = false;
                        
                        // Проверяем, есть ли значение в списке и есть ли права на него
                        for (Map.Entry<String, String> entry : argConfig.lists.entrySet()) {
                            String listKey = entry.getKey();
                            String permission = entry.getValue();
                            
                            if (listKey.equals(argValue)) {
                                foundInList = true;
                                if (player.hasPermission(permission)) {
                                    hasPermissionForValue = true;
                                    break;
                                }
                            }
                        }
                        
                        if (!foundInList) {
                            return new PermissionResult(false, "Значение аргумента '" + argValue + "' не найдено в списке допустимых значений");
                        }
                        
                        if (!hasPermissionForValue) {
                            return new PermissionResult(false, "Нет прав на значение аргумента: " + argValue);
                        }
                    }
                }
            }
        }

        return new PermissionResult(true, "Все права проверены успешно");
    }

    private PermissionResult checkSubCommandPermission(Player player, CommandBlockerUltra.CommandInfo commandInfo, 
                                                    String mainCommand, String subCommand) {
        CommandConfig commandConfig = commandConfigs.get(mainCommand);
        SubCommandConfig subCommandConfig = commandConfig.subcommands.get(subCommand);
        
        // Проверяем права на субкоманду
        if (subCommandConfig.permission != null && !player.hasPermission(subCommandConfig.permission)) {
            return new PermissionResult(false, "Нет прав на субкоманду: " + subCommandConfig.permission);
        }

        // Проверяем аргументы
        String[] subArgs = commandInfo.getSubCommandArguments();
        for (int i = 0; i < subArgs.length; i++) {
            String argKey = "arg#" + (i + 1);
            if (subCommandConfig.arguments.containsKey(argKey)) {
                ArgumentConfig argConfig = subCommandConfig.arguments.get(argKey);
                
                // Проверяем права на аргумент
                if (argConfig.permission != null && !player.hasPermission(argConfig.permission)) {
                    return new PermissionResult(false, "Нет прав на аргумент: " + argConfig.permission);
                }

                // Проверяем списки значений
                if (!argConfig.lists.isEmpty()) {
                    String argValue = subArgs[i];
                    boolean foundInList = false;
                    boolean hasPermissionForValue = false;
                    
                    for (Map.Entry<String, String> entry : argConfig.lists.entrySet()) {
                        String listKey = entry.getKey();
                        String permission = entry.getValue();
                        
                        if (listKey.equals(argValue)) {
                            foundInList = true;
                            if (player.hasPermission(permission)) {
                                hasPermissionForValue = true;
                                break;
                            }
                        }
                    }
                    
                    if (!foundInList) {
                        return new PermissionResult(false, "Значение аргумента '" + argValue + "' не найдено в списке допустимых значений");
                    }
                    
                    if (!hasPermissionForValue) {
                        return new PermissionResult(false, "Нет прав на значение аргумента: " + argValue);
                    }
                }
            }
        }

        return new PermissionResult(true, "Все права проверены успешно");
    }

    public static class CommandConfig {
        public String permission;
        public Map<String, SubCommandConfig> subcommands = new HashMap<>();
    }

    public static class SubCommandConfig {
        public String permission;
        public Map<String, ArgumentConfig> arguments = new HashMap<>();
    }

    public static class ArgumentConfig {
        public String permission;
        public Map<String, String> lists = new HashMap<>();
    }

    public static class PermissionResult {
        public final boolean allowed;
        public final String message;

        public PermissionResult(boolean allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }
    }

    public Map<String, CommandConfig> getCommandConfigs() {
        return commandConfigs;
    }
} 