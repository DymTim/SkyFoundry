package net.skyfoundry.api.addon;

import net.skyfoundry.api.SkyFoundryAPI;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.logging.Logger;

public interface AddonContext {

    AddonDescription description();

    SkyFoundryAPI api();

    Logger logger();

    File dataFolder();

    void registerListener(Listener listener);

    BukkitTask runTask(Runnable task);

    BukkitTask runTaskLater(Runnable task, long delayTicks);

    BukkitTask runTaskTimer(Runnable task, long delayTicks, long periodTicks);

    BukkitTask runTaskAsync(Runnable task);
}
