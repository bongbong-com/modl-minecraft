package com.bongbong.modl.minecraft.core.impl.commands.player;


import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.CommandAlias;
import com.bongbong.modl.minecraft.api.Account;
import com.bongbong.modl.minecraft.api.Punishment;
import com.bongbong.modl.minecraft.core.procedure.Database;
import com.bongbong.modl.minecraft.core.procedure.PunishmentHelper;
import com.bongbong.modl.minecraft.core.util.Colors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StandingCommand extends BaseCommand {
    private final Database database;

    @CommandAlias("standing")
    public void standing(CommandIssuer sender) {
        if (!sender.isPlayer()){
            sender.sendMessage("&cYou must be a player to use this command.");
            return;
        }

        sender.sendMessage(Colors.translate("&7Querying database for your account..."));
        Account account = database.fetchAccount(sender.getUniqueId());

        sender.sendMessage(Colors.translate("&fSocial standing: " +
                PunishmentHelper.getLevel(account.punishments(), Punishment.PointCategory.SOCIAL)));
        sender.sendMessage(Colors.translate("&fGameplay standing: " +
                PunishmentHelper.getLevel(account.punishments(), Punishment.PointCategory.GAMEPLAY)));
    }

}
