package cn.apisium.simplechairs;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.Collection;
import java.util.HashSet;

import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

@SuppressWarnings({ "deprecation", "unused" })
public class Main extends JavaPlugin implements Listener {
    private final String NAME = "$$Chairs$$";
    private final HashSet<ArmorStand> list = new HashSet<>();

    @Override
    public void onEnable() {
        getServer().getScheduler().runTaskTimer(this, () ->
            getServer().getWorlds().forEach(w -> w.getEntitiesByClasses(ArmorStand.class).forEach(this::check)),
            100, 100);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        list.forEach(it -> {
            it.getPassengers().forEach(Entity::leaveVehicle);
            it.remove();
        });
        list.clear();
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent e) {
        final Entity t = e.getPlayer().getVehicle();
        if (t instanceof ArmorStand) {
            e.getPlayer().leaveVehicle();
        }
    }

    @EventHandler
    void onPlayerInteract(final PlayerInteractEvent e) {
        final Block b = e.getClickedBlock();
        final Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.SPECTATOR || e.getItem() != null || e.getAction() != RIGHT_CLICK_BLOCK ||
            b == null || p.hasPermission("simplechairs.cannotsit") || b.getType().data != Stairs.class) return;
        final Stairs data = (Stairs) b.getBlockData();
        if (data.getHalf() == Bisected.Half.TOP) return;
        final Location l = b.getLocation().clone().add(0.5, -1.18, 0.5);
        final Collection<ArmorStand> entities = l.getNearbyEntitiesByType(ArmorStand.class, 0.5, 0.5, 0.5);
        int i = entities.size();
        if (i > 0) {
            for (ArmorStand it : entities) {
                check(it);
                i--;
            }
            if (i > 0) return;
        }
        switch (data.getFacing()) {
            case SOUTH: l.setYaw(180); break;
            case EAST: l.setYaw(90); break;
            case WEST: l.setYaw(270); break;
        }

        final ArmorStand a = (ArmorStand) b.getWorld().spawnEntity(l, EntityType.ARMOR_STAND);
        a.setAI(false);
        a.setCustomName(NAME);
        a.setCanMove(false);
        a.setBasePlate(false);
        a.setCanTick(false);
        a.setVisible(false);
        a.setCanPickupItems(false);
        a.setPassenger(p);
        list.add(a);
    }

    @EventHandler
    void onEntityDismount(final EntityDismountEvent e) {
        final Entity l = e.getDismounted();
        final String name = l.getCustomName();
        if (l instanceof ArmorStand && name != null && name.equals(NAME)) {
            l.remove();
            list.remove(l);
            final Entity p = e.getEntity();
            getServer().getScheduler().runTaskLater(this, () ->
                p.teleport(p.getLocation().add(0.0, 0.5, 0.0)), 1);
        }
    }

    private void check(final Entity it) {
        final String name = it.getCustomName();
        final Entity p = it.getPassenger();
        if (name != null && name.equals(NAME) && (p == null || p.getVehicle() != it)) {
            it.remove();
            //noinspection SuspiciousMethodCalls
            list.remove(it);
        }
    }
}
