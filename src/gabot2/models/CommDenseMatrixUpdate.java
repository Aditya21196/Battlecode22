package gabot2.models;

import battlecode.common.MapLocation;
import gabot2.enums.CommInfoBlockType;

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
