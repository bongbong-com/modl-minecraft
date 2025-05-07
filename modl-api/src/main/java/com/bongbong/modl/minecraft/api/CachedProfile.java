package com.bongbong.modl.minecraft.api;

import java.util.List;

public record CachedProfile(
        Punishment mute,
        List<String> chatMessages) {
}
