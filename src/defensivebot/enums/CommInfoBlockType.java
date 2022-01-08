package defensivebot.enums;

public enum CommInfoBlockType {

    LEAD_MAP(0,2), // cumulative offset - 2
    ATTACKING_FRIENDLY_UNITS(2,1), // cumulative offset - 3. Threshold - 5 units or at least one watch tower
    EXPLORATION(3,1), // cumulative offset - 4
    ENEMY_UNITS(4,2); // cumulative offset - 6

    // we still have at least 8 blocks for sparse signals (half array)


    public final int offset;
    public final int blockSize;


    CommInfoBlockType(int offset, int blockSize) {
        this.offset=offset;
        this.blockSize=blockSize;
    }
}
