package com.mishkaworld.cbu;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.HandlerList;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.io.File;

public class CommandBlockerUltra extends JavaPlugin implements Listener {

    private Logger logger;
    private DebugCommands debugCommands;
    private CommandBlocker commandBlocker;
    private PermissionChecker permissionChecker;
    private TabCompleteManager tabCompleteManager;
    private LogManager logManager;

    @Override
    public void onEnable() {
        initializePlugin();
    }

    private void initializePlugin() {
        logger = getLogger();
        logger.info("Command Blocker Ultra v1.12 загружается...");
        
        setupDataFolder();
        setupConfiguration();
        initializeComponents();
        registerEventListeners();
        
        logger.info("Command Blocker Ultra v1.12 успешно загружен!");
    }

    private void setupDataFolder() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
    }

    private void setupConfiguration() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        reloadConfig();
        logger.info("Конфиг загружен из: " + configFile.getAbsolutePath());
    }

    private void initializeComponents() {
        debugCommands = new DebugCommands(getConfig(), this);
        logManager = new LogManager(getConfig(), this);
        permissionChecker = new PermissionChecker(getConfig());
        commandBlocker = new CommandBlocker(this, permissionChecker);
        tabCompleteManager = new TabCompleteManager(permissionChecker, getConfig(), this);
    }

    private void registerEventListeners() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(commandBlocker, this);
        getServer().getPluginManager().registerEvents(tabCompleteManager, this);
    }

    @Override
    public void onDisable() {
        logger.info("Command Blocker Ultra v1.12 выгружается...");
        logger.info("Command Blocker Ultra v1.12 успешно выгружен!");
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String fullCommand = event.getMessage();
        
        CommandInfo commandInfo = parseCommand(fullCommand);
        PermissionChecker.PermissionResult permissionResult = permissionChecker.checkPermission(player, commandInfo);
        
        logCommandInfo(player.getName(), commandInfo, permissionResult, fullCommand);
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        String fullCommand = event.getCommand();
        CommandInfo commandInfo = parseCommand("/" + fullCommand);
        
        logCommandInfo("CONSOLE", commandInfo, null, "/" + fullCommand);
    }

    private void logCommandInfo(String playerName, CommandInfo commandInfo, 
                              PermissionChecker.PermissionResult permissionResult, String fullCommand) {
        if (getConfig().getBoolean("debug-mode", true)) {
            debugCommands.logDetailedCommandInfo(playerName, commandInfo, permissionResult);
        }
        
        if (getConfig().getBoolean("log-mode", true)) {
            CommandAnalyzer.CommandAnalysis analysis = CommandAnalyzer.analyzeCommand(fullCommand, playerName);
            logManager.log(analysis.getFormattedInfo());
        }
    }

    public static CommandInfo parseCommand(String fullCommand) {
        String command = fullCommand.startsWith("/") ? fullCommand.substring(1) : fullCommand;
        String[] parts = command.split("\\s+");
        
        if (parts.length == 0) {
            return new CommandInfo("", new String[0], fullCommand);
        }
        
        String mainCommand = parts[0].toLowerCase();
        String[] arguments = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
        
        return new CommandInfo(mainCommand, arguments, fullCommand);
    }

    public static class CommandInfo {
        private final String mainCommand;
        private final String[] arguments;
        private final String fullCommand;

        public CommandInfo(String mainCommand, String[] arguments, String fullCommand) {
            this.mainCommand = mainCommand;
            this.arguments = arguments.clone(); // Защита от изменения
            this.fullCommand = fullCommand;
        }

        public String getMainCommand() {
            return mainCommand;
        }

        public String[] getArguments() {
            return arguments.clone(); // Защита от изменения
        }

        public String getFullCommand() {
            return fullCommand;
        }

        public int getArgumentCount() {
            return arguments.length;
        }

        public String getArgument(int index) {
            return (index >= 0 && index < arguments.length) ? arguments[index] : null;
        }

        public boolean hasArguments() {
            return arguments.length > 0;
        }

        public boolean startsWith(String prefix) {
            return mainCommand.startsWith(prefix);
        }

        public boolean equalsCommand(String command) {
            return mainCommand.equals(command.toLowerCase());
        }

        public String getSubCommand() {
            return arguments.length > 0 ? arguments[0] : null;
        }

        public boolean hasSubCommand() {
            return arguments.length > 0;
        }

        public String[] getSubCommandArguments() {
            return arguments.length > 1 ? Arrays.copyOfRange(arguments, 1, arguments.length) : new String[0];
        }

        @Override
        public String toString() {
            return String.format("CommandInfo{mainCommand='%s', arguments=%s, fullCommand='%s'}", 
                               mainCommand, Arrays.toString(arguments), fullCommand);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("cbu")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("cbu.reload")) {
                    // Перезагружаем конфиг
                    reloadPluginConfig();
                    
                    sender.sendMessage("§aКонфигурация перезагружена!");
                    logger.info("Конфигурация перезагружена игроком: " + sender.getName());
                    return true;
                } else {
                    sender.sendMessage("§cНет прав на перезагрузку конфигурации!");
                    return true;
                }
            }
            sender.sendMessage("§eCommand Blocker Ultra v1.12");
            sender.sendMessage("§eИспользование: /cbu reload");
            return true;
        }
        return false;
    }

    public void reloadPluginConfig() {
        reloadConfig();
        // Пересоздаём все объекты, зависящие от конфига
        permissionChecker = new PermissionChecker(getConfig());
        logManager = new LogManager(getConfig(), this);
        debugCommands = new DebugCommands(getConfig(), this);

        // Перерегистрируем TabCompleteManager
        if (tabCompleteManager != null) {
            HandlerList.unregisterAll(tabCompleteManager);
        }
        tabCompleteManager = new TabCompleteManager(permissionChecker, getConfig(), this);
        getServer().getPluginManager().registerEvents(tabCompleteManager, this);

        // Перерегистрируем CommandBlocker
        if (commandBlocker != null) {
            HandlerList.unregisterAll(commandBlocker);
        }
        commandBlocker = new CommandBlocker(this, permissionChecker);
        getServer().getPluginManager().registerEvents(commandBlocker, this);
    }

    public LogManager getLogManager() {
        return logManager;
    }
} 