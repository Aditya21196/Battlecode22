package gabot4.models;

import battlecode.common.MapLocation;
import gabot4.datasturctures.CustomSet;
import gabot4.enums.SparseSignalType;

public class SparseSignal {

    public SparseSignalType type;
    public MapLocation target;
    public int offset=-1;
    public int fixedBitsVal=0;
    public boolean bitsModified=false;

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

    public void modifyBitVal(int newFixedBitsVal){
        this.fixedBitsVal = newFixedBitsVal;
        bitsModified = true;
    }

    public SparseSignal(SparseSignal signal){
        this.type = signal.type;
        this.target = signal.target;

    }

    public SparseSignal(SparseSignalType type, MapLocation target,int offset){
        this.type=type;
        this.target=target;
        this.offset=offset;
    }

    public SparseSignal(SparseSignalType type, MapLocation target,int offset,int fixedBitsVal){
        this.type=type;
        this.target=target;
        this.offset=offset;
        this.fixedBitsVal=fixedBitsVal;
    }

    @Override
    public int hashCode(){
        // TODO: simplify
        int ans = type.ordinal()*3600;
        if(target != null)ans += target.x + target.y*60;
        return ans;
    }

    @Override
    public boolean equals(Object obj){
        if (this == obj) return true;
        if (!(obj instanceof SparseSignal)) return false;

        SparseSignal that = (SparseSignal) obj;
        return this.hashCode() == that.hashCode();
    }

}
