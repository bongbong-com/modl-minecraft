package com.bongbong.modl.minecraft.core;

import co.aikar.commands.CommandManager;
import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import org.jetbrains.annotations.NotNull;

public interface Platform {
    @NotNull
    ModlHttpClient getHttpClient();

    @NotNull
    CommandManager<?,?,?,?,?,?> getCommandManager();
}
