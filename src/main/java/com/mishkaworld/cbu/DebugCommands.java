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
        logManager.log("=== Информация о команде ===");
        logManager.log("Отправитель: " + sender);
        logManager.log("Полная команда: " + commandInfo.getFullCommand());
        logManager.log("Основная команда: " + commandInfo.getMainCommand());
        logManager.log("Аргументы: " + Arrays.toString(commandInfo.getArguments()));
        logManager.log("Количество аргументов: " + commandInfo.getArguments().length);
        logManager.log("==========================");
    }

    public void logAnalysisInfo(String sender, CommandAnalyzer.CommandAnalysis analysis) {
        logManager.log("=== Анализ команды ===");
        logManager.log("Отправитель: " + sender);
        logManager.log("Команда: " + analysis.getCommandInfo().getMainCommand());
        logManager.log("Аргументы: " + Arrays.toString(analysis.getCommandInfo().getArguments()));
        logManager.log("Тип: " + analysis.getCommandType());
        logManager.log("Время: " + new java.util.Date(analysis.getTimestamp()));
        logManager.log("==========================");
    }

    public void logDetailedCommandInfo(String sender, CommandBlockerUltra.CommandInfo commandInfo, PermissionChecker.PermissionResult permissionResult) {
        logManager.log("=== Детальная информация о команде ===");
        logManager.log("Отправитель: " + sender);
        logManager.log("Полная команда: " + commandInfo.getFullCommand());
        logManager.log("Основная команда: " + commandInfo.getMainCommand());
        logManager.log("Субкоманда: " + (commandInfo.hasSubCommand() ? commandInfo.getSubCommand() : "НЕТ"));
        logManager.log("Аргументы: " + Arrays.toString(commandInfo.getArguments()));
        logManager.log("Аргументы субкоманды: " + Arrays.toString(commandInfo.getSubCommandArguments()));
        logManager.log("Статус прав: " + (permissionResult.allowed ? "РАЗРЕШЕНО" : "ЗАБЛОКИРОВАНО"));
        logManager.log("Сообщение о правах: " + permissionResult.message);
        logManager.log("======================================");
    }
} 