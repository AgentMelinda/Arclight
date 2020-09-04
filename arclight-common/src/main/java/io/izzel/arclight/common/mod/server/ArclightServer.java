package io.izzel.arclight.common.mod.server;

import io.izzel.arclight.common.bridge.bukkit.CraftServerBridge;
import io.izzel.arclight.common.bridge.server.MinecraftServerBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.management.PlayerList;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.command.ColouredConsoleSender;

import java.io.File;
import java.util.Objects;

public class ArclightServer {

    private static CraftServer server;

    @SuppressWarnings("ConstantConditions")
    public static CraftServer createOrLoad(DedicatedServer console, PlayerList playerList) {
        if (server == null) {
            try {
                server = new CraftServer(console, playerList);
                ((MinecraftServerBridge) console).bridge$setServer(server);
                ((MinecraftServerBridge) console).bridge$setConsole(ColouredConsoleSender.getInstance());
                ArclightPermissionHandler.INSTANCE.initialize();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            try {
                ArclightMod.LOGGER.info("registry.begin");
                BukkitRegistry.registerAll();
                org.spigotmc.SpigotConfig.init(new File("./spigot.yml"));
                org.spigotmc.SpigotConfig.registerCommands();
            } catch (Throwable t) {
                ArclightMod.LOGGER.error("registry.error", t);
            }
        } else {
            ((CraftServerBridge) (Object) server).bridge$setPlayerList(playerList);
        }
        return server;
    }

    public static CraftServer get() {
        return Objects.requireNonNull(server);
    }

    public static MinecraftServer getMinecraftServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }
}
