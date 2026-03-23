package novyXtreme;

import novyXtreme.utils.activationUtil;
import novyXtreme.utils.dbFunctions;
import novyXtreme.utils.gateValidation;
import novyXtreme.utils.stargateUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;

import java.util.UUID;

public class Stargate {

    private String name;
    private UUID ownerUuid;
    private transient Location leverBlock;
    private BlockFace facing;
    private Location tpCoordinates;
    private transient boolean isActive;
    private boolean locked = false;
    private transient Location[] portalBlocks;
    private Location signBlockLocation;
    private Location[] irisBlocks;
    private transient Stargate destinationGate;
    private int timesVisited;
    private String activatedby;

    public Stargate(String name, UUID ownerUuid, Location leverBlock, BlockFace facing) {
        this.name        = name;
        this.ownerUuid   = ownerUuid;
        this.leverBlock  = leverBlock;
        this.facing      = facing;
        this.tpCoordinates     = stargateUtils.calcTeleportBlock(this.leverBlock, this.facing);
        this.signBlockLocation = stargateUtils.calcGateSignLocation(leverBlock, facing);
        this.irisBlocks        = stargateUtils.calcIrisBlocks(leverBlock, facing);
        this.portalBlocks      = stargateUtils.calcPortalBlocks(leverBlock, facing);
        createGateSign();
        dbFunctions.addGateToList(this);
    }

    public Stargate() {}

    // getters

    public String     getActivatedby()       { return activatedby; }
    public Location   getLeverBlock()        { return leverBlock;}
    public BlockFace  getGateOrientation()   { return facing; }
    public String     getName()              { return name; }
    public boolean    isActive()             { return isActive; }
    public Location   getTpCoordinates()     { return tpCoordinates; }
    public Location[] getPortalBlocks()      { return portalBlocks; }
    public UUID       getOwnerUuid()         { return ownerUuid; }
    public Stargate   getDestinationGate()   { return destinationGate; }
    public BlockFace  getFacing()            { return facing; }
    public Location   getSignBlockLocation() { return signBlockLocation; }
    public Location[] getIrisBlocks()        { return irisBlocks; }
    public int        getTimesVisited()      { return timesVisited; }
    public boolean    isLocked()             { return locked; }

    public String getOwnerName() {
        if (ownerUuid == null) return "Unknown";
        OfflinePlayer op = Bukkit.getOfflinePlayer(ownerUuid);
        String name = op.getName();
        return (name != null && !name.isEmpty()) ? name : ownerUuid.toString();
    }

    // setters

    public void setName(String name)                     { this.name = name; }
    public void setOwnerUuid(UUID ownerUuid)             { this.ownerUuid = ownerUuid; }
    public void setLeverBlock(Location leverBlock)       { this.leverBlock = leverBlock; }
    public void setFacing(BlockFace facing)              { this.facing = facing; }
    public void setFacingString(String facing)           { this.facing = BlockFace.valueOf(facing); }
    public void setTpCoordinates(Location tpCoordinates) { this.tpCoordinates = tpCoordinates; }
    public void setPortalBlocks(Location[] portalBlocks) { this.portalBlocks = portalBlocks; }
    public void setSignBlockLocation(Location loc)       { this.signBlockLocation = loc; }
    public void setIrisBlocks(Location[] irisBlocks)     { this.irisBlocks = irisBlocks; }
    public void setActivatedby(String playerName)        { this.activatedby = playerName; }
    public void setDestinationGate(Stargate gate)        { this.destinationGate = gate; }

    public void setTimesVisited(int timesVisited) {
        this.timesVisited = timesVisited;
        dbFunctions.updateGateInDb(this);
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        updateGateSign();
        dbFunctions.updateGateInDb(this);
    }

    public void setLockedSilent(boolean locked) {
        this.locked = locked;
    }

    // sign methods

    public void createGateSign() {
        Block signBlock = signBlockLocation.getBlock();
        signBlock.setType(Material.OAK_WALL_SIGN);
        Sign sign = (Sign) signBlock.getState();
        Directional signData = (Directional) sign.getBlockData();
        signData.setFacing(facing);
        sign.setBlockData(signData);
        sign.setLine(1, "-" + name + "-");
        sign.setLine(2, "O:" + getOwnerName());
        sign.update(true, true);
    }

    public void updateGateSign() {
        if (signBlockLocation == null) return;
        Block signBlock = signBlockLocation.getBlock();
        if (signBlock.getType() != Material.OAK_WALL_SIGN) return;
        Sign sign = (Sign) signBlock.getState();
        sign.setLine(1, "-" + name + "-");
        sign.setLine(2, "O:" + getOwnerName());
        sign.setLine(3, locked ? "[LOCKED]" : "");
        sign.update(true, true);
    }

    //

    public boolean setActive(boolean active) {
        if (active) {
            for (Location irisLoc : irisBlocks) {
                if (!irisLoc.getBlock().getType().equals(Material.OBSIDIAN)) return false;
                irisLoc.getBlock().setType(Material.GLOWSTONE);
            }
            dbFunctions.activeStargates.add(this);
            if (this.destinationGate != null) {
                dbFunctions.activeStargates.add(this.destinationGate);
            }
        } else {
            for (Location irisLoc : irisBlocks) {
                if (irisLoc.getBlock().getType().equals(Material.GLOWSTONE)) {
                    irisLoc.getBlock().setType(Material.OBSIDIAN);
                }
            }
            this.setActivatedby(null);
            dbFunctions.activeStargates.remove(this);
            dbFunctions.activeStargates.remove(this.destinationGate);

            if (this.getDestinationGate() != null) {
                this.getDestinationGate().setActive(false);
                this.setDestinationGate(null);
            }
            if (!gateValidation.checkTestStargate(
                    gateValidation.buildTestGate(this.getLeverBlock(), this.getGateOrientation()))) {
                return false;
            }
        }

        isActive = active;
        return true;
    }

    public void setPortal(boolean active, Stargate destinationGate) {
        // why does this take an active argument and do nothing with it?
        this.destinationGate = destinationGate;
        activationUtil.activatePortal(this);
        activationUtil.activatePortal(destinationGate);
    }
}