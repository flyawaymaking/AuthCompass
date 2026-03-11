package com.flyaway.authcompass;

import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class SurvivalNotifierServer {
    private final AuthCompassPlugin plugin;
    private ServerSocket serverSocket;
    private boolean running = false;

    public SurvivalNotifierServer(AuthCompassPlugin plugin) {
        this.plugin = plugin;
    }

    public void start(int port) {
        running = true;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                serverSocket = new ServerSocket(port);
                plugin.getLogger().info("SurvivalNotifierServer запущен на порту " + port);

                while (running) {
                    Socket client = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String message = reader.readLine();
                    client.close();

                    if ("reloaded".equalsIgnoreCase(message)) {
                        Bukkit.getScheduler().runTask(plugin, plugin::onSurvivalReloaded);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка SurvivalNotifierServer: " + e.getMessage());
            }
        });
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {
        }
    }
}
