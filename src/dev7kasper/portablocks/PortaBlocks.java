package dev7kasper.portablocks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class PortaBlocks extends JavaPlugin implements Listener {
	
	private Set<UUID> portaECOpen = new HashSet<>();
	private Map<UUID, Integer> portaShulkerSlot = new HashMap<>();
	private Set<UUID> portaShulkerOnCursor = new HashSet<>();

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
			case CRAFTING_TABLE: {
				if (player.hasPermission("portablocks.use.workbench")) {
					player.openWorkbench(null, true);
					return true;
				}
				break;
			}
			case ENCHANTING_TABLE: {
				if (player.hasPermission("portablocks.use.enchantment_table")) {
					player.openEnchanting(null, true);
					return true;
				}
				break;
			}
			case ENDER_CHEST: {
				if (player.hasPermission("portablocks.use.ender_chest")) {
					player.openInventory(player.getEnderChest());
					player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, .1F, 1.0F);
					portaECOpen.add(player.getUniqueId());
					return true;
				}
				break;
			}
			case SHULKER_BOX:
			case LIGHT_GRAY_SHULKER_BOX:
			case BLACK_SHULKER_BOX:
			case BLUE_SHULKER_BOX:
			case BROWN_SHULKER_BOX:
			case CYAN_SHULKER_BOX:
			case GRAY_SHULKER_BOX:
			case GREEN_SHULKER_BOX:
			case LIGHT_BLUE_SHULKER_BOX:
			case LIME_SHULKER_BOX:
			case MAGENTA_SHULKER_BOX:
			case ORANGE_SHULKER_BOX:
			case PINK_SHULKER_BOX:
			case PURPLE_SHULKER_BOX:
			case RED_SHULKER_BOX:
			case WHITE_SHULKER_BOX:
			case YELLOW_SHULKER_BOX: {
				if (player.hasPermission("portablocks.use.shulker_box")) {
					if (container.getItemMeta() instanceof BlockStateMeta) {
						BlockStateMeta cMeta = (BlockStateMeta) container.getItemMeta();
						String title = (!cMeta.hasDisplayName()) ? "Shulker Box" : cMeta.getDisplayName();
						if (cMeta.getBlockState() instanceof ShulkerBox) {
							ShulkerBox box = (ShulkerBox) cMeta.getBlockState();
							Inventory inv = Bukkit.createInventory(null, 27, title);
							inv.setContents(box.getInventory().getContents());
							player.openInventory(inv);
							portaShulkerSlot.put(player.getUniqueId(), toRawSlot(player.getInventory().getHeldItemSlot()));
							player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, .1F, 1.0F);
							return true;
						}
					}
				}
				break;
			}
			default: {
				break;
			}
		}
		return false;
	}
	
	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		if (portaECOpen.contains(e.getPlayer().getUniqueId())) {
			e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, .1F, 1.0F);
			portaECOpen.remove(e.getPlayer().getUniqueId());
		}
		if (portaShulkerSlot.containsKey(e.getPlayer().getUniqueId())) {
			ItemStack[] items = e.getInventory().getContents();
			saveShulkerBox(e.getPlayer(), items);
			portaShulkerSlot.remove(e.getPlayer().getUniqueId());
			e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, .1F, 1.0F);
		}
		portaShulkerOnCursor.remove(e.getPlayer().getUniqueId());
	}

	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if (!portaShulkerSlot.containsKey(e.getWhoClicked().getUniqueId())) return;
		// Do not allow moving any shulker into the shulkerbox:
		if (e.getCursor() != null && isShulkerBox(e.getCursor().getType()) && isInShulkerBox(e.getRawSlot())) {
			e.setCancelled(true);
			return;
		}
		// Otherwise always save the inventory because of the action:
		saveShulkerBox(e.getWhoClicked(), e.getInventory().getContents());
		// Close inventory if opened shulker box is dropped:
		if (portaShulkerSlot.get(e.getWhoClicked().getUniqueId()).equals(e.getRawSlot())) {
			// If we are holding it on cursor we need to store this so we can see what happens later.
			if (isPickupAction(e.getAction())) {
				portaShulkerOnCursor.add(e.getWhoClicked().getUniqueId());
				return;
			} else if (e.getAction() == InventoryAction.DROP_ONE_SLOT || e.getAction() == InventoryAction.DROP_ALL_SLOT) {
				//simulate item drop, this is buggy
				dropItem(e.getCurrentItem(), e.getWhoClicked());
				e.setCurrentItem(null);
				e.getWhoClicked().closeInventory();
				return;
			}
		}
		Integer newItemSlot = null;
		// If shulker on cursor is put back down or dropped:
		if (portaShulkerOnCursor.contains(e.getWhoClicked().getUniqueId())) {
			if (e.getAction() == InventoryAction.DROP_ALL_CURSOR
					|| e.getAction() == InventoryAction.DROP_ONE_CURSOR) {
				dropItem(e.getCursor(), e.getWhoClicked());
				e.setCurrentItem(null);
				e.getWhoClicked().setItemOnCursor(null);
				e.getWhoClicked().closeInventory();
				return;
			} else if (isPlaceAction(e.getAction())) {
				newItemSlot = e.getRawSlot();
				portaShulkerOnCursor.remove(e.getWhoClicked().getUniqueId());
			}
		}
		// If sulker is swapped with hotbar:
		if (e.getClick() == ClickType.NUMBER_KEY
				&& (e.getAction() == InventoryAction.HOTBAR_SWAP
				|| e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD)) {
			// If shulker is somehow moved to slot inside shulker box
			if (isInShulkerBox(e.getRawSlot())
					&& e.getWhoClicked().getInventory().getItem(e.getHotbarButton()) != null
					&& isShulkerBox(e.getWhoClicked().getInventory().getItem(e.getHotbarButton()).getType())) {
				e.setCancelled(true);
				return;
			}
			// If mosue click on shulker depending on where we go or came from store new slot of the shulker.
			if (portaShulkerSlot.get(e.getWhoClicked().getUniqueId()).equals(e.getRawSlot())) {
				newItemSlot = toRawSlot(e.getHotbarButton());
			} else if (portaShulkerSlot.get(e.getWhoClicked().getUniqueId()).equals(toRawSlot(e.getHotbarButton()))) {
				newItemSlot = e.getRawSlot();
			}
		}
		// Shifting into inventory of shulker.
		if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
				&& e.getCurrentItem() != null
				&& isShulkerBox(e.getCurrentItem().getType())) {
			// Check if shulker box will be moved from hotbar to inventory
			if (e.getRawSlot() > 53 && e.getRawSlot() < 63) {
				// Move shulker box instead to next free inventory slot
				newItemSlot = moveItemToSlotRange(9, 36, e);
			// Check if shulker box will be moved from inventory to hotbar
			} else if (e.getRawSlot() > 26 && e.getRawSlot() < 54) {
				newItemSlot = moveItemToSlotRange(0, 9, e);
			}
			if (newItemSlot != null && !portaShulkerSlot.get(e.getWhoClicked().getUniqueId()).equals(e.getRawSlot())) {
				newItemSlot = null;
			}
			// Cancel original interaction always in this case.
			e.setCancelled(true);
		}
		// If the shulker box ends up somewhere else; store it.
		if (newItemSlot != null) {
			portaShulkerSlot.put(e.getWhoClicked().getUniqueId(), newItemSlot);
		}
	}

	@EventHandler
	public void onDrag(InventoryDragEvent e) {
		if (portaShulkerSlot.containsKey(e.getWhoClicked().getUniqueId())
				&& isShulkerBox(e.getOldCursor().getType())) {
			// Also no dragging a shulker into a shulker.
			if (e.getRawSlots().stream().anyMatch(a -> a < 27)
					|| e.getRawSlots().size() > 1) {
				e.setCancelled(true);
				return;
			}
			// But we can drag shulker it into normal part of inventory.
			if (portaShulkerOnCursor.contains(e.getWhoClicked().getUniqueId())) {
				portaShulkerSlot.put(e.getWhoClicked().getUniqueId(), toRawSlot((int) e.getInventorySlots().toArray()[0]));
				portaShulkerOnCursor.remove(e.getWhoClicked().getUniqueId());
			}
		}
	}
	
	// Helper methods (Thanks https://github.com/Querz/OpenShulkerBox) 
	
	private void saveShulkerBox(HumanEntity player, ItemStack[] items) {
		ItemStack shulkerbox = player.getInventory().getItem(toSlot(portaShulkerSlot.get(player.getUniqueId())));
		if (shulkerbox != null && shulkerbox.getItemMeta() instanceof BlockStateMeta) {			
			BlockStateMeta cMeta = (BlockStateMeta) shulkerbox.getItemMeta();
			if (cMeta.getBlockState() instanceof ShulkerBox) {
				ShulkerBox box = (ShulkerBox) cMeta.getBlockState();
				box.getInventory().setContents(items);
				cMeta.setBlockState(box);
				shulkerbox.setItemMeta(cMeta);
			}
		}
	}

	private Integer moveItemToSlotRange(int rangeMin, int rangeMax, InventoryClickEvent e) {
		for (int i = rangeMin; i < rangeMax; i++) {
			if (e.getClickedInventory().getItem(i) == null
					|| e.getClickedInventory().getItem(i).getType() == Material.AIR) {
				e.getClickedInventory().setItem(i, e.getCurrentItem());
				e.setCurrentItem(null);
				return toRawSlot(i);
			}
		}
		return null;
	}

	private void dropItem(ItemStack itemStack, HumanEntity player) {
		Item item = player.getWorld().dropItem(player.getEyeLocation(), itemStack);
		item.setVelocity(player.getLocation().getDirection().multiply(0.33));
		item.setPickupDelay(40);
	}

	private boolean isShulkerBox(Material m) {
		switch (m) {
			case SHULKER_BOX:
			case LIGHT_GRAY_SHULKER_BOX:
			case BLACK_SHULKER_BOX:
			case BLUE_SHULKER_BOX:
			case BROWN_SHULKER_BOX:
			case CYAN_SHULKER_BOX:
			case GRAY_SHULKER_BOX:
			case GREEN_SHULKER_BOX:
			case LIGHT_BLUE_SHULKER_BOX:
			case LIME_SHULKER_BOX:
			case MAGENTA_SHULKER_BOX:
			case ORANGE_SHULKER_BOX:
			case PINK_SHULKER_BOX:
			case PURPLE_SHULKER_BOX:
			case RED_SHULKER_BOX:
			case WHITE_SHULKER_BOX:
			case YELLOW_SHULKER_BOX:
				return true;
			default:
				return false;
		}
	}

	private boolean isPlaceAction(InventoryAction action) {
		return action == InventoryAction.PLACE_ALL
				|| action == InventoryAction.PLACE_ONE
				|| action == InventoryAction.PLACE_SOME
				|| action == InventoryAction.SWAP_WITH_CURSOR;
	}

	private boolean isPickupAction(InventoryAction action) {
		return action == InventoryAction.PICKUP_ALL
				|| action == InventoryAction.PICKUP_HALF
				|| action == InventoryAction.PICKUP_ONE
				|| action == InventoryAction.PICKUP_SOME
				|| action == InventoryAction.SWAP_WITH_CURSOR;
	}

	private int toRawSlot(int slot) {
		return slot >= 0 && slot < 9 ? slot + 54 : slot + 18;
	}

	private int toSlot(int rawSlot) {
		return rawSlot >= 54 ? rawSlot - 54 : rawSlot - 18;
	}

	private boolean isInShulkerBox(int rawSlot) {
		return rawSlot >= 0 && rawSlot < 27;
	}

}
