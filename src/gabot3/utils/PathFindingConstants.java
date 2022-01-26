package gabot3.utils;

public class PathFindingConstants {

    // granularity- 32. For 180 degrees vision, look left by 8 and right by 8
    public static final int[][] BORDER20 = new int[][]{{4 , 0 },{ 4 , 1 },{ 4 , 2 },{ 3 , 2 },{ 3 , 3 },{ 2 , 3 },{ 2 , 4 },{ 1 , 4 },{ 0 , 4 },{ -1 , 4 },{ -2 , 4 },{ -2 , 3 },{ -3 , 3 },{ -3 , 2 },{ -4 , 2 },{ -4 , 1 },{ -4 , 0 },{ -4 , -1 },{ -4 , -2 },{ -3 , -2 },{ -3 , -3 },{ -2 , -3 },{ -2 , -4 },{ -1 , -4 },{ 0 , -4 },{ 1 , -4 },{ 2 , -4 },{ 2 , -3 },{ 3 , -3 },{ 3 , -2 },{ 4 , -2 },{ 4 , -1 }};

    public static final double RUBBLE_SCORE_MULTIPLIER =9.1;
    public static final double RUBBLE_SCORE_LOCAL_MULTIPLIER =6.8;

    public static final int MIN_AFFORD_PATH_OPT =3000;
    public static final int NUM_BETTER_LOC_ITERATIONS =3;
    public static final int SOLDIER_PATHFINDING_LIMIT =2500;
    public static final int MINER_PATHFINDING_LIMIT =2960;
    public static final int DEFAULT_LIMIT =2060;
    public static final int CACHING_RUBBLE_LIMIT=12;
    // 8 means 180 degrees
    public static final int VISION_RADIUS=15;
    // bias towards 1 direction
    public static final int VISION_BIAS=3;

}
