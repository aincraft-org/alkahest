package dev.mintychochip.provenance;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.AQUA;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_AQUA;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.text.format.TextDecoration.STRIKETHROUGH;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.world.item.ItemStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Admin diagnostics for item provenance.
 *
 * <pre>
 * /provenance inspect   — held item identity + lineage tree
 * /provenance live      — live census size
 * /provenance audit [n] — last n audit events
 * /provenance collisions
 * /provenance dupe-sim  — claim held stack as two holders (tests detector)
 * /provenance clear     — wipe census/audit (ops, testing)
 * </pre>
 */
public final class ProvenanceBukkitCommand extends Command {

    private static final List<String> SUBCOMMANDS = List.of(
        "inspect", "live", "audit", "collisions", "dupe-sim", "clear"
    );

    private static final Component RULE_TOP = text("┌─ ", DARK_GRAY)
        .append(text("provenance", GOLD, BOLD))
        .append(text(" ────────────────", DARK_GRAY));
    private static final Component RULE_BOT = text("└────────────────────────────────", DARK_GRAY);

    public ProvenanceBukkitCommand() {
        super(
            "provenance",
            "Inspect mintychochip item provenance",
            "/provenance <inspect|live|audit|collisions|dupe-sim|clear>",
            List.of("prov", "mintyprov")
        );
        this.setPermission("mintychochip.provenance");
    }

    @Override
    public boolean execute(
        @NotNull final CommandSender sender,
        @NotNull final String commandLabel,
        @NotNull final String[] args
    ) {
        if (!sender.hasPermission("mintychochip.provenance") && !sender.isOp()) {
            sender.sendMessage(text("No permission.", RED));
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        final String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "inspect" -> inspect(sender);
            case "live" -> live(sender);
            case "audit" -> {
                final int n = args.length > 1 ? parseInt(args[1], 20) : 20;
                audit(sender, n);
            }
            case "collisions" -> collisions(sender);
            case "dupe-sim" -> dupeSim(sender);
            case "clear" -> {
                ItemProvenance.clearAll();
                sender.sendMessage(text("runtime census / audit cache cleared (durable lineage retained)", YELLOW));
            }
            default -> {
                sender.sendMessage(text("Unknown subcommand: ", RED).append(text(args[0], YELLOW)));
                sendUsage(sender);
            }
        }
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(
        @NotNull final CommandSender sender,
        @NotNull final String alias,
        @NotNull final String[] args
    ) {
        if (!sender.hasPermission("mintychochip.provenance") && !sender.isOp()) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && "audit".equalsIgnoreCase(args[0])) {
            return filter(List.of("5", "10", "20", "50", "100"), args[1]);
        }
        return Collections.emptyList();
    }

    // -------------------------------------------------------------------------
    // Subcommands
    // -------------------------------------------------------------------------

    private static void sendUsage(final CommandSender sender) {
        sender.sendMessage(RULE_TOP);
        sender.sendMessage(row("inspect", "held item identity + lineage tree"));
        sender.sendMessage(row("live", "census / lineage sizes"));
        sender.sendMessage(row("audit [n]", "last n audit events (default 20)"));
        sender.sendMessage(row("collisions", "recorded UUID collisions"));
        sender.sendMessage(row("dupe-sim", "force a synthetic double-claim"));
        sender.sendMessage(row("clear", "wipe census, audit, lineage"));
        sender.sendMessage(RULE_BOT);
        sender.sendMessage(text("aliases: ", DARK_GRAY).append(text("/prov  /mintyprov", GRAY)));
    }

    private static Component row(final String cmd, final String desc) {
        return text("│ ", DARK_GRAY)
            .append(text(pad(cmd, 14), AQUA))
            .append(text(desc, GRAY));
    }

    private static void live(final CommandSender sender) {
        sender.sendMessage(RULE_TOP);
        sender.sendMessage(kv("enabled", String.valueOf(ItemProvenance.isEnabled()),
            ItemProvenance.isEnabled() ? GREEN : RED));
        sender.sendMessage(kv("live census", String.valueOf(ItemProvenance.live().size()), AQUA));
        sender.sendMessage(kv("lineage cache", String.valueOf(ItemProvenance.lineage().size()), AQUA));
        sender.sendMessage(kv("collisions", String.valueOf(ItemProvenance.collisions().size()),
            ItemProvenance.collisions().isEmpty() ? GREEN : RED));
        sender.sendMessage(kv("audit buffer", String.valueOf(ItemProvenance.audit().size()), GRAY));
        sender.sendMessage(kv("store", dev.mintychochip.provenance.ProvenanceWriter.status(), GRAY));
        sender.sendMessage(RULE_BOT);
    }

    private static void audit(final CommandSender sender, final int n) {
        final List<ProvenanceEvent> events = ItemProvenance.audit().latest(n);
        sender.sendMessage(RULE_TOP);
        sender.sendMessage(
            text("│ ", DARK_GRAY)
                .append(text("audit", GOLD, BOLD))
                .append(text("  last ", GRAY))
                .append(text(String.valueOf(events.size()), AQUA))
                .append(text(" event(s)", GRAY))
        );
        sender.sendMessage(text("│", DARK_GRAY));
        if (events.isEmpty()) {
            sender.sendMessage(text("│ ", DARK_GRAY).append(text("(empty)", DARK_GRAY)));
        } else {
            // newest first feels better for forensics
            for (int i = events.size() - 1; i >= 0; i--) {
                sender.sendMessage(formatEvent(events.get(i)));
            }
        }
        sender.sendMessage(RULE_BOT);
    }

    private static Component formatEvent(final ProvenanceEvent e) {
        final NamedTextColor typeColor = switch (e.type()) {
            case BIRTH, TRANSFORM -> GREEN;
            case SPLIT -> LIGHT_PURPLE;
            case MERGE -> AQUA;
            case DEATH -> GRAY;
            case COLLISION, ZOMBIE -> RED;
            case CLAIM, REHYDRATE -> DARK_AQUA;
            default -> YELLOW;
        };
        Component line = text("│ ", DARK_GRAY)
            .append(text(pad(e.type().name(), 10), typeColor))
            .append(text(ItemProvenance.shortUuid(e.id()), DARK_GRAY)
                .hoverEvent(HoverEvent.showText(text(e.id().toString(), WHITE)))
                .clickEvent(ClickEvent.copyToClipboard(e.id().toString())));
        if (e.itemId() != null) {
            line = line.append(space()).append(text(ItemProvenance.shortItem(e.itemId()), WHITE));
        }
        if (e.source() != null) {
            line = line.append(space()).append(text(e.source().name(), sourceColor(e.source())));
        }
        if (e.reason() != null) {
            line = line.append(space()).append(text(e.reason().name(), YELLOW));
        }
        if (e.holder() != null) {
            line = line.append(space()).append(text(shortLabel(e.holder()), GRAY));
        }
        if (e.detail() != null) {
            line = line.append(space()).append(text("(" + e.detail() + ")", DARK_GRAY));
        }
        line = line.append(space()).append(text(relativeAge(e.epochMs()), DARK_GRAY));
        return line;
    }

    private static void collisions(final CommandSender sender) {
        final List<CollisionRecord> list = ItemProvenance.collisions();
        sender.sendMessage(RULE_TOP);
        if (list.isEmpty()) {
            sender.sendMessage(text("│ ", DARK_GRAY).append(text("no collisions recorded", GREEN)));
        } else {
            sender.sendMessage(
                text("│ ", DARK_GRAY)
                    .append(text(String.valueOf(list.size()), RED, BOLD))
                    .append(text(" collision(s)", RED))
            );
            sender.sendMessage(text("│", DARK_GRAY));
            for (final CollisionRecord c : list) {
                sender.sendMessage(
                    text("│ ", DARK_GRAY)
                        .append(text(ItemProvenance.shortUuid(c.id()), GOLD)
                            .hoverEvent(HoverEvent.showText(text(c.id().toString(), WHITE)))
                            .clickEvent(ClickEvent.copyToClipboard(c.id().toString())))
                        .append(text("  ", DARK_GRAY))
                        .append(text(shortLocation(c.existingLocation()), AQUA))
                        .append(text(" ↔ ", DARK_GRAY))
                        .append(text(shortLocation(c.observedLocation()), YELLOW))
                        .append(space())
                        .append(text(c.kind().name(), RED))
                        .append(space())
                        .append(text(relativeAge(c.epochMs()), DARK_GRAY))
                );
            }
        }
        sender.sendMessage(RULE_BOT);
    }

    private static void inspect(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(text("players only", RED));
            return;
        }
        final ItemStack nms = CraftItemStack.asNMSCopy(player.getInventory().getItemInMainHand());
        if (nms.isEmpty()) {
            sender.sendMessage(text("hold an item", GRAY));
            return;
        }
        final boolean wasUnstamped = ItemProvenance.of(nms).isEmpty();
        if (wasUnstamped) {
            ItemProvenance.ensure(nms, dev.mintychochip.provenance.StackLocation.playerSlot(player.getUniqueId(), -1));
            player.getInventory().setItemInMainHand(CraftItemStack.asBukkitCopy(nms));
        }
        final Optional<StackProvenance> stamp = ItemProvenance.of(nms);
        if (stamp.isEmpty()) {
            sender.sendMessage(text("failed to stamp held item", RED));
            return;
        }
        final StackProvenance s = stamp.get();
        final Optional<LineageNode> node = ItemProvenance.lineage().get(s.id());
        final Optional<LiveEntry> live = ItemProvenance.live().get(s.id());
        final String itemLabel = ItemProvenance.shortItem(ItemProvenance.itemId(nms));

        sender.sendMessage(RULE_TOP);
        sender.sendMessage(
            text("│ ", DARK_GRAY)
                .append(text("inspect", GOLD, BOLD))
                .append(text("  ", DARK_GRAY))
                .append(text(itemLabel, WHITE))
                .append(text(" ×" + nms.getCount(), GRAY))
        );
        if (wasUnstamped) {
            sender.sendMessage(
                text("│ ", DARK_GRAY).append(text("minted LEGACY stamp on first inspect", YELLOW))
            );
        }
        sender.sendMessage(text("│", DARK_GRAY));
        sender.sendMessage(kvClickableUuid("uuid", s.id()));
        sender.sendMessage(kv("source", s.source().name(), sourceColor(s.source())));
        sender.sendMessage(kv("born", relativeAge(s.bornEpochMs()) + "  (" + s.bornEpochMs() + ")", GRAY));
        if (live.isPresent()) {
            final LiveEntry e = live.get();
            final String locations = e.locations().isEmpty()
                ? "unknown"
                : e.locations().stream().map(StackLocation::display).reduce((a, b) -> a + ", " + b).orElse("?");
            sender.sendMessage(
                kv("status", "LIVE ×" + e.count() + "  @ " + locations, GREEN)
            );
        } else if (node.isPresent() && node.get().dead()) {
            sender.sendMessage(
                kv("status", "DEAD [" + node.get().deathReason() + "]", RED)
            );
        } else {
            sender.sendMessage(kv("status", "not in live census", YELLOW));
        }
        if (!s.parents().isEmpty()) {
            sender.sendMessage(
                text("│ ", DARK_GRAY)
                    .append(text("parents", GRAY))
                    .append(text("  ", DARK_GRAY))
                    .append(text(String.valueOf(s.parents().size()), AQUA))
            );
        }

        sender.sendMessage(text("│", DARK_GRAY));
        sender.sendMessage(
            text("│ ", DARK_GRAY).append(text("lineage", GOLD, BOLD))
                .append(text("  (current stack → contributors)", DARK_GRAY))
        );
        sender.sendMessage(text("│", DARK_GRAY));

        final Optional<LineageNode> root = ItemProvenance.lineage().get(s.id());
        if (root.isEmpty()) {
            sender.sendMessage(text("│ ", DARK_GRAY).append(text("(no lineage node)", DARK_GRAY)));
        } else {
            final Set<UUID> seen = new HashSet<>();
            sendTree(sender, root.get(), "│ ", true, true, seen, 0, 24);
        }
        sender.sendMessage(RULE_BOT);
        sender.sendMessage(
            text("  tip: ", DARK_GRAY)
                .append(text("click a UUID to copy", GRAY))
        );
    }

    /**
     * Recursive box-drawing tree of ancestry (root first, parents as children).
     */
    private static void sendTree(
        final CommandSender sender,
        final LineageNode node,
        final String prefix,
        final boolean isRoot,
        final boolean isLast,
        final Set<UUID> seen,
        final int depth,
        final int maxDepth
    ) {
        final Component branch;
        if (isRoot) {
            branch = text("│ ", DARK_GRAY);
        } else {
            branch = text(prefix, DARK_GRAY)
                .append(text(isLast ? "└─ " : "├─ ", DARK_GRAY));
        }

        Component line = branch
            .append(text(ItemProvenance.shortItem(node.itemId()), node.dead() ? GRAY : WHITE)
                .decoration(STRIKETHROUGH, node.dead()))
            .append(space())
            .append(text(node.source().name(), sourceColor(node.source())))
            .append(space())
            .append(text(ItemProvenance.shortUuid(node.id()), DARK_GRAY)
                .hoverEvent(HoverEvent.showText(
                    text(node.id().toString(), WHITE)
                        .append(newline())
                        .append(text("born " + relativeAge(node.bornEpochMs()), GRAY))
                        .append(node.bornHolder() != null
                            ? newline().append(text("by " + shortLabel(node.bornHolder()), GRAY))
                            : empty())
                        .append(newline())
                        .append(text("click to copy full UUID", DARK_GRAY))
                ))
                .clickEvent(ClickEvent.copyToClipboard(node.id().toString())));

        if (node.dead()) {
            line = line
                .append(space())
                .append(text("DEAD", RED, BOLD))
                .append(text("[" + node.deathReason() + "]", RED));
        }

        sender.sendMessage(line);

        if (depth >= maxDepth || !seen.add(node.id())) {
            if (depth >= maxDepth) {
                sender.sendMessage(
                    text(prefix + (isLast ? "   " : "│  "), DARK_GRAY)
                        .append(text("… depth limit", DARK_GRAY))
                );
            }
            return;
        }

        final List<UUID> parents = node.parents();
        final String childPrefix = isRoot ? "│ " : prefix + (isLast ? "   " : "│  ");
        for (int i = 0; i < parents.size(); i++) {
            final boolean last = i == parents.size() - 1;
            final UUID pid = parents.get(i);
            final Optional<LineageNode> parent = ItemProvenance.lineage().get(pid);
            if (parent.isEmpty()) {
                sender.sendMessage(
                    text(childPrefix, DARK_GRAY)
                        .append(text(last ? "└─ " : "├─ ", DARK_GRAY))
                        .append(text("?", YELLOW))
                        .append(space())
                        .append(text(ItemProvenance.shortUuid(pid), DARK_GRAY)
                            .hoverEvent(HoverEvent.showText(text(pid.toString(), WHITE)))
                            .clickEvent(ClickEvent.copyToClipboard(pid.toString())))
                        .append(text("  (missing node)", DARK_GRAY))
                );
                continue;
            }
            sendTree(sender, parent.get(), childPrefix, false, last, seen, depth + 1, maxDepth);
        }
    }

    private static void dupeSim(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(text("players only", RED));
            return;
        }
        final ItemStack nms = CraftItemStack.asNMSCopy(player.getInventory().getItemInMainHand());
        if (nms.isEmpty()) {
            sender.sendMessage(text("hold an item", GRAY));
            return;
        }
        final boolean hit = ItemProvenance.simulateDupe(
            nms,
            dev.mintychochip.provenance.StackLocation.playerSlot(player.getUniqueId(), -1),
            dev.mintychochip.provenance.StackLocation.labeled("dupe-sim:clone")
        );
        player.getInventory().setItemInMainHand(CraftItemStack.asBukkitCopy(nms));
        if (hit) {
            sender.sendMessage(
                text("COLLISION recorded", RED, BOLD)
                    .append(text(" — detector OK", GRAY))
            );
        } else {
            sender.sendMessage(
                text("no collision", YELLOW)
                    .append(text(" (stack may have been unstamped / first claim only)", GRAY))
            );
        }
    }

    // -------------------------------------------------------------------------
    // Formatting helpers
    // -------------------------------------------------------------------------

    private static Component kv(final String key, final String value, final TextColor valueColor) {
        return text("│ ", DARK_GRAY)
            .append(text(pad(key, 14), GRAY))
            .append(text(value, valueColor));
    }

    private static Component kvClickableUuid(final String key, final UUID id) {
        return text("│ ", DARK_GRAY)
            .append(text(pad(key, 14), GRAY))
            .append(text(id.toString(), AQUA)
                .hoverEvent(HoverEvent.showText(text("click to copy", GRAY)))
                .clickEvent(ClickEvent.copyToClipboard(id.toString())));
    }

    private static NamedTextColor sourceColor(final ProvenanceSource source) {
        return switch (source) {
            case CRAFT, SMELT, SPECIAL_RECIPE -> AQUA;
            case MERGE -> AQUA;
            case SPLIT -> LIGHT_PURPLE;
            case BLOCK_DROP, ENTITY_DROP, LOOT -> GREEN;
            case BLOCK_RECOVER -> GOLD;
            case TRADE -> YELLOW;
            case GIVE -> WHITE;
            case LEGACY, REHYDRATE, UNKNOWN -> GRAY;
        };
    }

    private static String shortLocation(final StackLocation location) {
        final String raw = location.display();
        if (raw.startsWith("player:")) {
            final String rest = raw.substring("player:".length());
            final int colon = rest.indexOf(':');
            final String uuidPart = colon > 0 ? rest.substring(0, colon) : rest;
            final String suffix = colon > 0 ? rest.substring(colon) : "";
            final String shortU = uuidPart.length() >= 8 ? uuidPart.substring(0, 8) : uuidPart;
            return "p:" + shortU + suffix;
        }
        if (raw.startsWith("item_entity:")) {
            final String uuidPart = raw.substring("item_entity:".length());
            return "item:" + (uuidPart.length() >= 8 ? uuidPart.substring(0, 8) : uuidPart);
        }
        return raw.length() > 32 ? raw.substring(0, 29) + "…" : raw;
    }

    private static String shortLabel(final String label) {
        if (label == null) {
            return "?";
        }
        return label.length() > 32 ? label.substring(0, 29) + "…" : label;
    }

    private static String relativeAge(final long epochMs) {
        final long delta = Math.max(0L, System.currentTimeMillis() - epochMs);
        if (delta < 1_000L) {
            return "just now";
        }
        if (delta < 60_000L) {
            return TimeUnit.MILLISECONDS.toSeconds(delta) + "s ago";
        }
        if (delta < 3_600_000L) {
            return TimeUnit.MILLISECONDS.toMinutes(delta) + "m ago";
        }
        if (delta < 86_400_000L) {
            return TimeUnit.MILLISECONDS.toHours(delta) + "h ago";
        }
        return TimeUnit.MILLISECONDS.toDays(delta) + "d ago";
    }

    private static Component newline() {
        return Component.newline();
    }

    private static String pad(final String s, final int width) {
        if (s.length() >= width) {
            return s;
        }
        return s + " ".repeat(width - s.length());
    }

    private static List<String> filter(final List<String> options, final String prefix) {
        final String p = prefix.toLowerCase(Locale.ROOT);
        final List<String> out = new ArrayList<>();
        for (final String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(o);
            }
        }
        return out;
    }

    private static int parseInt(final String raw, final int def) {
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (final NumberFormatException ex) {
            return def;
        }
    }
}
