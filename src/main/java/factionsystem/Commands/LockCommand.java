package factionsystem.Commands;

import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LockCommand {

    Main main = null;

    public LockCommand(Main plugin) {
        main = plugin;
    }

    public void lockBlock(CommandSender sender, String[] args) {
        // check if player
        if (sender instanceof Player) {

            Player player = (Player) sender;

            // check if has permission
            if (player.hasPermission("mf.lock") || player.hasPermission("mf.default")) {

                // check if argument exists
                if (args.length > 0) {

                    // cancel lock status if first argument is "cancel"
                    if (args[0].equalsIgnoreCase("cancel")) {
                        if (main.lockingPlayers.contains(player.getName())) {
                            main.lockingPlayers.remove(player.getName());
                            return;
                        }
                    }

                    // check that player has not already invoked this command without locking something
                    if (!main.lockingPlayers.contains(player.getName())) {
                        // add player to playersAboutToLockSomething list
                        main.lockingPlayers.add(player.getName());

                        // inform them they need to right click the block that they want to lock or type /mf lock cancel to cancel it
                        player.sendMessage(ChatColor.GREEN + "Right click a chest or door to lock it! (Type /mf lock cancel to cancel)");
                    }

                }

            }
            else {
                player.sendMessage(ChatColor.RED + "Sorry! In order to use this command you need the following permission: 'mf.lock'");
            }

        }
    }

}
