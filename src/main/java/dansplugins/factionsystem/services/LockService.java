/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.services;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import dansplugins.factionsystem.objects.domain.LockedBlock;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import preponderous.ponder.minecraft.bukkit.tools.BlockChecker;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 */
public class LockService {
    private final PersistentData persistentData;
    private final LocaleService localeService;
    private final BlockChecker blockChecker;
    private final EphemeralData ephemeralData;

    public LockService(PersistentData persistentData, LocaleService localeService, BlockChecker blockChecker, EphemeralData ephemeralData) {
        this.persistentData = persistentData;
        this.localeService = localeService;
        this.blockChecker = blockChecker;
        this.ephemeralData = ephemeralData;
    }

    public void handleLockingBlock(PlayerInteractEvent event, Player player, Block clickedBlock) {
        // if chunk is claimed
        ClaimedChunk chunk = persistentData.getChunkDataAccessor().getClaimedChunk(Objects.requireNonNull(event.getClickedBlock()).getLocation().getChunk());
        if (chunk != null) {

            // if claimed by other faction
            if (!chunk.getHolder().equalsIgnoreCase(persistentData.getPlayersFaction(player.getUniqueId()).getName())) {
                player.sendMessage(ChatColor.RED + localeService.get("CanOnlyLockInFactionTerritory"));
                event.setCancelled(true);
                return;
            }

            // if already locked
            if (persistentData.isBlockLocked(clickedBlock)) {
                player.sendMessage(ChatColor.RED + localeService.get("BlockAlreadyLocked"));
                event.setCancelled(true);
                return;
            }

            // block type check
            if (blockChecker.isDoor(clickedBlock) || blockChecker.isChest(clickedBlock) || blockChecker.isGate(clickedBlock) || blockChecker.isBarrel(clickedBlock) || blockChecker.isTrapdoor(clickedBlock) || blockChecker.isFurnace(clickedBlock) || blockChecker.isAnvil(clickedBlock)) {

                // specific to chests because they can be single or double.
                if (blockChecker.isChest(clickedBlock)) {
                    InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
                    if (holder instanceof DoubleChest) {
                        // chest multi-lock
                        DoubleChest doubleChest = (DoubleChest) holder;
                        Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                        Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                        LockedBlock left = new LockedBlock(player.getUniqueId(), persistentData.getPlayersFaction(player.getUniqueId()).getName(), leftChest.getX(), leftChest.getY(), leftChest.getZ(), leftChest.getWorld().getName());
                        persistentData.addLockedBlock(left);

                        lock1x1Block(player, rightChest);
                    } else {
                        // lock single chest
                        lock1x1Block(player, clickedBlock);
                    }
                }

                // door multi-lock (specific to doors because they have two block heights but you could have clicked either block).
                if (blockChecker.isDoor(clickedBlock)) {
                    // lock initial block
                    LockedBlock initial = new LockedBlock(player.getUniqueId(), persistentData.getPlayersFaction(player.getUniqueId()).getName(), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ(), clickedBlock.getWorld().getName());
                    persistentData.addLockedBlock(initial);
                    // check block above
                    if (blockChecker.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                        LockedBlock newLockedBlock2 = new LockedBlock(player.getUniqueId(), persistentData.getPlayersFaction(player.getUniqueId()).getName(), clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ(), clickedBlock.getWorld().getName());
                        persistentData.addLockedBlock(newLockedBlock2);
                    }
                    // check block below
                    if (blockChecker.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                        LockedBlock newLockedBlock2 = new LockedBlock(player.getUniqueId(), persistentData.getPlayersFaction(player.getUniqueId()).getName(), clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ(), clickedBlock.getWorld().getName());
                        persistentData.addLockedBlock(newLockedBlock2);
                    }

                    player.sendMessage(ChatColor.GREEN + localeService.get("Locked"));
                    ephemeralData.getLockingPlayers().remove(player.getUniqueId());
                }

                // Remainder of lockable blocks are only 1x1 so generic code will suffice.
                if (blockChecker.isGate(clickedBlock) || blockChecker.isBarrel(clickedBlock) || blockChecker.isTrapdoor(clickedBlock) || blockChecker.isFurnace(clickedBlock) || blockChecker.isAnvil(clickedBlock)) {
                    lock1x1Block(player, clickedBlock);
                }

                event.setCancelled(true);
            } else {
                player.sendMessage(ChatColor.RED + localeService.get("CanOnlyLockSpecificBlocks"));
            }
        } else {
            player.sendMessage(ChatColor.RED + localeService.get("CanOnlyLockBlocksInClaimedTerritory"));
            event.setCancelled(true);
        }
    }

    private void lock1x1Block(Player player, Block clickedBlock) {
        LockedBlock block = new LockedBlock(player.getUniqueId(), persistentData.getPlayersFaction(player.getUniqueId()).getName(),
                clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ(), clickedBlock.getWorld().getName());
        persistentData.addLockedBlock(block);
        player.sendMessage(ChatColor.GREEN + localeService.get("Locked"));
        ephemeralData.getLockingPlayers().remove(player.getUniqueId());
    }

    public void handleUnlockingBlock(PlayerInteractEvent event, Player player, Block clickedBlock) {
        // if locked
        if (persistentData.isBlockLocked(clickedBlock)) {
            if (persistentData.getLockedBlock(clickedBlock).getOwner().equals(player.getUniqueId())
                    || ephemeralData.getForcefullyUnlockingPlayers().contains(player.getUniqueId())) {

                if (blockChecker.isChest(clickedBlock)) {
                    InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
                    if (holder instanceof DoubleChest) {
                        // chest multi-unlock
                        DoubleChest doubleChest = (DoubleChest) holder;
                        Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                        Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                        // unlock leftChest and rightChest
                        persistentData.removeLockedBlock(leftChest);
                        persistentData.removeLockedBlock(rightChest);

                        player.sendMessage(ChatColor.GREEN + localeService.get("AlertUnlocked"));
                        ephemeralData.getUnlockingPlayers().remove(player.getUniqueId());
                    } else {
                        // unlock single chest
                        persistentData.removeLockedBlock(clickedBlock);
                        player.sendMessage(ChatColor.GREEN + localeService.get("AlertUnlocked"));
                        ephemeralData.getUnlockingPlayers().remove(player.getUniqueId());
                    }
                }

                // door multi-unlock
                if (blockChecker.isDoor(clickedBlock)) {
                    // unlock initial block
                    persistentData.removeLockedBlock(clickedBlock);
                    // check block above
                    if (blockChecker.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                        persistentData.removeLockedBlock(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()));
                    }
                    // check block below
                    if (blockChecker.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                        persistentData.removeLockedBlock(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()));
                    }

                    player.sendMessage(ChatColor.GREEN + localeService.get("AlertUnlocked"));
                    ephemeralData.getUnlockingPlayers().remove(player.getUniqueId());
                }

                // single block size lock logic.
                if (blockChecker.isGate(clickedBlock) || blockChecker.isBarrel(clickedBlock) || blockChecker.isTrapdoor(clickedBlock) || blockChecker.isFurnace(clickedBlock)) {
                    persistentData.removeLockedBlock(clickedBlock);

                    player.sendMessage(ChatColor.GREEN + localeService.get("AlertUnlocked"));
                    ephemeralData.getUnlockingPlayers().remove(player.getUniqueId());
                }

                // remove player from forcefully unlocking players list if they are in it
                ephemeralData.getForcefullyUnlockingPlayers().remove(player.getUniqueId());

                event.setCancelled(true);
            }
        } else {
            player.sendMessage(ChatColor.RED + localeService.get("BlockIsNotLocked"));
            event.setCancelled(true);
        }
    }

    public void handleGrantingAccess(PlayerInteractEvent event, Block clickedBlock, Player player) {
        UUIDChecker uuidChecker = new UUIDChecker();

        // if not owner
        if (persistentData.getLockedBlock(clickedBlock).getOwner() != player.getUniqueId()) {
            player.sendMessage(ChatColor.RED + localeService.get("NotTheOwnerOfThisBlock"));
            return;
        }

        // if chest
        if (blockChecker.isChest(clickedBlock)) {
            InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
            if (holder instanceof DoubleChest) { // if double chest
                // grant access to both chests
                DoubleChest doubleChest = (DoubleChest) holder;
                Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                persistentData.getLockedBlock(leftChest).addToAccessList(ephemeralData.getPlayersGrantingAccess().get(player.getUniqueId()));
                persistentData.getLockedBlock(rightChest).addToAccessList(ephemeralData.getPlayersGrantingAccess().get(player.getUniqueId()));

                player.sendMessage(ChatColor.GREEN + String.format(localeService.get("AlertAccessGrantedTo"), uuidChecker.findPlayerNameBasedOnUUID(ephemeralData.getPlayersGrantingAccess().get(player.getUniqueId()))));
                ephemeralData.getPlayersGrantingAccess().remove(player.getUniqueId());
            } else { // if single chest
                // grant access to single chest
                persistentData.getLockedBlock(clickedBlock).addToAccessList(ephemeralData.getPlayersGrantingAccess().get(player.getUniqueId()));
                player.sendMessage(ChatColor.GREEN + String.format(localeService.get("AlertAccessGrantedTo"), uuidChecker.findPlayerNameBasedOnUUID(ephemeralData.getPlayersGrantingAccess().get(player.getUniqueId()))));
                ephemeralData.getPlayersGrantingAccess().remove(player.getUniqueId());
            }

        }

        // if door
        if (blockChecker.isDoor(clickedBlock)) {
            // grant access to initial block
            persistentData.getLockedBlock(clickedBlock).addToAccessList(ephemeralData.getPlayersGrantingAccess().get(player.getUniqueId()));
            // check block above
            if (blockChecker.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                persistentData.getLockedBlock(clickedBlock).addToAccessList(ephemeralData.getPlayersGrantingAccess().get(player.getUniqueId()));
            }
            // check block below
            if (blockChecker.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                persistentData.getLockedBlock(clickedBlock).addToAccessList(ephemeralData.getPlayersGrantingAccess().get(player.getUniqueId()));
            }

            player.sendMessage(ChatColor.GREEN + String.format(localeService.get("AlertAccessGrantedTo"), uuidChecker.findPlayerNameBasedOnUUID(ephemeralData.getPlayersGrantingAccess().get(player.getUniqueId()))));
            ephemeralData.getPlayersGrantingAccess().remove(player.getUniqueId());
        }

        // if gate (or single-block sized lock)
        if (blockChecker.isGate(clickedBlock) || blockChecker.isBarrel(clickedBlock) || blockChecker.isTrapdoor(clickedBlock) || blockChecker.isFurnace(clickedBlock)) {
            persistentData.getLockedBlock(clickedBlock).addToAccessList(ephemeralData.getPlayersGrantingAccess().get(player.getUniqueId()));

            player.sendMessage(ChatColor.GREEN + String.format(localeService.get("AlertAccessGrantedTo"), uuidChecker.findPlayerNameBasedOnUUID(ephemeralData.getPlayersGrantingAccess().get(player.getUniqueId()))));
            ephemeralData.getPlayersGrantingAccess().remove(player.getUniqueId());
        }

        event.setCancelled(true);
    }

    public void handleCheckingAccess(PlayerInteractEvent event, LockedBlock lockedBlock, Player player) {
        UUIDChecker uuidChecker = new UUIDChecker();
        player.sendMessage(ChatColor.AQUA + localeService.get("FollowingPlayersHaveAccess"));
        for (UUID playerUUID : lockedBlock.getAccessList()) {
            player.sendMessage(ChatColor.AQUA + " - " + uuidChecker.findPlayerNameBasedOnUUID(playerUUID));
        }
        ephemeralData.getPlayersCheckingAccess().remove(player.getUniqueId());
        event.setCancelled(true);
    }

    public void handleRevokingAccess(PlayerInteractEvent event, Block clickedBlock, Player player) {
        UUIDChecker uuidChecker = new UUIDChecker();

        // if not owner
        if (persistentData.getLockedBlock(clickedBlock).getOwner() != player.getUniqueId()) {
            player.sendMessage(ChatColor.RED + localeService.get("NotTheOwnerOfThisBlock"));
            return;
        }

        // if chest
        if (blockChecker.isChest(clickedBlock)) {
            InventoryHolder holder = ((Chest) clickedBlock.getState()).getInventory().getHolder();
            if (holder instanceof DoubleChest) { // if double chest
                // revoke access to both chests
                DoubleChest doubleChest = (DoubleChest) holder;
                Block leftChest = ((Chest) doubleChest.getLeftSide()).getBlock();
                Block rightChest = ((Chest) doubleChest.getRightSide()).getBlock();

                persistentData.getLockedBlock(leftChest).removeFromAccessList(ephemeralData.getPlayersRevokingAccess().get(player.getUniqueId()));
                persistentData.getLockedBlock(rightChest).removeFromAccessList(ephemeralData.getPlayersRevokingAccess().get(player.getUniqueId()));

                player.sendMessage(ChatColor.GREEN + String.format(localeService.get("AlertAccessRevokedFor"), uuidChecker.findPlayerNameBasedOnUUID(ephemeralData.getPlayersRevokingAccess().get(player.getUniqueId()))));
                ephemeralData.getPlayersRevokingAccess().remove(player.getUniqueId());
            } else { // if single chest
                // revoke access to single chest
                persistentData.getLockedBlock(clickedBlock).removeFromAccessList(ephemeralData.getPlayersRevokingAccess().get(player.getUniqueId()));
                player.sendMessage(ChatColor.GREEN + String.format(localeService.get("AlertAccessRevokedFor"), uuidChecker.findPlayerNameBasedOnUUID(ephemeralData.getPlayersRevokingAccess().get(player.getUniqueId()))));
                ephemeralData.getPlayersRevokingAccess().remove(player.getUniqueId());
            }

        }

        // if door
        if (blockChecker.isDoor(clickedBlock)) {
            // revoke access to initial block
            persistentData.getLockedBlock(clickedBlock).removeFromAccessList(ephemeralData.getPlayersRevokingAccess().get(player.getUniqueId()));
            // check block above
            if (blockChecker.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() + 1, clickedBlock.getZ()))) {
                persistentData.getLockedBlock(clickedBlock).removeFromAccessList(ephemeralData.getPlayersRevokingAccess().get(player.getUniqueId()));
            }
            // check block below
            if (blockChecker.isDoor(clickedBlock.getWorld().getBlockAt(clickedBlock.getX(), clickedBlock.getY() - 1, clickedBlock.getZ()))) {
                persistentData.getLockedBlock(clickedBlock).removeFromAccessList(ephemeralData.getPlayersRevokingAccess().get(player.getUniqueId()));
            }

            player.sendMessage(ChatColor.GREEN + String.format(localeService.get("AlertAccessRevokedFor"), uuidChecker.findPlayerNameBasedOnUUID(ephemeralData.getPlayersRevokingAccess().get(player.getUniqueId()))));
            ephemeralData.getPlayersRevokingAccess().remove(player.getUniqueId());
        }

        // if gate or other single-block sized lock
        if (blockChecker.isGate(clickedBlock) || blockChecker.isBarrel(clickedBlock) || blockChecker.isTrapdoor(clickedBlock) || blockChecker.isFurnace(clickedBlock)) {
            persistentData.getLockedBlock(clickedBlock).removeFromAccessList(ephemeralData.getPlayersRevokingAccess().get(player.getUniqueId()));

            player.sendMessage(ChatColor.GREEN + String.format(localeService.get("AlertAccessRevokedFor"), uuidChecker.findPlayerNameBasedOnUUID(ephemeralData.getPlayersRevokingAccess().get(player.getUniqueId()))));
            ephemeralData.getPlayersRevokingAccess().remove(player.getUniqueId());
        }

        event.setCancelled(true);
    }
}