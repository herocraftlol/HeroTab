package com.herocraft.herotab.config;

import com.herocraft.herotab.HeroTabPlugin;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Charge et sauvegarde config.yml. Format simple et lisible pour un admin non-dev :
 * toutes les valeurs sont plates ou en listes, pas d'objets imbriqués complexes.
 */
public class ConfigManager {

    private final HeroTabPlugin plugin;
    private final Path dataDirectory;
    private final Logger logger;
    private final Path configFile;

    private HeroTabConfig config;

    public ConfigManager(HeroTabPlugin plugin, Path dataDirectory, Logger logger) {
        this.plugin = plugin;
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.configFile = dataDirectory.resolve("config.yml");
    }

    @SuppressWarnings("unchecked")
    public void load() {
        try {
            Files.createDirectories(dataDirectory);
            if (!Files.exists(configFile)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (in != null) {
                        Files.copy(in, configFile);
                    }
                }
            }

            Yaml yaml = new Yaml();
            Map<String, Object> raw;
            try (InputStream in = Files.newInputStream(configFile)) {
                raw = yaml.load(in);
            }
            if (raw == null) raw = new LinkedHashMap<>();

            this.config = HeroTabConfig.fromMap(raw);
            logger.info("Configuration HeroTab chargée ({} groupes de serveurs).", config.serverGroups.size());
        } catch (IOException e) {
            logger.error("Impossible de charger config.yml, utilisation des valeurs par défaut.", e);
            this.config = HeroTabConfig.defaults();
        }
    }

    public HeroTabConfig getConfig() {
        return config;
    }
}
