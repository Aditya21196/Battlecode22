package defensivebot2.utils;

public class PathFindingConstants {

    // granularity- 32. For 180 degrees vision, look left by 8 and right by 8
    public static final int[][] BORDER20 = new int[][]{{4 , 0 },{ 4 , 1 },{ 4 , 2 },{ 3 , 2 },{ 3 , 3 },{ 2 , 3 },{ 2 , 4 },{ 1 , 4 },{ 0 , 4 },{ -1 , 4 },{ -2 , 4 },{ -2 , 3 },{ -3 , 3 },{ -3 , 2 },{ -4 , 2 },{ -4 , 1 },{ -4 , 0 },{ -4 , -1 },{ -4 , -2 },{ -3 , -2 },{ -3 , -3 },{ -2 , -3 },{ -2 , -4 },{ -1 , -4 },{ 0 , -4 },{ 1 , -4 },{ 2 , -4 },{ 2 , -3 },{ 3 , -3 },{ 3 , -2 },{ 4 , -2 },{ 4 , -1 }};

    public static final double RUBBLE_SCORE_MULTIPLIER = 1;
    public static final double RUBBLE_SCORE_LOCAL_MULTIPLIER = 0.4;

    public static final int MIN_AFFORD_PATH_OPT = 3000;
    public static final int NUM_BETTER_LOC_ITERATIONS = 3;
    public static final int SOLDIER_PATHFINDING_LIMIT = 2500;

}
