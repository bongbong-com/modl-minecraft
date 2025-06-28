package com.bongbong.modl.minecraft.core.impl.commands.punishments;


import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.*;
import com.bongbong.modl.minecraft.api.*;
import com.bongbong.modl.minecraft.core.Constants;
import com.bongbong.modl.minecraft.core.impl.cache.Cache;
import com.bongbong.modl.minecraft.core.procedure.ArgumentChecker;
import com.bongbong.modl.minecraft.core.procedure.Database;
import com.bongbong.modl.minecraft.core.procedure.Platform;
import com.bongbong.modl.minecraft.core.procedure.PunishmentHelper;
import com.bongbong.modl.minecraft.core.util.Colors;
import com.bongbong.modl.minecraft.core.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
@CommandAlias("punishment")
public class ModifyCommands extends BaseCommand {
    private final Database database;
    private final Platform platform;
    private final Cache cache;

    @HelpCommand
    public static void onHelp(CommandIssuer sender, CommandHelp help) {
        sender.sendMessage(Colors.translate("&7IMPORTANT: Pardons will disregard the points of the punishment and stop account-wipes from being applied." +
                " If you wish to keep the points and account-wipes, use the 'setduration' command instead."));
        help.showHelp();
    }

    @CommandAlias("addnote")
    @Syntax("<id> <note>")
    @Description("Add a note to a punishment (not removable).")
    @CommandPermission(Constants.PERMISSION)
    public void addNote(CommandIssuer sender, String[] args) {
        String senderName = senderName(sender);
        Account target = getAccount(sender, args);
        if (target == null) {
            sender.sendMessage(Colors.translate("&cPunishment ID doesn't exist."));
            return;
        }

        Punishment punishment = getPunishment(target, args);
        punishment.notes().add(new Note(new Date(), senderName, note(args, 2)));
        database.saveAccount(target);

        sender.sendMessage(Colors.translate("&aNote added to punishment #" + punishment.id()));
    }

    @CommandAlias("setduration")
    @Syntax("<id> <new duration> [reason]")
    @Description("Set the new duration of a punishment (from current time). Example duration: 7d2h3m19s. Non-conforming durations interpreted as permanent.")
    @CommandPermission(Constants.PERMISSION)
    public void setDuration(CommandIssuer sender, String[] args) {
        String senderName = senderName(sender);
        Account target = getAccount(sender, args);
        if (target == null) {
            sender.sendMessage(Colors.translate("&cPunishment ID doesn't exist."));
            return;
        }

        Punishment punishment = getPunishment(target, args);

        if (punishment.modifications().stream().anyMatch(m -> m.type().equals(Modification.Type.MANUAL_PARDON) || m.type().equals(Modification.Type.APPEAL_ACCEPT))) {
            sender.sendMessage(Colors.translate("&cYou cannot change the duration on a pardoned punishment."));
            return;
        }

        long newDuration = ArgumentChecker.getDuration(args[1]);
        boolean permanent = newDuration == -1L;

        punishment.modifications().add(new Modification(Modification.Type.MANUAL_DURATION_CHANGE,
                senderName, new Date(), newDuration));

        punishment.notes().add(new Note(new Date(), senderName, "Duration set to " + (permanent ? "permanent" : TimeUtil.formatTimeMillis(newDuration))));
        punishment.notes().add(new Note(new Date(), senderName, note(args, 3)));

        database.saveAccount(target);

        CachedProfile cachedProfile = cache.get(target.uuid());
        if (cachedProfile != null) {
            cache.remove(target.uuid());
            cache.put(target.uuid(), new CachedProfile(punishment, new ArrayList<>()));
        }

        sender.sendMessage(Colors.translate("&aSuccessfully set duration to " + (permanent ? "permanent" : TimeUtil.formatTimeMillis(newDuration))
                + " for punishment #" + punishment.id()));

        if (!PunishmentHelper.blockEligble(punishment.type())) return;

        List<Account> linkedAccounts = database.findLinkedAccounts(target.uuid(), target.ipList());
        for (Account linkedAccount : linkedAccounts) {
            for (Punishment linkedPunishment : linkedAccount.punishments()) {
                if (linkedPunishment.type().equals(Punishment.Type.LINKED_RESTRICTION)
                        && linkedPunishment.data().linkedBanId().equals(punishment.id())) {

                    linkedPunishment.data().linkedBanExpiry().set(new Date((new Date()).getTime() + newDuration));
                    database.saveAccount(linkedAccount);
                    sender.sendMessage(Colors.translate("&aSuccessfully set new durations on linked punishment #" + linkedPunishment.id()));
                }
            }
        }
    }

    @CommandAlias("pardon")
    @Syntax("<id> [reason]")
    @Description("Pardon a punishment, removing the points and stopping a stat-wipe from being applied.")
    @CommandPermission(Constants.PERMISSION)
    public void pardon(CommandIssuer sender, String[] args) {
        String senderName = senderName(sender);
        Account target = getAccount(sender, args);
        if (target == null) {
            sender.sendMessage(Colors.translate("&cPunishment ID doesn't exist."));
            return;
        }
        Punishment punishment = getPunishment(target, args);

        if (punishment.modifications().stream().anyMatch(m -> m.type().equals(Modification.Type.MANUAL_PARDON) || m.type().equals(Modification.Type.APPEAL_ACCEPT))) {
            sender.sendMessage(Colors.translate("&cThis punishment has already been pardoned."));
            return;
        }

        punishment.modifications().add(new Modification(Modification.Type.MANUAL_PARDON,
                senderName, new Date(), 0L));

        punishment.notes().add(new Note(new Date(), senderName, "Punishment pardoned"));
        punishment.notes().add(new Note(new Date(), senderName, note(args, 2)));

        database.saveAccount(target);

        CachedProfile cachedProfile = cache.get(target.uuid());
        if (cachedProfile != null) cache.remove(target.uuid());

        sender.sendMessage(Colors.translate("&aSuccessfully pardoned punishment #" + punishment.id()));

        if (!PunishmentHelper.blockEligble(punishment.type())) return;

        List<Account> linkedAccounts = database.findLinkedAccounts(target.uuid(), target.ipList());
        for (Account linkedAccount : linkedAccounts) {
            for (Punishment linkedPunishment : linkedAccount.punishments()) {
                if (linkedPunishment.type().equals(Punishment.Type.LINKED_RESTRICTION)
                        && linkedPunishment.data().linkedBanId().equals(punishment.id())) {

                    linkedPunishment.data().linkedBanExpiry().set(new Date());
                    database.saveAccount(linkedAccount);
                    sender.sendMessage(Colors.translate("&aSuccessfully pardoned linked punishment #" + linkedPunishment.id()));
                }
            }
        }
    }

    @CommandAlias("setflags")
    @Syntax("<id> [-sw] [-ab] [reason]")
    @Description("Any current flags not present here will be removed. Flags are: -sw (stat wipe), -ab (alt block)")
    @CommandPermission(Constants.PERMISSION)
    public void setFlags(CommandIssuer sender, String[] args) {
        String senderName = senderName(sender);
        Account target = getAccount(sender, args);
        if (target == null) {
            sender.sendMessage(Colors.translate("&cPunishment ID doesn't exist."));
            return;
        }
        Punishment punishment = getPunishment(target, args);

        boolean wiping = false;
        boolean altBlocking = false;

        for (String arg : args) {
            if (arg.equals("-w")) wiping = true;
            if (arg.equals("-ab")) altBlocking = true;
        }

        if (wiping && !PunishmentHelper.wipeEligble(punishment.type())) {
            sender.sendMessage(Colors.translate("&cThis type of punishment cannot cause an account-wipe."));
            return;
        }

        if (altBlocking && !PunishmentHelper.blockEligble(punishment.type())) {
            sender.sendMessage(Colors.translate("&cThis type of punishment cannot be alt blocking."));
            return;
        }

        punishment.modifications().add(new Modification(wiping ? Modification.Type.SET_WIPING_TRUE :
                Modification.Type.SET_WIPING_FALSE, senderName, new Date(), PunishmentHelper.duration(punishment)));

        punishment.modifications().add(new Modification(altBlocking ? Modification.Type.SET_ALT_BLOCKING_TRUE :
                Modification.Type.SET_ALT_BLOCKING_FALSE, senderName, new Date(), PunishmentHelper.duration(punishment)));

        punishment.notes().add(new Note(new Date(), senderName, "Set wipe-account-after-expiry to "
                + wiping + " for punishment #" + punishment.id()));
        punishment.notes().add(new Note(new Date(), senderName, "Set alt-blocking to "
                + altBlocking + " for punishment #" + punishment.id()));

        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String string : args) {
            i++;
            if (i == 0) continue; // Skip the first argument
            if (string.equalsIgnoreCase("-w")) continue;
            if (string.equalsIgnoreCase("-ab")) continue;
            sb.append(string).append(" ");
        }

        punishment.notes().add(new Note(new Date(), senderName, sb.toString()));

        database.saveAccount(target);
    }

    private String senderName(CommandIssuer sender) {
        return (sender.isPlayer()) ? platform.getAbstractPlayer(sender.getUniqueId(), false).username() : "Console";
    }

    private Account getAccount(CommandIssuer sender, String[] args) {
        sender.sendMessage(Colors.translate("&7Querying database for punishment..."));
        return database.fetchAccountFromPunishmentId(args[0]);
    }

    private Punishment getPunishment(Account account, String[] args) {
        Punishment punishment = account.punishments().stream().filter(p -> p.id().equalsIgnoreCase(args[0])).findFirst().orElse(null);
        assert punishment != null;
        return punishment;
    }

    private static String note(String[] arguments, int start) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String string : arguments) {
            i++;
            if (i < start) continue; // Skip the first argument
            sb.append(string).append(" ");
        }

        return sb.toString();
    }



}