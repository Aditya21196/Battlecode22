package gabot1.utils;

public class PathFindingConstants {

    // granularity- 32. For 180 degrees vision, look left by 8 and right by 8
    public static final int[][] BORDER20 = new int[][]{{4 , 0 },{ 4 , 1 },{ 4 , 2 },{ 3 , 2 },{ 3 , 3 },{ 2 , 3 },{ 2 , 4 },{ 1 , 4 },{ 0 , 4 },{ -1 , 4 },{ -2 , 4 },{ -2 , 3 },{ -3 , 3 },{ -3 , 2 },{ -4 , 2 },{ -4 , 1 },{ -4 , 0 },{ -4 , -1 },{ -4 , -2 },{ -3 , -2 },{ -3 , -3 },{ -2 , -3 },{ -2 , -4 },{ -1 , -4 },{ 0 , -4 },{ 1 , -4 },{ 2 , -4 },{ 2 , -3 },{ 3 , -3 },{ 3 , -2 },{ 4 , -2 },{ 4 , -1 }};

    public static final double RUBBLE_SCORE_MULTIPLIER =8.7;
    public static final double RUBBLE_SCORE_LOCAL_MULTIPLIER =7.25;

    public static final int MIN_AFFORD_PATH_OPT =3500;
    public static final int NUM_BETTER_LOC_ITERATIONS =4;
    public static final int SOLDIER_PATHFINDING_LIMIT =3206;
    public static final int MINER_PATHFINDING_LIMIT =3160;
    public static final int DEFAULT_LIMIT =2634;
    public static final int CACHING_RUBBLE_LIMIT=30;
    // 8 means 180 degrees
    public static final int VISION_RADIUS=6;
    // bias towards 1 direction
    public static final int VISION_BIAS=2;

}
