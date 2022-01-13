package defensivebot2.bots;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
//import defensivebot2.enums.DroidSubType;

import static defensivebot2.bots.Archon.rng;
import static defensivebot2.utils.Constants.directions;
import static defensivebot2.utils.LogUtils.printDebugLog;

import java.util.Random;

public class Builder extends Robot{

    //DroidSubType type = null;
	public final Random rng = new Random(6147);

    public Builder(RobotController rc) throws GameActionException  {
        super(rc);
    }

    

    @Override
    public void executeRole() throws GameActionException {
    	localInfo.senseRobots(false);
    	
    	//is there a friendly archon nearby
    	if(localInfo.nearestFR[RobotType.ARCHON.ordinal()] != null) {
    		//does it need healing?
    		if(localInfo.nearestFR[RobotType.ARCHON.ordinal()].getHealth() < RobotType.ARCHON.health) {
    			//heal if in range
    			if(localInfo.nearestFR[RobotType.ARCHON.ordinal()].getLocation().isWithinDistanceSquared(rc.getLocation(), RobotType.BUILDER.actionRadiusSquared)) {
    				tryRepair(localInfo.nearestFR[RobotType.ARCHON.ordinal()].getLocation());
    				moveAwayInRadius(localInfo.nearestFR[RobotType.ARCHON.ordinal()].getLocation(), RobotType.BUILDER.actionRadiusSquared);
    				return;
    			}
    			//move toward if not in range
    			moveToward(localInfo.nearestFR[RobotType.ARCHON.ordinal()].getLocation());
    			return;
    		}
    		// friendly archon in range not damaged
    		tryDeath();
    		moveAway(localInfo.nearestFR[RobotType.ARCHON.ordinal()].getLocation());
    	}
    	 
    	if(localInfo.homeArchon != null) {
    		if(localInfo.homeArchon.getLocation().isWithinDistanceSquared(rc.getLocation(), 53)) {
    			tryDeath();
    			if(rng.nextDouble() < 0.5) {
	    			moveToward(localInfo.homeArchon.getLocation());
	    			return;
	    		}
    			moveAway(localInfo.homeArchon.getLocation());
    			return;
    		}
    		moveToward(localInfo.homeArchon.getLocation());
			return;
    		
    	}
    	//should never occur
    	rc.setIndicatorString("home archon not stored");
    	return;
    	
	}
    
    private void moveAwayInRadius(MapLocation toAvoid, int radius) throws GameActionException {
    	if(rc.isMovementReady()) {
			Direction dir = getBestValidDirection(toAvoid.directionTo(rc.getLocation()));
			if(dir != null && rc.getLocation().add(dir).isWithinDistanceSquared(toAvoid, radius)) {
				tryMove(dir);
			}
    	}
	}

    private void tryDeath() throws GameActionException {
    	int leadHere = rc.senseLead(rc.getLocation());
		if(leadHere == 0) {
			rc.disintegrate();
		}
    }

	private void tryRepair(MapLocation location) throws GameActionException {
		if(rc.canRepair(location))
			rc.repair(location);
	}
    private void tryMove(Direction dir) throws GameActionException {
    	if(dir!=null && rc.canMove(dir)) {
			rc.move(dir);
		}
    }

    private void moveToward(MapLocation target) throws GameActionException {
    	if(rc.isMovementReady() && !rc.getLocation().equals(target)) {
    		tryMove(getBestValidDirection(target));
		}
    }
    
    private void moveAway(MapLocation toAvoid) throws GameActionException {
    	if(rc.isMovementReady()) {
    		tryMove(getBestValidDirection(toAvoid.directionTo(rc.getLocation())));
    	}
	}
    
    @Override
    public void move() throws GameActionException {}
    @Override
    public void sense() throws GameActionException {

//        CustomSet<SparseSignal> sparseSignals = comms.querySparseSignals();
//        sparseSignals.initIteration();
//        SparseSignal next = sparseSignals.next();
//        while(next!=null){
//            printDebugLog("Sparse Signal Found: "+next.type);
//            if(next.type == )
//        }
    }
}
