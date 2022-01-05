package defensivebot.enums;

// TODO: Decide which signals we need and their relative importance
public enum SparseSignal {

    // bit notation: 0
    ATTACK_POSITION(0,1,10),
    // bit notation: 10
    CRITICAL_THREAT(2,1,10),
    // bit notation: 110
    SPREAD(6,0,10),
    // bit notation: 1110
    GOLD_SPOTTED(14,1,5),
    // bit notation: 11110
    SURROUND(30,1,5),
    // bit notation: 11111
    TERMINATE_SIGNAL_ARRAY(31,0,20); // highest priority

    // consider: EVACUATE

    // signal code
    int code;
    // if there is a position associated with signal
    int positionSlots;
    int priority;

    SparseSignal(int code, int positionSlots, int priority) {
        this.code=code;
        this.positionSlots=positionSlots;
        this.priority=priority;
    }

//    public int getTotalBits(){
//
//    }

}
