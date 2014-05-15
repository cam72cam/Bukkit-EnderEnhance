package me.cmesh.EnderEnhance;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;

public class EnderEnhance extends JavaPlugin {
	private EnderListener listener;
	protected String DataPath;
	protected static EnderEnhance Instance; 
	
	public EnderEnhance() {
		Instance = this;
		
		SetupFolder();
		listener = new EnderListener();
	}
	
	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(listener, this);
		listener.StartRunners();
	}
	
	public void onDisable() {
		listener.StopRunners();
	}
	
	private void SetupFolder() {
		File folder = new File(this.getDataFolder().getAbsolutePath());
		
		if (! folder.isDirectory()) {
			if(folder.exists()) {
				folder.delete();
			}
			folder.mkdir();
		}
		DataPath = folder.getAbsolutePath() + File.separator;
	}
}
