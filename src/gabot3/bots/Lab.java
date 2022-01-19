package gabot3.bots;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Lab extends Robot{
    public Lab(RobotController rc) throws GameActionException  {
        super(rc);
    }

    private int roundsSinceLastTransmute = 0;
    private int GOOD_RATE = 10;

    @Override
    public void executeRole() throws GameActionException {
    	int lead = rc.getTeamLeadAmount(rc.getTeam());
    	int rate = rc.getTransmutationRate();
    	if(rc.canTransmute() && (rate < GOOD_RATE || roundsSinceLastTransmute > 10)) {
    		rc.transmute();
    		roundsSinceLastTransmute = 0;
    	}else {
    		roundsSinceLastTransmute++;
    	}
    }
}
