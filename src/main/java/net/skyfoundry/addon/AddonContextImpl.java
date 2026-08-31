package net.skyfoundry.addon;

import net.skyfoundry.SkyFoundry;
import net.skyfoundry.api.SkyFoundryAPI;
import net.skyfoundry.api.addon.AddonContext;
import net.skyfoundry.api.addon.AddonDescription;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

final class AddonContextImpl implements AddonContext {

    private final SkyFoundry plugin;
    private final AddonDescription description;
    private final File dataFolder;
    private final Logger logger;

    private final List<Listener> listeners = new ArrayList<>();
    private final List<BukkitTask> tasks = new ArrayList<>();

    AddonContextImpl(
            SkyFoundry plugin,
            AddonDescription description,
            File dataFolder) {
        this.plugin = plugin;
        this.description = description;
        this.dataFolder = dataFolder;
        this.logger = Logger.getLogger("SkyFoundryAddon." + description.name());
        this.logger.setParent(plugin.getLogger());
        this.logger.setUseParentHandlers(true);
    }

    @Override
    public AddonDescription description() {
        return description;
    }

    @Override
    public SkyFoundryAPI api() {
        return SkyFoundryAPI.get();
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public File dataFolder() {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.warning("Could not create addon data folder: " + dataFolder.getAbsolutePath());
        }

        return dataFolder;
    }

    @Override
    public void registerListener(Listener listener) {
        Objects.requireNonNull(listener, "listener");
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        listeners.add(listener);
    }

    @Override
    public BukkitTask runTask(Runnable task) {
        BukkitTask scheduled = plugin.getServer().getScheduler().runTask(plugin, Objects.requireNonNull(task, "task"));
        tasks.add(scheduled);
        return scheduled;
    }

    @Override
    public BukkitTask runTaskLater(Runnable task, long delayTicks) {
        BukkitTask scheduled = plugin.getServer().getScheduler().runTaskLater(
                plugin,
                Objects.requireNonNull(task, "task"),
                Math.max(0L, delayTicks));
        tasks.add(scheduled);
        return scheduled;
    }

    @Override
    public BukkitTask runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        if (periodTicks <= 0L) {
            throw new IllegalArgumentException("periodTicks must be greater than 0");
        }

        BukkitTask scheduled = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                Objects.requireNonNull(task, "task"),
                Math.max(0L, delayTicks),
                periodTicks);
        tasks.add(scheduled);
        return scheduled;
    }

    @Override
    public BukkitTask runTaskAsync(Runnable task) {
        BukkitTask scheduled = plugin.getServer().getScheduler().runTaskAsynchronously(
                plugin,
                Objects.requireNonNull(task, "task"));
        tasks.add(scheduled);
        return scheduled;
    }

    void cleanup() {
        for (BukkitTask task : tasks) {
            if (!task.isCancelled()) {
                task.cancel();
            }
        }
        tasks.clear();

        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }
        listeners.clear();
    }
}
