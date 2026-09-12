package com.herocraft.herotab;

import com.herocraft.herotab.config.ConfigManager;
import com.herocraft.herotab.config.HeroTabConfig;
import com.herocraft.herotab.integration.FactionInfo;
import com.herocraft.herotab.integration.FactionSync;
import com.herocraft.herotab.integration.GradeInfo;
import com.herocraft.herotab.integration.GradeSync;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Construit et pousse le header/footer + les noms/l'ordre affichés dans le
 * tab pour chaque joueur connecté au proxy, quel que soit le sous-serveur/
 * monde sur lequel il se trouve. Le tab est donc unifié sur tout le réseau.
 *
 * Les grades (GradePlugin) et factions (FactionPlugin) sont lus directement
 * depuis MySQL via GradeSync / FactionSync — voir com.herocraft.herotab.integration.
 *
 * Réordonnancement : le protocole Minecraft n'a pas de notion de "position"
 * sur une entrée existante — pour vraiment changer l'ordre affiché, il faut
 * retirer puis ré-ajouter les entrées dans l'ordre voulu (Velocity respecte
 * alors l'ordre d'insertion). On récupère le profil/gamemode d'origine avant
 * de retirer une entrée pour ne pas perdre son skin dans le tab.
 *
 * IMPORTANT : on ne fait ce retrait/ré-ajout QUE quand l'ordre a réellement
 * changé (arrivée, départ, changement de serveur) — on compare à
 * `lastOrderPerViewer`. Sinon on se contente de patcher le nom affiché/ping
 * sur les entrées déjà en place. Le faire à CHAQUE cycle (comme dans une
 * version précédente) empêche le client de finir de charger les textures de
 * skin externes (SkinRestorer, Bedrock...) puisque l'entrée est détruite et
 * recréée avant que le rendu n'ait le temps de se stabiliser.
 */
public class TabListManager {

    private static final int MAX_GROUP_SPACERS = 32;
    private static final UUID[] SPACER_UUIDS = new UUID[MAX_GROUP_SPACERS];
    private static final GameProfile[] SPACER_PROFILES = new GameProfile[MAX_GROUP_SPACERS];
    /** Marqueur neutre utilisé uniquement pour détecter un changement d'ordre — jamais envoyé au client. */
    private static final UUID ORDER_KEY_SPACER_MARKER = new UUID(0L, 0L);
    static {
        for (int i = 0; i < MAX_GROUP_SPACERS; i++) {
            SPACER_UUIDS[i] = UUID.nameUUIDFromBytes(("herotab:group-spacer:" + i).getBytes(StandardCharsets.UTF_8));
            SPACER_PROFILES[i] = new GameProfile(SPACER_UUIDS[i], "herotab_spacer_" + i, List.of());
        }
    }

    private final HeroTabPlugin plugin;
    private final ProxyServer server;
    private final ConfigManager configManager;
    private final Logger logger;

    private volatile GradeSync gradeSync;
    private volatile FactionSync factionSync;

    /** Dernier ordre appliqué pour chaque viewer (uuid des joueurs, + SPACER_UUID s'il y a un séparateur). */
    private final Map<UUID, List<UUID>> lastOrderPerViewer = new ConcurrentHashMap<>();
    /** Dernier profil "complet" (avec skin) vu pour chaque joueur, en secours si une entrée doit être recréée. */
    private final Map<UUID, GameProfile> knownGoodProfiles = new ConcurrentHashMap<>();

    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public TabListManager(HeroTabPlugin plugin, ProxyServer server, ConfigManager configManager, Logger logger,
                           GradeSync gradeSync, FactionSync factionSync) {
        this.plugin = plugin;
        this.server = server;
        this.configManager = configManager;
        this.logger = logger;
        this.gradeSync = gradeSync;
        this.factionSync = factionSync;
    }

    /** Permet à /herotab reload de brancher de nouvelles instances (nouvelle config MySQL) sans recréer le manager. */
    public void setIntegrations(GradeSync gradeSync, FactionSync factionSync) {
        this.gradeSync = gradeSync;
        this.factionSync = factionSync;
    }

    public void reloadAnimationState() {
        // Rien à réinitialiser : l'index d'animation est dérivé de l'horloge système.
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        // Visibilité d'abord, immédiatement et de façon synchrone : un joueur qui
        // vient de se connecter (ex: arrivée au lobby) doit voir tout le monde
        // tout de suite, sans attendre la relecture MySQL ci-dessous.
        updateAll();

        // On force ensuite une relecture immédiate de MySQL (grades + factions) au lieu
        // d'attendre le prochain cycle périodique (jusqu'à refresh-interval-seconds,
        // 15s par défaut) — sinon un joueur qui vient de rejoindre/rejoindre une
        // faction peut mettre jusqu'à 15s à voir son grade/sa faction apparaître.
        server.getScheduler().buildTask(plugin, () -> {
            if (gradeSync != null && gradeSync.isEnabled()) gradeSync.refresh();
            if (factionSync != null && factionSync.isEnabled()) factionSync.refresh();
            updateAll();
        }).schedule();
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        lastOrderPerViewer.remove(event.getPlayer().getUniqueId());
        knownGoodProfiles.remove(event.getPlayer().getUniqueId());
        // Léger délai pour laisser Velocity retirer le joueur de sa liste interne avant de rafraîchir.
        server.getScheduler().buildTask(plugin, this::updateAll)
                .delay(java.time.Duration.ofMillis(150))
                .schedule();
    }

    public void updateAll() {
        HeroTabConfig cfg = configManager.getConfig();
        List<Player> online = new ArrayList<>(server.getAllPlayers());
        if (online.isEmpty()) return;

        // Nombre de joueurs par sous-serveur, calculé une seule fois par cycle
        // pour alimenter %server_online% sans reparcourir la liste pour chaque viewer.
        Map<String, Integer> countsByServer = new HashMap<>();
        for (Player p : online) {
            countsByServer.merge(serverNameOf(p), 1, Integer::sum);
        }

        for (Player viewer : online) {
            updateHeaderFooter(viewer, cfg, online.size(), countsByServer);
            updateEntries(viewer, cfg, online);
        }
    }

    // ── Header / footer ─────────────────────────────────────────────────────────

    private void updateHeaderFooter(Player viewer, HeroTabConfig cfg, int totalOnline, Map<String, Integer> countsByServer) {
        Component header = joinLines(cfg.header, viewer, cfg, totalOnline, countsByServer);
        Component footer = joinLines(cfg.footer, viewer, cfg, totalOnline, countsByServer);
        viewer.sendPlayerListHeaderAndFooter(header, footer);
    }

    private Component joinLines(List<String> lines, Player viewer, HeroTabConfig cfg, int totalOnline, Map<String, Integer> countsByServer) {
        Component result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            String frame = pickFrame(lines.get(i), cfg.animationIntervalTicks);
            String replaced = applyPlaceholders(frame, viewer, cfg, totalOnline, countsByServer);
            result = result.append(parse(replaced, cfg));
            if (i < lines.size() - 1) {
                result = result.append(Component.newline());
            }
        }
        return result;
    }

    /** Une ligne peut contenir plusieurs frames séparées par '||' pour être animée. */
    private String pickFrame(String line, long animationIntervalTicks) {
        if (!line.contains("||")) return line;
        String[] frames = line.split("\\|\\|");
        if (frames.length <= 1) return line;
        long millisPerFrame = Math.max(50L, animationIntervalTicks * 50L);
        int index = (int) ((System.currentTimeMillis() / millisPerFrame) % frames.length);
        return frames[index].trim();
    }

    // ── Entrées joueurs (ordre + texte) ─────────────────────────────────────────

    private void updateEntries(Player viewer, HeroTabConfig cfg, List<Player> online) {
        TabList tabList = viewer.getTabList();

        if (!cfg.reorderMode.equalsIgnoreCase("experimental")) {
            // Mode "safe" (par défaut) : on ne retire/recrée JAMAIS une entrée d'un
            // joueur toujours en ligne (donc jamais de risque pour son skin) —
            // seulement son texte et son ping. En revanche, si un joueur d'un autre
            // sous-serveur manque encore dans ce tab, on l'AJOUTE pour garder la
            // visibilité réseau entière ; et surtout, on retire les entrées de
            // joueurs qui ne sont PLUS en ligne — sinon elles restent affichées
            // indéfiniment jusqu'au prochain changement de serveur du viewer.
            // Retirer une entrée d'un joueur déjà parti n'a aucun risque pour les
            // skins (il n'y a plus personne à afficher).
            Set<UUID> onlineIds = new HashSet<>();
            for (Player p : online) onlineIds.add(p.getUniqueId());

            for (TabListEntry entry : new ArrayList<>(tabList.getEntries())) {
                UUID id = entry.getProfile().getId();
                if (!onlineIds.contains(id) && !isSpacerUuid(id)) {
                    tabList.removeEntry(id);
                }
            }

            for (Player target : online) {
                var existingEntry = tabList.getEntry(target.getUniqueId());
                if (existingEntry.isPresent()) {
                    TabListEntry entry = existingEntry.get();
                    if (!entry.getProfile().getProperties().isEmpty()) {
                        knownGoodProfiles.put(target.getUniqueId(), entry.getProfile());
                    }
                    entry.setDisplayName(parse(formatPlayerEntry(viewer, target, cfg), cfg));
                    entry.setLatency((int) Math.max(0, target.getPing()));
                } else {
                    GameProfile profile = knownGoodProfiles.getOrDefault(target.getUniqueId(),
                            new GameProfile(target.getUniqueId(), target.getUsername(), List.of()));
                    try {
                        tabList.addEntry(TabListEntry.builder()
                                .tabList(tabList)
                                .profile(profile)
                                .displayName(parse(formatPlayerEntry(viewer, target, cfg), cfg))
                                .latency((int) Math.max(0, target.getPing()))
                                .gameMode(0)
                                .build());
                    } catch (Exception ex) {
                        logger.warn("Impossible d'ajouter {} au tab de {} : {}",
                                target.getUsername(), viewer.getUsername(), ex.getMessage());
                    }
                }
            }
            return;
        }

        List<Player> ordered = buildOrder(viewer, online, cfg);
        List<Integer> spacerAfterIndices = computeGroupSpacerIndices(ordered, cfg);

        List<UUID> newOrderKey = new ArrayList<>(ordered.size() + spacerAfterIndices.size());
        for (int i = 0; i < ordered.size(); i++) {
            newOrderKey.add(ordered.get(i).getUniqueId());
            if (spacerAfterIndices.contains(i)) newOrderKey.add(ORDER_KEY_SPACER_MARKER);
        }

        List<UUID> previousOrderKey = lastOrderPerViewer.get(viewer.getUniqueId());
        boolean orderChanged = previousOrderKey == null || !previousOrderKey.equals(newOrderKey);

        if (!orderChanged) {
            // Chemin rapide et sans risque pour les skins : on patche juste le texte/ping
            // sur les entrées déjà en place, sans jamais les retirer ni les recréer.
            for (Player target : ordered) {
                tabList.getEntry(target.getUniqueId()).ifPresent(entry -> {
                    entry.setDisplayName(parse(formatPlayerEntry(viewer, target, cfg), cfg));
                    entry.setLatency((int) Math.max(0, target.getPing()));
                });
            }
            return;
        }

        // L'ordre a changé (arrivée/départ/changement de serveur) : on doit vraiment
        // retirer puis ré-ajouter dans le bon ordre. On récupère d'abord le profil
        // (avec skin) de chaque entrée existante pour ne rien perdre visuellement.
        Map<UUID, TabListEntry> existing = new HashMap<>();
        for (TabListEntry e : tabList.getEntries()) {
            existing.put(e.getProfile().getId(), e);
        }
        for (TabListEntry e : existing.values()) {
            if (!e.getProfile().getProperties().isEmpty()) {
                knownGoodProfiles.put(e.getProfile().getId(), e.getProfile());
            }
        }

        for (Player target : online) {
            tabList.removeEntry(target.getUniqueId());
        }
        for (UUID spacerUuid : SPACER_UUIDS) {
            tabList.removeEntry(spacerUuid);
        }

        int spacerCount = 0;
        for (int i = 0; i < ordered.size(); i++) {
            Player target = ordered.get(i);
            TabListEntry old = existing.get(target.getUniqueId());
            int gameMode = old != null ? old.getGameMode() : 0;
            GameProfile profile = old != null ? old.getProfile()
                    : knownGoodProfiles.getOrDefault(target.getUniqueId(),
                        new GameProfile(target.getUniqueId(), target.getUsername(), List.of()));

            Component displayName = parse(formatPlayerEntry(viewer, target, cfg), cfg);

            try {
                tabList.addEntry(TabListEntry.builder()
                        .tabList(tabList)
                        .profile(profile)
                        .displayName(displayName)
                        .latency((int) Math.max(0, target.getPing()))
                        .gameMode(gameMode)
                        .build());
            } catch (Exception ex) {
                logger.warn("Impossible d'ajouter l'entrée tab de {} : {}", target.getUsername(), ex.getMessage());
            }

            if (spacerAfterIndices.contains(i) && spacerCount < MAX_GROUP_SPACERS) {
                try {
                    tabList.addEntry(TabListEntry.builder()
                            .tabList(tabList)
                            .profile(SPACER_PROFILES[spacerCount])
                            .displayName(parse(replaceTheme(cfg.groupSpacerText, cfg), cfg))
                            .latency(0)
                            .gameMode(0)
                            .build());
                } catch (Exception ex) {
                    logger.warn("Impossible d'ajouter le séparateur de groupe : {}", ex.getMessage());
                }
                spacerCount++;
            }
        }

        lastOrderPerViewer.put(viewer.getUniqueId(), newOrderKey);
    }

    private String formatPlayerEntry(Player viewer, Player target, HeroTabConfig cfg) {
        String serverName = serverNameOf(target);
        String group = cfg.serverGroups.getOrDefault(serverName, serverName);
        String serverColor = cfg.serverColors.getOrDefault(serverName, cfg.themePrimary);
        long ping = target.getPing();

        GradeInfo grade = gradeSync != null ? gradeSync.get(target.getUniqueId()) : null;

        // La faction n'est affichée que si le VIEWER et la CIBLE sont tous les
        // deux sur le serveur Factions en ce moment — un joueur en faction mais
        // actuellement sur un autre sous-serveur/monde n'affiche rien, et
        // inversement un viewer ailleurs que sur Factions ne voit aucun tag.
        boolean viewerOnFactionsServer = serverNameOf(viewer).equalsIgnoreCase(cfg.factionsServerName);
        boolean targetOnFactionsServer = serverNameOf(target).equalsIgnoreCase(cfg.factionsServerName);
        FactionInfo faction = (viewerOnFactionsServer && targetOnFactionsServer && factionSync != null)
                ? factionSync.get(target.getUniqueId()) : null;

        String text = cfg.playerFormat
                .replace("%player%", target.getUsername())
                .replace("%server%", serverName)
                .replace("%server_color%", serverColor)
                .replace("%group%", group)
                .replace("%ping%", String.valueOf(Math.max(0, ping)))
                .replace("%grade%", grade != null && grade.displayName() != null ? grade.displayName() : "")
                .replace("%grade_prefix%", grade != null && grade.prefix() != null ? grade.prefix() : "")
                .replace("%grade_suffix%", grade != null && grade.suffix() != null ? grade.suffix() : "")
                .replace("%grade_color%", grade != null && grade.color() != null ? grade.color() : "&f")
                .replace("%faction%", faction != null ? faction.factionName() : "")
                .replace("%faction_rank%", faction != null && faction.rankName() != null ? faction.rankName() : "")
                .replace("%faction_tag%", buildFactionTag(faction));

        return replaceTheme(text, cfg);
    }

    /** Construit un petit tag lisible du type " [★ Or - MaFaction]" — vide si le joueur n'a pas de faction. */
    private String buildFactionTag(FactionInfo faction) {
        if (faction == null || faction.factionName() == null || faction.factionName().isBlank()) {
            return "";
        }
        String color = faction.rankColor() != null && !faction.rankColor().isBlank() ? faction.rankColor() : "&7";
        String icon = faction.rankIcon() != null ? faction.rankIcon() + " " : "";
        return " &7[" + color + icon + faction.factionName() + "&7]";
    }

    // ── Ordre d'affichage ────────────────────────────────────────────────────────

    private List<Player> buildOrder(Player viewer, List<Player> online, HeroTabConfig cfg) {
        List<Player> list = new ArrayList<>(online);
        String mode = cfg.sortMode.toUpperCase(Locale.ROOT);

        if (mode.equals("SERVER_SELF_FIRST")) {
            String viewerServer = serverNameOf(viewer);
            list.sort(
                    Comparator.<Player>comparingInt(p -> serverNameOf(p).equalsIgnoreCase(viewerServer) ? 0 : 1)
                            .thenComparing(TabListManager::serverNameOf, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(Player::getUsername, String.CASE_INSENSITIVE_ORDER)
            );
            return list;
        }

        Comparator<Player> comparator = switch (mode) {
            case "ALPHABETICAL" -> Comparator.comparing(Player::getUsername, String.CASE_INSENSITIVE_ORDER);
            case "PING" -> Comparator.comparingLong(Player::getPing);
            case "SERVER" -> Comparator.comparing(TabListManager::serverNameOf, String.CASE_INSENSITIVE_ORDER);
            default -> null; // NONE : on garde l'ordre d'itération de Velocity
        };
        if (comparator != null) {
            list.sort(comparator);
        }
        return list;
    }

    /**
     * Renvoie les indices (dans "ordered") après lesquels insérer un séparateur —
     * un à CHAQUE changement de serveur consécutif, pas seulement entre "toi" et
     * "les autres". Ne s'applique qu'aux modes SERVER et SERVER_SELF_FIRST, où
     * les joueurs d'un même serveur sont déjà regroupés de façon contiguë.
     */
    private List<Integer> computeGroupSpacerIndices(List<Player> ordered, HeroTabConfig cfg) {
        List<Integer> indices = new ArrayList<>();
        if (!cfg.groupSpacerEnabled) return indices;
        String mode = cfg.sortMode.toUpperCase(Locale.ROOT);
        if (!mode.equals("SERVER") && !mode.equals("SERVER_SELF_FIRST")) return indices;

        for (int i = 0; i < ordered.size() - 1 && indices.size() < MAX_GROUP_SPACERS; i++) {
            if (!serverNameOf(ordered.get(i)).equalsIgnoreCase(serverNameOf(ordered.get(i + 1)))) {
                indices.add(i);
            }
        }
        return indices;
    }

    private static boolean isSpacerUuid(UUID id) {
        for (UUID spacer : SPACER_UUIDS) {
            if (spacer.equals(id)) return true;
        }
        return false;
    }

    private static String serverNameOf(Player p) {
        return p.getCurrentServer().map(sc -> sc.getServerInfo().getName()).orElse("?");
    }

    // ── Placeholders ─────────────────────────────────────────────────────────────

    private String applyPlaceholders(String text, Player viewer, HeroTabConfig cfg, int totalOnline, Map<String, Integer> countsByServer) {
        String serverName = serverNameOf(viewer);
        String group = cfg.serverGroups.getOrDefault(serverName, serverName);
        String serverColor = cfg.serverColors.getOrDefault(serverName, cfg.themePrimary);
        int serverOnline = countsByServer.getOrDefault(serverName, 0);

        GradeInfo grade = gradeSync != null ? gradeSync.get(viewer.getUniqueId()) : null;
        FactionInfo faction = factionSync != null ? factionSync.get(viewer.getUniqueId()) : null;

        String replaced = text
                .replace("%player%", viewer.getUsername())
                .replace("%server%", serverName)
                .replace("%server_color%", serverColor)
                .replace("%group%", group)
                .replace("%ping%", String.valueOf(Math.max(0, viewer.getPing())))
                // %online% = total sur TOUT le réseau (tous les sous-serveurs confondus)
                .replace("%online%", String.valueOf(totalOnline))
                .replace("%max%", String.valueOf(server.getConfiguration().getShowMaxPlayers()))
                // %server_online% = uniquement les joueurs sur le sous-serveur actuel du viewer
                .replace("%server_online%", String.valueOf(serverOnline))
                .replace("%grade%", grade != null && grade.displayName() != null ? grade.displayName() : "")
                .replace("%grade_prefix%", grade != null && grade.prefix() != null ? grade.prefix() : "")
                .replace("%faction%", faction != null ? faction.factionName() : "")
                .replace("%faction_rank%", faction != null && faction.rankName() != null ? faction.rankName() : "");

        return replaceTheme(replaced, cfg);
    }

    /** Couleurs de décoration + adresses réseau/site, communes au header/footer et aux entrées joueurs. */
    private String replaceTheme(String text, HeroTabConfig cfg) {
        return text
                .replace("%primary%", cfg.themePrimary)
                .replace("%secondary%", cfg.themeSecondary)
                .replace("%network_address%", cfg.networkAddress)
                .replace("%website_address%", cfg.websiteAddress);
    }

    private Component parse(String text, HeroTabConfig cfg) {
        if (cfg.allowMiniMessage && text.indexOf('<') >= 0) {
            try {
                return miniMessage.deserialize(text);
            } catch (Exception e) {
                // Retombe sur les codes & si le MiniMessage est invalide.
            }
        }
        return legacy.deserialize(text);
    }
}
