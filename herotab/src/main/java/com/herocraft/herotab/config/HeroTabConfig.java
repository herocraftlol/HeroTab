package com.herocraft.herotab.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HeroTabConfig {

    /** Nombre de ticks (20 = 1s) entre deux rafraîchissements du tab. */
    public long updateIntervalTicks = 20;

    /**
     * Format d'un slot pour un joueur donné.
     * Placeholders : %player% %server% %ping% %group% %grade% %grade_prefix% %faction% %faction_rank%
     *                %faction_tag% %primary% %secondary%
     */
    public String playerFormat = "&7[%primary%%server%&7] %grade_prefix%&f%player%%faction_tag% &8•&7 %ping%ms";

    /** Lignes d'en-tête, une par ligne. Chaque ligne peut avoir plusieurs frames séparées par '||' pour l'animation. */
    public List<String> header = new ArrayList<>(List.of(
            "%primary%&m                                        ",
            "",
            "%primary%&lHeroCraft %secondary%&l• %primary%Le réseau",
            "&7Réseau : %primary%%online%&7/%primary%%max% &8| &7Ici : %secondary%%server_online%",
            "",
            "%primary%&m                                        "
    ));

    /** Lignes de pied de page. */
    public List<String> footer = new ArrayList<>(List.of(
            "%secondary%&m                                        ",
            "",
            "&7Serveur : %primary%%network_address%",
            "&7Site : %primary%%website_address%",
            "",
            "%secondary%&m                                        "
    ));

    /** Vitesse d'animation du header/footer si plusieurs frames sont fournies avec '||'. */
    public long animationIntervalTicks = 20;

    /**
     * Tri des joueurs dans la liste :
     *   ALPHABETICAL, PING, SERVER, NONE,
     *   SERVER_SELF_FIRST — les joueurs de TON sous-serveur actuel d'abord, puis les autres
     *                        (groupés par serveur), avec un séparateur décoratif entre les deux
     *                        si group-spacer-enabled est activé.
     *
     * ATTENTION : un vrai tri visuel nécessite de retirer puis recréer les
     * entrées du tab (le protocole Minecraft n'a pas de notion de "position").
     * Sur certains setups, ça peut empêcher les skins (SkinRestorer, Bedrock,
     * voire des comptes premium normaux) de s'afficher correctement. Ce
     * réglage n'a donc d'effet QUE si reorder-mode est sur "experimental"
     * ci-dessous — en mode "safe" (par défaut), l'ordre reste celui de
     * connexion mais les skins sont garantis intacts.
     */
    public String sortMode = "SERVER_SELF_FIRST";

    /**
     * "safe" (par défaut) : le plugin ne touche jamais à l'identité d'une
     *        entrée du tab, seulement au texte affiché et au ping — l'ordre
     *        des joueurs reste celui de Velocity (connexion), mais les skins
     *        sont garantis de fonctionner comme sans HeroTab.
     * "experimental" : active le vrai tri (sort-mode, group-spacer) en
     *        retirant/recréant les entrées quand l'ordre doit changer. Peut
     *        casser l'affichage des skins sur certains setups — à tester
     *        avant de mettre en prod.
     */
    public String reorderMode = "safe";

    /** Insère une ligne décorative entre "ton serveur" et "les autres joueurs" en mode SERVER_SELF_FIRST. */
    public boolean groupSpacerEnabled = true;

    /** Texte de cette ligne décorative. Placeholders de thème acceptés (%primary% %secondary%). */
    public String groupSpacerText = "%secondary%&m▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

    /**
     * Regroupement optionnel de sous-serveurs sous un même nom affiché.
     * ex: bedwars1, bedwars2 -> "BedWars"
     */
    public Map<String, String> serverGroups = new LinkedHashMap<>();

    /**
     * Couleur individuelle par sous-serveur, utilisée par %server_color%.
     * Une entrée absente retombe automatiquement sur %primary% — donc ajouter
     * ou retirer un serveur ne casse jamais rien, il suffit d'ajouter une
     * ligne ici si tu veux lui donner une couleur particulière.
     */
    public Map<String, String> serverColors = new LinkedHashMap<>();

    /** Si vrai, MiniMessage (&lt;red&gt;, &lt;bold&gt;...) est accepté en plus des codes &. */
    public boolean allowMiniMessage = true;

    /** Couleurs de décoration personnalisables, utilisables partout via %primary% / %secondary%. */
    public String themePrimary = "&b";   // bleu par défaut
    public String themeSecondary = "&e"; // jaune par défaut

    /** Adresse de connexion au réseau (IP/domaine), personnalisable, placeholder %network_address%. */
    public String networkAddress = "herocraft.servegame.com";

    /** Adresse du site web, personnalisable, placeholder %website_address%. */
    public String websiteAddress = "herocraft.servegame.com";

    /**
     * Nom (id backend) du serveur Factions. Les placeholders %faction%,
     * %faction_rank% et %faction_tag% ne sont remplis QUE si le joueur qui
     * regarde le tab est lui-même connecté à ce serveur — ailleurs sur le
     * réseau, ils restent vides pour tout le monde.
     */
    public String factionsServerName = "factions";

    /** Connexion à la base "grades_db" de GradePlugin (table player_grades + grades). */
    public MySQLTarget gradesMysql = new MySQLTarget();

    /** Connexion à la base "herocraft" de FactionPlugin (table faction_tab_sync). */
    public MySQLTarget factionsMysql = new MySQLTarget();

    /** Si vrai, un tag de faction est ajouté devant chaque message de chat des joueurs qui en ont une. */
    public boolean chatFactionTagEnabled = true;

    /**
     * Format du tag ajouté devant le message de chat (le message original du
     * joueur est complété automatiquement juste après, tel quel).
     * Placeholders : %faction% %faction_rank% %faction_icon%
     * PAS de couleur ici : le message est réécrit en texte brut et renvoyé au
     * serveur backend, qui le revalide comme s'il venait du client — le
     * caractère de code couleur (§) y est presque toujours traité comme un
     * "caractère interdit" par les anti-triche et fait kicker le joueur.
     * Nécessite factions-mysql activé plus bas (mêmes identifiants que pour le tab).
     */
    public String chatFactionFormat = "[%faction%] ";

    /**
     * Un bloc de connexion MySQL en lecture seule, utilisé pour récupérer les
     * grades et/ou factions déjà stockés par les plugins Paper correspondants.
     */
    public static class MySQLTarget {
        public boolean enabled = false;
        public String host = "127.0.0.1";
        public int port = 3306;
        public String database = "";
        public String user = "";
        public String password = "";
        /** Intervalle entre deux rechargements complets depuis MySQL, en secondes. */
        public int refreshIntervalSeconds = 15;

        public String jdbcUrl() {
            return "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
        }

        @SuppressWarnings("unchecked")
        static MySQLTarget fromMap(Object raw) {
            MySQLTarget t = new MySQLTarget();
            if (!(raw instanceof Map<?, ?> m)) return t;
            if (m.get("enabled") instanceof Boolean b) t.enabled = b;
            if (m.get("host") instanceof String s) t.host = s;
            if (m.get("port") instanceof Number n) t.port = n.intValue();
            if (m.get("database") instanceof String s) t.database = s;
            if (m.get("user") instanceof String s) t.user = s;
            if (m.get("password") instanceof String s) t.password = s;
            if (m.get("refresh-interval-seconds") instanceof Number n) t.refreshIntervalSeconds = n.intValue();
            return t;
        }
    }

    public static HeroTabConfig defaults() {
        return new HeroTabConfig();
    }

    @SuppressWarnings("unchecked")
    public static HeroTabConfig fromMap(Map<String, Object> raw) {
        HeroTabConfig c = new HeroTabConfig();

        if (raw.get("update-interval-ticks") instanceof Number n) c.updateIntervalTicks = n.longValue();
        if (raw.get("player-format") instanceof String s) c.playerFormat = s;
        if (raw.get("header") instanceof List<?> l) c.header = toStringList(l);
        if (raw.get("footer") instanceof List<?> l) c.footer = toStringList(l);
        if (raw.get("animation-interval-ticks") instanceof Number n) c.animationIntervalTicks = n.longValue();
        if (raw.get("sort-mode") instanceof String s) c.sortMode = s.toUpperCase();
        if (raw.get("reorder-mode") instanceof String s) c.reorderMode = s.toLowerCase();
        if (raw.get("group-spacer-enabled") instanceof Boolean b) c.groupSpacerEnabled = b;
        if (raw.get("group-spacer-text") instanceof String s) c.groupSpacerText = s;
        if (raw.get("allow-minimessage") instanceof Boolean b) c.allowMiniMessage = b;
        if (raw.get("theme-primary") instanceof String s) c.themePrimary = s;
        if (raw.get("theme-secondary") instanceof String s) c.themeSecondary = s;
        if (raw.get("network-address") instanceof String s) c.networkAddress = s;
        if (raw.get("website-address") instanceof String s) c.websiteAddress = s;
        if (raw.get("factions-server-name") instanceof String s) c.factionsServerName = s;

        if (raw.get("grades-mysql") != null) c.gradesMysql = MySQLTarget.fromMap(raw.get("grades-mysql"));
        if (raw.get("factions-mysql") != null) c.factionsMysql = MySQLTarget.fromMap(raw.get("factions-mysql"));

        if (raw.get("chat-faction-tag-enabled") instanceof Boolean b) c.chatFactionTagEnabled = b;
        if (raw.get("chat-faction-format") instanceof String s) c.chatFactionFormat = s;

        Object groups = raw.get("server-groups");
        if (groups instanceof Map<?, ?> gm) {
            Map<String, String> parsed = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : gm.entrySet()) {
                parsed.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
            c.serverGroups = parsed;
        }

        Object colors = raw.get("server-colors");
        if (colors instanceof Map<?, ?> cm) {
            Map<String, String> parsed = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : cm.entrySet()) {
                parsed.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
            c.serverColors = parsed;
        }

        return c;
    }

    private static List<String> toStringList(List<?> l) {
        List<String> out = new ArrayList<>();
        for (Object o : l) out.add(String.valueOf(o));
        return out;
    }
}
