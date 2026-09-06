package com.herocraft.herotab.integration;

/** Faction + rang d'un joueur, tel que synchronisé par FactionPlugin dans la table faction_tab_sync. */
public record FactionInfo(String factionName, String rankName, String rankColor, String rankIcon) {
}
