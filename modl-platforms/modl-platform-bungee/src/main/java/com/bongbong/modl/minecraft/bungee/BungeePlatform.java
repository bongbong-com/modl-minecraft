package com.bongbong.modl.minecraft.bungee;

import co.aikar.commands.BungeeCommandManager;
import co.aikar.commands.CommandManager;
import com.bongbong.modl.minecraft.core.Platform;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BungeePlatform implements Platform {
    private final BungeeCommandManager commandManager;

    @Override
    public CommandManager<?, ?, ?, ?, ?, ?> getCommandManager() {
        return commandManager;
    }
}
