package com.herocraft.herotab;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.herocraft.herotab.command.HeroTabCommand;
import com.herocraft.herotab.config.ConfigManager;
import com.herocraft.herotab.integration.FactionSync;
import com.herocraft.herotab.integration.GradeSync;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "herotab",
        name = "HeroTab",
        version = "1.1.0",
        description = "Tab list unifié, fluide et personnalisable pour tout le réseau HeroCraft (header/footer animés, intégration GradePlugin & FactionPlugin via MySQL, mode safe pour préserver les skins).",
        authors = {"HeroCraft"}
)
public class HeroTabPlugin {

    /** Version exposée aux commandes et logs (doit rester synchronisée avec @Plugin et pom.xml). */
    public static final String PLUGIN_VERSION = "1.1.0";

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private ConfigManager configManager;
    private TabListManager tabListManager;
    private ScheduledTask updateTask;

    private GradeSync gradeSync;
    private FactionSync factionSync;
    private ScheduledTask gradeSyncTask;
    private ScheduledTask factionSyncTask;

    @Inject
    public HeroTabPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        this.configManager = new ConfigManager(this, dataDirectory, logger);
        this.configManager.load();

        setupIntegrations();

        this.tabListManager = new TabListManager(this, server, configManager, logger, gradeSync, factionSync);

        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("herotab").aliases("htab").build(),
                new HeroTabCommand(this)
        );

        server.getEventManager().register(this, tabListManager);

        startUpdateTask();

        logger.info("HeroTab v{} activé — {} joueurs actuellement en ligne.", PLUGIN_VERSION, server.getPlayerCount());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (updateTask != null) updateTask.cancel();
        if (gradeSyncTask != null) gradeSyncTask.cancel();
        if (factionSyncTask != null) factionSyncTask.cancel();
    }

    private void setupIntegrations() {
        this.gradeSync = new GradeSync(configManager.getConfig().gradesMysql, logger);
        this.factionSync = new FactionSync(configManager.getConfig().factionsMysql, logger);

        if (gradeSync.isEnabled()) {
            gradeSync.refresh();
            gradeSyncTask = server.getScheduler()
                    .buildTask(this, gradeSync::refresh)
                    .repeat(Math.max(5, gradeSync.getRefreshIntervalSeconds()), TimeUnit.SECONDS)
                    .schedule();
            logger.info("HeroTab : synchronisation des grades (GradePlugin) activée.");
        }

        if (factionSync.isEnabled()) {
            factionSync.refresh();
            factionSyncTask = server.getScheduler()
                    .buildTask(this, factionSync::refresh)
                    .repeat(Math.max(5, factionSync.getRefreshIntervalSeconds()), TimeUnit.SECONDS)
                    .schedule();
            logger.info("HeroTab : synchronisation des factions (FactionPlugin) activée.");
        }
    }

    public void startUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        long intervalTicks = configManager.getConfig().updateIntervalTicks;
        // Velocity n'a pas de "ticks" proxy-side ; on convertit en secondes (20 ticks = 1s), minimum 200ms.
        long millis = Math.max(200L, (intervalTicks * 50L));
        updateTask = server.getScheduler()
                .buildTask(this, tabListManager::updateAll)
                .repeat(millis, TimeUnit.MILLISECONDS)
                .schedule();
    }

    public void reload() {
        configManager.load();

        if (gradeSyncTask != null) gradeSyncTask.cancel();
        if (factionSyncTask != null) factionSyncTask.cancel();
        setupIntegrations();
        tabListManager.setIntegrations(gradeSync, factionSync);

        tabListManager.reloadAnimationState();
        startUpdateTask();
        tabListManager.updateAll();
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    /** Version courante du plugin. */
    public String getPluginVersion() {
        return PLUGIN_VERSION;
    }

    public TabListManager getTabListManager() {
        return tabListManager;
    }

    /** Accès au syncer GradePlugin (peut être null si l'intégration est désactivée). */
    public GradeSync getGradeSync() {
        return gradeSync;
    }

    /** Accès au syncer FactionPlugin (peut être null si l'intégration est désactivée). */
    public FactionSync getFactionSync() {
        return factionSync;
    }
}
