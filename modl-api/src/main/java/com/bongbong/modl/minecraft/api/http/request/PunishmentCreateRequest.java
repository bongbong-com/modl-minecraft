package com.bongbong.modl.minecraft.api.http.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PunishmentCreateRequest {
    private final String targetUuid;
    private final String issuerName;
    private final String type;
    private final String reason;
    private final Long duration;
    private final Map<String, Object> data;
    private final List<String> notes;
    private final List<String> attachedTicketIds;
}