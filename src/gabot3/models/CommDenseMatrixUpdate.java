package gabot3.models;

import battlecode.common.MapLocation;
import gabot3.enums.CommInfoBlockType;

public class CommDenseMatrixUpdate {
    public CommInfoBlockType commInfoBlockType;
    public int val;
    public MapLocation targetOverride;

    public CommDenseMatrixUpdate(int val, CommInfoBlockType commInfoBlockType,MapLocation targetOverride){
        this.val=val;
        this.commInfoBlockType = commInfoBlockType;
        this.targetOverride = targetOverride;
    }
}
