package com.bongbong.modl.minecraft.core;

import com.bongbong.modl.minecraft.api.http.ModlHttpClient;
import com.bongbong.modl.minecraft.core.impl.http.ModlHttpClientImpl;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class HttpManager {
    @NotNull
    private final ModlHttpClient httpClient;

    public HttpManager(@NotNull String key, @NotNull String url) {
        this.httpClient = new ModlHttpClientImpl(url, key, Constants.DEBUG_HTTP);
    }
}