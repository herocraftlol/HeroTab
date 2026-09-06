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
 * Lit directement la base MySQL "grades_db" de GradePlugin (tables
 * player_grades + grades) pour connaître le grade le plus prioritaire de
 * chaque joueur, sans dépendre du plugin Paper lui-même.
 *
 * Lecture seule : ce plugin n'écrit jamais dans cette base.
 */
public class GradeSync {

    private final HeroTabConfig.MySQLTarget cfg;
    private final Logger logger;

    private volatile Map<UUID, GradeInfo> cache = new ConcurrentHashMap<>();

    private static final String QUERY = """
            SELECT pg.uuid AS uuid, g.display_name AS display_name,
                   g.prefix AS prefix, g.suffix AS suffix, g.color AS color
            FROM player_grades pg
            JOIN grades g ON g.id = pg.grade_id
            WHERE pg.active = 1 AND (pg.expires_at IS NULL OR pg.expires_at > NOW())
            ORDER BY g.priority DESC
            """;

    public GradeSync(HeroTabConfig.MySQLTarget cfg, Logger logger) {
        this.cfg = cfg;
        this.logger = logger;
    }

    public boolean isEnabled() {
        return cfg.enabled;
    }

    public int getRefreshIntervalSeconds() {
        return cfg.refreshIntervalSeconds;
    }

    /** Recharge l'intégralité du cache depuis MySQL. Appelé périodiquement par un ScheduledTask. */
    public void refresh() {
        if (!cfg.enabled) return;

        Map<UUID, GradeInfo> next = new HashMap<>();
        try {
            JdbcDriverLoader.ensureLoaded();
        } catch (ClassNotFoundException e) {
            logger.warn("[GradeSync] Driver MySQL introuvable dans le jar (mysql-connector-j) : {}", e.getMessage());
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
                // Le tri par priorité DESC garantit que la première ligne rencontrée
                // pour un uuid donné est déjà son grade le plus prioritaire.
                next.putIfAbsent(uuid, new GradeInfo(
                        rs.getString("display_name"),
                        rs.getString("prefix"),
                        rs.getString("suffix"),
                        rs.getString("color")
                ));
            }
            cache = next;
        } catch (SQLException e) {
            logger.warn("[GradeSync] Impossible de lire grades_db : {}", e.getMessage());
        }
    }

    public GradeInfo get(UUID uuid) {
        return cache.get(uuid);
    }
}
