package com.bongbong.modl.minecraft.api.http.response;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
public class LinkedAccountsResponse {
    private int status;
    @NotNull
    private List<LinkedAccount> linkedAccounts;

    @Data
    public static class LinkedAccount {
        @NotNull
        private String minecraftUuid;
        @NotNull
        private String username;
        private int activeBans;
        private int activeMutes;
    }
}