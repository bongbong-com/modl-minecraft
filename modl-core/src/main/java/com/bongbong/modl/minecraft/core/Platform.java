package com.bongbong.modl.minecraft.core;

import co.aikar.commands.CommandManager;

public interface Platform {
    CommandManager<?,?,?,?,?,?> getCommandManager();
}
