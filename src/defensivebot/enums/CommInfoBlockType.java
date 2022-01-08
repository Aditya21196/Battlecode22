package defensivebot.enums;

public enum CommInfoBlockType {

    LEAD_MAP(0,2), // cumulative offset - 2
    RUBBLE_MAP(2,2), // cumulative offset - 4
    FRIENDLY_UNITS(4,1), // cumulative offset - 6. Threshold - 5 units or at least one watch tower
    EXPLORATION(5,1), // cumulative offset - 5
    ENEMY_UNITS(6,2); // cumulative offset - 8

    // we still have at least 8 blocks for sparse signals (half array)


    public final int offset;
    public final int blockSize;


    CommInfoBlockType(int offset, int blockSize) {
        this.offset=offset;
        this.blockSize=blockSize;
    }
}
