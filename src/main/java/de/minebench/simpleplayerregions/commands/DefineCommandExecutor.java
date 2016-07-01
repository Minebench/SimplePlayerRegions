package de.minebench.simpleplayerregions.commands;

import com.google.common.util.concurrent.ListenableFuture;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Polygonal2DSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldguard.bukkit.commands.AsyncCommandHelper;
import com.sk89q.worldguard.bukkit.commands.task.RegionAdder;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.FlagContext;
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

        Selection selection = plugin.getWorldEdit().getSelection(player);
        if(selection == null) {
            sender.sendMessage(plugin.getMessage("no-selection", "world", player.getWorld().getName()));
            return true;
        }

        RegionManager regions = plugin.getWorldGuard().getRegionContainer().get(player.getWorld());
        if(regions == null) {
            sender.sendMessage(plugin.getMessage("world-not-enabled", "world", player.getWorld().getName()));
            return true;
        }

        String playerName = sender.getName();
        if(args.length > 0 && sender.hasPermission(command.getPermission() + ".others")) {
            playerName = args[0];
        }

        BlockVector pntMin = new BlockVector(selection.getNativeMinimumPoint());
        BlockVector pntMax = new BlockVector(selection.getNativeMaximumPoint());
        int yMin = plugin.getMinY();
        int yMax = plugin.getMaxY();
        if(sender.hasPermission(command.getPermission() + ".setymin")) {
            yMin = selection.getNativeMinimumPoint().getBlockY();
        } else {
            pntMin = pntMin.setY(plugin.getMinY()).toBlockPoint();
        }
        if(sender.hasPermission(command.getPermission() + ".setymax")) {
            yMax = selection.getNativeMaximumPoint().getBlockY();
        } else {
            pntMax = pntMax.setY(plugin.getMaxY()).toBlockPoint();
        }

        ProtectedRegion region;
        String regionBaseName = getRegionName(playerName).toLowerCase();
        String regionName = regionBaseName;
        int i = 0;
        while(regions.hasRegion(regionName)) {
            i++;
            regionName = regionBaseName + "_" + i;
        }
        if(selection instanceof CuboidSelection) {
            region = new ProtectedCuboidRegion(regionName, pntMin, pntMax);
            plugin.getWorldEdit().setSelection(player, new CuboidSelection(player.getWorld(), pntMin, pntMax));
        } else if(selection instanceof Polygonal2DSelection){
            region = new ProtectedPolygonalRegion(regionName, ((Polygonal2DSelection) selection).getNativePoints(), yMin, yMax);
            plugin.getWorldEdit().setSelection(player, new Polygonal2DSelection(player.getWorld(), ((Polygonal2DSelection) selection).getNativePoints(), yMin, yMax));
        } else {
            sender.sendMessage(plugin.getMessage("unsupported-selectiontype", "type", selection.getRegionSelector().getTypeName()));
            return true;
        }

        if(!sender.hasPermission(command.getPermission() + ".overlap")) {
            ApplicableRegionSet set = regions.getApplicableRegions(region);

            if(set.size() > 0) {
                sender.sendMessage(plugin.getMessage("overlapping-regions", "world", player.getWorld().getName()));
                return true;
            }
        }

        if(!sender.hasPermission(command.getPermission() + ".unlimited") && !plugin.checkRegionCount(player, player.getWorld())) {
            sender.sendMessage(plugin.getMessage("too-many-regions", "world", player.getWorld().getName()));
            return true;
        }

        int size = selection.getWidth() > selection.getLength() ? selection.getWidth() : selection.getLength();
        if(!sender.hasPermission(command.getPermission() + ".oversized") && !plugin.checkRegionSize(player, size)) {
            sender.sendMessage(plugin.getMessage("selection-to-big", "world", player.getWorld().getName()));
            return true;
        }

        try {
            region.setFlag(DefaultFlag.TELE_LOC, DefaultFlag.TELE_LOC.parseInput(
                    FlagContext.create().setSender(sender).setInput("here").setObject("region", region).build()
            ));
        } catch (InvalidFlagFormat e) {
            plugin.getLogger().log(Level.SEVERE, "Error while setting teleport flag!", e);
        }

        RegionAdder task = new RegionAdder(plugin.getWorldGuard(), regions, region);
        task.setOwnersInput(new String[]{playerName});
        ListenableFuture<?> future = plugin.getWorldGuard().getExecutorService().submit(task);

        AsyncCommandHelper.wrap(future, plugin.getWorldGuard(), player)
                .formatUsing(regionName)
                .registerWithSupervisor("Adding the region '%s'...")
                .sendMessageAfterDelay("(Please wait... adding '%s'...)")
                .thenRespondWith(
                        "A new region has been made named '%s'.",
                        "Failed to add the region '%s'");

        return true;
    }

    private String getRegionName(String playerName) {
        Calendar cal = Calendar.getInstance();
        return Normal.normalize(plugin.getMessage("regionformat", "playername", playerName, "date", plugin.getDateFormat().format(cal.getTime())));
    }

}
