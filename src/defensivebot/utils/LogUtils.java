package defensivebot.utils;

import battlecode.common.RobotController;

public class LogUtils {

    // TODO: change to false before tournament submission
    private static final boolean isDebugMode=true;
    private static final boolean isVerboseMode=true;

    public static void printDebugLog(RobotController rc,int age,String msg){
        if(!isDebugMode)return;
        // can add other debug info later
        System.out.printf("[%d] msg: %s%n",age,msg);
    }

    public static void printVerboseLog(RobotController rc,int age,String msg){
        if(!isVerboseMode)return;
        // can add other debug info later
        System.out.printf("[%d] msg: %s%n",age,msg);
    }


    // TODO: Improve this
    public static void printCriticalLog(String msg){
        System.out.printf("msg: %s%n",msg);
    }

}
