package com.mishkaworld.cbu;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogManager {
    private final FileConfiguration config;
    private final Plugin plugin;
    
    public LogManager(FileConfiguration config, Plugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }
    
    public void log(String message) {
        if (!config.getBoolean("debug-mode", false)) return;
        
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logMessage = "[" + timestamp + "] " + message;
        
        // Всегда записываем в файл
        try (PrintWriter writer = new PrintWriter(new FileWriter("plugins/CommandBlockerUltra/debug.log", true))) {
            writer.println(logMessage);
        } catch (IOException e) {
            plugin.getLogger().warning("[LogManager] Ошибка записи в файл: " + e.getMessage());
        }
        
        // Если file-mode: false, то также выводим в терминал через логгер плагина
        if (!config.getBoolean("file-mode", false)) {
            plugin.getLogger().info(logMessage);
        }
        // Если file-mode: true, то НЕ выводим в терминал, только в файл
    }
    
    public void logError(String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logMessage = "[" + timestamp + "] [ERROR] " + message;
        
        // Ошибки всегда записываем в файл
        try (PrintWriter writer = new PrintWriter(new FileWriter("plugins/CommandBlockerUltra/debug.log", true))) {
            writer.println(logMessage);
        } catch (IOException e) {
            plugin.getLogger().warning("[LogManager] Ошибка записи в файл: " + e.getMessage());
        }
        
        // Ошибки всегда выводим в терминал
        plugin.getLogger().warning(logMessage);
    }
} 