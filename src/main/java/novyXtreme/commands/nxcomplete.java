package novyXtreme.commands;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import novyXtreme.NovyXtreme;
import novyXtreme.Stargate;
import novyXtreme.utils.activationUtil;
import novyXtreme.utils.dbFunctions;
import novyXtreme.utils.messageUtils;
import novyXtreme.utils.stargateUtils;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.Location;
import java.util.List;

public class nxcomplete implements CommandExecutor {
    Plugin plugin = NovyXtreme.getPlugin(NovyXtreme.class);
    double minimumStargateDistance = plugin.getConfig().getDouble("MinimumStargateDistance");


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player) {

            Block leverblock = null;
            Player p = (Player) sender;
            //Player has not activated a stargate:
            if (!p.hasMetadata("NxCompleteActive")) {
                messageUtils.sendMessage("You have not activated a stargate.", p);
                return true;
            }

            //Player has activated a stargate:
            List<MetadataValue> metadata = p.getMetadata("NxCompleteActive");

            for (MetadataValue val : metadata) {
                //get the metadata associated with NovyXtreme (NxCompleteActive) and it's associated block.
                if (NovyXtreme.getPlugin().equals(val.getOwningPlugin())) {
                    leverblock = (Block) val.value();
                }
            }
            String GateName = args[0];

            //Check gatename is alphanumeric (I'm not dumb enough to forget this and have some kid do a JSON injection and wipe the entire database right?.. RIGHT?)
            if (!GateName.matches("^[a-zA-Z0-9_-]+$")) {
                messageUtils.sendMessage("Gate name can only contain letters, numbers, hyphens, and underscores.", p);
                return true;
            }

            // Stargate limit
            int defaultMax = plugin.getConfig().contains("MaxStargatesPerPlayer")
                    ? plugin.getConfig().getInt("MaxStargatesPerPlayer")
                    : 1;

            int maxPremium = plugin.getConfig().contains("MaxPremiumStargatesPerPlayer")
                    ? plugin.getConfig().getInt("MaxPremiumStargatesPerPlayer")
                    : 3;

            boolean bypassPermission = p.hasPermission("novyxtreme.bypass.maxstargates");
            boolean hasPremium = p.hasPermission("novyxtreme.premium");

            // choose the allowed amount depending on premium
            int allowed = hasPremium ? maxPremium : defaultMax;

            int owned = dbFunctions.getStargateCountByOwner(p.getName());
            if (!bypassPermission && owned >= allowed) {
                messageUtils.sendMessage("You already own the maximum number of stargates (" + allowed + ").", p);
                return true;
            }


            //Assign directional interface to leverblock for directional shenanigans
            Directional leverBlockData = (Directional) leverblock.getBlockData();
            Location teleportBlock = stargateUtils.calcTeleportBlock(leverblock.getLocation(), leverBlockData.getFacing());
            Stargate closestStargate = stargateUtils.getClosestGate(teleportBlock);

            //Stargate does not already exist
            if (dbFunctions.getGatebyName(GateName) == null) {
                if(closestStargate != null){
                    if (closestStargate.getTpCoordinates().distance(teleportBlock) < minimumStargateDistance){
                        messageUtils.sendMessage("Stargate is too close to another gate", p);
                        return true;
                    }
                }
                messageUtils.sendMessage("Stargate successfully created!", p);
                Stargate newStargate = new Stargate(GateName, p.getName(), leverblock.getLocation(), leverBlockData.getFacing());
                activationUtil.nxcompleteEnd(p);
                return true;

            } else {
                //Gatename already exists
                messageUtils.sendMessage("There is already a gate by that name!", p);
            }


        }
        return true;
    }


}
