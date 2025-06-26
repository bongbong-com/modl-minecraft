package com.bongbong.modl.minecraft.core.util;

import dev.simplix.protocolize.api.chat.ChatElement;

public class Colors {
    
    /**
     * Converts a string with & color codes to a ChatElement
     * @param string The string with & color codes
     * @return ChatElement with translated color codes
     */
    public static ChatElement<?> of(String string) {
        return ChatElement.ofLegacyText(translate(string));
    }
    
    /**
     * Translates & color codes to § color codes
     * @param string The string to translate
     * @return String with translated color codes
     */
    public static String translate(String string) {
        return string.replace('&', '\u00a7');
    }
}