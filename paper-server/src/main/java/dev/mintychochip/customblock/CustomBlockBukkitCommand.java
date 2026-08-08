package dev.mintychochip.customblock;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bukkit-map command so registration works during POSTWORLD without brigadier lifecycle.
 *
 * <pre>
 * /customblock give &lt;id&gt; [count] [player]
 * /customblock list
 * /customblock pack [resend]
 * </pre>
 */
public final class CustomBlockBukkitCommand extends Command {

    public CustomBlockBukkitCommand() {
        super(
            "customblock",
            CustomBlockCommand.DESCRIPTION,
            "/customblock <give|list|pack> ...",
            List.of("cb", "cblock")
        );
        this.setPermission("mintychochip.command.customblock");
    }

    @Override
    public boolean execute(
        @NotNull final CommandSender sender,
        @NotNull final String commandLabel,
        @NotNull final String[] args
    ) {
        if (!sender.isOp() && !sender.hasPermission("mintychochip.command.customblock")) {
            sender.sendMessage(text("No permission.", RED));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(text("Usage: /customblock <give|list|pack>", GRAY));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> give(sender, Arrays.copyOfRange(args, 1, args.length));
            case "list" -> list(sender);
            case "pack" -> pack(sender, Arrays.copyOfRange(args, 1, args.length));
            default -> sender.sendMessage(text("Unknown subcommand. Use give|list|pack", RED));
        }
        return true;
    }

    private static void give(final CommandSender sender, final String[] args) {
        if (args.length < 1) {
            sender.sendMessage(text("Usage: /customblock give <id> [count] [player]", GRAY));
            return;
        }
        final CustomBlockDefinition def = resolve(args[0]);
        if (def == null) {
            sender.sendMessage(text("Unknown custom block: ", RED).append(text(args[0], YELLOW)));
            sender.sendMessage(text("Try /customblock list", GRAY));
            return;
        }
        int count = 1;
        if (args.length >= 2) {
            try {
                count = Integer.parseInt(args[1]);
            } catch (final NumberFormatException e) {
                sender.sendMessage(text("Invalid count: " + args[1], RED));
                return;
            }
            if (count < 1 || count > 64) {
                sender.sendMessage(text("Count must be 1-64", RED));
                return;
            }
        }
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(text("Player not found: " + args[2], RED));
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(text("Console must specify a player.", RED));
            return;
        }

        final ItemStack stack = CustomBlockProvenance.createMinted(
            def,
            count,
            "give:" + target.getUniqueId()
        );
        final var leftover = target.getInventory().addItem(stack);
        leftover.values().forEach(item ->
            target.getWorld().dropItemNaturally(target.getLocation(), item)
        );
        sender.sendMessage(
            text("Gave ", GREEN)
                .append(text(count + "× ", GOLD))
                .append(text(def.namespacedKey().toString(), YELLOW))
                .append(text(" to ", GREEN))
                .append(text(target.getName(), GOLD))
        );
    }

    private static void list(final CommandSender sender) {
        if (CustomBlocks.all().isEmpty()) {
            sender.sendMessage(text("No custom blocks registered.", GRAY));
            return;
        }
        sender.sendMessage(text("Custom blocks:", GOLD));
        for (final CustomBlockDefinition def : CustomBlocks.all()) {
            sender.sendMessage(
                text("  • ", GRAY)
                    .append(text(def.namespacedKey().toString(), YELLOW))
                    .append(text(" [" + def.hostType() + "]", GRAY))
            );
        }
    }

    private static void pack(final CommandSender sender, final String[] args) {
        final var service = CustomBlockBootstrap.packService();
        if (service == null) {
            sender.sendMessage(text("Resource pack host is not running.", RED));
            return;
        }
        if (args.length > 0 && "resend".equalsIgnoreCase(args[0])) {
            if (sender instanceof Player player) {
                service.sendTo(player);
                sender.sendMessage(text("Resent mintychochip resource pack.", GREEN));
            } else {
                service.sendToAllOnline();
                sender.sendMessage(text("Resent pack to all online players.", GREEN));
            }
            return;
        }
        sender.sendMessage(text("mintychochip resource pack", GOLD));
        sender.sendMessage(text("  url  ", GRAY).append(text(service.publicPackUri().toString(), YELLOW)));
        sender.sendMessage(text("  sha1 ", GRAY).append(text(service.archive().sha1Hex(), YELLOW)));
        sender.sendMessage(text("  size ", GRAY).append(text(service.archive().size() + " bytes", YELLOW)));
        sender.sendMessage(text("  force ", GRAY).append(text(String.valueOf(service.settings().force()), YELLOW)));
        sender.sendMessage(text("Resend with: /customblock pack resend", GRAY));
    }

    private static @Nullable CustomBlockDefinition resolve(final String id) {
        final var byFull = CustomBlocks.get(id);
        if (byFull.isPresent()) {
            return byFull.get();
        }
        if (!id.contains(":")) {
            return CustomBlocks.get("mintychochip:" + id).orElse(null);
        }
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(
        @NotNull final CommandSender sender,
        @NotNull final String alias,
        @NotNull final String[] args
    ) {
        if (args.length == 1) {
            return filter(List.of("give", "list", "pack"), args[0]);
        }
        if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            final List<String> ids = new ArrayList<>();
            for (final CustomBlockDefinition def : CustomBlocks.all()) {
                ids.add(def.namespacedKey().getKey());
                ids.add(def.namespacedKey().toString());
            }
            return filter(ids, args[1]);
        }
        if (args.length == 2 && "pack".equalsIgnoreCase(args[0])) {
            return filter(List.of("resend"), args[1]);
        }
        if (args.length == 4 && "give".equalsIgnoreCase(args[0])) {
            final List<String> names = new ArrayList<>();
            for (final Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return filter(names, args[3]);
        }
        return Collections.emptyList();
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
}
