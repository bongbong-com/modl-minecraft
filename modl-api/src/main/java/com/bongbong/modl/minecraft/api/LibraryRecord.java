package com.bongbong.modl.minecraft.api;

public record LibraryRecord (
        String groupID,
        String artifactID,
        String version,
        String id,
        String oldRelocation,
        String newRelocation
) {}