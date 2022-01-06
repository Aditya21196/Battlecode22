package defensivebot.enums;

// TODO: Decide which signals we need and their relative importance
public enum SparseSignalType {

    // binary: 1
    ATTACK_POSITION(0,1,0,10,1),
    // binary: 00 =>If signal terminates at 00 then we don't have to handle signal termination. It will be automatic
    TERMINATE_SIGNAL_ARRAY(2,1,0,20,2), // highest priority. This can't change.
    // binary: 010
    SPREAD(6,0,0,10,3), // If a sage detects a charge anomaly in some x turns, it puts up a SPREAD flag
    // binary notation: 0110
    GOLD_SPOTTED(14,1,0,5,4),
    // binary: 01110
    SURROUND(30,1,0,5,5),
    // binary: 01111
    DEFEND(31,0,0,10,5);

    // consider: EVACUATE, VORTEX_SIGNAL

    // signal code
    public final int code;
    // if there is a position associated with signal
    public final int positionSlots;
    // We will need this to denote a number
    public final int fixedBits;
    public final int priority;
    public final int numBits;

    SparseSignalType(int code, int positionSlots,int fixedBits, int priority,int numBits) {
        this.code=code;
        this.positionSlots=positionSlots;
        this.fixedBits = fixedBits;
        this.priority=priority;
        this.numBits = numBits;
    }

//    public int getTotalBits(){
//
//    }

}
