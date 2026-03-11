# 🧭 AuthCompass

**AuthCompass** — это плагин для Minecraft (Paper 1.20+), который работает совместно с **AuthMe** и **VelocityConnect**.  
После успешной авторизации через AuthMe плагин выдаёт игроку **компас**, который при нажатии подключает его к серверу **survival**.

## ⚙️ Зависимости

Для корректной работы необходимы следующие плагины:
- [AuthMe Reloaded](https://www.spigotmc.org/resources/authme-reloaded.6269/)
- И настроенный [VelocityConnect](https://github.com/flyawaymaking/VelocityConnect) на вашем Velocity-сервере

## 🚀 Возможности

- ✅ Автоматическая выдача компаса после авторизации
- 🔒 Проверка авторизации через API AuthMe
- ⏱️ Защита от спама нажатием (2 секунды кулдаун)
- 🧰 Защита от выбрасывания и перемещения компаса
- 🌍 Подключение к серверу `survival` через Velocity
- 📢 Уведомление игроков о доступности сервера Survival после перезагрузки
- 🔁 Автоматическое подключение игроков с правом `rejoin.reloaded`

## 📦 Установка

1. Скачайте **последний релиз** из раздела [Releases](../../releases).
2. Поместите `.jar` файл в папку `plugins` вашего **Auth-сервера (Paper)**.
3. Убедитесь, что на сервере установлен **AuthMe Reloaded**.
4. Убедитесь, что на Velocity-сервере установлен **VelocityConnect**.
5. Перезапустите сервер.

## 🪄 Использование

1. Игрок заходит на сервер и проходит авторизацию через AuthMe.
2. После входа ему автоматически выдаётся компас.
3. При нажатии на компас игрок будет перенаправлен на сервер `survival`.

После перезагрузки сервера **survival**:

- Все игроки на Auth-сервере получают уведомление, что сервер снова доступен.
- Игроки с правом `rejoin.reloaded` автоматически подключаются к серверу `survival`.

> 💡 Компас нельзя выбросить или переместить — он защищён до момента переключения сервера.

## 🔔 Отправка уведомления после перезагрузки Survival

Чтобы Auth-сервер узнал, что сервер `survival` снова доступен, можно отправить **PluginMessage** после полной загрузки сервера.

Пример простого плагина для **survival сервера**:

```java
package com.example.survivalnotify;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SurvivalNotifyPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "authcompass:reload");
    }

    @org.bukkit.event.EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
            if (player == null) return;

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Reloaded");

            player.sendPluginMessage(this, "authcompass:reload", out.toByteArray());
        }, 40L);
    }
}
````

Этот код:

1. Ждёт полной загрузки сервера (`ServerLoadEvent`)
2. Через 2 секунды отправляет **PluginMessage**
3. AuthCompass на auth сервере:

    * уведомляет игроков
    * автоматически переподключает игроков с правом `rejoin.reloaded`

## ⚠️ Замечания

* Плагин работает только при корректно настроенном **Velocity** с плагином **VelocityConnect**.
* Название целевого сервера (`survival`) можно изменить в коде (в методе `sendToServer`).
* Автоподключение выполняется только для игроков с правом `rejoin.reloaded`.

## 📝 Лицензия

Этот проект распространяется под лицензией **MIT**.
Свободно используйте и модифицируйте плагин при сохранении указания автора.
