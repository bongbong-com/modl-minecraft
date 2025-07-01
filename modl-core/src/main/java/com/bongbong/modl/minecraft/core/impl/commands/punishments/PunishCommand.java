//package com.bongbong.modl.minecraft.core.impl.commands.punishments;
//
//import co.aikar.commands.BaseCommand;
//import co.aikar.commands.CommandIssuer;
//import co.aikar.commands.annotation.*;
//import com.bongbong.modl.minecraft.api.Account;
//import com.bongbong.modl.minecraft.core.Constants;
//import com.bongbong.modl.minecraft.core.impl.cache.Cache;
//import com.bongbong.modl.minecraft.core.impl.menus.PunishMenu;
//import com.bongbong.modl.minecraft.core.procedure.Database;
//import com.bongbong.modl.minecraft.core.procedure.Platform;
//import com.bongbong.modl.minecraft.core.util.Colors;
//import lombok.RequiredArgsConstructor;
//
//@RequiredArgsConstructor
//public class PunishCommand extends BaseCommand {
//    private final Database database;
//    private final Platform platform;
//    private final Cache cache;
//
//    @CommandCompletion("@players")
//    @CommandAlias("punish|p")
//    @Syntax("<target> [reason]")
//    @CommandPermission(Constants.PERMISSION)
//    public void punish(CommandIssuer sender, @Name("target") Account account,
//                       @Default("Unspecified") String reason) {
//        if (account == null) {
//            sender.sendMessage(Colors.translate("&cPlayer has never joined the server."));
//            return;
//        }
//
//        if (!sender.isPlayer()) {
//            sender.sendMessage("Player only command!");
//            return;
//        }
//
//        new PunishMenu(account, database, platform, cache, database.getReports(account.uuid()), reason, null)
//                .display(platform.getPlayerWrapper(sender.getUniqueId()));
//    }
//}
