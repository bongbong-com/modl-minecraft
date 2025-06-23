package com.bongbong.modl.minecraft.api.http.response;

import com.bongbong.modl.minecraft.api.Account;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
public class LinkedAccountsResponse {
    private int status;
    @NotNull
    private List<Account> linkedAccounts;
}