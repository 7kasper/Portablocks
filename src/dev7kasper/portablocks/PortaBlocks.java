package dev7kasper.portablocks;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class PortaBlocks extends JavaPlugin implements Listener {

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
	}

	@EventHandler
	public void onClick(PlayerInteractEvent e) {
		if (e.getPlayer().isSneaking() && (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)) {
			// Try if it is a container and open it. If the main hand fails, try the offhand.
			if(!clickWithBlock(e.getPlayer(), e.getPlayer().getInventory().getItemInMainHand())) {
				clickWithBlock(e.getPlayer(), e.getPlayer().getInventory().getItemInOffHand());
			}
		}

	}

	public boolean clickWithBlock(Player player, ItemStack container) {
		switch(container.getType()) {
			case WORKBENCH: {
				if (player.hasPermission("portablocks.use.workbench")) {
					player.openWorkbench(null, true);
					return true;
				}
				break;
			}
			case ENCHANTMENT_TABLE: {
				if (player.hasPermission("portablocks.use.enchantment_table")) {
					player.openEnchanting(null, true);
					return true;
				}
				break;
			}
			case ENDER_CHEST: {
				if (player.hasPermission("portablocks.use.ender_chest")) {
					player.openInventory(player.getEnderChest());
					return true;
				}
				break;
			}
			/*case WHITE_SHULKER_BOX:
			case ORANGE_SHULKER_BOX:
			case MAGENTA_SHULKER_BOX:
			case LIGHT_BLUE_SHULKER_BOX:
			case YELLOW_SHULKER_BOX:
			case LIME_SHULKER_BOX:
			case PINK_SHULKER_BOX:
			case GRAY_SHULKER_BOX:
			case SILVER_SHULKER_BOX:
			case CYAN_SHULKER_BOX:
			case PURPLE_SHULKER_BOX:
			case BLUE_SHULKER_BOX:
			case BROWN_SHULKER_BOX:
			case GREEN_SHULKER_BOX:
			case RED_SHULKER_BOX:
			case BLACK_SHULKER_BOX: {
				if (player.hasPermission("portablocks.use.shulker_box")) {
					if (container.getItemMeta() instanceof BlockStateMeta) {
						BlockStateMeta cMeta = (BlockStateMeta) container.getItemMeta();
						if (cMeta.getBlockState() instanceof ShulkerBox) {
							ShulkerBox box = (ShulkerBox) cMeta.getBlockState();
							box.getin
							Inventory inv = Bukkit.createInventory(null, 27, "Shulker Box");
							inv.setContents(box.getInventory().getContents());
							player.openInventory(inv);
							return true;
						}
					}
				}
				break;
			}*/
			default: {
				break;
			}
		}
		return false;
	}

}
