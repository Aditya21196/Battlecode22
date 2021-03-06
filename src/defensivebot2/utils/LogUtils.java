package defensivebot2.utils;

import battlecode.common.RobotController;

import static defensivebot2.bots.Robot.turnCount;

public class LogUtils {

    // TODO: change to false before tournament submission
    private static final boolean isDebugMode=true;
    private static final boolean isVerboseMode=false;

    public static void printDebugLog(String msg){
        if(!isDebugMode)return;
        // can add other debug info later
        System.out.printf("[%d] msg: %s%n",turnCount,msg);
    }

    public static void printVerboseLog(String msg){
        if(!isVerboseMode)return;
        // can add other debug info later
        System.out.printf("[%d] msg: %s%n",turnCount,msg);
    }


    // TODO: Improve this
    public static void printCriticalLog(String msg){
        System.out.printf("msg: %s%n",msg);
    }

}
