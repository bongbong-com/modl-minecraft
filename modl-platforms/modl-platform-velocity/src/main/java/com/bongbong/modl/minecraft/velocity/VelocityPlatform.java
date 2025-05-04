package com.bongbong.modl.minecraft.velocity;

import co.aikar.commands.CommandManager;
import co.aikar.commands.VelocityCommandManager;
import com.bongbong.modl.minecraft.core.Platform;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class VelocityPlatform implements Platform {
    private final VelocityCommandManager commandManager;

    @Override
    public CommandManager<?, ?, ?, ?, ?, ?> getCommandManager() {
        return commandManager;
    }
}
