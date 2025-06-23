package com.bongbong.modl.minecraft.api;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class Punishment {
    @NotNull
    @SerializedName("id")
    private final String id;

    @NotNull
    @SerializedName("issuerName")
    private final String issuerName;

    @NotNull
    @SerializedName("issued")
    private final Date issued;

    @NotNull
    @SerializedName("started")
    private final Date started;

    @SerializedName("type")
    private final Type type;

    @NotNull
    @SerializedName("modifications")
    private final List<Modification> modifications;

    @NotNull
    @SerializedName("notes")
    private final List<Note> notes;

    @NotNull
    @SerializedName("attachedTicketIds")
    private final List<String> attachedTicketIds;

    @NotNull
    @SerializedName("data")
    private final Map<String, Object> data;

    @RequiredArgsConstructor
    public enum Type {
        BAN,
        MUTE
    }
}
