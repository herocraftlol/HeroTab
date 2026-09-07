package com.herocraft.herotab;

import com.herocraft.herotab.config.ConfigManager;
import com.herocraft.herotab.config.HeroTabConfig;
import com.herocraft.herotab.integration.FactionInfo;
import com.herocraft.herotab.integration.FactionSync;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;

/**
 * Ajoute le tag de faction du joueur devant son message de chat, réseau
 * entier — peu importe le sous-serveur/monde sur lequel il se trouve —
 * en s'appuyant sur les mêmes données que le tab (table faction_tab_sync,
 * lue par FactionSync, cf. TabListManager pour le même principe côté tab).
 *
 * Le message brut envoyé au serveur backend est réécrit via
 * PlayerChatEvent.ChatResult.message(...), l'API officielle de Velocity
 * prévue pour ce cas d'usage.
 *
 * ATTENTION (chat sécurisé / 1.19.1+) : réécrire le contenu du message
 * change forcément sa signature cryptographique d'origine. Sur la grande
 * majorité des setups proxy + backend (Velocity + Paper avec la
 * redirection moderne), cela fonctionne normalement — Velocity gère la
 * bascule vers un message non signé. Si le backend a activé
 * enforce-secure-profile de façon stricte, certains clients peuvent
 * afficher une icône "non sécurisé" sur ces messages : c'est cosmétique,
 * pas bloquant, mais à tester sur ton setup avant la prod.
 */
public class ChatManager {

    private final ConfigManager configManager;
    private volatile FactionSync factionSync;

    public ChatManager(ConfigManager configManager, FactionSync factionSync) {
        this.configManager = configManager;
        this.factionSync = factionSync;
    }

    /** Permet à /herotab reload de brancher une nouvelle instance (nouvelle config MySQL) sans tout recréer. */
    public void setFactionSync(FactionSync factionSync) {
        this.factionSync = factionSync;
    }

    @Subscribe(order = PostOrder.LATE)
    public void onChat(PlayerChatEvent event) {
        HeroTabConfig cfg = configManager.getConfig();
        if (!cfg.chatFactionTagEnabled) return;
        if (factionSync == null || !factionSync.isEnabled()) return;

        Player player = event.getPlayer();
        FactionInfo faction = factionSync.get(player.getUniqueId());
        if (faction == null || faction.factionName() == null || faction.factionName().isBlank()) return;

        String prefix = buildPrefix(faction, cfg);
        if (prefix.isEmpty()) return;

        event.setResult(PlayerChatEvent.ChatResult.message(prefix + event.getMessage()));
    }

    private String buildPrefix(FactionInfo faction, HeroTabConfig cfg) {
        String color = faction.rankColor() != null && !faction.rankColor().isBlank() ? faction.rankColor() : "&7";
        String icon = faction.rankIcon() != null && !faction.rankIcon().isBlank() ? faction.rankIcon() + " " : "";

        String raw = cfg.chatFactionFormat
                .replace("%faction%", faction.factionName())
                .replace("%faction_rank%", faction.rankName() != null ? faction.rankName() : "")
                .replace("%faction_color%", color)
                .replace("%faction_icon%", icon)
                .replace("%primary%", cfg.themePrimary)
                .replace("%secondary%", cfg.themeSecondary);

        return translateColors(raw);
    }

    /**
     * Convertit les codes &amp; (ex: &amp;6) en codes couleur § réels : le message brut
     * envoyé au serveur backend est un simple texte, pas un Component, donc les
     * codes doivent déjà être sous leur forme finale pour s'afficher en couleur.
     */
    private String translateColors(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        String valid = "0123456789abcdefklmnorABCDEFKLMNOR";
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length() && valid.indexOf(text.charAt(i + 1)) >= 0) {
                sb.append('§').append(Character.toLowerCase(text.charAt(i + 1)));
                i++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
