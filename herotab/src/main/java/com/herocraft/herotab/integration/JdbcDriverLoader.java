package com.herocraft.herotab.integration;

/**
 * Sur Velocity, chaque plugin a son propre classloader isolé. Le mécanisme
 * automatique de découverte de driver JDBC (java.util.ServiceLoader, utilisé
 * en interne par DriverManager) s'appuie sur le classloader de contexte du
 * thread courant, qui n'est pas forcément celui du plugin quand on est
 * appelé depuis une tâche planifiée du proxy — résultat : "No suitable
 * driver found" même si mysql-connector-j est bien dans le jar shadé.
 *
 * On force donc explicitement le chargement de la classe du driver une
 * fois : son bloc static s'enregistre alors lui-même auprès de
 * DriverManager, et les connexions suivantes fonctionnent normalement.
 */
final class JdbcDriverLoader {

    private static volatile boolean loaded = false;

    private JdbcDriverLoader() {
    }

    static synchronized void ensureLoaded() throws ClassNotFoundException {
        if (loaded) return;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e1) {
            Class.forName("com.mysql.jdbc.Driver");
        }
        loaded = true;
    }
}
