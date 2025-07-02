package com.bongbong.modl.minecraft.velocity;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Colors {
    public static Component get(String string) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(string);
    }
}