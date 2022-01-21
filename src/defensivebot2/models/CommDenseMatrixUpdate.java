package defensivebot2.models;

import defensivebot2.enums.CommInfoBlockType;

public class CommDenseMatrixUpdate {
    public CommInfoBlockType commInfoBlockType;
    public int val;
    public CommDenseMatrixUpdate(int val, CommInfoBlockType commInfoBlockType){
        this.val=val;
        this.commInfoBlockType = commInfoBlockType;
    }
}
