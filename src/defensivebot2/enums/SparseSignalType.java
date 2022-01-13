package defensivebot2.enums;

// TODO: Decide which signals we need and their relative importance
public enum SparseSignalType {

    // binary: 1
    ATTACK_POSITION(1,1,0,10,1),
    // binary: 00 =>If signal terminates at 00 then we don't have to handle signal termination. It will be automatic
    TERMINATE_SIGNAL_ARRAY(0,1,0,20,2), // highest priority. This can't change.
    // binary: 010
    ENEMY_SPOTTED(2,0,0,10,3),
    // binary notation: 0110
    ENEMY_ARCHON_LOCATION(6,1,2,5,4),// first bit: enemy or friendly, 2nd bit: to attack or not to attack
    // binary: 01110
    SURROUND(14,1,0,5,5),
    // binary: 01111
    DEFEND(15,0,0,10,5);

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
