package com.mishkaworld.cbu;

import org.bukkit.entity.Player;
import java.util.Arrays;

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
        
        if (mainCommand.startsWith("minecraft:") || mainCommand.startsWith("bukkit:")) {
            return CommandType.VANILLA;
        } else if (mainCommand.contains(":")) {
            return CommandType.PLUGIN;
        } else {
            return CommandType.CUSTOM;
        }
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
            StringBuilder sb = new StringBuilder();
            sb.append("=== Анализ команды ===\n");
            sb.append("Отправитель: ").append(sender).append("\n");
            sb.append("Команда: ").append(commandInfo.getMainCommand()).append("\n");
            sb.append("Аргументы: ").append(Arrays.toString(commandInfo.getArguments())).append("\n");
            sb.append("Тип: ").append(commandType).append("\n");
            sb.append("Время: ").append(new java.util.Date(timestamp)).append("\n");
            sb.append("=====================");
            return sb.toString();
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