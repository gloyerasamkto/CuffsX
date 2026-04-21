package org.gloyer057.cuffsx.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import org.gloyer057.cuffsx.cuff.*;
import org.gloyer057.cuffsx.item.ModItems;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CuffsxCommand {

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private static Predicate<ServerCommandSource> perm(String node) {
        return Permissions.require(node, 4);
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("cuffsx")
                .then(literal("enable")
                    .requires(perm("cuffsx.enable"))
                    .executes(ctx -> {
                        CuffManager.setEnabled(true);
                        ctx.getSource().sendFeedback(new LiteralText("Наручники включены."), false);
                        return 1;
                    }))
                .then(literal("disable")
                    .requires(perm("cuffsx.disable"))
                    .executes(ctx -> {
                        CuffManager.setEnabled(false);
                        ctx.getSource().sendFeedback(new LiteralText("Наручники отключены."), false);
                        return 1;
                    }))
                .then(literal("list")
                    .requires(perm("cuffsx.list"))
                    .executes(ctx -> {
                        ServerCommandSource source = ctx.getSource();
                        CuffState state = CuffState.getOrCreate(source.getServer());
                        Collection<CuffRecord> all = state.getAllRecords();
                        if (all.isEmpty()) {
                            source.sendFeedback(new LiteralText("Нет игроков в наручниках."), false);
                        } else {
                            long now = System.currentTimeMillis();
                            for (CuffRecord r : all) {
                                long elapsed = (now - r.timestamp()) / 1000;
                                source.sendFeedback(new LiteralText(
                                    r.targetName() + " | " + r.cuffType() + " | " + r.applierName() + " | " + elapsed + " сек."), false);
                            }
                        }
                        return 1;
                    }))
                .then(literal("log")
                    .requires(perm("cuffsx.log"))
                    .executes(ctx -> {
                        ServerCommandSource source = ctx.getSource();
                        List<CuffLog.LogEntry> recent = CuffLog.getRecent();
                        if (recent.isEmpty()) {
                            source.sendFeedback(new LiteralText("Нет взаимодействий за последние 6 часов."), false);
                        } else {
                            for (CuffLog.LogEntry e : recent) {
                                String action = e.action() == CuffLog.Action.APPLY ? "APPLY" : "REMOVE";
                                String time = TIME_FMT.format(Instant.ofEpochMilli(e.timestamp()));
                                source.sendFeedback(new LiteralText(
                                    action + " | " + e.applierName() + " -> " + e.targetName() + " | " + e.cuffType() + " | " + time), false);
                            }
                        }
                        return 1;
                    }))
                .then(literal("remove")
                    .requires(perm("cuffsx.remove"))
                    .then(argument("player", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerCommandSource source = ctx.getSource();
                            String playerName = StringArgumentType.getString(ctx, "player");
                            ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(playerName);
                            if (target == null) {
                                source.sendError(new LiteralText("Игрок " + playerName + " не найден."));
                                return 0;
                            }
                            CuffState state = CuffState.getOrCreate(source.getServer());
                            Set<CuffRecord> records = state.getRecords(target.getUuid());
                            if (records.isEmpty()) {
                                source.sendError(new LiteralText("У игрока " + playerName + " нет наручников."));
                                return 0;
                            }
                            for (CuffType type : CuffType.values()) {
                                if (state.hasCuff(target.getUuid(), type)) {
                                    CuffManager.removeCuffsForced(target, type);
                                    ItemStack newStack = new ItemStack(
                                        type == CuffType.HANDS ? ModItems.HANDCUFFS_HANDS : ModItems.HANDCUFFS_LEGS);
                                    try {
                                        ServerPlayerEntity executor = source.getPlayer();
                                        if (!executor.getInventory().insertStack(newStack))
                                            executor.dropItem(newStack, false);
                                    } catch (Exception ignored) {}
                                }
                            }
                            source.sendFeedback(new LiteralText("Наручники сняты с " + playerName + "."), false);
                            return 1;
                        })))
        );
    }
}
