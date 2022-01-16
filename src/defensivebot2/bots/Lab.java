package defensivebot2.bots;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Lab extends Robot{
    public Lab(RobotController rc) throws GameActionException  {
        super(rc);
    }

    

    @Override
    public void executeRole() throws GameActionException {
    	if(rc.canTransmute()) {
    		rc.transmute();
    	}
    }
}
