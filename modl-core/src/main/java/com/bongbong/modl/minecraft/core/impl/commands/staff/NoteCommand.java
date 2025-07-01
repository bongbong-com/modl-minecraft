//package com.bongbong.modl.minecraft.core.impl.commands.staff;
//
//import co.aikar.commands.BaseCommand;
//import co.aikar.commands.CommandIssuer;
//import co.aikar.commands.annotation.CommandAlias;
//import co.aikar.commands.annotation.CommandCompletion;
//import co.aikar.commands.annotation.CommandPermission;
//import co.aikar.commands.annotation.Syntax;
//import com.bongbong.modl.minecraft.api.Account;
//import com.bongbong.modl.minecraft.api.Note;
//import com.bongbong.modl.minecraft.api.Punishment;
//import com.bongbong.modl.minecraft.core.Constants;
//import com.bongbong.modl.minecraft.core.procedure.Database;
//import com.bongbong.modl.minecraft.core.procedure.Platform;
//import com.bongbong.modl.minecraft.core.util.Colors;
//import com.bongbong.modl.minecraft.core.util.DateFormatter;
//import lombok.RequiredArgsConstructor;
//
//import java.util.Arrays;
//import java.util.Date;
//
//@RequiredArgsConstructor
//@CommandAlias("note")
//public class NoteCommand extends BaseCommand {
//    private final Database database;
//    private final Platform platform;
//
//    @CommandCompletion("@players")
//    @CommandAlias("list")
//    @Syntax("<target/#id>")
//    @CommandPermission(Constants.PERMISSION)
//    public void list(CommandIssuer sender, String arguments) {
//        if (arguments == null) {
//            sender.sendMessage(Colors.translate("&cPlease specify a target or ID."));
//            return;
//        }
//
//        if (arguments.startsWith("#")) {
//            sender.sendMessage(Colors.translate("&7Querying database using punishment ID..."));
//
//            String id = arguments.replace('#', ' ').trim();
//            Account account = database.fetchAccountFromPunishmentId(id);
//            if (account == null) {
//                sender.sendMessage(Colors.translate("&cNo punishment with that ID found."));
//                return;
//            }
//
//            Punishment punishment = account.punishments().stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
//            assert punishment != null;
//            sender.sendMessage(Colors.translate("&aNotes for punishment #" + punishment.id() + " :"));
//            for (Note note : punishment.notes()) {
//                sender.sendMessage(Colors.translate("&7" + note.issuer() + " (" + DateFormatter.format(note.issued()) + "): &f" + note.note()));
//            }
//        }
//
//        sender.sendMessage(Colors.translate("&7Querying database for account username..."));
//        Account account = database.fetchAccount(arguments);
//        if (account == null) {
//            sender.sendMessage(Colors.translate("&cNo account or punishment found."));
//            return;
//        }
//
//        sender.sendMessage(Colors.translate("&aNotes for " + account.username() + " :"));
//        for (Note note : account.notes()) {
//            sender.sendMessage(Colors.translate("&7" + note.issuer() + " (" + DateFormatter.format(note.issued()) + "): &f" + note.note()));
//        }
//    }
//
//    @CommandCompletion("@players")
//    @CommandAlias("add")
//    @Syntax("<target/#id> <note>")
//    @CommandPermission(Constants.PERMISSION)
//    public void add(CommandIssuer sender, String[] args) {
//        if (args == null || args.length < 2) {
//            sender.sendMessage(Colors.translate("&cPlease specify a target/ID and a note to add."));
//            return;
//        }
//
//        String note = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
//
//        if (args[0].startsWith("#")) {
//            sender.sendMessage(Colors.translate("&7Querying database using punishment ID..."));
//
//            String id = args[0].replace('#', ' ').trim();
//            Account account = database.fetchAccountFromPunishmentId(id);
//            if (account == null) {
//                sender.sendMessage(Colors.translate("&cNo punishment with that ID found."));
//                return;
//            }
//
//            Punishment punishment = account.punishments().stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
//            assert punishment != null;
//            punishment.notes().add(new Note(new Date(),
//                    platform.getAbstractPlayer(sender.getUniqueId(), false).username(), note));
//
//            database.saveAccount(account);
//            sender.sendMessage(Colors.translate("&aNote added to punishment #" + punishment.id() + "."));
//        }
//
//        sender.sendMessage(Colors.translate("&7Querying database for account username..."));
//        Account account = database.fetchAccount(args[0]);
//        if (account == null) {
//            sender.sendMessage(Colors.translate("&cNo account or punishment found."));
//            return;
//        }
//
//        account.notes().add(new Note(new Date(),
//                platform.getAbstractPlayer(sender.getUniqueId(), false).username(), note));
//
//        database.saveAccount(account);
//        sender.sendMessage(Colors.translate("&aNote added to " + account.username() + "."));
//    }
//}