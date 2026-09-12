package com.herocraft.herotab.command;

import com.herocraft.herotab.HeroTabPlugin;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class HeroTabCommand implements SimpleCommand {

    private final HeroTabPlugin plugin;

    public HeroTabCommand(HeroTabPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            invocation.source().sendMessage(Component.text("Usage: /herotab reload", NamedTextColor.GRAY));
            return;
        }

        if (!invocation.source().hasPermission("herotab.admin")) {
            invocation.source().sendMessage(Component.text("Tu n'as pas la permission pour ça.", NamedTextColor.RED));
            return;
        }

        plugin.reload();
        invocation.source().sendMessage(Component.text("HeroTab rechargé.", NamedTextColor.GREEN));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("herotab.admin");
    }
}
