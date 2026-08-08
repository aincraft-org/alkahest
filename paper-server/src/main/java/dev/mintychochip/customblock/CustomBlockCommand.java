package dev.mintychochip.customblock;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * {@code /customblock give <id> [count] [player]} — give a stamped custom-block item.
 */
public final class CustomBlockCommand {

    public static final String DESCRIPTION = "Give mintychochip custom block items";

    private CustomBlockCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("customblock")
            .requires(source -> source.getSender().hasPermission("mintychochip.command.customblock")
                || source.getSender().isOp())
            .then(Commands.literal("give")
                .then(Commands.argument("id", StringArgumentType.string())
                    .suggests(CustomBlockCommand::suggestIds)
                    .executes(ctx -> give(ctx, 1, null))
                    .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                        .executes(ctx -> give(ctx, IntegerArgumentType.getInteger(ctx, "count"), null))
                        .then(Commands.argument("player", ArgumentTypes.player())
                            .executes(ctx -> {
                                final PlayerSelectorArgumentResolver resolver =
                                    ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
                                final Player target = resolver.resolve(ctx.getSource()).get(0);
                                return give(ctx, IntegerArgumentType.getInteger(ctx, "count"), target);
                            })
                        )
                    )
                )
            )
            .then(Commands.literal("list")
                .executes(CustomBlockCommand::list)
            )
            .then(Commands.literal("pack")
                .executes(CustomBlockCommand::packInfo)
                .then(Commands.literal("resend")
                    .executes(CustomBlockCommand::packResend)
                )
            )
            .build();
    }

    private static CompletableFuture<Suggestions> suggestIds(
        final CommandContext<CommandSourceStack> ctx,
        final SuggestionsBuilder builder
    ) {
        final String remaining = builder.getRemainingLowerCase();
        for (final CustomBlockDefinition def : CustomBlocks.all()) {
            final String key = def.namespacedKey().toString();
            final String shortId = def.namespacedKey().getKey();
            if (key.toLowerCase(Locale.ROOT).startsWith(remaining)
                || shortId.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(shortId);
                builder.suggest(key);
            }
        }
        return builder.buildFuture();
    }

    private static int give(
        final CommandContext<CommandSourceStack> ctx,
        final int count,
        final Player explicitTarget
    ) throws CommandSyntaxException {
        final String id = StringArgumentType.getString(ctx, "id");
        final CustomBlockDefinition def = resolve(id);
        if (def == null) {
            ctx.getSource().getSender().sendMessage(
                text("Unknown custom block: ", RED).append(text(id, YELLOW))
            );
            ctx.getSource().getSender().sendMessage(
                text("Try /customblock list", GRAY)
            );
            return 0;
        }

        final Player target;
        if (explicitTarget != null) {
            target = explicitTarget;
        } else if (ctx.getSource().getSender() instanceof Player player) {
            target = player;
        } else {
            ctx.getSource().getSender().sendMessage(
                text("Console must specify a player: /customblock give <id> <count> <player>", RED)
            );
            return 0;
        }

        final ItemStack stack = CustomBlockProvenance.createMinted(
            def,
            count,
            "give:" + target.getUniqueId()
        );
        final var leftover = target.getInventory().addItem(stack);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(item ->
                target.getWorld().dropItemNaturally(target.getLocation(), item)
            );
        }

        ctx.getSource().getSender().sendMessage(
            text("Gave ", GREEN)
                .append(text(count + "× ", GOLD))
                .append(text(def.namespacedKey().toString(), YELLOW))
                .append(text(" to ", GREEN))
                .append(text(target.getName(), GOLD))
        );
        if (target != ctx.getSource().getSender()) {
            target.sendMessage(
                text("You received ", GREEN)
                    .append(text(count + "× ", GOLD))
                    .append(def.displayName() != null
                        ? def.displayName()
                        : text(def.namespacedKey().toString(), YELLOW))
            );
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int list(final CommandContext<CommandSourceStack> ctx) {
        if (CustomBlocks.all().isEmpty()) {
            ctx.getSource().getSender().sendMessage(text("No custom blocks registered.", GRAY));
            return Command.SINGLE_SUCCESS;
        }
        ctx.getSource().getSender().sendMessage(text("Custom blocks:", GOLD));
        for (final CustomBlockDefinition def : CustomBlocks.all()) {
            ctx.getSource().getSender().sendMessage(
                text("  • ", GRAY)
                    .append(text(def.namespacedKey().toString(), YELLOW))
                    .append(text(" [" + def.hostType() + "]", GRAY))
            );
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int packInfo(final CommandContext<CommandSourceStack> ctx) {
        final var service = CustomBlockBootstrap.packService();
        if (service == null) {
            ctx.getSource().getSender().sendMessage(
                text("Resource pack host is not running (disabled or failed to start).", RED)
            );
            return 0;
        }
        ctx.getSource().getSender().sendMessage(text("mintychochip resource pack", GOLD));
        ctx.getSource().getSender().sendMessage(
            text("  url  ", GRAY).append(text(service.publicPackUri().toString(), YELLOW))
        );
        ctx.getSource().getSender().sendMessage(
            text("  sha1 ", GRAY).append(text(service.archive().sha1Hex(), YELLOW))
        );
        ctx.getSource().getSender().sendMessage(
            text("  size ", GRAY).append(text(service.archive().size() + " bytes", YELLOW))
        );
        ctx.getSource().getSender().sendMessage(
            text("  force ", GRAY).append(text(String.valueOf(service.settings().force()), YELLOW))
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int packResend(final CommandContext<CommandSourceStack> ctx) {
        final var service = CustomBlockBootstrap.packService();
        if (service == null) {
            ctx.getSource().getSender().sendMessage(text("Resource pack host is not running.", RED));
            return 0;
        }
        if (ctx.getSource().getSender() instanceof org.bukkit.entity.Player player) {
            service.sendTo(player);
            player.sendMessage(text("Resent mintychochip resource pack.", GREEN));
            return Command.SINGLE_SUCCESS;
        }
        service.sendToAllOnline();
        ctx.getSource().getSender().sendMessage(
            text("Resent mintychochip resource pack to all online players.", GREEN)
        );
        return Command.SINGLE_SUCCESS;
    }

    private static CustomBlockDefinition resolve(final String id) {
        // full key
        final var byFull = CustomBlocks.get(id);
        if (byFull.isPresent()) {
            return byFull.get();
        }
        // short key under mintychochip:
        if (!id.contains(":")) {
            return CustomBlocks.get("mintychochip:" + id).orElse(null);
        }
        return null;
    }
}
