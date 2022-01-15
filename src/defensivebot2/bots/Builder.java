package defensivebot2.bots;

import battlecode.common.AnomalyScheduleEntry;
import battlecode.common.AnomalyType;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
//import defensivebot2.enums.DroidSubType;
import defensivebot2.utils.Constants;

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
    	localInfo.senseRobots(false, false);
    	
    	//movement priority 0: repel friends before charge anomaly (maybe if enemy sage is in range later)
    	AnomalyScheduleEntry  next = getNextAnomaly();
    	if(next.anomalyType == AnomalyType.CHARGE && next.roundNumber - rc.getRoundNum() < Constants.RUN_ROUNDS_BEFORE_CHARGE) {
    		tryMoveRepelFriends();
    	}
    	
    	//movement priority 1: to repair damaged buildings
    	tryMoveAndRepair();
    	tryRepair();
    	
    	//movement priority 2: run from danger in area (in this case we should mine first if able)
    	if(localInfo.getEnemyDamagerCount() > localInfo.getFriendlyDamagerCount()) {
    		
    	}
    	
    	
	}
    
    private void tryRepair() throws GameActionException {
    	if(!rc.isActionReady()) return;
		
		MapLocation target = null;
		if(localInfo.nearestDamagedFR[RobotType.WATCHTOWER.ordinal()] != null) {
			target = localInfo.nearestDamagedFR[RobotType.WATCHTOWER.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), RobotType.BUILDER.actionRadiusSquared)) {
				tryRepair(target);
				return;
			}
		}
		if(localInfo.nearestDamagedFR[RobotType.ARCHON.ordinal()] != null) {
			target = localInfo.nearestDamagedFR[RobotType.ARCHON.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), RobotType.BUILDER.actionRadiusSquared)) {
				tryRepair(target);
				return;
			}
		}
		if(localInfo.nearestDamagedFR[RobotType.LABORATORY.ordinal()] != null) {
			target = localInfo.nearestDamagedFR[RobotType.LABORATORY.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), RobotType.BUILDER.actionRadiusSquared)) {
				tryRepair(target);
				return;
			}
		}
	}



	//try to move toward damaged building, then try to repair
    private void tryMoveAndRepair() throws GameActionException {
    	if(!rc.isMovementReady()) return;
		
		MapLocation target = null;
		if(localInfo.nearestDamagedFR[RobotType.WATCHTOWER.ordinal()] != null) {
			target = localInfo.nearestDamagedFR[RobotType.WATCHTOWER.ordinal()].getLocation();
			//ok to surround watchtowers with builders
			moveToward(target); rc.setIndicatorString("best repair loc: "+target);
			if(!rc.isActionReady())
				return;
			tryRepair(target);
			return;
		}
		if(localInfo.nearestDamagedFR[RobotType.ARCHON.ordinal()] != null) {
			target = localInfo.nearestDamagedFR[RobotType.ARCHON.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), 1)) {
				moveAway(target); rc.setIndicatorString("repair loc (trying to prevent crowding archon): "+target);
				if(!rc.isActionReady())
					return;
				tryRepair(target);
				return;
			}
			moveToward(target); rc.setIndicatorString("best repair loc: "+target);
			if(!rc.isActionReady())
				return;
			tryRepair(target);
			return;
		}
		if(localInfo.nearestDamagedFR[RobotType.LABORATORY.ordinal()] != null) {
			target = localInfo.nearestDamagedFR[RobotType.WATCHTOWER.ordinal()].getLocation();
			//ok to surround laboratory with builders if they see it damaged
			moveToward(target); rc.setIndicatorString("best repair loc: "+target);
			if(!rc.isActionReady())
				return;
			tryRepair(target);
			return;
		}
	}



	private void tryMoveRepelFriends() throws GameActionException {
		if(!rc.isMovementReady() || localInfo.nearestFriend == null) return;
		
		moveAway(localInfo.nearestFriend.location);rc.setIndicatorString("full friend repel, from: "+localInfo.nearestFriend.location);
		
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
    
    
}
