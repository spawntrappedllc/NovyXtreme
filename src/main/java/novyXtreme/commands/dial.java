package novyXtreme.commands;


import novyXtreme.NovyXtreme;
import novyXtreme.Stargate;
import novyXtreme.utils.gateValidation;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import novyXtreme.utils.dbFunctions;
import novyXtreme.utils.messageUtils;
import org.bukkit.plugin.Plugin;

public class dial implements CommandExecutor
{
    Plugin plugin = NovyXtreme.getPlugin(NovyXtreme.class);

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

                    //TODO: do something about this duplication (code duplicated from nxcomplete)
                    // Stargate limit
                    int defaultMax = plugin.getConfig().contains("MaxStargatesPerPlayer")
                            ? plugin.getConfig().getInt("MaxStargatesPerPlayer")
                            : 1;

                    int maxPremium = plugin.getConfig().contains("MaxPremiumStargatesPerPlayer")
                            ? plugin.getConfig().getInt("MaxPremiumStargatesPerPlayer")
                            : 3;

                    boolean bypassPermission = player.hasPermission("novyxtreme.bypass.maxstargates");
                    boolean hasPremium = player.hasPermission("novyxtreme.premium");

                    // choose the allowed amount depending on premium
                    int allowed = hasPremium ? maxPremium : defaultMax;
                    int owned = dbFunctions.getStargateCountByOwner(player.getUniqueId());
                    if (!bypassPermission && owned > allowed) {
                        messageUtils.sendMessage("Your premium subscription has expired. Please pick a gate to keep, using /stkeep", player);
                        return true;
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

                    if (destinationStargate.isLocked() && !player.hasPermission("novyxtreme.bypass.nxlock") && !player.getUniqueId().equals(destinationStargate.getOwnerUuid())) {
                        messageUtils.sendMessage("No gate by that name found", player);
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
