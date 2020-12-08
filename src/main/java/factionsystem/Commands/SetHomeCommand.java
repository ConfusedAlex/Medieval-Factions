package factionsystem.Commands;

import factionsystem.MedievalFactions;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.*;

public class SetHomeCommand {

    public void setHome(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.sethome") || sender.hasPermission("mf.default")) {
                if (isInFaction(player.getUniqueId(), MedievalFactions.getInstance().factions)) {
                    Faction playersFaction = getPlayersFaction(player.getUniqueId(), MedievalFactions.getInstance().factions);
                    if (playersFaction.isOwner(player.getUniqueId()) || playersFaction.isOfficer(player.getUniqueId())) {

                        if (isClaimed(player.getLocation().getChunk(), MedievalFactions.getInstance().claimedChunks)) {
                            ClaimedChunk chunk = getClaimedChunk(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ(), player.getWorld().getName(), MedievalFactions.getInstance().claimedChunks);
                            if (chunk.getHolder().equalsIgnoreCase(playersFaction.getName())) {
                                playersFaction.setFactionHome(player.getLocation());
                                player.sendMessage(ChatColor.GREEN + "Faction home set!");
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "You can't set your faction home on land your faction hasn't claimed!");
                            }
                        }
                        else {
                            player.sendMessage(ChatColor.RED + "This land isn't claimed!");
                        }

                    }
                    else {
                        player.sendMessage(ChatColor.RED + "You need to be the owner of your faction or an officer of your faction to use this command.");
                    }
                }
                else {
                    player.sendMessage(ChatColor.RED + "You need to be in a faction to use this command.");
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + "Sorry! You need the following permission to use this command: 'mf.sethome'");
            }
        }
    }
}
