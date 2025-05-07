package com.bongbong.modl.minecraft.api;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public record IPAddress(
        boolean proxy,
        @NotNull String address,
        @NotNull String country,
        @NotNull String region,
        @NotNull String ASN,
        @NotNull Date firstLogin,
        @NotNull List<Date> logins
) {

    public Map<String, Object> export() {
        return Map.of(
                "proxy", proxy,
                "address", address,
                "country", country,
                "region", region,
                "ASN", ASN,
                "firstLogin", firstLogin.getTime(),
                "logins", logins.stream().map(Date::getTime).toList()
        );
    }

    @SuppressWarnings("unchecked")
    public static IPAddress fromMap(Map<String, Object> map) {
        return new IPAddress(
                (boolean) map.get("proxy"),
                (String) map.get("address"),
                (String) map.get("country"),
                (String) map.get("region"),
                (String) map.get("ASN"),
                new Date((long) map.get("firstLogin")),
                new ArrayList<>(((List<Long>) map.get("logins")).stream().map(Date::new).toList())
        );
    }
}
