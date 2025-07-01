//package com.bongbong.modl.minecraft.core.impl.commands.punishments;
//
//import co.aikar.commands.BaseCommand;
//import co.aikar.commands.CommandIssuer;
//import co.aikar.commands.annotation.*;
//import com.bongbong.modl.minecraft.api.Account;
//import com.bongbong.modl.minecraft.api.CachedProfile;
//import com.bongbong.modl.minecraft.api.Punishment;
//import com.bongbong.modl.minecraft.api.PunishmentData;
//import com.bongbong.modl.minecraft.core.Constants;
//import com.bongbong.modl.minecraft.core.impl.cache.Cache;
//import com.bongbong.modl.minecraft.core.procedure.ArgumentChecker;
//import com.bongbong.modl.minecraft.core.procedure.Database;
//import com.bongbong.modl.minecraft.core.procedure.Platform;
//import com.bongbong.modl.minecraft.core.procedure.PunishmentHelper;
//import com.bongbong.modl.minecraft.core.util.Colors;
//import com.bongbong.modl.minecraft.core.util.TimeUtil;
//import lombok.RequiredArgsConstructor;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//import java.util.concurrent.atomic.AtomicReference;
//
//@RequiredArgsConstructor
//public class ManualCommands extends BaseCommand {
//    private final Database database;
//    private final Platform platform;
//    private final Cache cache;
//
//    @CommandCompletion("@players")
//    @CommandAlias("mute")
//    @Syntax("<target> [time] [reason] [-s]")
//    @CommandPermission(Constants.PERMISSION)
//    public void mute(CommandIssuer sender, @Name("target") Account target,
//                     @Default("Unspecified") String args) {
//        if (target == null) {
//            sender.sendMessage(Colors.translate("&cPlayer has never joined the server."));
//            return;
//        }
//
//        punish(Punishment.Type.MANUAL_MUTE, sender, target, args);
//    }
//
//    @CommandCompletion("@players")
//    @CommandAlias("ban")
//    @Syntax("<target> [time] [reason] [-s] [-ab] [-sw]")
//    @CommandPermission(Constants.PERMISSION)
//    public void ban(CommandIssuer sender, @Name("target") Account target,
//                    @Default("Unspecified") String args) {
//        if (target == null) {
//            sender.sendMessage(Colors.translate("&cPlayer has never joined the server."));
//            return;
//        }
//
//        punish(Punishment.Type.MANUAL_BAN, sender, target, args);
//    }
//
//    @CommandCompletion("@players")
//    @CommandAlias("blacklist")
//    @Syntax("<target> [reason] [-s] [-ab] [-w]")
//    @CommandPermission(Constants.PERMISSION)
//    public void blacklist(CommandIssuer sender, @Name("target") Account target,
//                          @Default("Unspecified") String args) {
//        if (target == null) {
//            sender.sendMessage(Colors.translate("&cPlayer has never joined the server."));
//            return;
//        }
//
//        punish(Punishment.Type.MANUAL_BLACKLIST, sender, target, args);
//    }
//
//    @CommandCompletion("@players")
//    @CommandAlias("kick")
//    @Syntax("<target> [reason] [-s]")
//    @CommandPermission(Constants.PERMISSION)
//    public void kick(CommandIssuer sender, @Name("target") Account target,
//                     @Default("Unspecified") String args) {
//        if (target == null) {
//            sender.sendMessage(Colors.translate("&cPlayer has never joined the server."));
//            return;
//        }
//
//        punish(Punishment.Type.MANUAL_KICK, sender, target, args);
//    }
//
//    private void punish(Punishment.Type type, CommandIssuer sender, Account target, String arguments) {
//        UUID issuer = null;
//        if (sender.isPlayer()) issuer = sender.getUniqueId();
//        String issuerName = (issuer == null) ? "Console" : platform.getAbstractPlayer(issuer, false).username();
//
//        boolean online = platform.isOnline(target.uuid());
//        boolean start = (online && !PunishmentHelper.isCurrentlyPunished(type, target));
//
//        long duration = ArgumentChecker.getDuration(arguments);
//        boolean permanent = duration == -1L;
//        String reason = ArgumentChecker.getReason(arguments);
//        boolean silent = ArgumentChecker.isSilent(arguments);
//
//        boolean chatLogging = (type == Punishment.Type.MANUAL_MUTE) && ArgumentChecker.isChatLogging(arguments);
//        boolean altBlocking = (type == Punishment.Type.MANUAL_BAN || type == Punishment.Type.MANUAL_BLACKLIST) && ArgumentChecker.isAltBlocking(arguments);
//        boolean statWiping = (type == Punishment.Type.MANUAL_BAN || type == Punishment.Type.MANUAL_BLACKLIST) && ArgumentChecker.isStatWiping(arguments);
//
//        List<String> chatMessages = new ArrayList<>();
//        if (chatLogging) {
//            CachedProfile cachedProfile = cache.get(target.uuid());
//            if (cachedProfile != null && cachedProfile.chatMessages() != null)
//                chatMessages.addAll(cachedProfile.chatMessages());
//        }
//
//        PunishmentData data = new PunishmentData(null, null, null, new AtomicReference<>(null),
//                chatLogging ? chatMessages : null, duration, altBlocking, statWiping);
//
//        Punishment punishment = PunishmentHelper.createPunishment(issuerName, target.uuid(), start, type, reason, new ArrayList<>(), data);
//        target.punishments().add(punishment);
//
//        database.saveAccount(target);
//
//        if (start) {
//            if (type != Punishment.Type.MANUAL_MUTE)
//                platform.disconnect(target.uuid(), Colors.translate(String.join("\n", PunishmentHelper.getMessage(punishment, true))));
//            else {
//                platform.sendMessage(target.uuid(), Colors.translate(String.join("\n", PunishmentHelper.getMessage(punishment, true))));
//                if (cache.get(target.uuid()) != null) cache.remove(target.uuid());
//                cache.put(target.uuid(), new CachedProfile(punishment, new ArrayList<>()));
//            }
//        } else if (online)
//            platform.sendMessage(target.uuid(), Colors.translate(
//                    "&cYou have been issued a new punishment that will take effect after your current one lapses."));
//
//
//        sender.sendMessage(Colors.translate(
//                "&aYou successfully issued punishment &a&l#" + punishment.id() + "&a for &f" + target.username() + "&a ("
//                        + (permanent ? "permanent" : TimeUtil.formatTimeMillis(duration)) + ")."));
//
//        if (!silent) platform.broadcast(punishment.type().warning.replace("{player}", target.username()));
//
//
//        platform.staffBroadcast(Colors.translate(
//                        "&7&o[" + issuerName + ": created " + punishment.type().name().toLowerCase()
//                                + " #" + punishment.id() + " for " + target.username() + "&7&o]"));
//
//        // TODO: process banning linked accounts if alt-blocking
//    }
//}