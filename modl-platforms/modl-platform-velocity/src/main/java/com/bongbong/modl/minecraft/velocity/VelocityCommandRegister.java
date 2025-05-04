package com.bongbong.modl.minecraft.velocity;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.VelocityCommandManager;
import com.bongbong.modl.minecraft.core.PlatformCommandRegister;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class VelocityCommandRegister implements PlatformCommandRegister {
    private final VelocityCommandManager commandManager;

    @Override
    public void register(BaseCommand command) {
        commandManager.registerCommand(command);
    }
}
