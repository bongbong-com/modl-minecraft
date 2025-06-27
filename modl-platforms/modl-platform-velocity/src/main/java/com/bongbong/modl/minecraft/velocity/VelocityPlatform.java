package com.bongbong.modl.minecraft.velocity;

import co.aikar.commands.CommandManager;
import co.aikar.commands.VelocityCommandManager;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.core.HttpManager;
import com.bongbong.modl.minecraft.core.Platform;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class VelocityPlatform implements Platform {
    private final HttpManager httpManager;
    private final VelocityCommandManager commandManager;
    private final Logger logger = LoggerFactory.getLogger(VelocityPlatform.class);

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
    
    public Logger getLogger() {
        return logger;
    }
}
