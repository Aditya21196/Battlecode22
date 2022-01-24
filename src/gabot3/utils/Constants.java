package gabot3.utils;

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

    // BFS at distance 2
    public static final int[][] BFS2 = new int[][]{{-1,0},{-1,1},{-1,-1},{1,0},{1,1},{1,-1},{0,1},{0,-1},{0,0}};

    // BFS at distance 25
    public static final int[][][] BFS25 = {
            {  { 0 , 0 },{ 1 , 0 },{ 1 , 1 },{ 1 , -1 },{ -1 , 0 },{ -1 , 1 },{ -1 , -1 },{ 0 , 1 },{ 0 , -1 },{ 2 , 0 },{ 2 , 1 },{ 2 , -1 },{ 2 , 2 },{ 0 , 2 },{ 1 , 2 },{ 2 , -2 },{ 0 , -2 },{ 1 , -2 },{ -2 , 0 },{ -2 , 1 },{ -2 , -1 },{ -2 , 2 },{ -1 , 2 },{ -2 , -2 },{ -1 , -2 },{ 3 , 0 },{ 3 , 1 },{ 3 , -1 },{ 3 , 2 },{ 3 , -2 },{ 3 , 3 },{ 1 , 3 },{ 2 , 3 },{ -1 , 3 },{ 0 , 3 },{ 3 , -3 },{ 1 , -3 },{ 2 , -3 },{ -1 , -3 },{ 0 , -3 },{ -3 , 0 },{ -3 , 1 },{ -3 , -1 },{ -3 , 2 },{ -3 , -2 },{ -3 , 3 },{ -2 , 3 },{ -3 , -3 },{ -2 , -3 },{ 4 , 0 },{ 4 , 1 },{ 4 , -1 },{ 4 , 2 },{ 4 , -2 },{ 4 , 3 },{ 4 , -3 },{ 2 , 4 },{ 3 , 4 },{ 0 , 4 },{ 1 , 4 },{ -2 , 4 },{ -1 , 4 },{ 2 , -4 },{ 3 , -4 },{ 0 , -4 },{ 1 , -4 },{ -2 , -4 },{ -1 , -4 },{ -4 , 0 },{ -4 , 1 },{ -4 , -1 },{ -4 , 2 },{ -4 , -2 },{ -4 , 3 },{ -4 , -3 },{ -3 , 4 },{ -3 , -4 },{ 5 , 0 },{ 0 , 5 },{ 0 , -5 },{ -5 , 0 },},
            {  { 0 , 0 },{ 1 , 1 },{ 1 , -1 },{ 1 , 0 },{ -1 , 1 },{ -1 , -1 },{ -1 , 0 },{ 0 , 1 },{ 0 , -1 },{ 2 , 2 },{ 2 , 0 },{ 2 , 1 },{ 0 , 2 },{ 1 , 2 },{ 2 , -2 },{ 2 , -1 },{ 0 , -2 },{ 1 , -2 },{ -2 , 2 },{ -2 , 0 },{ -2 , 1 },{ -1 , 2 },{ -2 , -2 },{ -2 , -1 },{ -1 , -2 },{ 3 , 3 },{ 3 , 1 },{ 3 , 2 },{ 1 , 3 },{ 2 , 3 },{ 3 , -1 },{ 3 , 0 },{ -1 , 3 },{ 0 , 3 },{ 3 , -3 },{ 3 , -2 },{ 1 , -3 },{ 2 , -3 },{ -1 , -3 },{ 0 , -3 },{ -3 , 3 },{ -3 , 1 },{ -3 , 2 },{ -2 , 3 },{ -3 , -1 },{ -3 , 0 },{ -3 , -3 },{ -3 , -2 },{ -2 , -3 },{ 4 , 2 },{ 4 , 3 },{ 2 , 4 },{ 3 , 4 },{ 4 , 0 },{ 4 , 1 },{ 0 , 4 },{ 1 , 4 },{ 4 , -2 },{ 4 , -1 },{ -2 , 4 },{ -1 , 4 },{ 4 , -3 },{ 2 , -4 },{ 3 , -4 },{ 0 , -4 },{ 1 , -4 },{ -2 , -4 },{ -1 , -4 },{ -4 , 2 },{ -4 , 3 },{ -3 , 4 },{ -4 , 0 },{ -4 , 1 },{ -4 , -2 },{ -4 , -1 },{ -4 , -3 },{ -3 , -4 },{ 5 , 0 },{ 0 , 5 },{ 0 , -5 },{ -5 , 0 },},
            {  { 0 , 0 },{ 1 , -1 },{ 1 , 0 },{ 1 , 1 },{ -1 , -1 },{ -1 , 0 },{ -1 , 1 },{ 0 , -1 },{ 0 , 1 },{ 2 , -2 },{ 2 , -1 },{ 2 , 0 },{ 0 , -2 },{ 1 , -2 },{ 2 , 1 },{ 2 , 2 },{ 0 , 2 },{ 1 , 2 },{ -2 , -2 },{ -2 , -1 },{ -2 , 0 },{ -1 , -2 },{ -2 , 1 },{ -2 , 2 },{ -1 , 2 },{ 3 , -3 },{ 3 , -2 },{ 3 , -1 },{ 1 , -3 },{ 2 , -3 },{ 3 , 0 },{ 3 , 1 },{ -1 , -3 },{ 0 , -3 },{ 3 , 2 },{ 3 , 3 },{ 1 , 3 },{ 2 , 3 },{ -1 , 3 },{ 0 , 3 },{ -3 , -3 },{ -3 , -2 },{ -3 , -1 },{ -2 , -3 },{ -3 , 0 },{ -3 , 1 },{ -3 , 2 },{ -3 , 3 },{ -2 , 3 },{ 4 , -3 },{ 4 , -2 },{ 2 , -4 },{ 3 , -4 },{ 4 , -1 },{ 4 , 0 },{ 0 , -4 },{ 1 , -4 },{ 4 , 1 },{ 4 , 2 },{ -2 , -4 },{ -1 , -4 },{ 4 , 3 },{ 2 , 4 },{ 3 , 4 },{ 0 , 4 },{ 1 , 4 },{ -2 , 4 },{ -1 , 4 },{ -4 , -3 },{ -4 , -2 },{ -3 , -4 },{ -4 , -1 },{ -4 , 0 },{ -4 , 1 },{ -4 , 2 },{ -4 , 3 },{ -3 , 4 },{ 5 , 0 },{ 0 , -5 },{ 0 , 5 },{ -5 , 0 },},
            {  { 0 , 0 },{ 0 , 1 },{ 0 , -1 },{ 1 , 0 },{ 1 , 1 },{ 1 , -1 },{ -1 , 0 },{ -1 , 1 },{ -1 , -1 },{ 0 , 2 },{ 1 , 2 },{ -1 , 2 },{ 0 , -2 },{ 1 , -2 },{ -1 , -2 },{ 2 , 0 },{ 2 , 1 },{ 2 , -1 },{ 2 , 2 },{ 2 , -2 },{ -2 , 0 },{ -2 , 1 },{ -2 , -1 },{ -2 , 2 },{ -2 , -2 },{ 0 , 3 },{ 1 , 3 },{ -1 , 3 },{ 2 , 3 },{ -2 , 3 },{ 0 , -3 },{ 1 , -3 },{ -1 , -3 },{ 2 , -3 },{ -2 , -3 },{ 3 , 0 },{ 3 , 1 },{ 3 , -1 },{ 3 , 2 },{ 3 , -2 },{ 3 , 3 },{ 3 , -3 },{ -3 , 0 },{ -3 , 1 },{ -3 , -1 },{ -3 , 2 },{ -3 , -2 },{ -3 , 3 },{ -3 , -3 },{ 0 , 4 },{ 1 , 4 },{ -1 , 4 },{ 2 , 4 },{ -2 , 4 },{ 3 , 4 },{ -3 , 4 },{ 0 , -4 },{ 1 , -4 },{ -1 , -4 },{ 2 , -4 },{ -2 , -4 },{ 3 , -4 },{ -3 , -4 },{ 4 , 0 },{ 4 , 1 },{ 4 , -1 },{ 4 , 2 },{ 4 , -2 },{ 4 , 3 },{ 4 , -3 },{ -4 , 0 },{ -4 , 1 },{ -4 , -1 },{ -4 , 2 },{ -4 , -2 },{ -4 , 3 },{ -4 , -3 },{ 0 , 5 },{ 0 , -5 },{ 5 , 0 },{ -5 , 0 },},
            {  { 0 , 0 },{ 0 , 1 },{ 0 , -1 },{ 1 , 1 },{ 1 , -1 },{ 1 , 0 },{ -1 , 1 },{ -1 , -1 },{ -1 , 0 },{ 0 , 2 },{ 1 , 2 },{ -1 , 2 },{ 0 , -2 },{ 1 , -2 },{ -1 , -2 },{ 2 , 2 },{ 2 , 0 },{ 2 , 1 },{ 2 , -2 },{ 2 , -1 },{ -2 , 2 },{ -2 , 0 },{ -2 , 1 },{ -2 , -2 },{ -2 , -1 },{ 0 , 3 },{ 1 , 3 },{ -1 , 3 },{ 2 , 3 },{ -2 , 3 },{ 0 , -3 },{ 1 , -3 },{ -1 , -3 },{ 2 , -3 },{ -2 , -3 },{ 3 , 3 },{ 3 , 1 },{ 3 , 2 },{ 3 , -1 },{ 3 , 0 },{ 3 , -3 },{ 3 , -2 },{ -3 , 3 },{ -3 , 1 },{ -3 , 2 },{ -3 , -1 },{ -3 , 0 },{ -3 , -3 },{ -3 , -2 },{ 0 , 4 },{ 1 , 4 },{ -1 , 4 },{ 2 , 4 },{ -2 , 4 },{ 3 , 4 },{ -3 , 4 },{ 0 , -4 },{ 1 , -4 },{ -1 , -4 },{ 2 , -4 },{ -2 , -4 },{ 3 , -4 },{ -3 , -4 },{ 4 , 2 },{ 4 , 3 },{ 4 , 0 },{ 4 , 1 },{ 4 , -2 },{ 4 , -1 },{ 4 , -3 },{ -4 , 2 },{ -4 , 3 },{ -4 , 0 },{ -4 , 1 },{ -4 , -2 },{ -4 , -1 },{ -4 , -3 },{ 0 , 5 },{ 0 , -5 },{ 5 , 0 },{ -5 , 0 },},
            {  { 0 , 0 },{ 0 , -1 },{ 0 , 1 },{ 1 , -1 },{ 1 , 0 },{ 1 , 1 },{ -1 , -1 },{ -1 , 0 },{ -1 , 1 },{ 0 , -2 },{ 1 , -2 },{ -1 , -2 },{ 0 , 2 },{ 1 , 2 },{ -1 , 2 },{ 2 , -2 },{ 2 , -1 },{ 2 , 0 },{ 2 , 1 },{ 2 , 2 },{ -2 , -2 },{ -2 , -1 },{ -2 , 0 },{ -2 , 1 },{ -2 , 2 },{ 0 , -3 },{ 1 , -3 },{ -1 , -3 },{ 2 , -3 },{ -2 , -3 },{ 0 , 3 },{ 1 , 3 },{ -1 , 3 },{ 2 , 3 },{ -2 , 3 },{ 3 , -3 },{ 3 , -2 },{ 3 , -1 },{ 3 , 0 },{ 3 , 1 },{ 3 , 2 },{ 3 , 3 },{ -3 , -3 },{ -3 , -2 },{ -3 , -1 },{ -3 , 0 },{ -3 , 1 },{ -3 , 2 },{ -3 , 3 },{ 0 , -4 },{ 1 , -4 },{ -1 , -4 },{ 2 , -4 },{ -2 , -4 },{ 3 , -4 },{ -3 , -4 },{ 0 , 4 },{ 1 , 4 },{ -1 , 4 },{ 2 , 4 },{ -2 , 4 },{ 3 , 4 },{ -3 , 4 },{ 4 , -3 },{ 4 , -2 },{ 4 , -1 },{ 4 , 0 },{ 4 , 1 },{ 4 , 2 },{ 4 , 3 },{ -4 , -3 },{ -4 , -2 },{ -4 , -1 },{ -4 , 0 },{ -4 , 1 },{ -4 , 2 },{ -4 , 3 },{ 0 , -5 },{ 0 , 5 },{ 5 , 0 },{ -5 , 0 },},
            {  { 0 , 0 },{ -1 , 0 },{ -1 , 1 },{ -1 , -1 },{ 0 , 1 },{ 0 , -1 },{ 1 , 0 },{ 1 , 1 },{ 1 , -1 },{ -2 , 0 },{ -2 , 1 },{ -2 , -1 },{ -2 , 2 },{ -1 , 2 },{ 0 , 2 },{ -2 , -2 },{ -1 , -2 },{ 0 , -2 },{ 1 , 2 },{ 1 , -2 },{ 2 , 0 },{ 2 , 1 },{ 2 , -1 },{ 2 , 2 },{ 2 , -2 },{ -3 , 0 },{ -3 , 1 },{ -3 , -1 },{ -3 , 2 },{ -3 , -2 },{ -3 , 3 },{ -2 , 3 },{ -1 , 3 },{ 0 , 3 },{ 1 , 3 },{ -3 , -3 },{ -2 , -3 },{ -1 , -3 },{ 0 , -3 },{ 1 , -3 },{ 2 , 3 },{ 2 , -3 },{ 3 , 0 },{ 3 , 1 },{ 3 , -1 },{ 3 , 2 },{ 3 , -2 },{ 3 , 3 },{ 3 , -3 },{ -4 , 0 },{ -4 , 1 },{ -4 , -1 },{ -4 , 2 },{ -4 , -2 },{ -4 , 3 },{ -4 , -3 },{ -3 , 4 },{ -2 , 4 },{ -1 , 4 },{ 0 , 4 },{ 1 , 4 },{ 2 , 4 },{ -3 , -4 },{ -2 , -4 },{ -1 , -4 },{ 0 , -4 },{ 1 , -4 },{ 2 , -4 },{ 3 , 4 },{ 3 , -4 },{ 4 , 0 },{ 4 , 1 },{ 4 , -1 },{ 4 , 2 },{ 4 , -2 },{ 4 , 3 },{ 4 , -3 },{ -5 , 0 },{ 0 , 5 },{ 0 , -5 },{ 5 , 0 },},
            {  { 0 , 0 },{ -1 , 1 },{ -1 , -1 },{ -1 , 0 },{ 0 , 1 },{ 0 , -1 },{ 1 , 1 },{ 1 , -1 },{ 1 , 0 },{ -2 , 2 },{ -2 , 0 },{ -2 , 1 },{ -1 , 2 },{ 0 , 2 },{ -2 , -2 },{ -2 , -1 },{ -1 , -2 },{ 0 , -2 },{ 1 , 2 },{ 1 , -2 },{ 2 , 2 },{ 2 , 0 },{ 2 , 1 },{ 2 , -2 },{ 2 , -1 },{ -3 , 3 },{ -3 , 1 },{ -3 , 2 },{ -2 , 3 },{ -1 , 3 },{ -3 , -1 },{ -3 , 0 },{ 0 , 3 },{ 1 , 3 },{ -3 , -3 },{ -3 , -2 },{ -2 , -3 },{ -1 , -3 },{ 0 , -3 },{ 1 , -3 },{ 2 , 3 },{ 2 , -3 },{ 3 , 3 },{ 3 , 1 },{ 3 , 2 },{ 3 , -1 },{ 3 , 0 },{ 3 , -3 },{ 3 , -2 },{ -4 , 2 },{ -4 , 3 },{ -3 , 4 },{ -2 , 4 },{ -4 , 0 },{ -4 , 1 },{ -1 , 4 },{ 0 , 4 },{ -4 , -2 },{ -4 , -1 },{ 1 , 4 },{ 2 , 4 },{ -4 , -3 },{ -3 , -4 },{ -2 , -4 },{ -1 , -4 },{ 0 , -4 },{ 1 , -4 },{ 2 , -4 },{ 3 , 4 },{ 3 , -4 },{ 4 , 2 },{ 4 , 3 },{ 4 , 0 },{ 4 , 1 },{ 4 , -2 },{ 4 , -1 },{ 4 , -3 },{ -5 , 0 },{ 0 , 5 },{ 0 , -5 },{ 5 , 0 },},
    };

    // BFS at distance 5
    public static final int[][][] DFS25 = {
            {  { 0 , 0 },{ 0 , 1 },{ -1 , 0 },{ -2 , -1 },{ -3 , -2 },{ -4 , -3 },{ -3 , -4 },{ -2 , -4 },{ -1 , -4 },{ 0 , -5 },{ 0 , -4 },{ 1 , -4 },{ 2 , -4 },{ 3 , -4 },{ 2 , -3 },{ 1 , -3 },{ 0 , -3 },{ -1 , -3 },{ -2 , -3 },{ -3 , -3 },{ -4 , -2 },{ -4 , -1 },{ -3 , -1 },{ -2 , -2 },{ -1 , -2 },{ 0 , -2 },{ 1 , -2 },{ 2 , -2 },{ 3 , -3 },{ 4 , -3 },{ 3 , -2 },{ 4 , -2 },{ 3 , -1 },{ 2 , -1 },{ 1 , -1 },{ 0 , -1 },{ -1 , -1 },{ -2 , 0 },{ -3 , 0 },{ -4 , 0 },{ -5 , 0 },{ -4 , 1 },{ -3 , 1 },{ -2 , 1 },{ -1 , 1 },{ -2 , 2 },{ -3 , 2 },{ -4 , 2 },{ -4 , 3 },{ -3 , 3 },{ -2 , 3 },{ -1 , 2 },{ 0 , 2 },{ 1 , 1 },{ 1 , 0 },{ 2 , 0 },{ 3 , 0 },{ 4 , -1 },{ 4 , 0 },{ 5 , 0 },{ 4 , 1 },{ 3 , 1 },{ 2 , 1 },{ 1 , 2 },{ 2 , 2 },{ 3 , 2 },{ 4 , 2 },{ 3 , 3 },{ 2 , 3 },{ 1 , 3 },{ 0 , 3 },{ -1 , 3 },{ -2 , 4 },{ -3 , 4 },{ -1 , 4 },{ 0 , 4 },{ 1 , 4 },{ 2 , 4 },{ 3 , 4 },{ 4 , 3 },{ 0 , 5 },},
            {  { 0 , 0 },{ 1 , 1 },{ 1 , 0 },{ 0 , -1 },{ -1 , -2 },{ -2 , -3 },{ -3 , -4 },{ -2 , -4 },{ -1 , -4 },{ 0 , -5 },{ 0 , -4 },{ 1 , -4 },{ 2 , -4 },{ 3 , -4 },{ 2 , -3 },{ 1 , -3 },{ 0 , -3 },{ -1 , -3 },{ -2 , -2 },{ -3 , -3 },{ -4 , -3 },{ -4 , -2 },{ -3 , -2 },{ -4 , -1 },{ -3 , -1 },{ -2 , -1 },{ -1 , -1 },{ 0 , -2 },{ 1 , -2 },{ 2 , -2 },{ 3 , -3 },{ 4 , -3 },{ 3 , -2 },{ 4 , -2 },{ 3 , -1 },{ 2 , -1 },{ 1 , -1 },{ 2 , 0 },{ 3 , 0 },{ 4 , -1 },{ 4 , 0 },{ 5 , 0 },{ 4 , 1 },{ 3 , 1 },{ 2 , 1 },{ 1 , 2 },{ 0 , 1 },{ -1 , 0 },{ -2 , 0 },{ -3 , 0 },{ -4 , 0 },{ -5 , 0 },{ -4 , 1 },{ -3 , 1 },{ -2 , 1 },{ -1 , 1 },{ -2 , 2 },{ -3 , 2 },{ -4 , 2 },{ -4 , 3 },{ -3 , 3 },{ -2 , 3 },{ -1 , 2 },{ 0 , 2 },{ -1 , 3 },{ 0 , 3 },{ 1 , 3 },{ 2 , 2 },{ 3 , 2 },{ 4 , 2 },{ 3 , 3 },{ 2 , 3 },{ 1 , 4 },{ 0 , 4 },{ -1 , 4 },{ -2 , 4 },{ -3 , 4 },{ 0 , 5 },{ 2 , 4 },{ 3 , 4 },{ 4 , 3 },},
            {  { 0 , 0 },{ -1 , 1 },{ -2 , 0 },{ -3 , -1 },{ -4 , -2 },{ -4 , -3 },{ -3 , -4 },{ -2 , -4 },{ -1 , -4 },{ 0 , -5 },{ 0 , -4 },{ 1 , -4 },{ 2 , -4 },{ 3 , -4 },{ 2 , -3 },{ 1 , -3 },{ 0 , -3 },{ -1 , -3 },{ -2 , -3 },{ -3 , -3 },{ -3 , -2 },{ -2 , -2 },{ -1 , -2 },{ 0 , -2 },{ 1 , -2 },{ 2 , -2 },{ 3 , -3 },{ 4 , -3 },{ 3 , -2 },{ 4 , -2 },{ 3 , -1 },{ 2 , -1 },{ 1 , -1 },{ 0 , -1 },{ -1 , -1 },{ -2 , -1 },{ -3 , 0 },{ -4 , -1 },{ -5 , 0 },{ -4 , 0 },{ -4 , 1 },{ -3 , 1 },{ -2 , 1 },{ -1 , 0 },{ 0 , 1 },{ 1 , 0 },{ 2 , 0 },{ 3 , 0 },{ 4 , -1 },{ 4 , 0 },{ 5 , 0 },{ 4 , 1 },{ 3 , 1 },{ 2 , 1 },{ 1 , 1 },{ 0 , 2 },{ -1 , 2 },{ -2 , 2 },{ -3 , 2 },{ -4 , 2 },{ -4 , 3 },{ -3 , 3 },{ -2 , 3 },{ -1 , 3 },{ 0 , 3 },{ 1 , 2 },{ 2 , 2 },{ 3 , 2 },{ 4 , 2 },{ 3 , 3 },{ 2 , 3 },{ 1 , 3 },{ 0 , 4 },{ -1 , 4 },{ -2 , 4 },{ -3 , 4 },{ 0 , 5 },{ 1 , 4 },{ 2 , 4 },{ 3 , 4 },{ 4 , 3 },},
            {  { 0 , 0 },{ 1 , 0 },{ 0 , -1 },{ -1 , -2 },{ -2 , -3 },{ -3 , -4 },{ -2 , -4 },{ -1 , -4 },{ 0 , -5 },{ 0 , -4 },{ 1 , -4 },{ 2 , -4 },{ 3 , -4 },{ 2 , -3 },{ 1 , -3 },{ 0 , -3 },{ -1 , -3 },{ -2 , -2 },{ -3 , -3 },{ -4 , -3 },{ -4 , -2 },{ -3 , -2 },{ -4 , -1 },{ -3 , -1 },{ -2 , -1 },{ -1 , -1 },{ 0 , -2 },{ 1 , -2 },{ 2 , -2 },{ 3 , -3 },{ 4 , -3 },{ 3 , -2 },{ 4 , -2 },{ 3 , -1 },{ 2 , -1 },{ 1 , -1 },{ 2 , 0 },{ 3 , 0 },{ 4 , -1 },{ 4 , 0 },{ 5 , 0 },{ 4 , 1 },{ 3 , 1 },{ 2 , 1 },{ 1 , 1 },{ 0 , 1 },{ -1 , 0 },{ -2 , 0 },{ -3 , 0 },{ -4 , 0 },{ -5 , 0 },{ -4 , 1 },{ -3 , 1 },{ -2 , 1 },{ -1 , 1 },{ -2 , 2 },{ -3 , 2 },{ -4 , 2 },{ -4 , 3 },{ -3 , 3 },{ -2 , 3 },{ -1 , 2 },{ 0 , 2 },{ 1 , 2 },{ 2 , 2 },{ 3 , 2 },{ 4 , 2 },{ 3 , 3 },{ 2 , 3 },{ 1 , 3 },{ 0 , 3 },{ -1 , 3 },{ -2 , 4 },{ -3 , 4 },{ -1 , 4 },{ 0 , 4 },{ 1 , 4 },{ 2 , 4 },{ 3 , 4 },{ 4 , 3 },{ 0 , 5 },},
            {  { 0 , 0 },{ 1 , 0 },{ 0 , -1 },{ -1 , -2 },{ -2 , -3 },{ -3 , -4 },{ -2 , -4 },{ -1 , -4 },{ 0 , -5 },{ 0 , -4 },{ 1 , -4 },{ 2 , -4 },{ 3 , -4 },{ 2 , -3 },{ 1 , -3 },{ 0 , -3 },{ -1 , -3 },{ -2 , -2 },{ -3 , -3 },{ -4 , -3 },{ -4 , -2 },{ -3 , -2 },{ -4 , -1 },{ -3 , -1 },{ -2 , -1 },{ -1 , -1 },{ 0 , -2 },{ 1 , -2 },{ 2 , -2 },{ 3 , -3 },{ 4 , -3 },{ 3 , -2 },{ 4 , -2 },{ 3 , -1 },{ 2 , -1 },{ 1 , -1 },{ 2 , 0 },{ 3 , 0 },{ 4 , -1 },{ 4 , 0 },{ 5 , 0 },{ 4 , 1 },{ 3 , 1 },{ 2 , 1 },{ 1 , 1 },{ 0 , 1 },{ -1 , 0 },{ -2 , 0 },{ -3 , 0 },{ -4 , 0 },{ -5 , 0 },{ -4 , 1 },{ -3 , 1 },{ -2 , 1 },{ -1 , 1 },{ -2 , 2 },{ -3 , 2 },{ -4 , 2 },{ -4 , 3 },{ -3 , 3 },{ -2 , 3 },{ -1 , 2 },{ 0 , 2 },{ 1 , 2 },{ 2 , 2 },{ 3 , 2 },{ 4 , 2 },{ 3 , 3 },{ 2 , 3 },{ 1 , 3 },{ 0 , 3 },{ -1 , 3 },{ -2 , 4 },{ -3 , 4 },{ -1 , 4 },{ 0 , 4 },{ 1 , 4 },{ 2 , 4 },{ 3 , 4 },{ 4 , 3 },{ 0 , 5 },},
            {  { 0 , 0 },{ -1 , 0 },{ -2 , -1 },{ -3 , -2 },{ -4 , -3 },{ -3 , -4 },{ -2 , -4 },{ -1 , -4 },{ 0 , -5 },{ 0 , -4 },{ 1 , -4 },{ 2 , -4 },{ 3 , -4 },{ 2 , -3 },{ 1 , -3 },{ 0 , -3 },{ -1 , -3 },{ -2 , -3 },{ -3 , -3 },{ -4 , -2 },{ -4 , -1 },{ -3 , -1 },{ -2 , -2 },{ -1 , -2 },{ 0 , -2 },{ 1 , -2 },{ 2 , -2 },{ 3 , -3 },{ 4 , -3 },{ 3 , -2 },{ 4 , -2 },{ 3 , -1 },{ 2 , -1 },{ 1 , -1 },{ 0 , -1 },{ -1 , -1 },{ -2 , 0 },{ -3 , 0 },{ -4 , 0 },{ -5 , 0 },{ -4 , 1 },{ -3 , 1 },{ -2 , 1 },{ -1 , 1 },{ 0 , 1 },{ 1 , 0 },{ 2 , 0 },{ 3 , 0 },{ 4 , -1 },{ 4 , 0 },{ 5 , 0 },{ 4 , 1 },{ 3 , 1 },{ 2 , 1 },{ 1 , 1 },{ 0 , 2 },{ -1 , 2 },{ -2 , 2 },{ -3 , 2 },{ -4 , 2 },{ -4 , 3 },{ -3 , 3 },{ -2 , 3 },{ -1 , 3 },{ 0 , 3 },{ 1 , 2 },{ 2 , 2 },{ 3 , 2 },{ 4 , 2 },{ 3 , 3 },{ 2 , 3 },{ 1 , 3 },{ 0 , 4 },{ -1 , 4 },{ -2 , 4 },{ -3 , 4 },{ 0 , 5 },{ 1 , 4 },{ 2 , 4 },{ 3 , 4 },{ 4 , 3 },},
            {  { 0 , 0 },{ 0 , -1 },{ -1 , -2 },{ -2 , -3 },{ -3 , -4 },{ -2 , -4 },{ -1 , -4 },{ 0 , -5 },{ 0 , -4 },{ 1 , -4 },{ 2 , -4 },{ 3 , -4 },{ 2 , -3 },{ 1 , -3 },{ 0 , -3 },{ -1 , -3 },{ -2 , -2 },{ -3 , -3 },{ -4 , -3 },{ -4 , -2 },{ -3 , -2 },{ -4 , -1 },{ -3 , -1 },{ -2 , -1 },{ -1 , -1 },{ 0 , -2 },{ 1 , -2 },{ 2 , -2 },{ 3 , -3 },{ 4 , -3 },{ 3 , -2 },{ 4 , -2 },{ 3 , -1 },{ 2 , -1 },{ 1 , -1 },{ 1 , 0 },{ 2 , 0 },{ 3 , 0 },{ 4 , -1 },{ 4 , 0 },{ 5 , 0 },{ 4 , 1 },{ 3 , 1 },{ 2 , 1 },{ 1 , 1 },{ 0 , 1 },{ -1 , 0 },{ -2 , 0 },{ -3 , 0 },{ -4 , 0 },{ -5 , 0 },{ -4 , 1 },{ -3 , 1 },{ -2 , 1 },{ -1 , 1 },{ -2 , 2 },{ -3 , 2 },{ -4 , 2 },{ -4 , 3 },{ -3 , 3 },{ -2 , 3 },{ -1 , 2 },{ 0 , 2 },{ 1 , 2 },{ 2 , 2 },{ 3 , 2 },{ 4 , 2 },{ 3 , 3 },{ 2 , 3 },{ 1 , 3 },{ 0 , 3 },{ -1 , 3 },{ -2 , 4 },{ -3 , 4 },{ -1 , 4 },{ 0 , 4 },{ 1 , 4 },{ 2 , 4 },{ 3 , 4 },{ 4 , 3 },{ 0 , 5 },},
            {  { 0 , 0 },{ 1 , -1 },{ 0 , -2 },{ -1 , -3 },{ -2 , -4 },{ -3 , -4 },{ -4 , -3 },{ -3 , -3 },{ -2 , -3 },{ -1 , -4 },{ 0 , -5 },{ 0 , -4 },{ 1 , -4 },{ 2 , -4 },{ 3 , -4 },{ 2 , -3 },{ 1 , -3 },{ 0 , -3 },{ -1 , -2 },{ -2 , -2 },{ -3 , -2 },{ -4 , -2 },{ -4 , -1 },{ -3 , -1 },{ -2 , -1 },{ -1 , -1 },{ 0 , -1 },{ 1 , -2 },{ 2 , -2 },{ 3 , -3 },{ 4 , -3 },{ 3 , -2 },{ 4 , -2 },{ 3 , -1 },{ 2 , -1 },{ 1 , 0 },{ 2 , 0 },{ 3 , 0 },{ 4 , -1 },{ 4 , 0 },{ 5 , 0 },{ 4 , 1 },{ 3 , 1 },{ 2 , 1 },{ 1 , 1 },{ 0 , 1 },{ -1 , 0 },{ -2 , 0 },{ -3 , 0 },{ -4 , 0 },{ -5 , 0 },{ -4 , 1 },{ -3 , 1 },{ -2 , 1 },{ -1 , 1 },{ -2 , 2 },{ -3 , 2 },{ -4 , 2 },{ -4 , 3 },{ -3 , 3 },{ -2 , 3 },{ -1 , 2 },{ 0 , 2 },{ 1 , 2 },{ 2 , 2 },{ 3 , 2 },{ 4 , 2 },{ 3 , 3 },{ 2 , 3 },{ 1 , 3 },{ 0 , 3 },{ -1 , 3 },{ -2 , 4 },{ -3 , 4 },{ -1 , 4 },{ 0 , 4 },{ 1 , 4 },{ 2 , 4 },{ 3 , 4 },{ 4 , 3 },{ 0 , 5 },},
    };

    public static final int[][] BFS_MANHATTAN_5 = { { 0, 0 }, { 1, 0 }, { 0, -1 }, { -1, 0 }, { 0, 1 }, { 2, 0 }, { 1, -1 },
            { 0, -2 }, { -1, -1 }, { -2, 0 }, { -1, 1 }, { 0, 2 }, { 1, 1 }, { 3, 0 }, { 2, -1 }, { 1, -2 }, { 0, -3 },
            { -1, -2 }, { -2, -1 }, { -3, 0 }, { -2, 1 }, { -1, 2 }, { 0, 3 }, { 1, 2 }, { 2, 1 }, { 4, 0 }, { 3, -1 },
            { 2, -2 }, { 1, -3 }, { 0, -4 }, { -1, -3 }, { -2, -2 }, { -3, -1 }, { -4, 0 }, { -3, 1 }, { -2, 2 },
            { -1, 3 }, { 0, 4 }, { 1, 3 }, { 2, 2 }, { 3, 1 }, { 4, -1 }, { 3, -2 }, { 2, -3 }, { 1, -4 }, { -1, -4 }};

    public static final int[][] BFS5_MANHATTAN = { { 0, 0 }, { 1, 0 }, { 0, -1 }, { -1, 0 }, { 0, 1 }, { 2, 0 }, { 1, -1 },
            { 0, -2 }, { -1, -1 }, { -2, 0 }, { -1, 1 }, { 0, 2 }, { 1, 1 }, { 3, 0 }, { 2, -1 }, { 1, -2 }, { 0, -3 },
            { -1, -2 }, { -2, -1 }, { -3, 0 }, { -2, 1 }, { -1, 2 }, { 0, 3 }, { 1, 2 }, { 2, 1 }, { 4, 0 }, { 3, -1 },
            { 2, -2 }, { 1, -3 }, { 0, -4 }, { -1, -3 }, { -2, -2 }, { -3, -1 }, { -4, 0 }, { -3, 1 }, { -2, 2 },
            { -1, 3 }, { 0, 4 }, { 1, 3 }, { 2, 2 }, { 3, 1 }, { 4, -1 }, { 3, -2 }, { 2, -3 }, { 1, -4 }, { -1, -4 }};

    public static final int LEAD_LOWER_THRESHOLD_FOR_SENSING =7;
    public static final int LEAD_UPPER_THRESHOLD_FOR_SENSING =24;

    //set to best case for miners (0 rubble means miners can mine 5 times)
    public static final double MINES_PER_ROUND =4.53;
    public static final int CLOSE_RADIUS =7;

    public static final int ROUNDS_PER_ACTION =2;

    public static final int UNITS_AVAILABLE=7;

    // meaning 1/3 of messages will be removed from comms
    public static final int INVERSE_FRACTION_OF_MESSAGES_TO_LEAVE = 3;

    // comms cleanup
    public static final int EXTRA_BYTECODE_FOR_COMMS_CLEANUP =7200;

    public static final int DENSE_COMMS_UPDATE_LIMIT =2;

    // only say archon is dead if you are very close to center of sector
    public static final int ARCHON_DEATH_CONFIRMATION =3;

    public static final int RUN_ROUNDS_BEFORE_CHARGE =7;

    // greater than half equals closer to enemies.
    public static final double BUILDER_WATCHTOWER_FRACTION =0.3;

    public static final int ROBOTS_UPPER_THRESHOLD_FOR_SENSING =14;

    public static final int BUILDER_INCH_FORWARD =11;

	public static final int ARCHON_LOW_RUBBLE = 10;

	public static final int ARCHON_CLOSE_RADIUS =84;

    public static final int LEAD_MOVE_THRESHOLD=67;

    public static final int LEAD_WORTH_PURSUING=13;

    // 1- 10 (for genetic algo to adjust)
    public static final int CHOOSE_SECTOR_GA=4;

    public static final int BUILDER_LEAD_THRESH=500;


}