package defensivebot2.strategies;

import battlecode.common.*;
import defensivebot2.datasturctures.LinkedList;
import defensivebot2.enums.CommInfoBlockType;
import defensivebot2.enums.FixedDataSignalType;
import defensivebot2.models.CommDenseMatrixUpdate;
import static defensivebot2.utils.Constants.*;
import static defensivebot2.utils.CustomMath.*;

public class Comms2 {


    public static int xSectorSize,ySectorSize,xSectors,
            ySectors,blockOffset, fixedSignalOffset,
            numBitsSingleSectorInfo,sectorIsolationMask;

    private static int exploreDir;
    private static RobotController rc;
    public static boolean denseUpdateAllowed=false;

    private static LocalInfo localInfo;
    public static int w,h,curSectorX,curSectorY;
    private static MapLocation currentLocation;

    private static LinkedList<CommDenseMatrixUpdate> commUpdateLinkedList = new LinkedList<>();

    private static int[] data = new int[64];

    private static final int AVAILAIBILITY_IDX = 63;

    public static MapLocation[] friendlyArchons = new MapLocation[4];
    public static MapLocation[] enemyArchons = new MapLocation[4];

    static MapLocation firstGatherPoint = null, secondGatherPoint = null;

    private static MapLocation getMapLocationFromSectorInfo(int mapSector){
        return getCenterOfSector(mapSector/ySectors,mapSector%ySectors);
    }

    public static void init(LocalInfo localInfoPass, RobotController rcPass){
        localInfo = localInfoPass;
        rc = rcPass;
        w = rc.getMapWidth();
        h = rc.getMapHeight();
        xSectorSize = getBestSectorSize(w);
        ySectorSize = getBestSectorSize(h);
        exploreDir = rc.getID()%8;

        xSectors = ceilDivision(w,xSectorSize);
        ySectors = ceilDivision(h,ySectorSize);
        blockOffset = xSectors * ySectors;

        CommInfoBlockType[] enumValues = CommInfoBlockType.values();
        CommInfoBlockType lastBlock = enumValues[enumValues.length-1];

        numBitsSingleSectorInfo = findClosestGreaterOrEqualPowerOf2(blockOffset);
        sectorIsolationMask = (1 << numBitsSingleSectorInfo) - 1;
        fixedSignalOffset = (lastBlock.offset + lastBlock.blockSize)*blockOffset;
    }

    public static void initTurn() throws GameActionException {
        // read shared data
        for(int i=64;--i >= 0;){
            data[i] = rc.readSharedArray(i);
        }

        // check if dense update is allowed
        denseUpdateAllowed = isDenseUpdateAllowed();

        currentLocation = rc.getLocation();

        curSectorX = currentLocation.x/xSectorSize;
        curSectorY = currentLocation.y/ySectorSize;
    }

    public static void processUpdateQueues() throws GameActionException{
        // if no updates are there then no need to process
        if(commUpdateLinkedList.size == 0)return;

        int[] updatedCommsValues = new int[64];
        for(int i=64;--i>=0;)updatedCommsValues[i]=data[i];

        if(denseUpdateAllowed)processDenseSignalUpdates(updatedCommsValues);

        // update shared array
        for(int i=64;--i>=0;){
            if(updatedCommsValues[i]!=data[i]){
                rc.writeSharedArray(i,updatedCommsValues[i]);
            }
        }
        data = updatedCommsValues;
    }

    private static void processDenseSignalUpdates(int[] updatedCommsValues){
        while(commUpdateLinkedList.size>0){
            CommDenseMatrixUpdate update = commUpdateLinkedList.dequeue().val;
            int offset = getCommOffset(update.commInfoBlockType,curSectorX,curSectorY);
            writeBits(updatedCommsValues,offset,update.commInfoBlockType.getStoreVal(update.val),update.commInfoBlockType.blockSize);
        }
    }

    private static int getCommOffset(CommInfoBlockType commInfoBlockType, int sectorX, int sectorY){
        return blockOffset* commInfoBlockType.offset + (sectorX*ySectors + sectorY)*commInfoBlockType.blockSize;
    }

    private static int getBestSectorSize(int dimension){
//        if(dimension%6 == 0)return 6;
//        return 5;
        int dim7 = (int)Math.ceil(1.0*dimension/7);
        int dim8 = (int)Math.ceil(1.0*dimension/8);
        if(dim7 == dim8)return 7;
        // more bits saved if we choose 8
        return 8;
    }

    private static int findClosestGreaterOrEqualPowerOf2(int num){
        for(int i=0;i<16;i++)if(num<=bitMasks[i])return i;
        throw new RuntimeException("something wrong.");
    }

    private static boolean isDenseUpdateAllowed(){
        if(rc.getMode() == RobotMode.DROID){
            MapLocation curLoc = rc.getLocation();
            int xSectorRef = curLoc.x%xSectorSize;
            int ySectorRef = curLoc.y%ySectorSize;

                // if droid is away from sector border by a factor of 2, it probably doesn't see enough of this sector
                return xSectorRef+ySectorRef>=DENSE_COMMS_UPDATE_LIMIT && (xSectorSize-xSectorRef)+(ySectorSize-ySectorRef)>=DENSE_COMMS_UPDATE_LIMIT;
                //denseUpdateAllowed = curLoc.isWithinDistanceSquared(getCenterOfSector(xSectorRef,ySectorRef),10);
            }
        return true;
    }

    public static MapLocation getNearestLeadLoc() throws GameActionException {
        MapLocation loc = rc.getLocation();

        CommInfoBlockType commInfoBlockType = CommInfoBlockType.LEAD_MAP;
        for(int i=1;i<BFS_MANHATTAN_5.length/2;i++){
            int checkX = BFS_MANHATTAN_5[i][0]+curSectorX,checkY = BFS_MANHATTAN_5[i][1]+curSectorY;

            // check if sector is valid
            if(checkX<0 || checkX>=xSectors || checkY<0 || checkY>=ySectors)continue;
            int val = readInfo(commInfoBlockType,checkX,checkY);
            if(val >=2){
                return getCenterOfSector(checkX,checkY);
            }
        }
        return null;
    }

    // Only returns enemy locations with low threat which needs to be chipped off. High threat areas are left alone
    public static MapLocation getNearestEnemyLoc() throws GameActionException {
        MapLocation loc = rc.getLocation();
        int curSectorX = loc.x/xSectorSize,curSectorY = loc.y/ySectorSize;

        CommInfoBlockType commInfoBlockType = CommInfoBlockType.ENEMY_UNITS;
        for(int i=1;i<BFS_MANHATTAN_5.length/2;i++){
            int checkX = BFS_MANHATTAN_5[i][0]+curSectorX,checkY = BFS_MANHATTAN_5[i][1]+curSectorY;

            // check if sector is valid
            if(checkX<0 || checkX>=xSectors || checkY<0 || checkY>=ySectors)continue;
            int val = readInfo(commInfoBlockType,checkX,checkY);
            if(val == 2){
                return getCenterOfSector(checkX,checkY);
            }
        }
        return null;
    }


    public static MapLocation getNearbyUnexplored() throws GameActionException {
        MapLocation loc = rc.getLocation();
        int curSectorX = loc.x/xSectorSize,curSectorY = loc.y/ySectorSize;

        CommInfoBlockType commInfoBlockType = CommInfoBlockType.EXPLORATION;
        // DFS finds enemies quicker
        // BFS protects friendly units while exploring
        int[][] search = BFS25[exploreDir];
        for(int i=1;i<search.length;i++){
            int checkX = search[i][0]+curSectorX,checkY = search[i][1]+curSectorY;

            // check if sector is valid
            if(checkX<0 || checkX>=xSectors || checkY<0 || checkY>=ySectors)continue;
            int val = readInfo(commInfoBlockType,checkX,checkY);
            if(val == 0){
                return getCenterOfSector(checkX,checkY);
            }
        }
        return null;
    }

    private static MapLocation getCenterOfSector(int sectorX, int sectorY){
        int x = sectorX*xSectorSize+xSectorSize/2;
        int y = sectorY*ySectorSize+ySectorSize/2;
        if(x>=w)x = w-1;
        if(y>=h)y = h-1;
        return new MapLocation(x,y);
    }

    private static int readInfo(CommInfoBlockType commInfoBlockType, int sectorX, int sectorY) throws GameActionException {
        return readBits(data,getCommOffset(commInfoBlockType,sectorX,sectorY), commInfoBlockType.blockSize);
    }

    private static int readBits(int[] arr, int offset, int num) {
        int val=0;
        for(int j=num;--j>=0;){
            // read (offset + j) th bit
            int updateIdx = (offset+j)/16;
            int bitIdx = (offset+j)%16;
            // update jth bit of val
            if((arr[updateIdx] & bitMasks[bitIdx])>0){
                val |= bitMasks[j];
            }
        }
        return val;
    }

    public static void queueDenseMatrixUpdate(int val, CommInfoBlockType commInfoBlockType){
        commUpdateLinkedList.add(new CommDenseMatrixUpdate(val, commInfoBlockType));
    }

    public static void markLocationSafe(MapLocation location) throws GameActionException {
        if(location == firstGatherPoint){
            // remove this gather point from comms
            removeData(FixedDataSignalType.FIRST_GATHER_POINT);
        }else if(location == secondGatherPoint){
            // remove this gather point from comms
            removeData(FixedDataSignalType.SECOND_GATHER_POINT);
        }
        // also remove from comms
        if(location == enemyArchons[0]){
            removeData(FixedDataSignalType.FIRST_ENEMY_ARCHON_IDX);
        }
    }

    private static void removeData(FixedDataSignalType fixedDataSignalType) throws GameActionException {
        rc.writeSharedArray(fixedDataSignalType.arrayIdx,0);
        int newAvailability = modifyBit(data[AVAILAIBILITY_IDX],fixedDataSignalType.availabilityIdx,0);
        data[AVAILAIBILITY_IDX] = newAvailability;
        rc.writeSharedArray(AVAILAIBILITY_IDX,newAvailability);
    }

    public static MapLocation getClosestTarget(){
        return firstGatherPoint != null ? firstGatherPoint:secondGatherPoint;
    }

    public static void updateCommsInfo(){
        if((data[AVAILAIBILITY_IDX] &  1) >0){
            // first archon available
            int val = data[FixedDataSignalType.FIRST_FRIENDLY_ARCHON_IDX.arrayIdx];
            friendlyArchons[0] = getMapLocationFromSectorInfo(val);
        }

        if((data[AVAILAIBILITY_IDX] &  (1 << 1)) >0){
            // 2nd archon available
            int val = data[FixedDataSignalType.SECOND_FRIENDLY_ARCHON_IDX.arrayIdx];
            friendlyArchons[1] = getMapLocationFromSectorInfo(val);
        }

        if((data[AVAILAIBILITY_IDX] &  (1 << 2)) >0){
            // 3rd archon available
            int val = data[FixedDataSignalType.THIRD_FRIENDLY_ARCHON_IDX.arrayIdx];
            friendlyArchons[2] = getMapLocationFromSectorInfo(val);
        }

        if((data[AVAILAIBILITY_IDX] &  (1 << 3)) >0){
            // 4th archon available
            int val = data[FixedDataSignalType.FOURTH_FRIENDLY_ARCHON_IDX.arrayIdx];
            friendlyArchons[3] = getMapLocationFromSectorInfo(val);
        }

        if((data[AVAILAIBILITY_IDX] &  (1 << 4)) >0){
            // first enemy archon available
            int val = data[FixedDataSignalType.FIRST_ENEMY_ARCHON_IDX.arrayIdx];
            enemyArchons[0] = getMapLocationFromSectorInfo(val);
        }

        if((data[AVAILAIBILITY_IDX] &  (1 << 5)) >0){
            // 2nd enemy archon available
            int val = data[FixedDataSignalType.SECOND_ENEMY_ARCHON_IDX.arrayIdx];
            enemyArchons[1] = getMapLocationFromSectorInfo(val);
        }

        if((data[AVAILAIBILITY_IDX] &  (1 << 6)) >0){
            // 3rd enemy archon available
            int val = data[FixedDataSignalType.THRID_ENEMY_ARCHON_IDX.arrayIdx];
            enemyArchons[2] = getMapLocationFromSectorInfo(val);
        }

        if((data[AVAILAIBILITY_IDX] &  (1 << 7)) >0){
            // 3rd enemy archon available
            int val = data[FixedDataSignalType.FOURTH_ENEMY_ARCHON_IDX.arrayIdx];
            enemyArchons[3] = getMapLocationFromSectorInfo(val);
        }

        if((data[AVAILAIBILITY_IDX] &  (1 << 8)) >0){
            // 1st gather point available
            int val = data[FixedDataSignalType.FIRST_GATHER_POINT.arrayIdx];
            firstGatherPoint = getMapLocationFromSectorInfo(val);
        }

        if((data[AVAILAIBILITY_IDX] &  (1 << 9)) >0){
            // 2nd gather point available
            int val = data[FixedDataSignalType.SECOND_GATHER_POINT.arrayIdx];
            secondGatherPoint = getMapLocationFromSectorInfo(val);
        }
    }

    public static void registerEnemyArchon() throws GameActionException {
        if(localInfo.nearestER[RobotType.ARCHON.ordinal()] != null){
            MapLocation location = localInfo.nearestER[RobotType.ARCHON.ordinal()].location;
            int val = locToSectorInfo(location);
            if(enemyArchons[0] == null){
                writeData(val,FixedDataSignalType.FIRST_ENEMY_ARCHON_IDX);
            }else if(enemyArchons[1] == null){
                writeData(val,FixedDataSignalType.SECOND_ENEMY_ARCHON_IDX);
            }else if(enemyArchons[2] == null){
                writeData(val,FixedDataSignalType.THRID_ENEMY_ARCHON_IDX);
            }else if(enemyArchons[3] == null){
                writeData(val,FixedDataSignalType.FOURTH_ENEMY_ARCHON_IDX);
            }
        }
    }

    public static void registerGatherPoint(MapLocation location) throws GameActionException {
        if(firstGatherPoint == null){
            writeData(locToSectorInfo(location),FixedDataSignalType.FIRST_GATHER_POINT);
        }else if(secondGatherPoint == null){
            writeData(locToSectorInfo(location),FixedDataSignalType.SECOND_GATHER_POINT);
        }
    }

    public static int getNumGatherPoints(){
        int count = 0;
        if(firstGatherPoint != null)count++;
        if(secondGatherPoint != null)count++;
        return count;
    }

    private static void writeData(int val, FixedDataSignalType fixedDataSignalType) throws GameActionException {
        rc.writeSharedArray(fixedDataSignalType.arrayIdx, val);
        int newAvail = data[AVAILAIBILITY_IDX] | 1<< fixedDataSignalType.availabilityIdx;
        rc.writeSharedArray(AVAILAIBILITY_IDX,newAvail);
        data[fixedDataSignalType.arrayIdx] = val;
        data[AVAILAIBILITY_IDX] = newAvail;

    }

    public static int registerFriendlyArchon(MapLocation location) throws GameActionException {
        int val = locToSectorInfo(location);
        if(friendlyArchons[0] == null){
            writeData(val,FixedDataSignalType.FIRST_FRIENDLY_ARCHON_IDX);
            return 0;
        }else if(friendlyArchons[1] == null){
            writeData(val,FixedDataSignalType.SECOND_FRIENDLY_ARCHON_IDX);
            return 1;
        }else if(friendlyArchons[2] == null){
            writeData(val,FixedDataSignalType.THIRD_FRIENDLY_ARCHON_IDX);
            return 2;
        }else if(friendlyArchons[3] == null){
            writeData(val,FixedDataSignalType.FOURTH_FRIENDLY_ARCHON_IDX);
            return 3;
        }
        // TODO: what to do here?
        return -1;
    }
    
    public static void updateFriendlyArchon(int archonIdx, MapLocation location) throws GameActionException {
        int val = locToSectorInfo(location);
        if(archonIdx == 0){
            writeData(val,FixedDataSignalType.FIRST_FRIENDLY_ARCHON_IDX);
        }else if(archonIdx == 1){
            writeData(val,FixedDataSignalType.SECOND_FRIENDLY_ARCHON_IDX);
        }else if(archonIdx == 2){
            writeData(val,FixedDataSignalType.THIRD_FRIENDLY_ARCHON_IDX);
        }else if(archonIdx == 3){
            writeData(val,FixedDataSignalType.FOURTH_FRIENDLY_ARCHON_IDX);
        }
    }

    public static int locToSectorInfo(MapLocation location){
        int xSector = location.x/xSectorSize, ySector = location.y/ySectorSize;
        return xSector*ySectors + ySector;
    }

    public static MapLocation getClosestArchon(boolean friendly){
        MapLocation[] search = friendly? friendlyArchons:enemyArchons;
        int d = Integer.MAX_VALUE;
        MapLocation out = null;
        for(int i=4;--i>=0;){
            if(search[i] == null)continue;
            int query = currentLocation.distanceSquaredTo(search[i]);
            if(query<d){
                d = query;
                out = search[i];
            }
        }
        return out;
    }
}
