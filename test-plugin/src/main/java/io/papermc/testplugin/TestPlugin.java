package io.papermc.testplugin;

import io.papermc.testplugin.genetics.GeneticsBreedListener;
import io.papermc.testplugin.genetics.GeneticsDemoCommand;
import io.papermc.testplugin.genetics.GeneticsInspectListener;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class TestPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getPluginManager().registerEvents(new GeneticsBreedListener(this), this);
        this.getServer().getPluginManager().registerEvents(new GeneticsInspectListener(), this);

        // Paper plugins: use registerCommand — not getCommand / plugin.yml commands.
        this.registerCommand(
            "geneticsdemo",
            "Offline mintychochip genetics pure-API demo",
            new GeneticsDemoCommand()
        );

        this.getComponentLogger().info("mintychochip genetics demo enabled — breed animals or /geneticsdemo");

        // io.papermc.testplugin.brigtests.Registration.registerViaOnEnable(this);
    }
}
