package gabot3.enums;

public enum FixedDataSignalType {

    FIRST_FRIENDLY_ARCHON_IDX(62,0),
    SECOND_FRIENDLY_ARCHON_IDX(61,1),
    THIRD_FRIENDLY_ARCHON_IDX(60,2),
    FOURTH_FRIENDLY_ARCHON_IDX(59,3),
    FIRST_ENEMY_ARCHON_IDX(58,4),
    SECOND_ENEMY_ARCHON_IDX(57,5),
    THRID_ENEMY_ARCHON_IDX(56,6),
    FOURTH_ENEMY_ARCHON_IDX(55,7),
    FIRST_GATHER_POINT(54,8),
    SECOND_GATHER_POINT(53,9);
    
    public int availabilityIdx;
    public int arrayIdx;

    FixedDataSignalType(int arrayIdx, int availabilityIdx) {
        this.arrayIdx=arrayIdx;
        this.availabilityIdx=availabilityIdx;
    }
}
