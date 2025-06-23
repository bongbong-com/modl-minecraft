package com.bongbong.modl.minecraft.api;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class Account {
    @NotNull
    @SerializedName("minecraftUuid")
    private final UUID minecraftUuid;

    @NotNull
    @SerializedName("username")
    private final String username;

    @SerializedName("activeBans")
    private final int activeBans;

    @SerializedName("activeMutes")
    private final int activeMutes;
}
