package gabot3.utils;

import battlecode.common.MapLocation;

public class CustomMath {

    public static int ceilDivision(int dividend, int divisor){
        if(divisor == 0)throw new RuntimeException("division by zero");
        return (dividend+divisor-1)/divisor;
    }

    // TODO: replace with bytecode efficient math
    public static int manhattanDist(MapLocation a,MapLocation b){
        return Math.max(Math.abs(a.x-b.x),Math.abs(a.y-b.y));
    }

}
