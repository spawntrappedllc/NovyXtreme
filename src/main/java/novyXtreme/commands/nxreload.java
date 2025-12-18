package novyXtreme.commands;

import novyXtreme.NovyXtreme;
import novyXtreme.Stargate;
import novyXtreme.utils.dbFunctions;
import novyXtreme.utils.messageUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.ArrayList;

public class nxreload implements CommandExecutor
{
    Plugin plugin = NovyXtreme.getPlugin(NovyXtreme.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(!sender.hasPermission("novyxtreme.debug")){messageUtils.sendMessage("You do not have permission to use that command!", sender);return true;}
        try
        {
            dbFunctions.saveStargates();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        try
        {
            dbFunctions.loadStargates();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        plugin.reloadConfig();
        messageUtils.sendMessage("Reload Complete", sender);
        return true;
    }
}
