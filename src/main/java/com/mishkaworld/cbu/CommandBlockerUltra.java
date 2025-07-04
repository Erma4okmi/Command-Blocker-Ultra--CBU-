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

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class CommandBlockerUltra extends JavaPlugin implements Listener {

    private Logger logger;
    private DebugCommands debugCommands;
    private CommandBlocker commandBlocker;
    private PermissionChecker permissionChecker;
    private TabCompleteManager tabCompleteManager;

    @Override
    public void onEnable() {
        logger = getLogger();
        logger.info("Command Blocker Ultra v1.12 загружается...");
        
        // Сохраняем конфиг по умолчанию
        saveDefaultConfig();
        
        // Инициализация отладочного класса
        debugCommands = new DebugCommands(logger);
        
        // Инициализация проверки прав
        permissionChecker = new PermissionChecker(getConfig());
        
        // Регистрируем слушатели событий
        getServer().getPluginManager().registerEvents(this, this);
        
        // Регистрируем блокировщик команд
        commandBlocker = new CommandBlocker(this, permissionChecker);
        getServer().getPluginManager().registerEvents(commandBlocker, this);
        
        // Регистрируем менеджер автодополнения
        tabCompleteManager = new TabCompleteManager(permissionChecker);
        getServer().getPluginManager().registerEvents(tabCompleteManager, this);
        
        logger.info("Command Blocker Ultra v1.12 успешно загружен!");
    }

    @Override
    public void onDisable() {
        logger.info("Command Blocker Ultra v1.12 выгружается...");
        logger.info("Command Blocker Ultra v1.12 успешно выгружен!");
    }

    /**
     * Обработчик команд игроков
     */
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String fullCommand = event.getMessage();
        
        // Получаем и разбираем команду
        CommandInfo commandInfo = parseCommand(fullCommand);
        
        // Проверяем права
        PermissionChecker.PermissionResult permissionResult = permissionChecker.checkPermission(player, commandInfo);
        
        // Логируем детальную информацию только если включен debug-mode
        if (getConfig().getBoolean("debug-mode", true)) {
            debugCommands.logDetailedCommandInfo(player.getName(), commandInfo, permissionResult);
        }
        
        // Выполняем детальный анализ команды только если включен log-mode
        if (getConfig().getBoolean("log-mode", true)) {
            CommandAnalyzer.CommandAnalysis analysis = CommandAnalyzer.analyzeCommand(fullCommand, player.getName());
            logger.info(analysis.getFormattedInfo());
        }
    }

    /**
     * Обработчик консольных команд
     */
    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        String fullCommand = event.getCommand();
        CommandInfo commandInfo = parseCommand("/" + fullCommand);
        
        // Логируем через DebugCommands только если включен debug-mode
        if (getConfig().getBoolean("debug-mode", true)) {
            debugCommands.logCommandInfo("CONSOLE", commandInfo);
        }
        
        // Выполняем детальный анализ команды только если включен log-mode
        if (getConfig().getBoolean("log-mode", true)) {
            CommandAnalyzer.CommandAnalysis analysis = CommandAnalyzer.analyzeCommand("/" + fullCommand, "CONSOLE");
            logger.info(analysis.getFormattedInfo());
        }
    }

    /**
     * Разбирает команду на части
     * @param fullCommand Полная команда (например: "/tp player1 player2")
     * @return Объект с информацией о команде
     */
    public static CommandInfo parseCommand(String fullCommand) {
        // Убираем начальный слеш если есть
        String command = fullCommand.startsWith("/") ? fullCommand.substring(1) : fullCommand;
        
        // Разбиваем команду на части
        String[] parts = command.split("\\s+");
        
        if (parts.length == 0) {
            return new CommandInfo("", new String[0], fullCommand);
        }
        
        String mainCommand = parts[0].toLowerCase();
        String[] arguments = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
        
        return new CommandInfo(mainCommand, arguments, fullCommand);
    }

    /**
     * Класс для хранения информации о команде
     */
    public static class CommandInfo {
        private final String mainCommand;
        private final String[] arguments;
        private final String fullCommand;

        public CommandInfo(String mainCommand, String[] arguments, String fullCommand) {
            this.mainCommand = mainCommand;
            this.arguments = arguments;
            this.fullCommand = fullCommand;
        }

        public String getMainCommand() {
            return mainCommand;
        }

        public String[] getArguments() {
            return arguments;
        }

        public String getFullCommand() {
            return fullCommand;
        }

        public int getArgumentCount() {
            return arguments.length;
        }

        public String getArgument(int index) {
            if (index >= 0 && index < arguments.length) {
                return arguments[index];
            }
            return null;
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
    }
} 