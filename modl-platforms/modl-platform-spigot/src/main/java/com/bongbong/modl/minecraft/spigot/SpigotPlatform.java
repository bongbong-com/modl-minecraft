package com.bongbong.modl.minecraft.spigot;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.CommandManager;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.core.HttpManager;
import com.bongbong.modl.minecraft.core.Platform;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class SpigotPlatform implements Platform {
    private final HttpManager httpManager;
    private final BukkitCommandManager commandManager;

    @NotNull
    @Override
    public ModlHttpClient getHttpClient() {
        return httpManager.getHttpClient();
    }

    @NotNull
    @Override
    public CommandManager<?, ?, ?, ?, ?, ?> getCommandManager() {
        return commandManager;
    }
}
