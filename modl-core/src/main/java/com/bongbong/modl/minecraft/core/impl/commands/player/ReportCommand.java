//package com.bongbong.modl.minecraft.core.impl.commands.player;
//
//import co.aikar.commands.BaseCommand;
//import co.aikar.commands.CommandIssuer;
//import co.aikar.commands.annotation.*;
//import com.bongbong.modl.minecraft.api.AbstractPlayer;
//import com.bongbong.modl.minecraft.core.Constants;
//import com.bongbong.modl.minecraft.core.Platform;
//import com.bongbong.modl.minecraft.core.impl.menus.ReportMenu;
//import lombok.RequiredArgsConstructor;
//
//@RequiredArgsConstructor
//public class ReportCommand extends BaseCommand {
//    private final Platform platform;
//
//    @CommandCompletion("@players")
//    @CommandAlias("report")
//    @Syntax("<target> [reason]")
//    public void report(CommandIssuer sender, @Name("target") AbstractPlayer target,
//                       @Default("Unspecified") String reason) {
//        if (target == null) {
//            sender.sendMessage(Constants.NOT_FOUND);
//            return;
//        }
//
//        if (!sender.isPlayer()) {
//            ReportMenu.report(target, null, Report.Type.CHEATING, reason, database, platform);
//            return;
//        }
//
//        new ReportMenu(target, platform.getAbstractPlayer(sender.getUniqueId(), false), reason, database, platform)
//                .display(platform.getPlayerWrapper(sender.getUniqueId()));
//    }
//
//    @CommandCompletion("@players")
//    @CommandAlias("anticheatreport|acr|hackreport|hr|wdr")
//    @Syntax("<target> [reason]")
//    public void anticheatReport(CommandIssuer sender, @Name("target") AbstractPlayer target,
//                                @Default("Unspecified") String reason) {
//        if (target == null) {
//            sender.sendMessage(Constants.NOT_FOUND);
//            return;
//        }
//
//        ReportMenu.report(target, platform.getAbstractPlayer(sender.getUniqueId(), false), Report.Type.CHEATING, reason, database, platform);
//    }
//
//    @CommandCompletion("@players")
//    @CommandAlias("chatreport|cr")
//    @Syntax("<target> [reason]")
//    public void chatReport(CommandIssuer sender, @Name("target") AbstractPlayer target,
//                           @Default("Unspecified") String reason) {
//        if (target == null) {
//            sender.sendMessage(Constants.NOT_FOUND);
//            return;
//        }
//
//        ReportMenu.report(target, platform.getAbstractPlayer(sender.getUniqueId(), false), Report.Type.CHAT, reason, database, platform);
//    }
//}
