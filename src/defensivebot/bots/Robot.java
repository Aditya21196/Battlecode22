package defensivebot.bots;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import static defensivebot.utils.LogUtils.printDebugLog;
import static defensivebot.utils.LogUtils.printVerboseLog;

public abstract class Robot {

    private final RobotController rc;
    public static int turnCount=0;

    public Robot(RobotController rc){
        this.rc = rc;
    }

    // factory method for robots
    public static Robot getRobot(RobotController rc){
        switch (rc.getType()){
            case SAGE:return new Sage(rc);
            case MINER:return new Miner(rc);
            case SOLDIER:return new Soldier(rc);
            case BUILDER: return new Builder(rc);
            case ARCHON:return new Archon(rc);
            case LABORATORY: return new Lab(rc);
            case WATCHTOWER: return new WatchTower(rc);
            default: throw new RuntimeException("unidentified robot type");
        }
    }

    public void runRobot() throws GameActionException{
        // common code for all robots
        turnCount++;
        executeRole();
        verbose("bytecode remaining: "+ Clock.getBytecodesLeft());
    }

    protected void debug(String msg){
        printDebugLog(rc,turnCount,msg);
    }



    protected void verbose(String msg){
        printVerboseLog(rc,turnCount,msg);
    }

    public abstract void executeRole() throws GameActionException;

}
