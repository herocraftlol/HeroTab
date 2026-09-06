package com.herocraft.herotab.integration;

/** Grade le plus prioritaire d'un joueur, tel que stocké par GradePlugin (MySQL). */
public record GradeInfo(String displayName, String prefix, String suffix, String color) {
}
