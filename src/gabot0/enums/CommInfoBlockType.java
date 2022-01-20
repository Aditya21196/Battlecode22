package gabot0.enums;

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

    public int getStoreVal(int val){
        switch (this){
            case LEAD_MAP:
                if(val>30)return 3;
                if(val>20)return 2;
                return 0;
            case EXPLORATION:
                if(val>0)return 1;
                return 0;
        }
        return 1;
    }

}
