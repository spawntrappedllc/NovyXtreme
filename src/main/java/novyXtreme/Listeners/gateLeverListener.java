package novyXtreme.Listeners;

import novyXtreme.Stargate;
import novyXtreme.utils.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.Block;

public class gateLeverListener implements Listener {
    @EventHandler
    public void onLeverPull(PlayerInteractEvent e) {
        Block leverBlock = e.getClickedBlock();
        if (e.getClickedBlock() == null) {
            return;
        }
        if (e.getClickedBlock().getType() == Material.LEVER) {

            Directional leverBlockData = (Directional) leverBlock.getBlockData();
            Player player = e.getPlayer();
            //checks is pedestal is correct
            if (gateValidation.checkPedestal(leverBlock, leverBlockData.getFacing())) {
                String checkedGate = dbFunctions.isGateHere(player, leverBlockData.getFacing(), leverBlock.getLocation());
                if (checkedGate == null) {
                    if (gateValidation.checkTestStargate(gateValidation.buildTestGate(leverBlock.getLocation(), leverBlockData.getFacing()))) {
                        // prompts user with /nxcomplete [gatename] if validation passes
                        // adds nxactive metadata to player
                        stargateUtils.promptNxComplete(player, leverBlock);
                        return;
                    }
                    return;
                } // return if no gate found at location and no valid structure


                Stargate stargate = dbFunctions.getGatebyName(checkedGate);

                if (stargate.isActive()) {
                    // if stargate is active, deactivate it and inform player
                    activationUtil.deactivateGate(stargate, player);
                    messageUtils.sendMessage("Stargate Deactivated!", player);
                    return;
                }

                if (!gateValidation.checkTestStargate(gateValidation.buildTestGate(stargate.getLeverBlock(), stargate.getGateOrientation()))) {
                    messageUtils.sendMessage("This stargate does not have a valid structure.. please re-construct and activate", player);
                    dbFunctions.removeGateByName(stargate.getName());
                    return;
                }
                if (dbFunctions.getActivatedGate(player.getName()) != null) {
                    messageUtils.sendMessage("You may only have one active stargate at a time!", player);
                    return;
                }
                stargateUtils.promptDial(player, stargate);
            }

        }
    }
}
