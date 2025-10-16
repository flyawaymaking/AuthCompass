package com.flyaway.authcompass;

import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.LogoutEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.common.io.ByteStreams;
import com.google.common.io.ByteArrayDataOutput;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AuthCompassPlugin extends JavaPlugin implements Listener {

    private final Set<UUID> cooldownPlayers = new HashSet<>();
    private static final long COOLDOWN_MS = 2000; // 2 секунды cooldown

    @Override
    public void onEnable() {
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "velocity:player");
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("AuthCompass включен!");
    }

    @Override
    public void onDisable() {
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        cooldownPlayers.clear();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getInventory().clear();
    }

    // Выдача компаса после логина
    @EventHandler
    public void onPlayerLogin(LoginEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            giveAuthCompass(player);
        }, 10L); // Уменьшена задержка
    }

    @EventHandler
    public void onPlayerLogout(LogoutEvent event) {
        Player player = event.getPlayer();
        player.getInventory().clear();
        cooldownPlayers.remove(player.getUniqueId());
    }

    private void giveAuthCompass(Player player) {
        // Проверяем, нет ли уже компаса у игрока
        if (hasAuthCompass(player)) {
            return;
        }

        ItemStack compass = createCompass();
        player.getInventory().addItem(compass);
        player.sendMessage(ChatColor.GREEN + "Вам выдан компас для перехода на сервер выживания!");
    }

    private ItemStack createCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Выбор сервера");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Нажмите, чтобы перейти",
            ChatColor.GRAY + "на сервер выживания"
        ));
        compass.setItemMeta(meta);
        return compass;
    }

    private boolean hasAuthCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isAuthCompass(item)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isAuthCompass(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Нельзя выбросить этот предмет!");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() != null && isAuthCompass(event.getCurrentItem())) {
            event.setCancelled(true);
        }
    }

    private boolean isAuthCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() &&
               meta.getDisplayName().equals(ChatColor.AQUA + "Выбор сервера");
    }

    @EventHandler
    public void onCompassClick(PlayerInteractEvent event) {
        if (event.getItem() == null || !isAuthCompass(event.getItem())) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);

        // Проверка авторизации
        if (!AuthMeApi.getInstance().isAuthenticated(player)) {
            player.sendMessage(ChatColor.RED + "Сначала авторизуйтесь!");
            return;
        }

        // Проверка коoldown'а
        UUID playerId = player.getUniqueId();
        if (cooldownPlayers.contains(playerId)) {
            player.sendMessage(ChatColor.YELLOW + "Подождите перед повторным использованием!");
            return;
        }

        // Устанавливаем cooldown
        cooldownPlayers.add(playerId);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            cooldownPlayers.remove(playerId);
        }, COOLDOWN_MS / 50); // Конвертация миллисекунд в тики

        player.sendMessage(ChatColor.YELLOW + "Подключение к серверу выживания...");

        // Отправка в отдельном потоке для избежания лагов
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            // player.performCommand("server survival"); НЕ РАБОТАЕТ
            sendToServer(player, "survival");
        });
    }

    private void sendToServer(Player player, String serverName) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            byte[] data = out.toByteArray();

            // Двойная проверка, что игрок онлайн
            if (player.isOnline()) {
                player.sendPluginMessage(this, "velocity:player", data);
                getLogger().info("Отправлен запрос на подключение для " + player.getName() + " к серверу " + serverName);
            }
        } catch (Exception e) {
            getLogger().warning("Ошибка при отправке PluginMessage для " + player.getName() + ": " + e.getMessage());

            // Сообщение игроку в основном потоке
            Bukkit.getScheduler().runTask(this, () -> {
                if (player.isOnline()) {
                    player.sendMessage(ChatColor.RED + "Ошибка при подключении к серверу!");
                }
            });
        }
    }
}
