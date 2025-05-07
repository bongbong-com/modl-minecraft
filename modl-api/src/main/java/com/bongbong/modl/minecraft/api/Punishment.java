package com.bongbong.modl.minecraft.api;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


public record Punishment(
        @NotNull String id,
        @NotNull String issuer,
        @NotNull Date issued,
        @NotNull AtomicReference<Date> started,
        @NotNull Type type,
        @NotNull List<Modification> modifications,
        @NotNull List<Note> notes,
        @NotNull List<Report> reports,
        @NotNull PunishmentData data
) {

    @RequiredArgsConstructor
    public enum Type {
        MANUAL_BAN("Manual Ban", "Banish someone from the server.", PointCategory.ADMINISTRATIVE, "&c&lThe ban hammer has struck {player}!", 0),

        MANUAL_MUTE("Manual Mute", "Stop someone from chatting.", PointCategory.ADMINISTRATIVE, "&c&l{player} has been silenced.", 0),

        MANUAL_BLACKLIST("Manual Blacklist", "Blacklist a bad actor.", PointCategory.ADMINISTRATIVE, "&c&l{player} has been placed on the blacklist.", 0),

        MANUAL_KICK("Manual Kick", "Disconnect a player.",PointCategory.ADMINISTRATIVE, "&c&l{player} has been booted.", 0),

        CHAT_ABUSE_MUTE("&aChat Abuse (mute)", "&7Spam, Low-quality, Advertising, Non-English, etc", PointCategory.SOCIAL,
                "&c&l{player} has been muted for violating public chat decorum. Remember to refrain from posting low-quality messages.", 1),

        ANTI_SOCIAL_MUTE("&6Anti-Social (mute)", "&7Bigotry, Rudeness, Obscenity, NSFW, Negative Content", PointCategory.SOCIAL,
                "&c&l{player} has been muted for anti-social behavior in chat. Please be respectful and appropriate in public chat.",3),

        TARGETING_BAN("&cTargeting (ban)", "&7Torment, Threats, Harassment, DDoS/DoX", PointCategory.SOCIAL,
                "&c&l{player} has been banned for targeting other players. This server has a zero tolerance policy on abusive behavior.",5),

        INAPPROPRIATE_SKIN_RESTRICTION("&eInappropriate Skin (restriction)", "&7Ban an account until the skin is changed", PointCategory.SOCIAL,
                "&c&l{player} has been banned for wearing an inappropriate skin. Obscene/Insensitive skins are against the rules!",2),

        INAPPROPRIATE_NAME_RESTRICTION("&eInappropriate Name (restriction)", "&7Ban an account until the username is changed", PointCategory.SOCIAL,
                "&c&l{player} has been banned for having an inappropriate username. Obscene/Insensitive usernames are against the rules!",2),

        INAPPROPRIATE_CONTENT_BAN("&6Inappropriate Content (ban)", "&7Obscene/Hateful Content in Builds, Books, Signs, etc", PointCategory.SOCIAL,
                "&c&l{player} has been banned for creating inappropriate content. Creating hateful or obscene content in-game is strictly prohibited!",2),

        TEAM_ABUSE_BAN("&aTeam Abuse (ban)", "&7Sabotage, Griefing, Cross-teaming, Aiding Cheater", PointCategory.GAMEPLAY,
                "&c&l{player} has been banned for team abuse (sabotaging / griefing / cross-teaming / aiding cheater)!",3),

        GAME_ABUSE_BAN("&6Game Abuse (ban)", "&7Breaking game specific rules for fair play", PointCategory.GAMEPLAY,
                "&c&l{player} has been banned for game abuse (violating game specific rules intended to ensure fair play).",1),

        CHEATING_BAN("&cCheating (ban)", "&7Hacked client, exploiting/abusing bugs, macros, etc", PointCategory.GAMEPLAY,
                "&c&l{player} has been banned for cheating through the use of client modifications or exploits.",5),

        GAME_TRADING_BAN("&6Game Trading (ban)", "&7Unauthorized trading of items, services, or currency", PointCategory.GAMEPLAY,
                "&c&l{player} has been banned for unauthorized trading of items, services, and/or currency.",4),

        ACCOUNT_ABUSE_BAN("&cAccount Abuse (ban)", "&7Account sharing, boosting (alt-accounts), trading/selling accounts", PointCategory.GAMEPLAY,
                "&c&l{player} has been banned for account abuse. Misusing multiple accounts for monetary or boosting purposes is prohibited. ", 4),

        SECURITY_RESTRICTION("&4Security (restriction)", "&7Compromised account or suspicious activity", PointCategory.ADMINISTRATIVE,
                "&c&l{player} has been banned for security reasons.", 0),

        LINKED_RESTRICTION("&4Linked Account (restriction)", "&7Account Abuse (sharing/alting)", PointCategory.ADMINISTRATIVE,
                "&c&l{player} has been banned for account abuse.",0),;

        public final String label;
        public final String description;
        public final PointCategory pointCategory;
        public final String warning;
        public final int points;
    }

    @RequiredArgsConstructor
    public enum Level {
        LOW("&aLow"),
        MEDIUM("&6Medium"),
        HABITUAL("&cHabitual");

        public final String colored;
    }

    public enum Leniency {
        LENIENT, NORMAL, AGGRAVATED
    }

    public enum PointCategory {
        SOCIAL, GAMEPLAY, ADMINISTRATIVE
    }

    public Map<String, Object> export() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("issuer", issuer);
        map.put("issued", issued.getTime());
        if (started.get() != null) map.put("started", started.get().getTime());
        map.put("modifications", modifications.stream().map(Modification::export).toList());
        map.put("notes", notes.stream().map(Note::export).toList());
        map.put("reports", reports.stream().map(Report::export).toList());
        map.put("type", type.name());
        map.put("data", data.export());

        return map;
    }

    @SuppressWarnings("unchecked")
    public static Punishment fromMap(Map<String, Object> map) {
        return new Punishment(
                (String) map.get("id"),
                (String) map.get("issuer"),
                new Date((long) map.get("issued")),
                map.containsKey("started") ? new AtomicReference<>(new Date((long) map.get("started"))) : new AtomicReference<>(null),
                Type.valueOf((String) map.get("type")),
                new ArrayList<>(((List<Map<String, Object>>) map.get("modifications")).stream().map(Modification::fromMap).toList()),
                new ArrayList<>(((List<Map<String, Object>>) map.get("notes")).stream().map(Note::fromMap).toList()),
                new ArrayList<>(((List<Map<String, Object>>) map.get("reports")).stream().map(Report::fromMap).toList()),
                PunishmentData.fromMap((Map<String, Object>) map.get("data"))
        );
    }
}
