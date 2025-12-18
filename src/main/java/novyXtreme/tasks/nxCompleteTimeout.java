package novyXtreme.tasks;

import novyXtreme.NovyXtreme;
import novyXtreme.utils.activationUtil;
import novyXtreme.utils.messageUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;


public class nxCompleteTimeout extends BukkitRunnable
{
    NovyXtreme plugin;
    Player player;

    public nxCompleteTimeout(NovyXtreme plugin, Player player) {
        this.player = player;
        this.plugin = plugin;
    }
//An async task which manages the timeout after a valid gate has been activated
    @Override
    public void run()
    {
        if (player.hasMetadata("NxCompleteActive"))
        {
            messageUtils.sendMessage("NxComplete Timed out", player);
            activationUtil.nxcompleteEnd(player);
        }
    }
}


