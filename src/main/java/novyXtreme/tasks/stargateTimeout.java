package novyXtreme.tasks;

import novyXtreme.NovyXtreme;
import novyXtreme.Stargate;
import novyXtreme.utils.activationUtil;
import novyXtreme.utils.messageUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class stargateTimeout extends BukkitRunnable
    {
        NovyXtreme plugin;
        Stargate stargate;
        Player player;

        public stargateTimeout(NovyXtreme plugin, Stargate stargate, Player player)
        {
            this.player = player;
            this.stargate = stargate;
            this.plugin = plugin;
        }

        @Override
        public void run()
        {
            if(stargate.isActive())
            {
                activationUtil.deactivateGate(stargate, player);
                //stargate.setActive(false);
                messageUtils.sendMessage("Stargate: " + stargate.getName() + " Timed out", player);
                //watch for crossthread issue
            }

        }

    }
