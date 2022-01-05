package defensivebot.bots;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Miner extends Robot{
    public Miner(RobotController rc) throws GameActionException  {
        super(rc);
    }

    @Override
    public void sense() throws GameActionException{
        localInfo.senseTerrain();
    }

    @Override
    public void executeRole() throws GameActionException {
            // signal

    }
}
