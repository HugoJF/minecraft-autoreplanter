package com.denerdtv;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

import static com.denerdtv.AutoReplanter.SERVER_PREFIX;
import static org.bukkit.ChatColor.*;

public class AutoReplanterCommand implements Listener, CommandExecutor, TabCompleter {
    private final AutoReplanter ar;

    public AutoReplanterCommand() {
        this.ar = AutoReplanter.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase(AutoReplanter.COMMAND)) {
            Player p = (Player) sender;

            if (args.length == 1 && args[0].equals("on")) {
                ar.setEnabled(p, true);
                p.sendMessage(SERVER_PREFIX + "AutoReplanter is now turned " + GREEN + "ON");
                return true;
            } else if (args.length == 1 && args[0].equals("off")) {
                ar.setEnabled(p, false);
                p.sendMessage(SERVER_PREFIX + "AutoReplanter is now turned " + RED + "OFF");
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("visible")) {
                ar.setShowing(true);
                p.sendMessage(SERVER_PREFIX + "AutoReplanter is now " + GREEN + "VISIBLE");
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("hide")) {
                ar.setShowing(false);
                p.sendMessage(SERVER_PREFIX + "AutoReplanter is now " + RED + "HIDDEN");
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
                p.sendMessage(SERVER_PREFIX + "Currently tracking " + ar.getLocationCount() + " farms");
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("chests")) {
                p.sendMessage(SERVER_PREFIX + "Currently working with " + AutoReplanter.getInstance().getChests().size() + " chests");
                return true;
            } else {
                p.sendMessage(SERVER_PREFIX + ("Usage: AutoReplanter <" + RED + "on|off|visible|hide|status|chests" + RESET + ">").replaceAll("\\|", RESET + "|" + RED));
            }
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> cmds = new ArrayList<>();
        Player p = (Player) sender;

        // Generate TAB complete for AutoReplant
        if (cmd.getName().equalsIgnoreCase(AutoReplanter.COMMAND)) {
            generateEnabledTabComplete(cmds, p);
            generateVisibilityTabComplete(cmds);
            generateStatusTabComplete(cmds);
            generateChestsTabComplete(cmds);
        }

        // Filter entries based on already typed arguments
        filterBasedOnHints(args, cmds);

        return cmds;
    }

    private void generateChestsTabComplete(List<String> cmds) {
        cmds.add("chests");
    }

    private void generateStatusTabComplete(List<String> cmds) {
        cmds.add("status");
    }

    private void generateVisibilityTabComplete(List<String> cmds) {
        if (!ar.getShowing()) cmds.add("visible");
        if (ar.getShowing()) cmds.add("hide");
    }

    private void generateEnabledTabComplete(List<String> cmds, Player p) {
        boolean off = ar.getEnabled(p);

        if (!off) cmds.add("on");
        if (off) cmds.add("off");
    }

    private void filterBasedOnHints(String[] args, List<String> cmds) {
        if (args.length >= 1) {
            for (int i = 0; i < cmds.size(); i++) {
                if (!cmds.get(i).startsWith(args[0])) {
                    cmds.remove(i--);
                }
            }
        }
    }

}
