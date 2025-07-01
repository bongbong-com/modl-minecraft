//package com.bongbong.modl.minecraft.core.impl.commands.punishments;
//
//import co.aikar.commands.BaseCommand;
//import co.aikar.commands.CommandIssuer;
//import co.aikar.commands.annotation.CommandAlias;
//import co.aikar.commands.annotation.CommandCompletion;
//import co.aikar.commands.annotation.CommandPermission;
//import co.aikar.commands.annotation.Syntax;
//import com.bongbong.modl.minecraft.api.*;
//import com.bongbong.modl.minecraft.core.Constants;
//import com.bongbong.modl.minecraft.core.procedure.Database;
//import com.bongbong.modl.minecraft.core.procedure.PunishmentHelper;
//import com.bongbong.modl.minecraft.core.util.Colors;
//import com.bongbong.modl.minecraft.core.util.DateFormatter;
//import com.bongbong.modl.minecraft.core.util.TimeUtil;
//import lombok.RequiredArgsConstructor;
//
//import java.util.Date;
//import java.util.Optional;
//
//@RequiredArgsConstructor
//public class LookupCommand extends BaseCommand {
//    private final Database database;
//
//    @CommandCompletion("@players")
//    @CommandAlias("lookup")
//    @Syntax("<target/#id>")
//    @CommandPermission(Constants.PERMISSION)
//    public void lookup(CommandIssuer sender, String arguments) {
//        if (arguments.startsWith("#")) {
//            sender.sendMessage(Colors.translate("&7Querying database using punishment ID..."));
//
//            String id = arguments.replace('#', ' ').trim();
//            Account account = database.fetchAccountFromPunishmentId(id);
//            if (account != null) {
//                Punishment punishment = account.punishments().stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
//                assert punishment != null;
//
//                String expiry = PunishmentHelper.expiration(punishment).isPresent() ? DateFormatter.format(PunishmentHelper.expiration(punishment).get())
//                        : "Never";
//
//                Optional<Date> ogExpiration = PunishmentHelper.originalExpirationOnlyWhenNeeded(punishment);
//                if (ogExpiration.isPresent()) expiry = expiry + " (Original: " + DateFormatter.format(ogExpiration.get()) + ")";
//
//                sender.sendMessage(Colors.translate("&aPunishment #" + punishment.id() + " found!"));
//                sender.sendMessage(Colors.translate("&7Target: &f" + account.username() + " (" + account.uuid() + ")"));
//                sender.sendMessage(Colors.translate("&7Active: &f" + PunishmentHelper.isPunishmentActive(punishment)));
//                sender.sendMessage(Colors.translate("&7Started: &f" + (punishment.started().get() == null ? "False" : DateFormatter.format(punishment.started().get()))));
//                sender.sendMessage(Colors.translate("&7Issued: &f" + DateFormatter.format(punishment.issued())));
//                sender.sendMessage(Colors.translate("&7Type: &f" + punishment.type().name().toLowerCase()));
//                sender.sendMessage(Colors.translate("&7Issuer: &f" + punishment.issuer()));
//                sender.sendMessage(Colors.translate("&7Expiry: &f" + expiry));
//                sender.sendMessage(Colors.translate("&7Data: &f"));
//
//                if (punishment.data().duration() != -1L)
//                    sender.sendMessage(Colors.translate("&f > Duration: " + TimeUtil.formatTimeMillis(punishment.data().duration())));
//                else sender.sendMessage(Colors.translate("&f > Duration: Permanent"));
//
//                if (punishment.data().blockedName() != null) sender.sendMessage(Colors.translate("&f > Blocked Name: " + punishment.data().blockedName()));
//                if (punishment.data().linkedBanId() != null) sender.sendMessage(Colors.translate("&f > Linked ID: " + punishment.data().linkedBanId()));
//                if (punishment.data().chatLog() != null) {
//                    sender.sendMessage(Colors.translate("&f > Chat Logs:"));
//                    for (String log : punishment.data().chatLog()) sender.sendMessage("   - " + log);
//                }
//
//                if (punishment.data().wipeAfterExpiry()) sender.sendMessage(Colors.translate("&f > Wipe After Expiry: True"));
//                if (punishment.data().altBlocking()) sender.sendMessage(Colors.translate("&f > Alt-Blocking: True"));
//
//                sender.sendMessage(Colors.translate("&7Notes: "));
//                for (Note note : punishment.notes()) {
//                    sender.sendMessage(Colors.translate("&f > " + note.issued() + ": " + note.note()));
//                }
//
//                sender.sendMessage(Colors.translate("&7Modifications: "));
//                for (Modification modification : punishment.modifications()) {
//                    sender.sendMessage(Colors.translate("&f > " + DateFormatter.format(modification.issued()) + " " + modification.issuer() + ": " + modification.type() +
//                            ((modification.type() == Modification.Type.MANUAL_DURATION_CHANGE || modification.type() == Modification.Type.APPEAL_DURATION_CHANGE) ?
//                                    " (New Duration: " + TimeUtil.formatTimeMillis(modification.effectiveDuration()) + ")." : ".")));
//                }
//                return;
//            }
//        }
//
//        sender.sendMessage(Colors.translate("&7Querying database for account username..."));
//        Account account = database.fetchAccount(arguments);
//        if (account == null) {
//            sender.sendMessage(Colors.translate("&cNo account or punishment found (Don't forget # if querying punishment ID)."));
//            return;
//        }
//
//        sender.sendMessage(Colors.translate("&aAccount " + account.username() + " found!"));
//        sender.sendMessage(Colors.translate("&7UUID: &f" + account.uuid()));
//        sender.sendMessage(Colors.translate("&7Notes: "));
//        for (Note note : account.notes())
//            sender.sendMessage(Colors.translate("&f > " + note.issued() + ": " + note.note()));
//
//        sender.sendMessage(Colors.translate("&7Punishments: "));
//        for (Punishment punishment : account.punishments()) {
//            sender.sendMessage(Colors.translate("&f > #" + punishment.id() + ": " + punishment.type().name().toLowerCase() + " on "
//                    + DateFormatter.format(punishment.issued())));
//        }
//
//        sender.sendMessage(Colors.translate("&7IP Addresses: "));
//        for (IPAddress ipAddress : account.ipList()) {
//            sender.sendMessage(Colors.translate("&f > " + ipAddress.address() + ": ASN: " + ipAddress.ASN() + (ipAddress.proxy() ? " (proxy)" : "")
//                    + " | Location: " + ipAddress.region() + ", " + ipAddress.country() + " | Logins: &f" + ipAddress.logins().size() + " (First: "
//            + DateFormatter.format(ipAddress.firstLogin()) + " | Last: " + DateFormatter.format(ipAddress.logins().get(ipAddress.logins().size() - 1)) + ")"));
//        }
//
//    }
//}