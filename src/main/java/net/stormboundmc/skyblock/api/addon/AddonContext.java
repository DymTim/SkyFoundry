package net.stormboundmc.skyblock.api.addon;

import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import net.stormboundmc.skyblock.api.StormboundAPI;

import java.io.File;
import java.util.logging.Logger;

public interface AddonContext {

    AddonDescription description();

    StormboundAPI api();

    Logger logger();

    File dataFolder();

    void registerListener(Listener listener);

    BukkitTask runTask(Runnable task);

    BukkitTask runTaskLater(Runnable task, long delayTicks);

    BukkitTask runTaskTimer(Runnable task, long delayTicks, long periodTicks);

    BukkitTask runTaskAsync(Runnable task);
}
