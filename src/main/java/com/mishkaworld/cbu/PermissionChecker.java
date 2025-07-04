package com.mishkaworld.cbu;

import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class PermissionChecker {
    private final FileConfiguration config;
    private final Map<String, CommandConfig> commandConfigs = new HashMap<>();
    private final Map<String, SuperCommandConfig> superCommandConfigs = new HashMap<>();

    public PermissionChecker(FileConfiguration config) {
        this.config = config;
        loadCommandConfigs();
    }

    private void loadCommandConfigs() {
        loadRegularCommands();
        loadSuperCommands();
    }

    private void loadRegularCommands() {
        ConfigurationSection commandsSection = config.getConfigurationSection("commands");
        if (commandsSection == null) return;

        for (String commandName : commandsSection.getKeys(false)) {
            ConfigurationSection commandSection = commandsSection.getConfigurationSection(commandName);
            if (commandSection == null) continue;

            CommandConfig commandConfig = new CommandConfig();
            commandConfig.setMainCommand(commandName);
            commandConfig.setPermission(commandSection.getString("permission"));
            commandConfig.setAliases(commandSection.getStringList("aliases"));
            loadArguments(commandSection, commandConfig);

            commandConfigs.put(commandName, commandConfig);
        }
    }

    private void loadSuperCommands() {
        ConfigurationSection superCommandsSection = config.getConfigurationSection("super-commands");
        if (superCommandsSection == null) return;

        for (String commandName : superCommandsSection.getKeys(false)) {
            ConfigurationSection commandSection = superCommandsSection.getConfigurationSection(commandName);
            if (commandSection == null) continue;

            SuperCommandConfig superCommandConfig = new SuperCommandConfig();
            superCommandConfig.setPermission(commandSection.getString("permission"));
            loadSubCommands(commandSection, superCommandConfig, commandName);

            superCommandConfigs.put(commandName, superCommandConfig);
        }
    }

    private void loadArguments(ConfigurationSection commandSection, CommandConfig commandConfig) {
        ConfigurationSection argumentsSection = commandSection.getConfigurationSection("arguments");
        if (argumentsSection == null) return;

        for (String argName : argumentsSection.getKeys(false)) {
            ConfigurationSection argSection = argumentsSection.getConfigurationSection(argName);
            if (argSection == null) continue;

            ArgumentConfig argConfig = new ArgumentConfig();
            argConfig.setPermission(argSection.getString("permission"));
            loadArgumentLists(argSection, argConfig);

            commandConfig.addArgument(argName, argConfig);
        }
    }

    private void loadSubCommands(ConfigurationSection commandSection, SuperCommandConfig superCommandConfig, String mainCommandName) {
        ConfigurationSection subcommandsSection = commandSection.getConfigurationSection("subcommands");
        if (subcommandsSection == null) return;

        for (String subcommandName : subcommandsSection.getKeys(false)) {
            ConfigurationSection subcommandSection = subcommandsSection.getConfigurationSection(subcommandName);
            if (subcommandSection == null) continue;

            SubCommandConfig subCommandConfig = new SubCommandConfig();
            subCommandConfig.setSubCommandName(subcommandName);
            subCommandConfig.setPermission(subcommandSection.getString("permission"));
            loadSubCommandArguments(subcommandSection, subCommandConfig);

            superCommandConfig.addSubCommand(subcommandName, subCommandConfig);
        }
    }

    private void loadSubCommandArguments(ConfigurationSection subcommandSection, SubCommandConfig subCommandConfig) {
        ConfigurationSection argumentsSection = subcommandSection.getConfigurationSection("arguments");
        if (argumentsSection == null) return;

        for (String argName : argumentsSection.getKeys(false)) {
            ConfigurationSection argSection = argumentsSection.getConfigurationSection(argName);
            if (argSection == null) continue;

            ArgumentConfig argConfig = new ArgumentConfig();
            argConfig.setPermission(argSection.getString("permission"));
            loadArgumentLists(argSection, argConfig);

            subCommandConfig.addArgument(argName, argConfig);
        }
    }

    private void loadArgumentLists(ConfigurationSection argSection, ArgumentConfig argConfig) {
        ConfigurationSection listsSection = argSection.getConfigurationSection("lists");
        if (listsSection == null) return;

        for (String listKey : listsSection.getKeys(false)) {
            argConfig.addListValue(listKey, listsSection.getString(listKey));
        }
    }

    public PermissionResult checkPermission(Player player, CommandBlockerUltra.CommandInfo commandInfo) {
        String mainCommand = commandInfo.getMainCommand();
        String[] args = commandInfo.getArguments();

        // Проверяем обычные команды
        PermissionResult result = checkRegularCommands(player, commandInfo, mainCommand);
        if (result != null) return result;

        // Проверяем супер-команды
        return checkSuperCommands(player, mainCommand, args);
    }

    private PermissionResult checkRegularCommands(Player player, CommandBlockerUltra.CommandInfo commandInfo, String mainCommand) {
        // Прямая проверка
        CommandConfig commandConfig = commandConfigs.get(mainCommand);
        if (commandConfig != null) {
            return checkRegularCommandPermission(player, commandInfo, commandConfig);
        }

        // Проверка алиасов
        for (CommandConfig config : commandConfigs.values()) {
            if (config.hasAlias(mainCommand)) {
                return checkRegularCommandPermission(player, commandInfo, config);
            }
        }

        return null;
    }

    private PermissionResult checkSuperCommands(Player player, String mainCommand, String[] args) {
        if (args.length == 0) {
            return new PermissionResult(false, "Команда не найдена в конфиге");
        }

        String subCommand = args[0];
        String[] subArgs = createSubArgs(args);

        // Прямая проверка
        SuperCommandConfig superCommandConfig = superCommandConfigs.get(mainCommand);
        if (superCommandConfig != null) {
            SubCommandConfig subConfig = superCommandConfig.getSubCommand(subCommand);
            if (subConfig != null) {
                return checkSubCommandPermission(player, subConfig, subArgs);
            }
        }

        // Проверка алиасов
        for (SuperCommandConfig config : superCommandConfigs.values()) {
            if (config.hasAlias(mainCommand)) {
                SubCommandConfig subConfig = config.getSubCommand(subCommand);
                if (subConfig != null) {
                    return checkSubCommandPermission(player, subConfig, subArgs);
                }
            }
        }

        return new PermissionResult(false, "Команда не найдена в конфиге");
    }

    private String[] createSubArgs(String[] args) {
        if (args.length <= 1) return new String[0];
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, args.length - 1);
        return subArgs;
    }

    private PermissionResult checkRegularCommandPermission(Player player, CommandBlockerUltra.CommandInfo commandInfo, CommandConfig commandConfig) {
        // Проверяем права на основную команду
        if (!hasPermission(player, commandConfig.getPermission())) {
            return new PermissionResult(false, "Нет прав на команду: " + commandConfig.getPermission());
        }

        // Проверяем аргументы
        return checkArguments(player, commandInfo.getArguments(), commandConfig.getArguments());
    }

    private PermissionResult checkSubCommandPermission(Player player, SubCommandConfig subCommandConfig, String[] subArgs) {
        // Проверяем права на субкоманду
        if (!hasPermission(player, subCommandConfig.getPermission())) {
            return new PermissionResult(false, "Нет прав на субкоманду: " + subCommandConfig.getPermission());
        }

        // Проверяем аргументы
        return checkArguments(player, subArgs, subCommandConfig.getArguments());
    }

    private boolean hasPermission(Player player, String permission) {
        return permission == null || permission.equalsIgnoreCase("none") || player.hasPermission(permission);
    }

    private PermissionResult checkArguments(Player player, String[] args, Map<String, ArgumentConfig> arguments) {
        for (int i = 0; i < args.length; i++) {
            String argKey = "arg#" + (i + 1);
            ArgumentConfig argConfig = arguments.get(argKey);
            if (argConfig == null) continue;

            // Проверяем права на аргумент
            if (!hasPermission(player, argConfig.getPermission())) {
                return new PermissionResult(false, "Нет прав на аргумент: " + argConfig.getPermission());
            }

            // Проверяем списки значений
            if (!argConfig.getLists().isEmpty()) {
                PermissionResult listResult = checkListValue(player, args[i], argConfig.getLists());
                if (!listResult.allowed) {
                    return listResult;
                }
            }
        }

        return new PermissionResult(true, "Все права проверены успешно");
    }

    private PermissionResult checkListValue(Player player, String argValue, Map<String, String> lists) {
        boolean foundInList = false;
        boolean hasPermissionForValue = false;

        for (Map.Entry<String, String> entry : lists.entrySet()) {
            String listKey = entry.getKey();
            String permission = entry.getValue();

            if (listKey.equals(argValue)) {
                foundInList = true;
                if (hasPermission(player, permission)) {
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

        return new PermissionResult(true, "Значение аргумента проверено успешно");
    }

    public static class CommandConfig {
        private String permission;
        private List<String> aliases = new ArrayList<>();
        private Map<String, ArgumentConfig> arguments = new HashMap<>();
        private String mainCommand;

        public void setPermission(String permission) {
            this.permission = permission;
        }

        public void setAliases(List<String> aliases) {
            this.aliases = new ArrayList<>(aliases);
        }

        public void addArgument(String argName, ArgumentConfig argConfig) {
            arguments.put(argName, argConfig);
        }

        public boolean hasAlias(String command) {
            return aliases != null && aliases.contains(command);
        }

        public String getMainCommand() {
            return mainCommand;
        }

        public void setMainCommand(String mainCommand) {
            this.mainCommand = mainCommand;
        }

        public String getPermission() {
            return permission;
        }

        public List<String> getAliases() {
            return aliases;
        }

        public Map<String, ArgumentConfig> getArguments() {
            return arguments;
        }
    }

    public static class SuperCommandConfig {
        private String permission;
        private List<String> aliases = new ArrayList<>();
        private Map<String, SubCommandConfig> subcommands = new HashMap<>();

        public void setPermission(String permission) {
            this.permission = permission;
        }

        public void addSubCommand(String subcommandName, SubCommandConfig subCommandConfig) {
            subcommands.put(subcommandName, subCommandConfig);
        }

        public SubCommandConfig getSubCommand(String subcommandName) {
            return subcommands.get(subcommandName);
        }

        public boolean hasAlias(String command) {
            return aliases != null && aliases.contains(command);
        }

        public String getPermission() {
            return permission;
        }

        public List<String> getAliases() {
            return aliases;
        }

        public Map<String, SubCommandConfig> getSubCommands() {
            return subcommands;
        }
    }

    public static class SubCommandConfig {
        private String permission;
        private Map<String, ArgumentConfig> arguments = new HashMap<>();
        private String subCommandName;

        public void setPermission(String permission) {
            this.permission = permission;
        }

        public void addArgument(String argName, ArgumentConfig argConfig) {
            arguments.put(argName, argConfig);
        }

        public String getPermission() {
            return permission;
        }

        public Map<String, ArgumentConfig> getArguments() {
            return arguments;
        }

        public String getSubCommandName() {
            return subCommandName;
        }

        public void setSubCommandName(String subCommandName) {
            this.subCommandName = subCommandName;
        }
    }

    public static class ArgumentConfig {
        private String permission;
        private Map<String, String> lists = new HashMap<>();

        public void setPermission(String permission) {
            this.permission = permission;
        }

        public void addListValue(String listKey, String value) {
            lists.put(listKey, value);
        }

        public String getPermission() {
            return permission;
        }

        public Map<String, String> getLists() {
            return lists;
        }
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