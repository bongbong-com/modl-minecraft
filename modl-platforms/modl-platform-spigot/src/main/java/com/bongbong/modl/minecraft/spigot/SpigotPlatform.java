package com.bongbong.modl.minecraft.spigot;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.CommandManager;
import com.bongbong.modl.minecraft.core.Platform;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SpigotPlatform implements Platform {
    private final BukkitCommandManager commandManager;

    @Override
    public CommandManager<?, ?, ?, ?, ?, ?> getCommandManager() {
        return commandManager;
    }
}
