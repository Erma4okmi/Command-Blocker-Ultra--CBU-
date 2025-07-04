package com.mishkaworld.cbu;

import java.util.Arrays;
import java.util.logging.Logger;

public class DebugCommands {
    private final Logger logger;

    public DebugCommands(Logger logger) {
        this.logger = logger;
    }

    public void logCommandInfo(String sender, CommandBlockerUltra.CommandInfo commandInfo) {
        logger.info("[DEBUG] === Информация о команде ===");
        logger.info("[DEBUG] Отправитель: " + sender);
        logger.info("[DEBUG] Полная команда: " + commandInfo.getFullCommand());
        logger.info("[DEBUG] Основная команда: " + commandInfo.getMainCommand());
        logger.info("[DEBUG] Аргументы: " + Arrays.toString(commandInfo.getArguments()));
        logger.info("[DEBUG] Количество аргументов: " + commandInfo.getArguments().length);
        logger.info("[DEBUG] ==========================");
    }

    public void logAnalysis(String sender, CommandAnalyzer.CommandAnalysis analysis) {
        logger.info("[DEBUG] === Анализ команды ===");
        logger.info("[DEBUG] Отправитель: " + sender);
        logger.info("[DEBUG] Команда: " + analysis.getCommandInfo().getMainCommand());
        logger.info("[DEBUG] Аргументы: " + Arrays.toString(analysis.getCommandInfo().getArguments()));
        logger.info("[DEBUG] Тип: " + analysis.getCommandType());
        logger.info("[DEBUG] Время: " + new java.util.Date(analysis.getTimestamp()));
        logger.info("[DEBUG] ==========================");
    }

    public void logDetailedCommandInfo(String sender, CommandBlockerUltra.CommandInfo commandInfo, PermissionChecker.PermissionResult permissionResult) {
        logger.info("[DEBUG] === Детальная информация о команде ===");
        logger.info("[DEBUG] Отправитель: " + sender);
        logger.info("[DEBUG] Полная команда: " + commandInfo.getFullCommand());
        logger.info("[DEBUG] Основная команда: " + commandInfo.getMainCommand());
        logger.info("[DEBUG] Субкоманда: " + (commandInfo.hasSubCommand() ? commandInfo.getSubCommand() : "НЕТ"));
        logger.info("[DEBUG] Аргументы: " + Arrays.toString(commandInfo.getArguments()));
        logger.info("[DEBUG] Аргументы субкоманды: " + Arrays.toString(commandInfo.getSubCommandArguments()));
        logger.info("[DEBUG] Статус прав: " + (permissionResult.allowed ? "РАЗРЕШЕНО" : "ЗАБЛОКИРОВАНО"));
        logger.info("[DEBUG] Сообщение о правах: " + permissionResult.message);
        logger.info("[DEBUG] ======================================");
    }
} 