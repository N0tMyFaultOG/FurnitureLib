package de.Ste3et_C0st.FurnitureLib.Events;

import de.Ste3et_C0st.FurnitureLib.Crafting.Project;
import de.Ste3et_C0st.FurnitureLib.SchematicLoader.Events.ProjectBreakEvent;
import de.Ste3et_C0st.FurnitureLib.SchematicLoader.Events.ProjectClickEvent;
import de.Ste3et_C0st.FurnitureLib.Utilitis.HiddenStringUtils;
import de.Ste3et_C0st.FurnitureLib.main.FurnitureLib;
import de.Ste3et_C0st.FurnitureLib.main.FurnitureManager;
import de.Ste3et_C0st.FurnitureLib.main.ObjectID;
import de.Ste3et_C0st.FurnitureLib.main.Type.SQLAction;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashSet;
import java.util.Objects;

public class ChunkOnLoad implements Listener {

    public HashSet<Player> eventList = new HashSet<Player>();

    /*
     * Spawn furniture from Project
     */

    public static Project getProjectByItem(ItemStack is) {
        if (is == null) return null;
        ItemStack stack = is.clone();
        if (stack.hasItemMeta()) {
            if (stack.getItemMeta().hasLore()) {
                String projectString = HiddenStringUtils.extractHiddenString(stack.getItemMeta().getLore().get(0));
                if (projectString != null)
                    return FurnitureManager.getInstance().getProjects().stream().filter(pro -> pro.getSystemID().equalsIgnoreCase(projectString)).findFirst().orElse(null);
            }
        }
        return null;
    }

    /*
     * RightClick Block
     */

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpawn(final PlayerInteractEvent e) {
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            if (!e.hasBlock()) return;
            if (!e.hasItem()) return;
            if (e.useInteractedBlock().equals(Result.DENY)) return;
            if (e.useItemInHand().equals(Result.DENY)) return;
            final Block b = e.getClickedBlock();
            final ItemStack stack = e.getItem();
            if (stack == null) return;
            final Project pro = getProjectByItem(stack);
            if (pro == null) return;
            e.setCancelled(true);
            if (FurnitureLib.getInstance().getBlockManager().getList().contains(b.getLocation())) return;
            if (eventList.contains(e.getPlayer())) return;
            if (b.isLiquid()) return;
            if (!e.getHand().equals(EquipmentSlot.HAND)) return;
            eventList.add(e.getPlayer());
            final BlockFace face = e.getBlockFace();
            final Location loc = b.getLocation();
            final Player p = e.getPlayer();
            loc.setYaw(FurnitureLib.getInstance().getLocationUtil().FaceToYaw(FurnitureLib.getInstance().getLocationUtil().yawToFace(p.getLocation().getYaw())));
            Bukkit.getScheduler().scheduleSyncDelayedTask(FurnitureLib.getInstance(), new Runnable() {
                @Override
                public void run() {
                    FurnitureItemEvent e = new FurnitureItemEvent(p, stack, pro, loc, face);
                    FurnitureLib.debug("FurnitureLib -> Place Furniture Start (" + pro.getName() + ").");
                    Bukkit.getPluginManager().callEvent(e);
                    FurnitureLib.debug("FurnitureLib -> Call FurnitureItemEvent cancel (" + e.isCancelled() + ").");
                    if (!e.isCancelled()) {
                        if (e.canBuild()) {
                            FurnitureLib.debug("FurnitureLib -> Can Place Model (" + pro.getName() + ") here");
                            if (e.isTimeToPlace()) {
                                if (e.sendAnnouncer()) {
                                    if (Objects.nonNull(e.getProject().getModelschematic())) {
                                        FurnitureLib.debug("FurnitureLib -> Model " + pro.getName() + " have Schematic place it.");
                                        if (pro.getModelschematic().isPlaceable(e.getObjID().getStartLocation())) {
                                            FurnitureLib.debug("FurnitureLib -> Model " + pro.getName() + " is Placeable");
                                            spawn(e);
                                        } else {
                                            p.sendMessage(FurnitureLib.getInstance().getLangManager().getString("message.NotEnoughSpace"));
                                        }
                                    } else {
                                        FurnitureLib.debug("FurnitureLib -> Can't place model [no Modelschematic (" + pro.getName() + ")]");
                                    }
                                }
                            }
                        } else {
                            FurnitureLib.debug("FurnitureLib -> Can't place model " + pro.getName() + " here canBuild(" + e.canBuild() + ")");
                        }
                    }
                }
            });
            removePlayer(p);
        } else if (e.getAction().equals(Action.RIGHT_CLICK_AIR)) {
            final ItemStack stack = e.getItem();
            if (stack == null) return;
            final Project pro = getProjectByItem(stack);
            if (pro == null) return;
            e.setCancelled(true);
        }


    }

    @EventHandler
    public void onRightClickBlock(final PlayerInteractEvent e) {
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            final Block b = e.getClickedBlock();
            if (b == null) return;
            if (!FurnitureLib.getInstance().getBlockManager().getList().contains(b.getLocation())) return;
            e.setCancelled(true);
            if (!e.getHand().equals(EquipmentSlot.HAND)) return;
            final Location loc = b.getLocation();
            final Player p = e.getPlayer();
            loc.setYaw(FurnitureLib.getInstance().getLocationUtil().FaceToYaw(FurnitureLib.getInstance().getLocationUtil().yawToFace(p.getLocation().getYaw())));
            Location blockLocation = b.getLocation();
            boolean bool = !b.getType().equals(Material.FLOWER_POT);
            final ObjectID objID = FurnitureManager.getInstance().getObjectList().stream().filter(obj -> obj.getBlockList().contains(blockLocation)).findFirst().orElse(null);
            if (objID == null) {
                return;
            }
            if (objID.isPrivate()) {
                return;
            }
            e.setCancelled(bool);
            if (bool && !objID.getSQLAction().equals(SQLAction.REMOVE)) {
                if ((p.getGameMode() == GameMode.CREATIVE) && !FurnitureLib.getInstance().creativeInteract()) {
                    if (!FurnitureLib.getInstance().getPermission().hasPerm(p, "furniture.bypass.creative.interact")) {
                        return;
                    }
                }

                if (!FurnitureLib.getInstance().getFurnitureManager().getIgnoreList().contains(p.getUniqueId())) {
                    Bukkit.getScheduler().runTask(FurnitureLib.getInstance(), () -> {
                        ProjectClickEvent projectBreakEvent = new ProjectClickEvent(p, objID);
                        Bukkit.getPluginManager().callEvent(projectBreakEvent);
                        if (!projectBreakEvent.isCancelled()) {
                            objID.callFunction("onClick", p);
                        }
                    });
                    return;
                } else {
                    e.getPlayer().sendMessage(FurnitureLib.getInstance().getLangManager().getString("message.FurnitureToggleEvent"));
                }
            }
        }
    }

    @EventHandler
    public void onClick(final PlayerInteractEvent event) {
        final Player p = event.getPlayer();
        if (p == null) return;
        if (p.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            if (event.getClickedBlock() == null) {
                return;
            }
            if (event.getClickedBlock().getLocation() == null) {
                return;
            }
            if (FurnitureLib.getInstance() == null) {
                return;
            }
            if (FurnitureLib.getInstance().getBlockManager() == null) {
                return;
            }
            if (FurnitureLib.getInstance().getBlockManager().getList() == null) {
                return;
            }
            if (FurnitureLib.getInstance().getBlockManager().getList().contains(event.getClickedBlock().getLocation())) {
                ObjectID objID = null;
                for (ObjectID obj : FurnitureLib.getInstance().getFurnitureManager().getObjectList()) {
                    if (obj.getBlockList().contains(event.getClickedBlock().getLocation())) {
                        objID = obj;
                        break;
                    }
                }
                if (objID != null) {
                    if (objID.isPrivate()) {
                        return;
                    }
                } else {
                    return;
                }
                event.setCancelled(true);
                if (!event.getHand().equals(EquipmentSlot.HAND)) return;
                if (!objID.getSQLAction().equals(SQLAction.REMOVE)) {
                    final ObjectID o = objID;
                    if (!FurnitureLib.getInstance().getFurnitureManager().getIgnoreList().contains(p.getUniqueId())) {
                        Bukkit.getScheduler().runTask(FurnitureLib.getInstance(), () -> {
                            ProjectBreakEvent projectBreakEvent = new ProjectBreakEvent(p, o);
                            Bukkit.getPluginManager().callEvent(projectBreakEvent);
                            if (!projectBreakEvent.isCancelled()) {
                                o.callFunction("onBreak", p);
                            }
                        });
                        return;
                    } else {
                        event.getPlayer().sendMessage(FurnitureLib.getInstance().getLangManager().getString("message.FurnitureToggleEvent"));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityRightClick(PlayerInteractEntityEvent e) {
        if (!e.getHand().equals(EquipmentSlot.HAND)) return;
        if (e.getRightClicked() != null && e.getPlayer() != null) {
            PlayerInventory inv = e.getPlayer().getInventory();
            if (getProjectByItem(inv.getItemInMainHand()) != null) {
                e.setCancelled(true);
                return;
            }
            if (getProjectByItem(inv.getItemInOffHand()) != null) {
                e.setCancelled(true);
                return;
            }
        }
    }

    public void spawn(FurnitureItemEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (!e.getProject().hasPermissions(e.getPlayer())) {
            return;
        }
        ObjectID obj = e.getObjID();
        if (FurnitureLib.getInstance().getFurnitureManager().getIgnoreList().contains(e.getPlayer().getUniqueId())) {
            e.getPlayer().sendMessage(FurnitureLib.getInstance().getLangManager().getString("message.FurnitureToggleEvent"));
            return;
        }
        if (FurnitureManager.getInstance().furnitureAlreadyExistOnBlock(obj.getStartLocation().getBlock())) {
            e.getPlayer().sendMessage(FurnitureLib.getInstance().getLangManager().getString("message.FurnitureOnThisPlace"));
            return;
        }
        FurnitureLib.getInstance().spawn(obj.getProjectOBJ(), obj);
        e.finish();
        e.removeItem();
        FurnitureManager.getInstance().addObjectID(obj);
    }

    private void removePlayer(final Player p) {
        Bukkit.getScheduler().runTaskLater(FurnitureLib.getInstance(), () -> {
			if (eventList != null && !eventList.isEmpty() && p != null && p.isOnline()) {
				eventList.remove(p);
			}
		}, 1);
    }
}
