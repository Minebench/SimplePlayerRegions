package de.minebench.simpleplayerregions.commands;

import de.minebench.simpleplayerregions.SimplePlayerRegions;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

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
public class PluginCommandExecutor implements CommandExecutor {
    private final SimplePlayerRegions plugin;

    public PluginCommandExecutor(SimplePlayerRegions plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length > 0 && sender.hasPermission(command.getPermission() + "." + args[0].toLowerCase())) {
            if("reload".equalsIgnoreCase(args[0])) {
                plugin.reloadConfig();
                sender.sendMessage(ChatColor.YELLOW + "Config reloaded!");
                return true;
            }
        }

        sender.sendMessage(ChatColor.AQUA + plugin.getName() + " v" + plugin.getDescription().getVersion() + " by " + plugin.getDescription().getAuthors().get(0));
        return false;
    }
}
