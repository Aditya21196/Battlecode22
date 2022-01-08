package defensivebot.strategies;

import battlecode.common.*;
import defensivebot.datasturctures.CustomHashMap;
import defensivebot.datasturctures.CustomSet;
import defensivebot.datasturctures.HashMapNodeVal;
import defensivebot.datasturctures.LinkedList;
import defensivebot.enums.CommInfoBlockType;
import defensivebot.enums.DroidSubType;
import defensivebot.enums.SparseSignalType;
import defensivebot.models.SparseSignal;

import static defensivebot.bots.Robot.turnCount;
import static defensivebot.enums.DroidSubType.getSubType;
import static defensivebot.models.SparseSignal.ALL_SPARSE_SIGNAL_CODES;
import static defensivebot.models.SparseSignal.CODE_TO_SPARSE_SIGNAL;
import static defensivebot.utils.Constants.*;
import static defensivebot.utils.CustomMath.ceilDivision;
import static defensivebot.utils.LogUtils.printDebugLog;

public class Comms {

    private final RobotController rc;
    private final int xSectorSize,ySectorSize,xSectors,
            ySectors,blockOffset, sparseSignalOffset,
            unitTypeSpareSignalOffset,unitTypeSignalOffset,numBitsSingleSectorInfo;
    private int[] data = new int[64];
    private LinkedList<CommDenseMatrixUpdate> commUpdateLinkedList = new LinkedList<>();
    private LinkedList<SparseSignal> sparseSignalUpdates = new LinkedList<>();
    private CustomSet<SparseSignal> sparseSignals;
    private LinkedList<SparseSignal> orderedSparseSignals;
    boolean denseUpdateAllowed=false;
    private DroidSubType droidSubType=null;
    private MapLocation droidTarget=null;
    private final int sectorIsolationMask;


    public boolean isSignalArrayFull = false;

    // usage: This is a bit hacky but while reading sparse signal array, I am recording index of last signal
    private int lastSignalBeginsHere;

    private int readDataTime=-1,querySignalDataTime=-1,readDenseUpdateAllowedTime=-1;
    private int archonIndex = -1;

    public Comms(RobotController rc) throws GameActionException {
        this.rc = rc;
        int w = rc.getMapWidth(),h = rc.getMapHeight();
        xSectorSize = getBestSectorSize(w);
        ySectorSize = getBestSectorSize(h);


        xSectors = ceilDivision(w,xSectorSize);
        ySectors = ceilDivision(h,ySectorSize);
        blockOffset = xSectors * ySectors;

        CommInfoBlockType[] enumValues = CommInfoBlockType.values();
        CommInfoBlockType lastBlock = enumValues[enumValues.length-1];

        numBitsSingleSectorInfo = findClosestGreaterOrEqualPowerOf2(blockOffset);
        unitTypeSpareSignalOffset = numBitsSingleSectorInfo + UNIT_TYPE_SIGNAL_BITS;
        sectorIsolationMask = (1 << numBitsSingleSectorInfo) - 1;
        // single sector takes number of bits required to represent 0 ... blockOffset-1
        unitTypeSignalOffset = (lastBlock.offset + lastBlock.blockSize)*blockOffset;
        // using max archon count and leaving rest of the bits as empty. This is to avoid confusion when an Archon dies
        // This wastes a few bits. Might need to figure out a wy around this if Comms become a bottleneck
        sparseSignalOffset = unitTypeSignalOffset + 4 * unitTypeSpareSignalOffset;
        lastSignalBeginsHere = sparseSignalOffset;
    }

    public DroidSubType getSubtypeFromSignal(RobotInfo homeArchon) throws GameActionException{
        readSharedData();
        int homeArchonSectorX = homeArchon.location.x/xSectorSize, homeArchonSectorY = homeArchon.location.y/ySectorSize;
        int offset = unitTypeSignalOffset;
        for(int i=4;--i>0;){
            int info = readBits(data,offset,unitTypeSpareSignalOffset);
            int code = info >> numBitsSingleSectorInfo;
            info &= sectorIsolationMask;
            if(info/ySectors == homeArchonSectorX && info%ySectors == homeArchonSectorY)
                return getSubType(rc.getType(),code);
            offset += unitTypeSpareSignalOffset;
        }
        return null;
    }

    public void signalUnitSubType(DroidSubType type,MapLocation target) {
        this.droidSubType = type;
        droidTarget = target;
    }

    public int claimArchonIndex() throws GameActionException{
        readSharedData();
        int offset = unitTypeSignalOffset;
        int val = readBits(data,offset,unitTypeSpareSignalOffset);
        if(val == 0)return 0;
        offset+=unitTypeSpareSignalOffset;
        val = readBits(data,offset,unitTypeSpareSignalOffset);
        if(val == 0)return 1;
        offset+=unitTypeSpareSignalOffset;
        val = readBits(data,offset,unitTypeSpareSignalOffset);
        if(val == 0)return 2;
        return 3;
    }

    public void processUpdateQueues() throws GameActionException{
        // if no updates are there then no need to process
        // we have updates to process only if we need to reserve
        if(commUpdateLinkedList.size == 0 && sparseSignalUpdates.size == 0 && droidSubType == null)return;
        // read shared data
        readSharedData();
        int[] updatedCommsValues = new int[64];
        for(int i=64;--i>=0;)updatedCommsValues[i]=data[i];

        // claim an Archon index if it hasn't already been claimed
        if(rc.getType() == RobotType.ARCHON && archonIndex == -1){
            archonIndex = claimArchonIndex();
            if(droidSubType == null){
                writeBits(updatedCommsValues,unitTypeSignalOffset+archonIndex*unitTypeSpareSignalOffset,1,1);
            }
        }

        // process unit type signal first
        if(droidSubType != null){
            int code = droidSubType.code;
            code <<= numBitsSingleSectorInfo;
            if(droidTarget!=null){
                int xSector = droidTarget.x/xSectorSize,ySector = droidTarget.y/ySectorSize;
                code |= xSector*ySectors + ySector;
                // set null for next turn
                droidTarget = null;
            }
            writeBits(updatedCommsValues,unitTypeSignalOffset + unitTypeSpareSignalOffset*archonIndex,code,unitTypeSpareSignalOffset);
            // set null for next turn
            droidSubType = null;

        }

        // process dense signal updates
        if(denseUpdateAllowed)processDenseSignalUpdates(updatedCommsValues);

        // process sparse updates
        processSparseSignalUpdates(updatedCommsValues);

        // update shared array
        updateSharedArray(updatedCommsValues);
    }

    private void processDenseSignalUpdates(int[] updatedCommsValues){
        // 8 adjacent sectors and current sector are only possibilities so total 9
        // TODO: Replace by hash map
        CustomHashMap<Integer,Integer> map = new CustomHashMap<>(5);

        while(commUpdateLinkedList.size>0){
            CommDenseMatrixUpdate commDenseMatrixUpdate = commUpdateLinkedList.dequeue().val;
            int hash = commDenseMatrixUpdate.commInfoBlockType.ordinal()*blockOffset+commDenseMatrixUpdate.xSector*ySectors + commDenseMatrixUpdate.ySector;
            Integer freq = map.get(hash);
            if(freq == null)map.put(hash,commDenseMatrixUpdate.val);
            else map.setAlreadyContainedValue(hash,freq+commDenseMatrixUpdate.val);
        }

        map.resetIterator();
        HashMapNodeVal<Integer, Integer> next = map.next();
        while(next!=null){
            CommInfoBlockType[] commVals = CommInfoBlockType.values();
            CommInfoBlockType commInfoBlockType = commVals[next.key/3600];
            // can directly use the key to calculate offset
            int offset = blockOffset* commInfoBlockType.offset + (next.key%blockOffset)*commInfoBlockType.blockSize;
            offset = writeBits(updatedCommsValues,offset,next.val,commInfoBlockType.blockSize);
            next = map.next();
        }
    }

    private int processSparseSignalUpdates(int[] updatedCommsValues) throws GameActionException {
        querySparseSignals();
        int offset = lastSignalBeginsHere;
        isSignalArrayFull = false;
        while(sparseSignalUpdates.size>0){
            SparseSignal signal = sparseSignalUpdates.dequeue().val;
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

    public int writeBits(int[] updatedCommsValues,int offset,int val,int numBits){
        for(int j = 0; j<numBits;j++){
            if(offset >= 512 && offset < 512+unitTypeSpareSignalOffset){
                printDebugLog("hello");
            }
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
    public MapLocation readNearbyInfo(CommInfoBlockType commInfoBlockType, int threshold) throws GameActionException {
        MapLocation loc = rc.getLocation();
        int curSectorX = loc.x/xSectorSize,curSectorY = loc.y/ySectorSize;

        int maxVal = 0;
        MapLocation output = null;
        for(int i=BFS2.length;--i>=0;){
            int checkX = BFS2[i][0]+curSectorX,checkY = BFS2[i][1]+curSectorY;

            // check if sector is valid
            if(checkX<0 || checkX>=xSectors || checkY<0 || checkY>=ySectors)continue;
            int val = readInfo(commInfoBlockType,checkX,checkY);
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

    public void queueDenseMatrixUpdate(int x, int y, int val, CommInfoBlockType commInfoBlockType){
        int xSector = x/xSectorSize,ySector = y/ySectorSize;
        MapLocation loc = rc.getLocation();
        if(xSector != loc.x/xSectorSize || ySector != loc.y/ySectorSize){
            if(turnCount > 1 || rc.getType() != RobotType.ARCHON)return;
        }
        commUpdateLinkedList.add(new CommDenseMatrixUpdate(xSector,ySector , val, commInfoBlockType));
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

    public boolean isDenseUpdateAllowed(MapLocation curLoc){
        if(readDenseUpdateAllowedTime == turnCount)return denseUpdateAllowed;
        else readDenseUpdateAllowedTime = turnCount;
        // Archon needs to see everything in its first turn, no matter what
        if(rc.getType() == RobotType.ARCHON && turnCount == 1){
            denseUpdateAllowed = true;
        }

        int xSectorRef = curLoc.x - curLoc.x%xSectorSize;
        int ySectorRef = curLoc.y - curLoc.y%ySectorSize;

        // only allow update of own sector
        if(xSectorRef<0 || xSectorRef>=xSectorSize || ySectorRef<0 || ySectorRef>=ySectorSize){
            denseUpdateAllowed = false;
        }

        if(rc.getMode() == RobotMode.DROID){
            // if droid is away from sector border by a factor of 2, it probably doesn't see enough of this sector
            denseUpdateAllowed = xSectorRef+ySectorRef>=2 && (xSectorSize-xSectorRef)+(ySectorSize-ySectorRef)>=2;
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
        int lenRemaining = orderedSparseSignals.size/INVERSE_FRACTION_OF_MESSAGES_TO_LEAVE;
        while(orderedSparseSignals.size>lenRemaining)orderedSparseSignals.dequeue();
        // only these messages should remain now
        readSharedData();
        int[] updatedCommsValues = new int[64];
        for(int i=64;--i>=0;)updatedCommsValues[i] = data[i];

        while(orderedSparseSignals.size>0)sparseSignalUpdates.add(orderedSparseSignals.dequeue().val);
        int offset = processSparseSignalUpdates(updatedCommsValues);
        // set all bits to 0
        int updateIdx = offset/16;
        int bitIdx = offset%16;
        // TODO: improve this?
        for(int i=16;--i>=bitIdx;)updatedCommsValues[updateIdx] = modifyBit(updatedCommsValues[updateIdx],i,0);
        for(int i=16;--i>updateIdx;)updatedCommsValues[i] = 0;
        updateSharedArray(updatedCommsValues);
    }

    static class CommDenseMatrixUpdate{
        int xSector,ySector,val;
        CommInfoBlockType commInfoBlockType;
        CommDenseMatrixUpdate(int xSector, int ySector, int val, CommInfoBlockType commInfoBlockType){
            this.xSector=xSector;
            this.ySector=ySector;
            this.val=val;
            this.commInfoBlockType = commInfoBlockType;
        }
    }

}
