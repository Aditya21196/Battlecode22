package defensivebot.utils;

import battlecode.common.Direction;

public class Constants {

    /** Array containing all the possible movement directions. */
    public static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    // at ith index, we have 1<<i
    public static final int[] bitMasks = new int[]{1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768};

    public static final int MAX_MAP_SIZE=60;
    public static final int UNITS_AVAILABLE=7;

}
