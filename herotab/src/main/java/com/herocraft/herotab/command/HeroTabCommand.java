package com.herocraft.herotab.command;

import com.herocraft.herotab.HeroTabPlugin;
import com.herocraft.herotab.config.HeroTabConfig;
import com.herocraft.herotab.integration.FactionSync;
import com.herocraft.herotab.integration.GradeSync;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Map;

public class HeroTabCommand implements SimpleCommand {

    private final HeroTabPlugin plugin;

    public HeroTabCommand(HeroTabPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            sendHelp(invocation);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                runReload(invocation);
                break;
            case "status":
                runStatus(invocation);
                break;
            default:
                sendHelp(invocation);
        }
    }

    private void sendHelp(Invocation invocation) {
        invocation.source().sendMessage(Component.text("HeroTab v" + plugin.getPluginVersion() + " — commandes :", NamedTextColor.GRAY));
        invocation.source().sendMessage(Component.text("  /herotab reload", NamedTextColor.GRAY)
                .append(Component.text(" — recharger config.yml & intégrations", NamedTextColor.DARK_GRAY)));
        invocation.source().sendMessage(Component.text("  /herotab status", NamedTextColor.GRAY)
                .append(Component.text(" — afficher l'état du plugin (intégrations, caches, joueurs)", NamedTextColor.DARK_GRAY)));
    }

    private void runReload(Invocation invocation) {
        if (!invocation.source().hasPermission("herotab.admin")) {
            invocation.source().sendMessage(Component.text("Tu n'as pas la permission pour ça.", NamedTextColor.RED));
            return;
        }
        plugin.reload();
        invocation.source().sendMessage(Component.text("HeroTab rechargé.", NamedTextColor.GREEN));
    }

    private void runStatus(Invocation invocation) {
        if (!invocation.source().hasPermission("herotab.admin")) {
            invocation.source().sendMessage(Component.text("Tu n'as pas la permission pour ça.", NamedTextColor.RED));
            return;
        }

        HeroTabConfig cfg = plugin.getConfigManager().getConfig();
        int online = plugin.getServer().getPlayerCount();
        int max = plugin.getServer().getConfiguration().getShowMaxPlayers();

        Component header = Component.text("── HeroTab v", NamedTextColor.GRAY)
                .append(Component.text(plugin.getPluginVersion(), NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text(" ──", NamedTextColor.GRAY));
        invocation.source().sendMessage(header);

        // Joueurs
        Map<String, Integer> byServer = new java.util.HashMap<>();
        for (var p : plugin.getServer().getAllPlayers()) {
            byServer.merge(p.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("?"), 1, Integer::sum);
        }
        String servers = byServer.isEmpty() ? "aucun" : byServer.toString();
        invocation.source().sendMessage(
                Component.text("Joueurs : ", NamedTextColor.GRAY)
                        .append(Component.text(online + "/" + max, NamedTextColor.WHITE))
                        .append(Component.text("  •  Sous-serveurs : ", NamedTextColor.GRAY))
                        .append(Component.text(servers, NamedTextColor.DARK_GRAY))
        );

        // Mode d'affichage
        invocation.source().sendMessage(
                Component.text("Sort : ", NamedTextColor.GRAY)
                        .append(Component.text(cfg.sortMode, NamedTextColor.WHITE))
                        .append(Component.text("  •  Reorder : ", NamedTextColor.GRAY))
                        .append(Component.text(cfg.reorderMode, NamedTextColor.WHITE))
                        .append(Component.text("  •  Spacer : ", NamedTextColor.GRAY))
                        .append(Component.text(cfg.groupSpacerEnabled ? "oui" : "non", NamedTextColor.WHITE))
        );

        // Thème & refresh
        invocation.source().sendMessage(
                Component.text("Thème : ", NamedTextColor.GRAY)
                        .append(Component.text("primary=" + cfg.themePrimary + " ", NamedTextColor.WHITE))
                        .append(Component.text("secondary=" + cfg.themeSecondary + " ", NamedTextColor.WHITE))
                        .append(Component.text("  •  Refresh : ", NamedTextColor.GRAY))
                        .append(Component.text(cfg.updateIntervalTicks + " ticks", NamedTextColor.WHITE))
                        .append(Component.text("  •  MiniMessage : ", NamedTextColor.GRAY))
                        .append(Component.text(cfg.allowMiniMessage ? "oui" : "non", NamedTextColor.WHITE))
        );

        // Intégrations
        GradeSync gs = plugin.getGradeSync();
        FactionSync fs = plugin.getFactionSync();
        String gradesStatus = gs != null && gs.isEnabled()
                ? "activée (cache=" + gs.cacheSize() + ", refresh=" + gs.getRefreshIntervalSeconds() + "s)"
                : "désactivée";
        String factionsStatus = fs != null && fs.isEnabled()
                ? "activée (cache=" + fs.cacheSize() + ", refresh=" + fs.getRefreshIntervalSeconds() + "s)"
                : "désactivée";
        invocation.source().sendMessage(
                Component.text("GradePlugin : ", NamedTextColor.GRAY)
                        .append(Component.text(gradesStatus, NamedTextColor.WHITE))
        );
        invocation.source().sendMessage(
                Component.text("FactionPlugin : ", NamedTextColor.GRAY)
                        .append(Component.text(factionsStatus, NamedTextColor.WHITE))
        );

        invocation.source().sendMessage(Component.text("──────────────────────────────────────────", NamedTextColor.DARK_GRAY));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("herotab.admin");
    }
}
