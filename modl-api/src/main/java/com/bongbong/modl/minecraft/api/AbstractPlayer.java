package com.bongbong.modl.minecraft.api;

import java.util.UUID;

public record AbstractPlayer(UUID uuid, String username, boolean online) {
}
