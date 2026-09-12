package com.herocraft.herotab.integration;

import com.herocraft.herotab.config.HeroTabConfig;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lit la table "faction_tab_sync" (écrite par FactionPlugin, cf. sa classe
 * FactionTabSync) pour connaître la faction et le rang de chaque joueur,
 * réseau entier, sans dépendre de sur quel sous-serveur il se trouve.
 *
 * Lecture seule.
 */
public class FactionSync {

    private final HeroTabConfig.MySQLTarget cfg;
    private final Logger logger;

    private volatile Map<UUID, FactionInfo> cache = new ConcurrentHashMap<>();

    private static final String QUERY = """
            SELECT uuid, faction_name, rank_name, rank_color, rank_icon
            FROM faction_tab_sync
            """;

    public FactionSync(HeroTabConfig.MySQLTarget cfg, Logger logger) {
        this.cfg = cfg;
        this.logger = logger;
    }

    public boolean isEnabled() {
        return cfg.enabled;
    }

    public int getRefreshIntervalSeconds() {
        return cfg.refreshIntervalSeconds;
    }

    public void refresh() {
        if (!cfg.enabled) return;

        Map<UUID, FactionInfo> next = new HashMap<>();
        try {
            JdbcDriverLoader.ensureLoaded();
        } catch (ClassNotFoundException e) {
            logger.warn("[FactionSync] Driver MySQL introuvable dans le jar (mysql-connector-j) : {}", e.getMessage());
            return;
        }

        try (Connection conn = DriverManager.getConnection(cfg.jdbcUrl(), cfg.user, cfg.password);
             PreparedStatement ps = conn.prepareStatement(QUERY);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(rs.getString("uuid"));
                } catch (IllegalArgumentException e) {
                    continue;
                }
                String factionName = rs.getString("faction_name");
                if (factionName == null) continue; // pas de faction, rien à afficher

                next.put(uuid, new FactionInfo(
                        factionName,
                        rs.getString("rank_name"),
                        rs.getString("rank_color"),
                        rs.getString("rank_icon")
                ));
            }
            cache = next;
        } catch (SQLException e) {
            logger.warn("[FactionSync] Impossible de lire faction_tab_sync : {}", e.getMessage());
        }
    }

    public FactionInfo get(UUID uuid) {
        return cache.get(uuid);
    }
}
