package world.bentobox.oneblock.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.ItemStack;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.oneblock.OneBlock;
import world.bentobox.oneblock.listeners.OneBlockObject.Rarity;
import world.bentobox.oneblock.listeners.OneBlockPhase;

public class AdminSetChestCommand extends CompositeCommand {

    private static final List<String> RARITY_LIST;
    static {
        List<String> l = Arrays.stream(Rarity.values()).map(Enum::name).collect(Collectors.toList());
        RARITY_LIST = Collections.unmodifiableList(l);
    }
    private OneBlock addon;
    private OneBlockPhase phase;
    private Rarity rarity;
    private Chest chest;

    public AdminSetChestCommand(CompositeCommand islandCommand) {
        super(islandCommand, "setchest");
    }

    @Override
    public void setup() {
        setParametersHelp("oneblock.commands.admin.setchest.parameters");
        setDescription("oneblock.commands.admin.setchest.description");
        // Permission
        setPermission("admin.setchest");
        addon = (OneBlock)getAddon();
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // Command is setchest phase rarity
        if (args.size() != 2) {
            showHelp(this, user);
            return false;
        }
        // Check phase
        Optional<OneBlockPhase> opPhase = addon.getOneBlockManager().getPhase(args.get(0).toUpperCase());
        if (!opPhase.isPresent()) {
            user.sendMessage("oneblock.commands.admin.setchest.unknown-phase");
            return false;
        } else {
            phase = opPhase.get();
        }

        // Get rarity
        try {
            rarity = Rarity.valueOf(args.get(1).toUpperCase());
        } catch (Exception e) {
            user.sendMessage("oneblock.commands.admin.setchest.unknown-rarity");
            return false;
        }

        // Check that player is looking at a chest
        Block target = user.getPlayer().getTargetBlock(null, 5);
        if (!target.getType().equals(Material.CHEST)) {
            user.sendMessage("oneblock.commands.admin.setchest.look-at-chest");
            return false;
        }
        chest = (Chest)target.getState();
        if (chest.getInventory().getHolder() instanceof DoubleChest) {
            user.sendMessage("oneblock.commands.admin.setchest.only-single-chest");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Open up a chest GUI
        // Get the items
        Map<Integer, ItemStack> items = new HashMap<>();
        for (int slot = 0; slot < chest.getInventory().getSize(); slot++) {
            ItemStack item = chest.getInventory().getItem(slot);
            if (item != null && !item.getType().equals(Material.AIR)) {
                items.put(slot, item);
            }
        }
        if (items.isEmpty()) {
            user.sendMessage("oneblock.commands.admin.setchest.chest-is-empty");
            return false;
        }
        phase.addChest(items, rarity);
        if (addon.getOneBlockManager().saveOneBlockConfig()) {
            user.sendMessage("oneblock.commands.admin.setchest.success");
        } else {
            user.sendMessage("oneblock.commands.admin.setchest.failure");
        }
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        if (args.size() == 2) {
            // Get a list of phases
            return Optional.of(addon.getOneBlockManager().getPhaseList());
        }
        if (args.size() == 3) {
            // Rarity
            return Optional.of(RARITY_LIST);
        }
        // Return nothing
        return Optional.empty();
    }
}
