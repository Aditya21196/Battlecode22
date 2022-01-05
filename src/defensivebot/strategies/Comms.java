package defensivebot.strategies;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import defensivebot.datasturctures.LinkedList;
import defensivebot.enums.CommInfoBlock;

import static defensivebot.utils.CustomMath.ceilDivision;

public class Comms {



    class CommDenseMatrixUpdate{
        int x,y,val;
        CommInfoBlock commInfoBlock;
        CommDenseMatrixUpdate(int x,int y,int val,CommInfoBlock commInfoBlock){
            this.x=x;
            this.y=y;
            this.val=val;
            this.commInfoBlock=commInfoBlock;
        }

    }

    private int xSectorSize,ySectorSize,xSectors,ySectors,blockOffset, sparseSignalOffset;
    private int[] data = new int[64];
    private LinkedList<CommDenseMatrixUpdate> commUpdateLinkedList = new LinkedList<>();

    private RobotController rc;

    public void processUpdateQueue() throws GameActionException{
        // we need offset within info block // TODO: Increment lead for all x,y in same sector
        // TODO: Impose update restriction
        readSharedData();
        int[] updatedCommsValues = new int[64];
        for(int i=64;--i>=0;)updatedCommsValues[i]=data[i];
        while(commUpdateLinkedList.size>0){
            CommDenseMatrixUpdate update = commUpdateLinkedList.dequeue().val;
            // TODO: put this entire thing in a function in a function
            int offset = blockOffset*update.commInfoBlock.offset + getPositionOffset(update.x, update.y)*update.commInfoBlock.blockSize;

            for(int j=update.commInfoBlock.blockSize;--j>=0;){
                int updateIdx = (offset+j)/16;
                int bitIdx = (offset+j)%16;
                int updateVal = (update.val & 1<<j) > 0? 1: 0;
                updatedCommsValues[updateIdx] = modifyBit(updatedCommsValues[updateIdx],bitIdx,updateVal);
            }
        }

        for(int i=64;--i>=0;){
            if(updatedCommsValues[i]!=data[i]){
                rc.writeSharedArray(i,updatedCommsValues[i]);
            }
        }

    }

    public int modifyBit(int original, int pos, int val)
    {
        int mask = 1 << pos;
        return (original & ~mask) | ((val << pos) & mask);
    }

    public void queueDenseMatrixUpdate(int x,int y,int val,CommInfoBlock commInfoBlock){
        commUpdateLinkedList.add(new CommDenseMatrixUpdate(x,y,val,commInfoBlock));
    }

    public Comms(RobotController rc) throws GameActionException {
        this.rc = rc;
        int w = rc.getMapWidth(),h = rc.getMapHeight();
        xSectorSize = getBestSectorSize(w);
        ySectorSize = getBestSectorSize(h);

        xSectors = ceilDivision(w,xSectorSize);
        ySectors = ceilDivision(h,ySectorSize);
        blockOffset = xSectors * ySectors;

        CommInfoBlock[] enumValues = CommInfoBlock.values();
        CommInfoBlock lastBlock = enumValues[enumValues.length-1];

        sparseSignalOffset = (lastBlock.offset + lastBlock.blockSize)*blockOffset;

    }

    public int getPositionOffset(int x,int y){
        int sectorX = x/xSectorSize,sectorY = y/ySectorSize;
        return sectorX*ySectors + sectorY;
    }

    private void readSharedData() throws GameActionException {
        for(int i=64;--i >= 0;)data[i] = rc.readSharedArray(i);
    }

    private static int getBestSectorSize(int dimension){
        int dim7 = (int)Math.ceil(1.0*dimension/7);
        int dim8 = (int)Math.ceil(1.0*dimension/8);
        if(dim7 == dim8)return 7;
        // more bits saved if we choose 8
        return 8;
    }


}
