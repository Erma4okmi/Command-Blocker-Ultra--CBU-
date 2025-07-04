package com.mishkaworld.cbu;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import java.util.Arrays;

public class DebugCommands {
    private final LogManager logManager;

    public DebugCommands(FileConfiguration config, Plugin plugin) {
        this.logManager = new LogManager(config, plugin);
    }

    public void logCommandInfo(String sender, CommandBlockerUltra.CommandInfo commandInfo) {
        logSection("Информация о команде", () -> {
            logField("Отправитель", sender);
            logField("Полная команда", commandInfo.getFullCommand());
            logField("Основная команда", commandInfo.getMainCommand());
            logField("Аргументы", Arrays.toString(commandInfo.getArguments()));
            logField("Количество аргументов", String.valueOf(commandInfo.getArguments().length));
        });
    }

    public void logAnalysisInfo(String sender, CommandAnalyzer.CommandAnalysis analysis) {
        logSection("Анализ команды", () -> {
            logField("Отправитель", sender);
            logField("Команда", analysis.getCommandInfo().getMainCommand());
            logField("Аргументы", Arrays.toString(analysis.getCommandInfo().getArguments()));
            logField("Тип", analysis.getCommandType().toString());
            logField("Время", new java.util.Date(analysis.getTimestamp()).toString());
        });
    }

    public void logDetailedCommandInfo(String sender, CommandBlockerUltra.CommandInfo commandInfo, 
                                     PermissionChecker.PermissionResult permissionResult) {
        logSection("Детальная информация о команде", () -> {
            logField("Отправитель", sender);
            logField("Полная команда", commandInfo.getFullCommand());
            logField("Основная команда", commandInfo.getMainCommand());
            logField("Субкоманда", commandInfo.hasSubCommand() ? commandInfo.getSubCommand() : "НЕТ");
            logField("Аргументы", Arrays.toString(commandInfo.getArguments()));
            logField("Аргументы субкоманды", Arrays.toString(commandInfo.getSubCommandArguments()));
            logField("Статус прав", permissionResult != null ? (permissionResult.allowed ? "РАЗРЕШЕНО" : "ЗАБЛОКИРОВАНО") : "N/A");
            logField("Сообщение о правах", permissionResult != null ? permissionResult.message : "N/A");
        });
    }

    private void logSection(String title, Runnable content) {
        logManager.log("=== " + title + " ===");
        content.run();
        logManager.log("=".repeat(title.length() + 8));
    }

    private void logField(String fieldName, String value) {
        logManager.log(fieldName + ": " + value);
    }
} 