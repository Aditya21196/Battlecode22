package defensivebot.models;

import battlecode.common.MapLocation;
import defensivebot.datasturctures.CustomSet;
import defensivebot.enums.SparseSignalType;

public class SparseSignal {

    public SparseSignalType type;
    public MapLocation target;
    public int offset;
    public int fixedBitsVal=0;

    public static final CustomSet<Integer> ALL_SPARSE_SIGNAL_CODES = new CustomSet<>(10);
    public static final SparseSignalType[] CODE_TO_SPARSE_SIGNAL;

    static{
        int maxCode = 0;
        for(SparseSignalType sparseSignalType:SparseSignalType.values()){
            ALL_SPARSE_SIGNAL_CODES.add(sparseSignalType.code);
            maxCode = Math.max(sparseSignalType.code,maxCode);
        }
        CODE_TO_SPARSE_SIGNAL = new SparseSignalType[maxCode+1];
        // TODO: REMOVE THIS. This is bad code
        for(SparseSignalType sparseSignalType:SparseSignalType.values())
            CODE_TO_SPARSE_SIGNAL[sparseSignalType.code] = sparseSignalType;
    }

    public SparseSignal(SparseSignalType type, MapLocation target,int offset){
        this.type=type;
        this.target=target;
        this.offset=offset;
    }

    @Override
    public int hashCode(){
        if(target == null)return -type.ordinal();
        return target.x + target.y*60 + type.ordinal()*3600;
    }

    @Override
    public boolean equals(Object obj){
        if (this == obj) return true;
        if (!(obj instanceof SparseSignal)) return false;

        SparseSignal that = (SparseSignal) obj;
        return this.hashCode() == that.hashCode();
    }

}
