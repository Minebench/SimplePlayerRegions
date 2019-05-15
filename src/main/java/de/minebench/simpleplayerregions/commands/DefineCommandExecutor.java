package de.minebench.simpleplayerregions.commands;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.command.util.AsyncCommandBuilder;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.commands.task.RegionAdder;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.util.Normal;
import de.minebench.simpleplayerregions.SimplePlayerRegions;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Calendar;
import java.util.logging.Level;

/**
 * Copyright 2016 Max Lee (https://github.com/Phoenix616/)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Mozilla Public License as published by
 * the Mozilla Foundation, version 2.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Mozilla Public License v2.0 for more details.
 * <p/>
 * You should have received a copy of the Mozilla Public License v2.0
 * along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
 */
public class DefineCommandExecutor implements CommandExecutor {
    private final SimplePlayerRegions plugin;

    public DefineCommandExecutor(SimplePlayerRegions plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player!");
            return true;
        }

        Player player = (Player) sender;
        BukkitPlayer wePlayer = BukkitAdapter.adapt(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);
        Region selection = null;
        try {
            selection = session.getSelection(wePlayer.getWorld());
        } catch (IncompleteRegionException e) {
            e.printStackTrace();
        }
        if(selection == null) {
            sender.sendMessage(plugin.getMessage("no-selection", "world", wePlayer.getWorld().getName()));
            return true;
        }

        RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(wePlayer.getWorld());
        if(regions == null) {
            sender.sendMessage(plugin.getMessage("world-not-enabled", "world", wePlayer.getWorld().getName()));
            return true;
        }

        String playerName = sender.getName();
        if(args.length > 0 && sender.hasPermission(command.getPermission() + ".others")) {
            playerName = args[0];
        }

        BlockVector3 pntMin = selection.getMinimumPoint();
        BlockVector3 pntMax = selection.getMaximumPoint();
        int yMin = plugin.getMinY();
        int yMax = plugin.getMaxY();
        if(sender.hasPermission(command.getPermission() + ".setymin")) {
            yMin = selection.getMinimumPoint().getBlockY();
        } else {
            pntMin = pntMin.withY(plugin.getMinY());
        }
        if(sender.hasPermission(command.getPermission() + ".setymax")) {
            yMax = selection.getMaximumPoint().getBlockY();
        } else {
            pntMax = pntMax.withY(plugin.getMaxY());
        }

        ProtectedRegion region;
        String regionBaseName = getRegionName(playerName).toLowerCase();
        String regionName = regionBaseName;
        int i = 0;
        while(regions.hasRegion(regionName)) {
            i++;
            regionName = regionBaseName + "_" + i;
        }
        RegionSelector regionSelector;
        if(selection instanceof CuboidRegion) {
            region = new ProtectedCuboidRegion(regionName, pntMin, pntMax);
            regionSelector = new CuboidRegionSelector(wePlayer.getWorld(), pntMin, pntMax);
        } else if(selection instanceof Polygonal2DRegion){
            region = new ProtectedPolygonalRegion(regionName, ((Polygonal2DRegion) selection).getPoints(), yMin, yMax);
            regionSelector = new Polygonal2DRegionSelector(wePlayer.getWorld(), ((Polygonal2DRegion) selection).getPoints(), yMin, yMax);
        } else {
            sender.sendMessage(plugin.getMessage("unsupported-selectiontype", "type", session.getRegionSelector(wePlayer.getWorld()).getTypeName()));
            return true;
        }
        session.setRegionSelector(wePlayer.getWorld(), regionSelector);

        if(!sender.hasPermission(command.getPermission() + ".overlap")) {
            ApplicableRegionSet set = regions.getApplicableRegions(region);

            if(set.size() > 0) {
                sender.sendMessage(plugin.getMessage("overlapping-regions", "world", wePlayer.getWorld().getName()));
                return true;
            }
        }

        if(!sender.hasPermission(command.getPermission() + ".unlimited") && !plugin.checkRegionCount(player, player.getWorld())) {
            sender.sendMessage(plugin.getMessage("too-many-regions", "world", wePlayer.getWorld().getName()));
            return true;
        }

        int size = selection.getWidth() > selection.getLength() ? selection.getWidth() : selection.getLength();
        if(!sender.hasPermission(command.getPermission() + ".oversized") && !plugin.checkRegionSize(player, size)) {
            sender.sendMessage(plugin.getMessage("selection-to-big", "world", wePlayer.getWorld().getName()));
            return true;
        }

        try {
            region.setFlag(Flags.TELE_LOC, Flags.TELE_LOC.parseInput(
                    FlagContext.create().setSender(wePlayer).setInput("here").setObject("region", region).build()
            ));
        } catch (InvalidFlagFormat e) {
            plugin.getLogger().log(Level.SEVERE, "Error while setting teleport flag!", e);
        }

        RegionAdder task = new RegionAdder(regions, region);
        task.setOwnersInput(new String[]{playerName});

        AsyncCommandBuilder.wrap(task, wePlayer)
                .registerWithSupervisor(WorldGuard.getInstance().getSupervisor(), String.format("Adding the region '%s'...", region.getId()))
                .onSuccess(String.format("A new region has been made named '%s'.", region.getId()), null)
                .sendMessageAfterDelay("(Please wait... adding '%s'...)")
                .onFailure(String.format("Failed to add the region '%s'", region.getId()), WorldGuard.getInstance().getExceptionConverter())
                .buildAndExec(WorldGuard.getInstance().getExecutorService());
        return true;
    }

    private String getRegionName(String playerName) {
        Calendar cal = Calendar.getInstance();
        return Normal.normalize(plugin.getMessage("regionformat", "playername", playerName, "date", plugin.getDateFormat().format(cal.getTime())));
    }

}
