package defensivebot.strategies;

import battlecode.common.*;
import defensivebot.datasturctures.LinkedList;
import defensivebot.enums.CommInfoBlock;
import defensivebot.enums.SparseSignal;

import static defensivebot.bots.Robot.turnCount;
import static defensivebot.utils.Constants.BFS2;
import static defensivebot.utils.Constants.bitMasks;
import static defensivebot.utils.CustomMath.ceilDivision;

public class Comms {

    static class CommDenseMatrixUpdate{
        int x,y,val;
        CommInfoBlock commInfoBlock;
        CommDenseMatrixUpdate(int x,int y,int val,CommInfoBlock commInfoBlock){
            this.x=x;
            this.y=y;
            this.val=val;
            this.commInfoBlock=commInfoBlock;
        }
    }

    static class CommSparseMatrixUpdate{
        int x,y;
        SparseSignal sparseSignal;
        CommSparseMatrixUpdate(int x,int y,SparseSignal sparseSignal){
            this.x=x;
            this.y=y;
            this.sparseSignal=sparseSignal;
        }
    }

    private final RobotController rc;
    private final int xSectorSize,ySectorSize,xSectors,ySectors,blockOffset, sparseSignalOffset,unitTypeSpareSignalOffset,unitTypeSignalOffset;
    private int[] data = new int[64];
    private LinkedList<CommDenseMatrixUpdate> commUpdateLinkedList = new LinkedList<>();
    private LinkedList<CommSparseMatrixUpdate> sparseSignalLinkedList = new LinkedList<>();

    private int readDataTime=-1;

    public void processUpdateQueues() throws GameActionException{
        // 8 adjacent sectors and current sector are only possibilities so total 9
        int[][][] valMap = new int[3][3][CommInfoBlock.values().length];
        int valMapOffset = 1;
        MapLocation loc = rc.getLocation();
        int curSectorX = loc.x/xSectorSize,curSectorY = loc.y/ySectorSize;

        // aggregate updates in valMap
        while(commUpdateLinkedList.size>0){
            CommDenseMatrixUpdate update = commUpdateLinkedList.dequeue().val;
            int sectorX = update.x/xSectorSize,sectorY = update.y/ySectorSize;

            // TODO: think about removing this as this should never happen
            if(Math.abs(curSectorX-sectorX)>1 || Math.abs(curSectorY-sectorY)>1)continue;
            // need to adjust relative sector by offset for 0 indexing
            valMap[curSectorX-sectorX+valMapOffset][curSectorY-sectorY+valMapOffset][update.commInfoBlock.ordinal()] += update.val;
        }

        // read shared data
        readSharedData();
        int[] updatedCommsValues = new int[64];
        for(int i=64;--i>=0;)updatedCommsValues[i]=data[i];

        // only check adjacent sectors. Starting with 0,0 (current sector)
        for(int i=BFS2.length;--i>=0;){
            int checkX = BFS2[i][0]+curSectorX,checkY = BFS2[i][1]+curSectorY;

            // check if sector is valid
            if(checkX<0 || checkX>=xSectors || checkY<0 || checkY>=ySectors)continue;

            for(CommInfoBlock commInfoBlock:CommInfoBlock.values()){
                int val = valMap[BFS2[i][0]+valMapOffset][BFS2[i][1]+valMapOffset][commInfoBlock.ordinal()];
                if(val>0){
                    // convert val to requisite level. For now, we only use 1
                    val = 1;
                    int offset = getCommOffset(commInfoBlock,checkX,checkY);
                    for(int j=commInfoBlock.blockSize;--j>=0;){
                        int updateIdx = (offset+j)/16;
                        int bitIdx = (offset+j)%16;
                        int updateVal = (val & 1<<j) > 0? 1: 0;
                        updatedCommsValues[updateIdx] = modifyBit(updatedCommsValues[updateIdx],bitIdx,updateVal);
                    }
                }

            }

            // we check adjacent sectors only for Archon and only on turn 1
            if(turnCount>1 || rc.getType() != RobotType.ARCHON)break;
        }

        // process sparse signal updates

        for(int i=64;--i>=0;){
            if(updatedCommsValues[i]!=data[i]){
                rc.writeSharedArray(i,updatedCommsValues[i]);
            }
        }
    }

    private int getCommOffset(CommInfoBlock commInfoBlock,int sectorX,int sectorY){
        return blockOffset*commInfoBlock.offset + sectorX*ySectors + sectorY;
    }

    /*
    * Read Shared array to check whether if nearby sectors have high density of queried droid/resource
    * Typically, threshold should be 1 or 2 or 3
    * Should use this function to look for high density friends, resources etc.
    * Don't use for checking directions to run away from enemy.
    * For that, we need to find a direction which runs from all high density enemy sectors and towards high density friends sector
    * TODO: prioritise location with less rubble
    * */
    public MapLocation readNearbyInfo(CommInfoBlock commInfoBlock,int threshold) throws GameActionException {
        MapLocation loc = rc.getLocation();
        int curSectorX = loc.x/xSectorSize,curSectorY = loc.y/ySectorSize;

        int maxVal = 0;
        MapLocation output = null;
        for(int i=BFS2.length;--i>=0;){
            int checkX = BFS2[i][0]+curSectorX,checkY = BFS2[i][1]+curSectorY;

            // check if sector is valid
            if(checkX<0 || checkX>=xSectors || checkY<0 || checkY>=ySectors)continue;
            int val = readInfo(commInfoBlock,checkX,checkY);
            if(val>maxVal){
                maxVal = val;
                output = getCenterOfSector(checkX,checkY);
            }
        }
        if(maxVal>=threshold)return output;
        else return null;
    }

    private MapLocation getCenterOfSector(int sectorX,int sectorY){
        int xSectorRef = sectorX*xSectorSize;
        int ySectorRef = sectorY*ySectorSize;

        return new MapLocation(xSectorRef+xSectorSize/2,ySectorRef+ySectorSize/2);
    }

    private int readInfo(CommInfoBlock commInfoBlock, int sectorX, int sectorY) throws GameActionException {
        return readBits(getCommOffset(commInfoBlock,sectorX,sectorY),commInfoBlock.blockSize);
    }

    private int readBits(int offset,int num) throws GameActionException {
        // TODO: Remove this from here when it becomes unnecessary
        readSharedData();

        int val=0;
        for(int j=num;--j>=0;){
            // read (offset + j) th bit
            int updateIdx = (offset+j)/16;
            int bitIdx = (offset+j)%16;
            // update jth bit of val
            if((data[updateIdx] | bitMasks[bitIdx])>0){
                val |= 1<<j;
            }
        }
        return val;
    }

    public int modifyBit(int original, int pos, int val)
    {
        int mask = 1 << pos;
        return (original & ~mask) | ((val << pos) & mask);
    }

    public void queueDenseMatrixUpdate(int x,int y,int val,CommInfoBlock commInfoBlock){
        commUpdateLinkedList.add(new CommDenseMatrixUpdate(x, y, val, commInfoBlock));
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

        unitTypeSpareSignalOffset = findClosestGreaterOrEqualPowerOf2(blockOffset);
        // single sector takes number of bits required to represent 0 ... blockOffset-1
        unitTypeSignalOffset = (lastBlock.offset + lastBlock.blockSize)*blockOffset;
        sparseSignalOffset = unitTypeSignalOffset + rc.getArchonCount() * unitTypeSpareSignalOffset;
    }

    public int findClosestGreaterOrEqualPowerOf2(int num){
        for(int i=7;--i>=0;)if(num<=bitMasks[i])return i;
        // TODO: throw exception here
        return -1;
    }

    public int getPositionOffset(int x,int y){
        int sectorX = x/xSectorSize,sectorY = y/ySectorSize;
        return sectorX*ySectors + sectorY;
    }

    private void readSharedData() throws GameActionException {
        if(readDataTime == turnCount)return;
        else readDataTime = turnCount;
        for(int i=64;--i >= 0;)data[i] = rc.readSharedArray(i);
    }

    private static int getBestSectorSize(int dimension){
        int dim7 = (int)Math.ceil(1.0*dimension/7);
        int dim8 = (int)Math.ceil(1.0*dimension/8);
        if(dim7 == dim8)return 7;
        // more bits saved if we choose 8
        return 8;
    }

    public boolean isDenseUpdateAllowed(MapLocation curLoc){
        // Archon needs to see everything in its first turn, no matter what
        if(rc.getType() == RobotType.ARCHON && turnCount == 1)return true;

        int xSectorRef = curLoc.x - curLoc.x%xSectorSize;
        int ySectorRef = curLoc.y - curLoc.y%ySectorSize;

        // only allow update of own sector
        if(xSectorRef<0 || xSectorRef>=xSectorSize || ySectorRef<0 || ySectorRef>=ySectorSize)return false;

        if(rc.getMode() == RobotMode.DROID){
            // if droid is away from sector border by a factor of 2, it probably doesn't see enough of this sector
            return xSectorRef+ySectorRef>=2 && (xSectorSize-xSectorRef)+(ySectorSize-ySectorRef)>=2;
        } else return true;
    }

    public void queueSparseSignalUpdate(int x,int y,SparseSignal signal){
        sparseSignalLinkedList.add(new CommSparseMatrixUpdate(x,y,signal));
    }


}
