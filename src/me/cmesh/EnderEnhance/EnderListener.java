package me.cmesh.EnderEnhance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitScheduler;

public class EnderListener implements Listener {
	private static final BlockFace[] hopperMap = {
		BlockFace.DOWN,		//0
		BlockFace.SELF, 	//none
		BlockFace.NORTH,	//2
		BlockFace.SOUTH,	//3
		BlockFace.WEST,		//4
		BlockFace.EAST,		//5
	};
	
	private static final BlockFace[] surrounding = {
		BlockFace.UP,
		BlockFace.NORTH,
		BlockFace.EAST,
		BlockFace.SOUTH,
		BlockFace.WEST,
	};
	
	private void Move(ItemStack stack, Inventory in, Inventory out) {
		if (stack == null || stack.getType() == Material.AIR) {
			return;
		}
		
		HashMap<Integer, ItemStack> leftoverList = out.addItem(stack);
		
		if (!leftoverList.values().isEmpty()) {
			ItemStack leftover = leftoverList.get(0);
			
			if (leftover.getAmount() != stack.getAmount()) {
				ItemStack toMove = leftover.clone();
				toMove.setAmount(stack.getAmount() - leftover.getAmount());
				
				in.removeItem(toMove);
			} else {
			}
		} else {
			in.removeItem(stack);
		}
	}
	private void MoveInventory(Inventory in, Inventory out) {
		for(ItemStack stack : in.getContents()) {
			Move(stack, in, out);
		}
	}
	
	private class EnderRunner implements Runnable {
		
		private UUID playerId;
		private int task;
		
		public EnderRunner(UUID uuid) {
			playerId = uuid;
			BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
			task = scheduler.scheduleSyncRepeatingTask(EnderEnhance.Instance, this, 0L, 4L);
		}
		
		public void Stop() {
			BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
			scheduler.cancelTask(task);
		}
		
		@Override
		public void run() {
			Player p = Bukkit.getPlayer(playerId);
			if (p == null) {
				//cannot access inv if player is offline
				//TODO maintian a separate inv for our chest
				return;
			}
			
			
			List<Location> chests = new ArrayList<Location>();
			for (Location chestloc : EnderSerial.GetByUUID(playerId)) {
				if(chestloc.getChunk().isLoaded()) {
					chests.add(chestloc);
				}
			}
			
			/*
			 * From enderchest to hopper
			 */
			for(Location chestloc : chests) {
				Block down = chestloc.getBlock().getRelative(BlockFace.DOWN);//hopper?
				if (down.getType() == Material.HOPPER) {
					Hopper h = (Hopper)down.getState();
					MoveInventory(p.getEnderChest(), h.getInventory());
				}
			}
			
			/*
			 * From Hopper to enderchest
			 */
			for(Location chestloc : chests) {
				for (BlockFace face : surrounding) {
					Block block = chestloc.getBlock().getRelative(face);
					if (block.getType() == Material.HOPPER) {
						Hopper h = (Hopper)block.getState();
						block = getHopperBlock(h);
						if (block.getType() == Material.ENDER_CHEST && playerId.equals(ownerEnderChest(block))) {
							MoveInventory(h.getInventory(), p.getEnderChest());
						}
					}
				}
			}
		}
	}
	
	private HashMap<UUID, EnderRunner> runners;
	
	public void StartRunners() {
		runners = new HashMap<UUID, EnderRunner>();
		for(UUID id : EnderSerial.GetAllUUID()) {
			runners.put(id, new EnderRunner(id));
		}
	}
	
	public void StopRunners() {
		for(UUID id : EnderSerial.GetAllUUID()) {
			runners.get(id).Stop();
		}
	}
	
	
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerRightClick(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (event.getAction() == Action.RIGHT_CLICK_AIR && player.getItemInHand().getType() == Material.ENDER_CHEST) {
			player.openInventory(player.getEnderChest());
		}
	}
	
	private void markEnderChest(Block block, UUID uuid) {
		block.setMetadata("owner", new FixedMetadataValue(EnderEnhance.Instance, uuid));
	}
	private UUID ownerEnderChest(Block block) {
		List<MetadataValue> meta = block.getMetadata("owner");
		if (meta.isEmpty()) {
			return null;
		}
		return (UUID)meta.get(0).value();
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerPlaceBlock(BlockPlaceEvent ev) {
		Player player = ev.getPlayer();
		if (player != null && ev.getBlock().getType() == Material.ENDER_CHEST) {
			UUID uuid = player.getUniqueId();
			EnderSerial.AddChest(uuid, ev.getBlock().getLocation());
			if (!runners.containsKey(uuid)) {
				runners.put(uuid, new EnderRunner(uuid));
			}
			markEnderChest(ev.getBlock(), uuid);
		}
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerBreakBlock(BlockBreakEvent ev) {
		if (ev.getBlock().getType() == Material.ENDER_CHEST) {
			UUID uuid = ownerEnderChest(ev.getBlock());
			if (uuid != null) {
				EnderSerial.RemoveChest(uuid, ev.getBlock().getLocation());
				if (EnderSerial.GetByUUID(uuid).isEmpty()) {
					runners.get(uuid).Stop();
					runners.remove(uuid);
				}
			}
		}
	}
	
	private Block getHopperBlock(Hopper h) {
		int i = h.getData().toString().charAt(7) - '0';
		if (i <= 5) {
			return h.getBlock().getRelative(hopperMap[i]);
		} else {
			//Something strange happened, I keep getting the number 8  when I look inside a chest above a hopper?
			return h.getBlock();
		}
	}
}