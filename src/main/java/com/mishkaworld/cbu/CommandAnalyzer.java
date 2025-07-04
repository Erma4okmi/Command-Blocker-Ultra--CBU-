package com.mishkaworld.cbu;

import org.bukkit.entity.Player;
import java.util.Arrays;
import java.util.Date;

/**
 * Класс для детального анализа команд
 */
public class CommandAnalyzer {

    /**
     * Анализирует команду и возвращает детальную информацию
     */
    public static CommandAnalysis analyzeCommand(String fullCommand, String sender) {
        CommandBlockerUltra.CommandInfo commandInfo = CommandBlockerUltra.parseCommand(fullCommand);
        return new CommandAnalysis(commandInfo, sender);
    }

    /**
     * Получает тип команды на основе её названия
     */
    public static CommandType getCommandType(CommandBlockerUltra.CommandInfo commandInfo) {
        String mainCommand = commandInfo.getMainCommand();
        
        if (isVanillaCommand(mainCommand)) {
            return CommandType.VANILLA;
        } else if (isPluginCommand(mainCommand)) {
            return CommandType.PLUGIN;
        } else {
            return CommandType.CUSTOM;
        }
    }

    private static boolean isVanillaCommand(String command) {
        return command.startsWith("minecraft:") || command.startsWith("bukkit:");
    }

    private static boolean isPluginCommand(String command) {
        return command.contains(":");
    }

    /**
     * Проверяет, имеет ли игрок права на выполнение команды
     */
    public static boolean hasPermission(Player player, String command) {
        return player.hasPermission("*") || 
               player.hasPermission(command) || 
               player.hasPermission(command + ".*");
    }

    /**
     * Класс для хранения детального анализа команды
     */
    public static class CommandAnalysis {
        private final CommandBlockerUltra.CommandInfo commandInfo;
        private final String sender;
        private final CommandType commandType;
        private final long timestamp;

        public CommandAnalysis(CommandBlockerUltra.CommandInfo commandInfo, String sender) {
            this.commandInfo = commandInfo;
            this.sender = sender;
            this.commandType = CommandAnalyzer.getCommandType(commandInfo);
            this.timestamp = System.currentTimeMillis();
        }

        public CommandBlockerUltra.CommandInfo getCommandInfo() {
            return commandInfo;
        }

        public String getSender() {
            return sender;
        }

        public CommandType getCommandType() {
            return commandType;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getFormattedInfo() {
            return String.format(
                "=== Анализ команды ===\n" +
                "Отправитель: %s\n" +
                "Команда: %s\n" +
                "Аргументы: %s\n" +
                "Тип: %s\n" +
                "Время: %s\n" +
                "=====================",
                sender,
                commandInfo.getMainCommand(),
                Arrays.toString(commandInfo.getArguments()),
                commandType,
                new Date(timestamp)
            );
        }
    }

    /**
     * Перечисление типов команд
     */
    public enum CommandType {
        VANILLA("Ванильная команда"),
        PLUGIN("Команда плагина"),
        CUSTOM("Пользовательская команда");

        private final String description;

        CommandType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return description;
        }
    }
} 