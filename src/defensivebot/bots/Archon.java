package defensivebot.bots;

import java.util.Random;

import battlecode.common.*;
import defensivebot.utils.*;

import static defensivebot.utils.Constants.UNITS_AVAILABLE;

public class Archon extends Robot{

    static final Random rng = new Random(6147);

    static int[] unitCounts = new int[UNITS_AVAILABLE];

    public Archon(RobotController rc) throws GameActionException  {
        super(rc);
    }

    @Override
    public void sense() throws GameActionException {
        //localInfo.senseRobots();
        //localInfo.senseTerrain();
    }

    @Override
    public void executeRole() throws GameActionException {

        Direction dir = Constants.directions[rng.nextInt(Constants.directions.length)];
        RobotType toBuild = RobotType.SAGE;
        switch (0/*turnCount%2*/){
            case 1:
                toBuild = RobotType.SOLDIER;
                break;
            case 0:
                toBuild = RobotType.MINER;
                break;
        }

        if(unitCounts[RobotType.MINER.ordinal()] > 50)toBuild = RobotType.SOLDIER;
//        if(unitCounts[RobotType.SOLDIER.ordinal()] > 30)toBuild = RobotType.SAGE;

        rc.setIndicatorString("Trying to build a: "+toBuild);
        if (rc.canBuildRobot(toBuild, dir)) {
            rc.buildRobot(toBuild, dir);
            unitCounts[toBuild.ordinal()]++;
        }
    }

    @Override
    public void move() throws GameActionException {

    }


}
