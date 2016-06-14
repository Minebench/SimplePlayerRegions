package de.minebench.simpleplayerregions;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import de.minebench.simpleplayerregions.commands.DefineCommandExecutor;
import de.minebench.simpleplayerregions.commands.PluginCommandExecutor;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Copyright 2016 Max Lee (https://github.com/Phoenix616/)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Mozilla Public License as published by
 * the Mozilla Foundation, version 2.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Mozilla Public License v2.0 for more details.
 *
 * You should have received a copy of the Mozilla Public License v2.0
 * along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
 */

public class SimplePlayerRegions extends JavaPlugin {

    private WorldEditPlugin worldEdit;
    private WorldGuardPlugin worldGuard;

    private int yMax;
    private int yMin;
    private SimpleDateFormat dateFormat;

    private Map<String, Integer> permGroups;

    public void onEnable() {
        worldEdit = (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
        worldGuard = (WorldGuardPlugin) getServer().getPluginManager().getPlugin("WorldGuard");
        loadConfig();
        getCommand(getName().toLowerCase()).setExecutor(new PluginCommandExecutor(this));
        getCommand("define").setExecutor(new DefineCommandExecutor(this));
    }

    public void loadConfig() {
        saveDefaultConfig();
        reloadConfig();
        yMax = getConfig().getInt("ymax");
        yMin = getConfig().getInt("ymin");
        try {
            dateFormat = new SimpleDateFormat(getConfig().getString("dateformat"));
        } catch(IllegalArgumentException e) {
            getLogger().log(Level.SEVERE, "Invalid dateformat in config! (" + getConfig().getString("dateformat") + ") Setting to default!", e);
            dateFormat = new SimpleDateFormat(getConfig().getDefaults().getString("dateformat"));
        }
        permGroups = new HashMap<>();
        ConfigurationSection groupSection = getConfig().getConfigurationSection("perworldcounts");
        for(String group : groupSection.getKeys(false)) {
            int count = groupSection.getInt(group, 0);
            if(count > 0) {
                permGroups.put(group.toLowerCase(), count);
                Permission perm = new Permission(getName().toLowerCase() + ".count." + group.toLowerCase());
                try {
                    getServer().getPluginManager().addPermission(perm);
                } catch(IllegalArgumentException e) {
                    // Perm was already defined
                }
            }
        }
    }

    public String getMessage(String key, String... repl) {
        String msg =  getConfig().getString(key);
        if(msg == null) {
            return ChatColor.RED + getName() + ": Unknown language key " + ChatColor.GOLD + key;
        }
        for(int i = 0; i + 1 < repl.length; i += 2) {
            msg = msg.replace("%" + repl[i] + "%", repl[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public WorldEditPlugin getWorldEdit() {
        return worldEdit;
    }

    public WorldGuardPlugin getWorldGuard() {
        return worldGuard;
    }

    public int getMaxY() {
        return yMax;
    }

    public int getMinY() {
        return yMin;
    }

    public SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    public boolean checkRegionCount(Player player, World world) {
        RegionManager regions = getWorldGuard().getRegionManager(world);
        if(regions == null) {
            return true;
        }
        int count = regions.getRegionCountOfPlayer(getWorldGuard().wrapPlayer(player));

        for(Map.Entry<String, Integer> group : permGroups.entrySet()) {
            if(group.getValue() > count && player.hasPermission(getName().toLowerCase() + ".count." + group.getKey())) {
                return true;
            }
        }

        return false;
    }
}
