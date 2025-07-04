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
    private CommandBlocker commandBlocker;
    private PermissionChecker permissionChecker;
    private TabCompleteManager tabCompleteManager;

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
        // Пустой обработчик - вся логика в CommandBlocker
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        // Пустой обработчик
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
            this.arguments = arguments.clone();
            this.fullCommand = fullCommand;
        }

        public String getMainCommand() {
            return mainCommand;
        }

        public String[] getArguments() {
            return arguments.clone();
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
        if (label.equalsIgnoreCase("cbu")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("cbu.reload")) {
                    reloadPluginConfig();
                    sender.sendMessage("§aКонфигурация перезагружена!");
                } else {
                    sender.sendMessage("§cУ вас нет прав для выполнения этой команды!");
                }
                return true;
            }
        }
        return false;
    }

    public void reloadPluginConfig() {
        reloadConfig();
        permissionChecker = new PermissionChecker(getConfig());

        if (tabCompleteManager != null) {
            HandlerList.unregisterAll(tabCompleteManager);
        }
        tabCompleteManager = new TabCompleteManager(permissionChecker, getConfig(), this);
        getServer().getPluginManager().registerEvents(tabCompleteManager, this);

        if (commandBlocker != null) {
            HandlerList.unregisterAll(commandBlocker);
        }
        commandBlocker = new CommandBlocker(this, permissionChecker);
        getServer().getPluginManager().registerEvents(commandBlocker, this);
    }
} 