package com.bongbong.modl.minecraft.spigot;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.BukkitCommandManager;
import com.bongbong.modl.minecraft.core.PlatformCommandRegister;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SpigotCommandRegister implements PlatformCommandRegister {
    private final BukkitCommandManager commandManager;

    @Override
    public void register(BaseCommand command) {
        commandManager.registerCommand(command);
    }
}
