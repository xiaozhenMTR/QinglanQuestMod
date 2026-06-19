package com.xz.nquestql.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.xz.nquestql.NQuestQinglanEdition;
import com.xz.nquestql.data.QuestData;
import com.xz.nquestql.manager.QuestManager;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;

public class QuestCommand {

    // ★ questId 自动补全
    private static final SuggestionProvider<ServerCommandSource> QUEST_ID_SUGGESTIONS =
            (ctx, builder) -> {
                for (QuestData q : QuestManager.getAll()) {
                    builder.suggest(q.questId);
                }
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(literal("qlquest")
                .then(literal("start")
                        .then(argument("questId", StringArgumentType.word())
                                .suggests(QUEST_ID_SUGGESTIONS)          // ★ 补全
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                                    QuestData q = QuestManager.getQuest(StringArgumentType.getString(ctx, "questId"));
                                    if (q == null) {
                                        ctx.getSource().sendFeedback(() -> Text.literal("§c任务不存在"), false);
                                        return 0;
                                    }
                                    return QuestManager.trigger(p, q, 0) ? 1 : 0; // code=0 表示 start
                                })))
                .then(literal("trigger")
                        .requires(s -> s.hasPermissionLevel(2))
                        .then(argument("questId", StringArgumentType.word())
                                .suggests(QUEST_ID_SUGGESTIONS)          // ★ 补全
                                .then(argument("code", IntegerArgumentType.integer(1))  // ★ code≥1
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                                            QuestData q = QuestManager.getQuest(StringArgumentType.getString(ctx, "questId"));
                                            if (q == null) {
                                                ctx.getSource().sendFeedback(() -> Text.literal("§c任务不存在"), false);
                                                return 0;
                                            }
                                            int code = IntegerArgumentType.getInteger(ctx, "code");
                                            return QuestManager.trigger(p, q, code) ? 1 : 0;
                                        })
                                        .then(argument("player", EntityArgumentType.player())
                                                .executes(ctx -> {
                                                    ServerPlayerEntity t = EntityArgumentType.getPlayer(ctx, "player");
                                                    QuestData q = QuestManager.getQuest(StringArgumentType.getString(ctx, "questId"));
                                                    if (q == null) {
                                                        ctx.getSource().sendFeedback(() -> Text.literal("§c任务不存在"), false);
                                                        return 0;
                                                    }
                                                    int code = IntegerArgumentType.getInteger(ctx, "code");
                                                    boolean ok = QuestManager.trigger(t, q, code);
                                                    if (ok) ctx.getSource().sendFeedback(
                                                            () -> Text.literal("§a已为 " + t.getName().getString() + " 推进"), true);
                                                    return ok ? 1 : 0;
                                                })))))
                .then(literal("abort")
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            return QuestManager.abort(p) ? 1 : 0;
                        }))
                .then(literal("list")
                        .executes(ctx -> {
                            var list = QuestManager.getAll();
                            ctx.getSource().sendFeedback(
                                    () -> Text.literal("§7=== 可用任务 (" + list.size() + ") ==="), false);
                            for (QuestData q : list)
                                ctx.getSource().sendFeedback(
                                        () -> Text.literal("§e- " + q.questId + ": " + q.questName + " §7(+" + q.qpReward + " QP, " + (q.points != null ? q.points.size() : 0) + "步)"), false);
                            return 1;
                        }))
                .then(literal("reload")
                        .requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            NQuestQinglanEdition.getInstance().loadQuests();
                            ctx.getSource().sendFeedback(() -> Text.literal("§a已重载"), true);
                            return 1;
                        }))
                .then(literal("qp")
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            int qp = NQuestQinglanEdition.getInstance().getQPStorage().getQP(p.getUuid());
                            p.sendMessage(Text.literal("§6你的 QP: " + qp), false);
                            return 1;
                        }))
        );
    }
}
