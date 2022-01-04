package defensivebot.bots;

import java.util.Random;

import battlecode.common.*;
import defensivebot.utils.*;

public class Archon extends Robot{
    public Archon(RobotController rc) {
        super(rc);
    }

    
    static final Random rng = new Random(6147);
    @Override
    public void executeRole() throws GameActionException {
    	
    	//for testing miners
    	//build miners in random direction whenever possible
    	Direction dir = Constants.directions[rng.nextInt(Constants.directions.length)];
    	rc.setIndicatorString("Trying to build a miner");
        if (rc.canBuildRobot(RobotType.MINER, dir)) {
            rc.buildRobot(RobotType.MINER, dir);
        }
    }
}
