package fr.xephi.authme.process.register;

import me.muizers.Notifications.Notification;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.events.RegisterTeleportEvent;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;

public class ProcessSyncronousEmailRegister implements Runnable {

	protected Player player;
	protected String name;
	private AuthMe plugin;
	private Messages m = Messages.getInstance();
	public ProcessSyncronousEmailRegister(Player player, AuthMe plugin) {
		this.player = player;
		this.name = player.getName().toLowerCase();
		this.plugin = plugin;
	}
	@Override
	public void run() {
        if(!Settings.getRegisteredGroup.isEmpty()){
            Utils.getInstance().setGroup(player, Utils.groupType.REGISTERED);
        }
        m._(player, "vb_nonActiv");
    	int time = Settings.getRegistrationTimeout * 20;
    	int msgInterval = Settings.getWarnMessageInterval;
        if (time != 0) {
        	Bukkit.getScheduler().cancelTask(LimboCache.getInstance().getLimboPlayer(name).getTimeoutTaskId());
            int id = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new TimeoutTask(plugin, name), time);
            LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id);
        }

        Bukkit.getScheduler().cancelTask(LimboCache.getInstance().getLimboPlayer(name).getMessageTaskId());
        int nwMsg = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new MessageTask(plugin, name, m._("login_msg"), msgInterval));
        LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(nwMsg);

        if (Settings.isTeleportToSpawnEnabled && !Settings.noTeleport) {
        	Location loca = plugin.getSpawnLocation(player);
            RegisterTeleportEvent tpEvent = new RegisterTeleportEvent(player, loca);
            plugin.getServer().getPluginManager().callEvent(tpEvent);
            if(!tpEvent.isCancelled()) {
            	if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
            		tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
            	}
          	  	player.teleport(tpEvent.getTo());
            }
        }
        if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
        if (Settings.applyBlindEffect)
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.saveData();
        if (!Settings.noConsoleSpam)
        ConsoleLogger.info(player.getName() + " registered "+plugin.getIP(player));
        if(plugin.notifications != null) {
        	plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " has registered by email!"));
        }
	}

}
