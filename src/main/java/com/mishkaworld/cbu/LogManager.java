package com.mishkaworld.cbu;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

public class LogManager {
    private final FileConfiguration config;
    private final Plugin plugin;
    private final File logFile;
    private final ReentrantLock writeLock = new ReentrantLock();
    private final ThreadLocal<SimpleDateFormat> dateFormat = ThreadLocal.withInitial(
        () -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    );
    
    public LogManager(FileConfiguration config, Plugin plugin) {
        this.config = config;
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "debug.log");
        
        // Создаем директорию если не существует
        if (!logFile.getParentFile().exists()) {
            logFile.getParentFile().mkdirs();
        }
    }
    
    public void log(String message) {
        if (!config.getBoolean("debug-mode", false)) return;
        
        String timestamp = dateFormat.get().format(new Date());
        String logMessage = "[" + timestamp + "] " + message;
        
        writeToFile(logMessage);
        
        // Если file-mode: false, то также выводим в терминал
        if (!config.getBoolean("file-mode", false)) {
            plugin.getLogger().info(logMessage);
        }
    }
    
    public void logError(String message) {
        String timestamp = dateFormat.get().format(new Date());
        String logMessage = "[" + timestamp + "] [ERROR] " + message;
        
        writeToFile(logMessage);
        plugin.getLogger().warning(logMessage);
    }
    
    private void writeToFile(String logMessage) {
        writeLock.lock();
        try (PrintWriter writer = new PrintWriter(
                new FileWriter(logFile, StandardCharsets.UTF_8, true))) {
            writer.println(logMessage);
        } catch (IOException e) {
            plugin.getLogger().warning("[LogManager] Ошибка записи в файл: " + e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }
} 