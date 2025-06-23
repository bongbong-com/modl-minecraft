package com.bongbong.modl.minecraft.api.http.response;

import com.bongbong.modl.minecraft.api.Account;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class PlayerProfileResponse {
    private int status;
    @NotNull
    private Account player;
}