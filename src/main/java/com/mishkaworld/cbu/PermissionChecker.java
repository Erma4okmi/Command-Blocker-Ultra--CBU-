package com.mishkaworld.cbu;

import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class PermissionChecker {
    private final FileConfiguration config;
    private final Map<String, CommandConfig> commandConfigs = new HashMap<>();
    private final Map<String, SuperCommandConfig> superCommandConfigs = new HashMap<>();

    public PermissionChecker(FileConfiguration config) {
        this.config = config;
        loadCommandConfigs();
    }

    private void loadCommandConfigs() {
        // Загружаем обычные команды
        ConfigurationSection commandsSection = config.getConfigurationSection("commands");
        if (commandsSection != null) {
            for (String commandName : commandsSection.getKeys(false)) {
                ConfigurationSection commandSection = commandsSection.getConfigurationSection(commandName);
                if (commandSection == null) continue;

                CommandConfig commandConfig = new CommandConfig();
                commandConfig.permission = commandSection.getString("permission");
                
                // Загружаем алиасы
                List<String> aliases = commandSection.getStringList("aliases");
                if (aliases != null) {
                    commandConfig.aliases = new ArrayList<>(aliases);
                }

                // Загружаем аргументы (неограниченное количество)
                ConfigurationSection argumentsSection = commandSection.getConfigurationSection("arguments");
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

                        commandConfig.arguments.put(argName, argConfig);
                    }
                }

                commandConfigs.put(commandName, commandConfig);
            }
        }

        // Загружаем супер-команды (с субкомандами)
        ConfigurationSection superCommandsSection = config.getConfigurationSection("super-commands");
        if (superCommandsSection != null) {
            for (String commandName : superCommandsSection.getKeys(false)) {
                ConfigurationSection commandSection = superCommandsSection.getConfigurationSection(commandName);
                if (commandSection == null) continue;

                SuperCommandConfig superCommandConfig = new SuperCommandConfig();
                superCommandConfig.permission = commandSection.getString("permission");

                // Загружаем субкоманды
                ConfigurationSection subcommandsSection = commandSection.getConfigurationSection("subcommands");
                if (subcommandsSection != null) {
                    for (String subcommandName : subcommandsSection.getKeys(false)) {
                        ConfigurationSection subcommandSection = subcommandsSection.getConfigurationSection(subcommandName);
                        if (subcommandSection == null) continue;

                        SubCommandConfig subCommandConfig = new SubCommandConfig();
                        subCommandConfig.permission = subcommandSection.getString("permission");

                        // Загружаем аргументы (неограниченное количество)
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

                        superCommandConfig.subcommands.put(subcommandName, subCommandConfig);
                    }
                }

                superCommandConfigs.put(commandName, superCommandConfig);
            }
        }
    }

    public PermissionResult checkPermission(Player player, CommandBlockerUltra.CommandInfo commandInfo) {
        String mainCommand = commandInfo.getMainCommand();
        String[] args = commandInfo.getArguments();

        // Проверяем обычные команды
        CommandConfig commandConfig = commandConfigs.get(mainCommand);
        if (commandConfig != null) {
            return checkRegularCommandPermission(player, commandInfo, commandConfig);
        }

        // Проверяем алиасы обычных команд
        for (Map.Entry<String, CommandConfig> entry : commandConfigs.entrySet()) {
            CommandConfig config = entry.getValue();
            if (config.aliases != null && config.aliases.contains(mainCommand)) {
                return checkRegularCommandPermission(player, commandInfo, config);
            }
        }

        // Проверяем super-commands (с субкомандами)
        SuperCommandConfig superCommandConfig = superCommandConfigs.get(mainCommand);
        if (superCommandConfig != null && args.length > 0) {
            String subCommand = args[0];
            SubCommandConfig subConfig = superCommandConfig.subcommands.get(subCommand);
            if (subConfig != null) {
                // Формируем массив аргументов для субкоманды (без первого, т.к. это субкоманда)
                String[] subArgs = new String[args.length - 1];
                if (args.length > 1) {
                    System.arraycopy(args, 1, subArgs, 0, args.length - 1);
                }
                return checkSubCommandPermission(player, subConfig, subArgs);
            }
        }

        // Проверяем алиасы super-commands
        for (Map.Entry<String, SuperCommandConfig> entry : superCommandConfigs.entrySet()) {
            SuperCommandConfig config = entry.getValue();
            if (config.aliases != null && config.aliases.contains(mainCommand) && args.length > 0) {
                String subCommand = args[0];
                SubCommandConfig subConfig = config.subcommands.get(subCommand);
                if (subConfig != null) {
                    String[] subArgs = new String[args.length - 1];
                    if (args.length > 1) {
                        System.arraycopy(args, 1, subArgs, 0, args.length - 1);
                    }
                    return checkSubCommandPermission(player, subConfig, subArgs);
                }
            }
        }

        return new PermissionResult(false, "Команда не найдена в конфиге");
    }

    private PermissionResult checkRegularCommandPermission(Player player, CommandBlockerUltra.CommandInfo commandInfo, CommandConfig commandConfig) {
        // Проверяем права на основную команду
        if (commandConfig.permission != null && !commandConfig.permission.equalsIgnoreCase("none") && !player.hasPermission(commandConfig.permission)) {
            return new PermissionResult(false, "Нет прав на команду: " + commandConfig.permission);
        }

        // Проверяем аргументы (динамически)
        String[] args = commandInfo.getArguments();
        for (int i = 0; i < args.length; i++) {
            String argKey = "arg#" + (i + 1);
            if (commandConfig.arguments.containsKey(argKey)) {
                ArgumentConfig argConfig = commandConfig.arguments.get(argKey);
                
                // Проверяем права на аргумент
                if (argConfig.permission != null && !argConfig.permission.equalsIgnoreCase("none") && !player.hasPermission(argConfig.permission)) {
                    return new PermissionResult(false, "Нет прав на аргумент: " + argConfig.permission);
                }

                // Проверяем списки значений
                if (!argConfig.lists.isEmpty()) {
                    String argValue = args[i];
                    boolean foundInList = false;
                    boolean hasPermissionForValue = false;
                    
                    for (Map.Entry<String, String> entry : argConfig.lists.entrySet()) {
                        String listKey = entry.getKey();
                        String permission = entry.getValue();
                        
                        if (listKey.equals(argValue)) {
                            foundInList = true;
                            if (permission == null || permission.equalsIgnoreCase("none") || player.hasPermission(permission)) {
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

    private PermissionResult checkSubCommandPermission(Player player, SubCommandConfig subCommandConfig, String[] subArgs) {
        // Проверяем права на субкоманду
        if (subCommandConfig.permission != null && !subCommandConfig.permission.equalsIgnoreCase("none") && !player.hasPermission(subCommandConfig.permission)) {
            return new PermissionResult(false, "Нет прав на субкоманду: " + subCommandConfig.permission);
        }

        // Проверяем аргументы (динамически)
        for (int i = 0; i < subArgs.length; i++) {
            String argKey = "arg#" + (i + 1);
            if (subCommandConfig.arguments.containsKey(argKey)) {
                ArgumentConfig argConfig = subCommandConfig.arguments.get(argKey);
                // Проверяем права на аргумент
                if (argConfig.permission != null && !argConfig.permission.equalsIgnoreCase("none") && !player.hasPermission(argConfig.permission)) {
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
                            if (permission == null || permission.equalsIgnoreCase("none") || player.hasPermission(permission)) {
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
        public List<String> aliases = new ArrayList<>();
        public Map<String, ArgumentConfig> arguments = new HashMap<>();
    }

    public static class SuperCommandConfig {
        public String permission;
        public List<String> aliases = new ArrayList<>();
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

    public Map<String, SuperCommandConfig> getSuperCommandConfigs() {
        return superCommandConfigs;
    }
} 