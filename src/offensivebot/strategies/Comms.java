package offensivebot.strategies;

import battlecode.common.*;
import offensivebot.datasturctures.CustomHashMap;
import offensivebot.datasturctures.CustomSet;
import offensivebot.datasturctures.HashMapNodeVal;
import offensivebot.datasturctures.LinkedList;
import offensivebot.enums.CommInfoBlockType;
import offensivebot.enums.SparseSignalType;
import offensivebot.models.SparseSignal;

import java.util.Map;

import static defensivebot.utils.Constants.DENSE_COMMS_UPDATE_LIMIT;
import static offensivebot.bots.Robot.turnCount;

import static offensivebot.models.SparseSignal.ALL_SPARSE_SIGNAL_CODES;
import static offensivebot.models.SparseSignal.CODE_TO_SPARSE_SIGNAL;
import static offensivebot.utils.Constants.*;
import static offensivebot.utils.CustomMath.ceilDivision;
import static offensivebot.utils.LogUtils.printDebugLog;

public class Comms {

    private final RobotController rc;
    public final int xSectorSize,ySectorSize,xSectors,
            ySectors,blockOffset, sparseSignalOffset,
            numBitsSingleSectorInfo,sectorIsolationMask;
    private int[] data = new int[64];
    private LinkedList<CommDenseMatrixUpdate> commUpdateLinkedList = new LinkedList<>();
    private LinkedList<SparseSignal> sparseSignalUpdates = new LinkedList<>();
    private CustomSet<SparseSignal> sparseSignals;
    private LinkedList<SparseSignal> orderedSparseSignals;
    boolean denseUpdateAllowed=false;


    public boolean isSignalArrayFull = false;
    private final int w,h;

    // usage: This is a bit hacky but while reading sparse signal array, I am recording index of last signal
    private int lastSignalBeginsHere;

    private int readDataTime=-1,querySignalDataTime=-1,readDenseUpdateAllowedTime=-1;

    public Comms(RobotController rc) throws GameActionException {
        this.rc = rc;
        w = rc.getMapWidth();
        h = rc.getMapHeight();
        xSectorSize = getBestSectorSize(w);
        ySectorSize = getBestSectorSize(h);


        xSectors = ceilDivision(w,xSectorSize);
        ySectors = ceilDivision(h,ySectorSize);
        blockOffset = xSectors * ySectors;

        CommInfoBlockType[] enumValues = CommInfoBlockType.values();
        CommInfoBlockType lastBlock = enumValues[enumValues.length-1];

        numBitsSingleSectorInfo = findClosestGreaterOrEqualPowerOf2(blockOffset);
        sectorIsolationMask = (1 << numBitsSingleSectorInfo) - 1;
        sparseSignalOffset = (lastBlock.offset + lastBlock.blockSize)*blockOffset;
        lastSignalBeginsHere = sparseSignalOffset;
    }

    public void processUpdateQueues() throws GameActionException{
        // if no updates are there then no need to process
        if(commUpdateLinkedList.size == 0 && sparseSignalUpdates.size == 0)return;

        // read shared data
        readSharedData();
        int[] updatedCommsValues = new int[64];
        for(int i=64;--i>=0;)updatedCommsValues[i]=data[i];

        // process dense signal updates
        if(denseUpdateAllowed)processDenseSignalUpdates(updatedCommsValues);

        // process sparse updates
        if(sparseSignalUpdates.size>0)processSparseSignalUpdates(updatedCommsValues);

        // update shared array
        updateSharedArray(updatedCommsValues);
    }

    private void processDenseSignalUpdates(int[] updatedCommsValues){
        MapLocation loc = rc.getLocation();
        int xSector = loc.x/xSectorSize,ySector = loc.y/ySectorSize;
        while(commUpdateLinkedList.size>0){
            CommDenseMatrixUpdate update = commUpdateLinkedList.dequeue().val;
            int offset = getCommOffset(update.commInfoBlockType,xSector,ySector);
            writeBits(updatedCommsValues,offset,update.commInfoBlockType.getStoreVal(update.val),update.commInfoBlockType.blockSize);
        }
    }

    private int processSparseSignalUpdates(int[] updatedCommsValues) throws GameActionException {
        querySparseSignals();
        int offset = lastSignalBeginsHere;
        isSignalArrayFull = false;
        while(sparseSignalUpdates.size>0){
            SparseSignal signal = sparseSignalUpdates.dequeue().val;
            signal.target = convertToCenterOfSector(signal.target);
            if(sparseSignals.contains(signal))continue;
            int numBits = signal.type.numBits + signal.type.positionSlots*numBitsSingleSectorInfo+signal.type.fixedBits;
            // not enough bits to write signal
            if(offset+numBits>=1024){
                isSignalArrayFull = true;
                break;
            }

            int val = signalToCommsValue(signal);
            offset = writeBits(updatedCommsValues,offset,val,numBits);

            lastSignalBeginsHere = offset;
        }

        return offset;
    }

    private MapLocation convertToCenterOfSector(MapLocation target){
        if(target == null)return null;
        int curSectorX = target.x/xSectorSize,curSectorY = target.y/ySectorSize;
        return getCenterOfSector(curSectorX,curSectorY);
    }

    public int writeBits(int[] updatedCommsValues,int offset,int val,int numBits){
        for(int j = 0; j<numBits;j++){
            int updateIdx = offset/16;
            int bitIdx = offset%16;
            int updateVal = (val & 1<<j) > 0? 1: 0;
            updatedCommsValues[updateIdx] = modifyBit(updatedCommsValues[updateIdx],bitIdx,updateVal);
            offset++;
        }
        return offset;
    }

    private int signalToCommsValue(SparseSignal signal){
        int val = signal.type.code;
        if(signal.type.positionSlots>0){
            // TODO: For more than 1 location signal, handle this
            int curSectorX = signal.target.x/xSectorSize,curSectorY = signal.target.y/ySectorSize;
            int sectorInfo = curSectorX*ySectors + curSectorY;
            val |= (sectorInfo << signal.type.numBits);
        }
        if(signal.fixedBitsVal > 0){
            val |= (signal.type.numBits + numBitsSingleSectorInfo*signal.type.positionSlots);
        }
        return val;
    }

    private void updateSharedArray(int[] updatedCommsValues) throws GameActionException {
        for(int i=64;--i>=0;){
            if(updatedCommsValues[i]!=data[i]){
                rc.writeSharedArray(i,updatedCommsValues[i]);
            }
        }
        data = updatedCommsValues;
    }


    private int getCommOffset(CommInfoBlockType commInfoBlockType, int sectorX, int sectorY){
        return blockOffset* commInfoBlockType.offset + (sectorX*ySectors + sectorY)*commInfoBlockType.blockSize;
    }



    /*
    * Read Shared array to check whether if nearby sectors have high density of queried droid/resource
    * Typically, threshold should be 1 or 2 or 3
    * Should use this function to look for high density friends, resources etc.
    * Don't use for checking directions to run away from enemy.
    * For that, we need to find a direction which runs from all high density enemy sectors and towards high density friends sector
    * TODO: prioritise location with less rubble
    * */

    public MapLocation getNearestLeadLoc() throws GameActionException {
        MapLocation loc = rc.getLocation();
        int curSectorX = loc.x/xSectorSize,curSectorY = loc.y/ySectorSize;

        CommInfoBlockType commInfoBlockType = CommInfoBlockType.LEAD_MAP;
        for(int i=1;i<BFS_MANHATTAN_5.length;i++){
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


    public MapLocation getNearbyUnexplored() throws GameActionException {
        MapLocation loc = rc.getLocation();
        int curSectorX = loc.x/xSectorSize,curSectorY = loc.y/ySectorSize;

        CommInfoBlockType commInfoBlockType = CommInfoBlockType.EXPLORATION;
        int randomDir = rc.getID()%4;
        int[][] BFS = BFS25[randomDir];
        for(int i=1;i<BFS.length;i++){
            int checkX = BFS[i][0]+curSectorX,checkY = BFS[i][1]+curSectorY;

            // check if sector is valid
            if(checkX<0 || checkX>=xSectors || checkY<0 || checkY>=ySectors)continue;
            int val = readInfo(commInfoBlockType,checkX,checkY);
            if(val == 0){
                return getCenterOfSector(checkX,checkY);
            }
        }
        return null;
    }

    private MapLocation getCenterOfSector(int sectorX,int sectorY){
        int x = sectorX*xSectorSize+xSectorSize/2;
        int y = sectorY*ySectorSize+ySectorSize/2;
        if(x>=w)x = w-1;
        if(y>=h)y = h-1;
        return new MapLocation(x,y);
    }

    private int readInfo(CommInfoBlockType commInfoBlockType, int sectorX, int sectorY) throws GameActionException {
        return readBits(data,getCommOffset(commInfoBlockType,sectorX,sectorY), commInfoBlockType.blockSize);
    }

    private int readBits(int[] arr,int offset,int num) throws GameActionException {
        // TODO: Remove this from here when it becomes unnecessary
        readSharedData();

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

    public int modifyBit(int original, int pos, int val)
    {
        int mask = 1 << pos;
        return (original & ~mask) | ((val << pos) & mask);
    }

    public void queueDenseMatrixUpdate(int val, CommInfoBlockType commInfoBlockType){
        commUpdateLinkedList.add(new CommDenseMatrixUpdate(val, commInfoBlockType));
    }

    public void queueSparseSignalUpdate(SparseSignal sparseSignal){
        sparseSignalUpdates.add(sparseSignal);
    }

    public int findClosestGreaterOrEqualPowerOf2(int num){
        for(int i=7;--i>=0;)if(num<=bitMasks[i])return i;
        // TODO: throw exception here
        return -1;
    }

    private void readSharedData() throws GameActionException {
        if(readDataTime == turnCount)return;
        else readDataTime = turnCount;
        for(int i=64;--i >= 0;){
            data[i] = rc.readSharedArray(i);
        }
    }

    private static int getBestSectorSize(int dimension){
        int dim7 = (int)Math.ceil(1.0*dimension/7);
        int dim8 = (int)Math.ceil(1.0*dimension/8);
        if(dim7 == dim8)return 7;
        // more bits saved if we choose 8
        return 8;
    }

    public boolean isDenseUpdateAllowed(){
        if(readDenseUpdateAllowedTime == turnCount)return denseUpdateAllowed;
        else readDenseUpdateAllowedTime = turnCount;
        // Archon needs to see everything in its first turn, no matter what
        if(rc.getType() == RobotType.ARCHON && turnCount == 1){
            denseUpdateAllowed = true;
            return denseUpdateAllowed;
        }

        MapLocation curLoc = rc.getLocation();

        int xSectorRef = curLoc.x%xSectorSize;
        int ySectorRef = curLoc.y%ySectorSize;

        if(rc.getMode() == RobotMode.DROID){
            // if droid is away from sector border by a factor of 2, it probably doesn't see enough of this sector
            denseUpdateAllowed = getCenterOfSector(xSectorRef,ySectorRef).isWithinDistanceSquared(curLoc,DENSE_COMMS_UPDATE_LIMIT);
        } else denseUpdateAllowed = true;
        return denseUpdateAllowed;
    }

    /*
    * Option A: Rewrite all signals while removing sparse signals (This is easier). Write back some signals
    * Option B: Handle contiguous segments. This is extremely complicated. Not going with this
    * */
    public CustomSet<SparseSignal> querySparseSignals() throws GameActionException {
        // TODO: figure out how to minimize this
        readSharedData();

        if(querySignalDataTime == turnCount)return sparseSignals;
        else querySignalDataTime = turnCount;

        int offset = sparseSignalOffset;
        sparseSignals = new CustomSet<>(30);
        orderedSparseSignals = new LinkedList<>();
        int val = 0;
        int bitCount = 0;
        while(offset<1024){
            if(bitCount == 0)lastSignalBeginsHere = offset;
            bitCount++;
            val |= readBits(data,offset,1);
            offset++;
            if(ALL_SPARSE_SIGNAL_CODES.contains(val) && CODE_TO_SPARSE_SIGNAL[val].numBits == bitCount){
                SparseSignalType signal = CODE_TO_SPARSE_SIGNAL[val];
                if(signal == SparseSignalType.TERMINATE_SIGNAL_ARRAY)break;
                SparseSignal sparseSignal = new SparseSignal(signal,null,lastSignalBeginsHere);

                // next few bits might be part of signal
                // TODO: handle multiple location reads if necessary
                if(signal.positionSlots>0){
                    // if not enough bits, this is not a signal
                    if(offset+numBitsSingleSectorInfo>=1024)break;
                    int mapSector = readBits(data,offset,numBitsSingleSectorInfo);
                    sparseSignal.target = getCenterOfSector(mapSector/ySectors,mapSector%ySectors);
                    offset += numBitsSingleSectorInfo;
                }

                // parsing fixed bits
                if(signal.fixedBits>0){
                    if(offset+signal.fixedBits>=1024)break;
                    sparseSignal.fixedBitsVal = readBits(data,offset,signal.fixedBits);
                    offset += signal.fixedBits;
                }

                sparseSignals.add(sparseSignal);
                // need to store in ordered fashion for cleanup
                orderedSparseSignals.add(sparseSignal);
                bitCount = 0;
                val = 0;

            }else val <<= 1;
        }
        if(offset == 1024)lastSignalBeginsHere = 1024;
        return sparseSignals;
    }

    /*
    * Do this when enough bytecode is left
    * TODO: test this
    * */
    public void cleanComms() throws GameActionException {
        querySparseSignals();
        printDebugLog("Comms clean up!");

        // only these messages should remain now
        int lenRemaining = orderedSparseSignals.size/INVERSE_FRACTION_OF_MESSAGES_TO_LEAVE;
        while(orderedSparseSignals.size>lenRemaining)orderedSparseSignals.dequeue();

        readSharedData();
        int[] updatedCommsValues = new int[64];
        for(int i=64;--i>=0;)updatedCommsValues[i] = data[i];

        while(orderedSparseSignals.size>0)sparseSignalUpdates.add(orderedSparseSignals.dequeue().val);

        lastSignalBeginsHere = sparseSignalOffset;
        int offset = processSparseSignalUpdates(updatedCommsValues);

        // set all bits to 0
        int updateIdx = offset/16;
        int bitIdx = offset%16;
        for(int i=16;--i>=bitIdx;)updatedCommsValues[updateIdx] = modifyBit(updatedCommsValues[updateIdx],i,0);
        for(int i=16;--i>updateIdx;)updatedCommsValues[i] = 0;
        updateSharedArray(updatedCommsValues);
    }

    static class CommDenseMatrixUpdate{
        int val;
        CommInfoBlockType commInfoBlockType;
        CommDenseMatrixUpdate(int val, CommInfoBlockType commInfoBlockType){
            this.val=val;
            this.commInfoBlockType = commInfoBlockType;
        }
    }

    public int[][] checkMap(CommInfoBlockType commInfoBlockType) throws GameActionException{
        readSharedData();
        int ctr = 0;
        int[][] explorationMap = new int[xSectors][ySectors];
        int offset = commInfoBlockType.offset*blockOffset;
        for(int i=blockOffset;--i>=0;){
            int num =readBits(data,offset,commInfoBlockType.blockSize);
            if(num>0){
                ctr++;
                explorationMap[i%ySectors][ySectors - 1 - i/ySectors] = num;
            }
            offset++;
        }
        return explorationMap;
    }

    public MapLocation getClosestEnemyArchon(CustomSet<MapLocation> discoveredArchons) throws GameActionException {
        querySparseSignals();
        sparseSignals.initIteration();
        SparseSignal signal = sparseSignals.next();
        MapLocation loc = rc.getLocation();
        int minDist = Integer.MAX_VALUE;
        MapLocation closestArchon = null;
        while (signal != null){
            if(signal.type==SparseSignalType.ENEMY_ARCHON_LOCATION && signal.target != null){
                if(discoveredArchons.contains(signal.target))continue;
                int d = loc.distanceSquaredTo(signal.target);
                if(d<minDist){
                    minDist = d;
                    closestArchon = signal.target;
                }
            }
            signal = sparseSignals.next();
        }
        return closestArchon;
    }

}
