package me.cmesh.EnderEnhance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
	
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.google.common.base.Charsets;

public class EnderSerial {
	private static HashMap<UUID, List<Location>> ownership = new HashMap<UUID, List<Location>>();
	
	private static  List<Location> loadFile(UUID uuid) {
		List<Location> res = new ArrayList<Location>();
		try {
			List<String> lines = Files.readAllLines(Paths.get(EnderEnhance.Instance.DataPath + uuid), Charsets.UTF_8);
			for (String line : lines) {
				Scanner scanner = new Scanner(line);
				int x,y,z;
				x = scanner.nextInt();
				y = scanner.nextInt();
				z = scanner.nextInt();
				String worldString = scanner.next();
				UUID worldId = UUID.fromString(worldString);
				if (worldId != null) {
					World w = Bukkit.getServer().getWorld(worldId);
					if (w != null) {
						res.add(new Location(w, x, y, z));
					}
				}
				scanner.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}
	private static void saveFile(UUID uuid, List<Location> locs) {
		if (locs.isEmpty()) {
			removeFile(uuid);
			return;
		}
		
		try {
			PrintWriter writer = new PrintWriter(EnderEnhance.Instance.DataPath + uuid, "UTF-8");
			for (Location loc : locs) {
				writer.println(String.format("%d %d %d %s", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getUID().toString()));
			}
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			//I am lazy
			e.printStackTrace();
		}
	}
	private static void removeFile(UUID uuid) {
		File f = new File(EnderEnhance.Instance.DataPath + uuid);
		if (f.isFile()) {
			f.delete();
		}
	}
	
	public static void AddChest(UUID uuid, Location location) {
		List<Location> list;
		if (!ownership.containsKey(uuid)) {
			list = loadFile(uuid);
			if (list == null) {
				list = new ArrayList<Location>();
			}
			ownership.put(uuid, list);
		}
		list = ownership.get(uuid);
		
		list.add(location);
		
		saveFile(uuid, list);
	}
	
	public static void RemoveChest(UUID uuid, Location location) {
		List<Location> list = ownership.get(uuid);
		list.remove(location);
		saveFile(uuid, list);
	}
	
	public static boolean OwnsChest(UUID uuid, Location location) {
		if (ownership.containsKey(uuid)) {
			for (Location loc : ownership.get(uuid)) {
				if (loc.equals(location)) {
					return true;
				}
			}
		}
		return false;
	}

	public static List<UUID> GetAllUUID() {
		List<UUID> list = new ArrayList<UUID>();
		
		File folder = new File(EnderEnhance.Instance.DataPath);
		for (File fileEntry : folder.listFiles()) {
			UUID uuid = UUID.fromString(fileEntry.getName());
			if (uuid != null) {
				list.add(uuid);
			}
		}
		
		return list;
	}

	public static List<Location> GetByUUID(UUID uuid) {
		List<Location> list;
		if (!ownership.containsKey(uuid)) {
			list = loadFile(uuid);
			if (list == null) {
				list = new ArrayList<Location>();
			}
			ownership.put(uuid, list);
		}
		return ownership.get(uuid);
	}
	public static UUID GetOwner(Location location) {
		for (UUID id : ownership.keySet()) {
			if (OwnsChest(id, location)) {
				return id;
			}
		}
		return null;
	}
}
