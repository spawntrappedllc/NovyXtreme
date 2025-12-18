package novyXtreme.commands;


import novyXtreme.Stargate;
import novyXtreme.utils.gateValidation;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import novyXtreme.utils.dbFunctions;
import novyXtreme.utils.messageUtils;

public class dial implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
        {
            Stargate entranceGate = null;
            if (sender instanceof Player)
            {
                Player player = (Player) sender;

                //checks if player already has an active gate.
                entranceGate = dbFunctions.getActivatedGate(sender.getName());
                if(entranceGate != null)
                {
                    boolean hasPremium = player.hasPermission("novyxtreme.premium");

                    if (!hasPremium
                            && entranceGate.getOwner() != null
                            && entranceGate.getOwner().equalsIgnoreCase(player.getName())) {
                        dbFunctions.removeAllGatesForOwnerExcept(player.getName(), entranceGate.getName());
                    }

                    if(args.length != 1){messageUtils.sendMessage("Must specify stargate name!", player); return true;}
                    if(entranceGate.getDestinationGate() != null){messageUtils.sendMessage("You already have an active portal!", player);return true;}
                    Stargate destinationStargate = dbFunctions.getGatebyName(args[0]);
                    if(destinationStargate == entranceGate){messageUtils.sendMessage("Destination gate cannot be origin gate", player); return true;}
                    if(destinationStargate == null){messageUtils.sendMessage("No gate by that name found", player); return true;}
                    if(destinationStargate.isActive()){messageUtils.sendMessage("That gate is currently in use!", player); return true;}
                    if(!gateValidation.checkTestStargate(gateValidation.buildTestGate(destinationStargate.getLeverBlock(), destinationStargate.getGateOrientation())))
                    {
                        messageUtils.sendMessage("Destination gate no longer exists", player);
                        dbFunctions.removeGateByName(destinationStargate.getName());
                        return true;
                    }
                    //TODO breaking a stargate after activating it then /dialing will not destroy gate allowing for floating portals


                  /* if(!gateValidation.checkActiveTestStargate(gateValidation.buildTestGate(entranceGate.getLeverBlock(), entranceGate.getGateOrientation())))
                    {
                        sender.sendMessage(ChatColor.DARK_PURPLE + "[NovyXTreme]: " + ChatColor.GRAY + "Stargate no longer exists");
                        dbFunctions.removeGateByName(entranceGate.getName());
                        return true;
                    }*/
                    destinationStargate.setActivatedby(sender.getName());
                    entranceGate.setPortal(true, destinationStargate);

                    messageUtils.sendMessage("Portal Connected!", player);
                    destinationStargate.setTimesVisited(destinationStargate.getTimesVisited()+1);

                } else
                {
                    messageUtils.sendMessage("You have not activated a stargate!", player);
                }

            }
            return true;
        }
}
