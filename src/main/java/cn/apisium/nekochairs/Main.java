package cn.apisium.nekochairs;

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
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.permission.Permission;
import org.bukkit.plugin.java.annotation.plugin.*;
import org.bukkit.plugin.java.annotation.plugin.author.Author;
import org.spigotmc.event.entity.EntityDismountEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

@SuppressWarnings({ "deprecation", "unused" })
@Plugin(name = "NekoChairs", version = "1.0")
@Description("Make some chairs in Minecraft!")
@Author("Shirasawa")
@Website("https://apisium.cn")
@Permission(name = "nekochairs.use", defaultValue = PermissionDefault.TRUE)
@ApiVersion(ApiVersion.Target.v1_13)
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
            check(t);
        }
    }

    @EventHandler
    void onPlayerInteract(final PlayerInteractEvent e) {
        final Block b = e.getClickedBlock();
        final Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.SPECTATOR || e.getItem() != null || e.getAction() != RIGHT_CLICK_BLOCK ||
            b == null || !p.hasPermission("nekochairs.use") ||
            b.getType().data != Stairs.class) return;
        final Stairs data = (Stairs) b.getBlockData();
        if (data.getHalf() == Bisected.Half.TOP) return;
        final Location l = b.getLocation().clone().add(0.5, -1.18, 0.5);
        final Collection<ArmorStand> entities = l.getNearbyEntitiesByType(ArmorStand.class, 0.5, 0.5, 0.5);
        int i = entities.size();
        if (i > 0) {
            for (ArmorStand it : entities) if (!check(it)) i--;
            if (i > 0) return;
        }
        l.setYaw(switch (data.getFacing()) {
            case SOUTH -> 180;
            case EAST -> 90;
            case WEST -> 270;
            default -> 0;
        });

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
        if (l instanceof ArmorStand && name != null && name.equals(NAME)) leaveChair(l, e.getEntity());
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    private void leaveChair(final Entity l, @Nullable final Entity p) {
        list.remove(l);
        l.remove();
        getServer().getScheduler().runTaskLater(this, () -> {
            final Entity p2 = p == null ? l.getPassenger() : p;
            if (p2 == null) return;
            p2.teleport(p2.getLocation().add(0.0, 0.5, 0.0));
        }, 1);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    private boolean check(final Entity it) {
        final String name = it.getCustomName();
        final Entity p = it.getPassenger();
        if (name != null && name.equals(NAME)) {
            if (p != null && p.getVehicle() == it && it.getLocation().clone().add(-0.5, 1.18, -0.5)
                .getBlock().getType().data == Stairs.class) return true;
            it.remove();
            list.remove(it);
        }
        return false;
    }

    private ArrayList<ArmorStand> getChairsNearBy(final Location l) {
        final Collection<ArmorStand> entities = l.getNearbyEntitiesByType(ArmorStand.class, 0.5, 0.5, 0.5);
        final ArrayList<ArmorStand> list = new ArrayList<>();
        for (ArmorStand it : entities) if (check(it)) list.add(it);
        return list;
    }
}
