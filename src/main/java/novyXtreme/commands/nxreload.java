package novyXtreme.commands;

import novyXtreme.NovyXtreme;
import novyXtreme.utils.dbFunctions;
import novyXtreme.utils.messageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;

public class nxreload implements CommandExecutor
{
    Plugin plugin = NovyXtreme.getPlugin(NovyXtreme.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!sender.hasPermission("novyxtreme.debug"))
        {
            messageUtils.sendMessage("You do not have permission to use that command!", sender);
            return true;
        }

        try
        {
            dbFunctions.saveStargates();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }

        try
        {
            dbFunctions.loadStargates();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }

        plugin.reloadConfig();
        messageUtils.sendMessage("Reload Complete", sender);
        return true;
    }
}