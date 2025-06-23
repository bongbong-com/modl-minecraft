package com.bongbong.modl.minecraft.velocity;

import co.aikar.commands.CommandManager;
import co.aikar.commands.VelocityCommandManager;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.core.HttpManager;
import com.bongbong.modl.minecraft.core.Platform;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class VelocityPlatform implements Platform {
    private final HttpManager httpManager;
    private final VelocityCommandManager commandManager;

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
