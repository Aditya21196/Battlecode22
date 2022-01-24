package gabot4.utils;

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

    public static int writeBits(int[] updatedCommsValues,int offset,int val,int numBits){
        for(int j = 0; j<numBits;j++){
            int updateIdx = offset/16;
            int bitIdx = offset%16;
            int updateVal = (val & 1<<j) > 0? 1: 0;
            updatedCommsValues[updateIdx] = modifyBit(updatedCommsValues[updateIdx],bitIdx,updateVal);
            offset++;
        }
        return offset;
    }

    public static int modifyBit(int original, int pos, int val)
    {
        int mask = 1 << pos;
        return (original & ~mask) | ((val << pos) & mask);
    }

}
