package defensivebot2.bots;

import battlecode.common.AnomalyScheduleEntry;
import battlecode.common.AnomalyType;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
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

	private MapLocation taskLoc = null;
	private boolean isMapExplored = false;
	private boolean tryLeadFromComms = true;
	private MapLocation nearestCornerToSpawn;
	
	private double[] labWeights = {
			0.0,//threshold
			-1.0,//corner
			-1.0,//home
			1.0,//lead
			-5.0,//gold
			-2.0,//friend count
			-5.0,//rubble
	};
	
	private double[] wtWeights = {
			1000.0,//threshold
			-1.0,//home
			0.1,//lead in reserve
			-5.0,//rubble
			10.0,//lead deposits
			1.0,//near corner
	};
	
	
	//public final Random rng = new Random(6147);

    public Builder(RobotController rc) throws GameActionException  {
        super(rc);
        nearestCornerToSpawn = getNearestCorner();
    }

    

    @Override
    public void executeRole() throws GameActionException {
    	localInfo.senseRobots(false, true);
    	
    	//movement priority 0: repel friends before charge anomaly (maybe if enemy sage is in range later)
    	AnomalyScheduleEntry  next = getNextAnomaly();
    	if(next.anomalyType == AnomalyType.CHARGE && next.roundNumber - rc.getRoundNum() < Constants.RUN_ROUNDS_BEFORE_CHARGE) {
    		tryMoveRepelFriends();
    	}
    	
    	//movement priority 1: to repair damaged buildings
    	tryMoveAndRepair();
    	tryRepair();
    	
    	//movement priority 2: run from danger in area
    	if(localInfo.getEnemyDamagerCount() > localInfo.getFriendlyDamagerCount()) {
    		tryMoveInDanger();
    	}
    	
    	tryMutate();
    	tryBuild();
    	//movement priority 3: move to location for mutating or building (lab or watchtower)
    	
    	//tryMoveAndSpendLead();
    	tryMoveRepelFriends();
    	
    	
	}
    
    private void tryMutate() throws GameActionException {
    	if(!rc.isActionReady()) return;
    	//we probably don't want to mutate when enemies are around because of 10 round cooldown
    	if(localInfo.getEnemyDamagerCount() > 0)
    		return;
    	
    	int lead = rc.getTeamLeadAmount(rc.getTeam());
		int gold = rc.getTeamGoldAmount(rc.getTeam());
		
		//current strat: mutate archons if able. Mutate watchtowers only if lead/gold is super high
		if(localInfo.nearestFR[RobotType.ARCHON.ordinal()] != null && 
				((lead > RobotType.ARCHON.getLeadMutateCost(1)+RobotType.ARCHON.getGoldMutateCost(1) && localInfo.nearestFR[RobotType.ARCHON.ordinal()].getLevel() == 1) ||
				(gold > RobotType.ARCHON.getLeadMutateCost(2)+RobotType.ARCHON.getGoldMutateCost(2) && localInfo.nearestFR[RobotType.ARCHON.ordinal()].getLevel() == 2)) &&
				localInfo.nearestFR[RobotType.ARCHON.ordinal()].location.isWithinDistanceSquared(rc.getLocation(), 2)) {
			tryMutate(localInfo.nearestFR[RobotType.ARCHON.ordinal()].location);
			return;
			
		}
		
		if(localInfo.nearestFR[RobotType.WATCHTOWER.ordinal()] != null && 
				((lead*2.5 > RobotType.WATCHTOWER.getLeadMutateCost(1)+RobotType.WATCHTOWER.getGoldMutateCost(1) && localInfo.nearestFR[RobotType.WATCHTOWER.ordinal()].getLevel() == 1) ||
				(gold*2.5 > RobotType.WATCHTOWER.getLeadMutateCost(2)+RobotType.WATCHTOWER.getGoldMutateCost(2) && localInfo.nearestFR[RobotType.WATCHTOWER.ordinal()].getLevel() == 2)) &&
				localInfo.nearestFR[RobotType.WATCHTOWER.ordinal()].location.isWithinDistanceSquared(rc.getLocation(), 2)) {
			tryMutate(localInfo.nearestFR[RobotType.ARCHON.ordinal()].location);
			return;
		}
    }
    
    private void tryBuild() throws GameActionException {
    	if(!rc.isActionReady()) return;
    	//we probably don't want to build when enemies are around because of prototype state
    	if(localInfo.getEnemyDamagerCount() > 0)
    		return;
    	
    	int lead = rc.getTeamLeadAmount(rc.getTeam());
		int gold = rc.getTeamGoldAmount(rc.getTeam());
		
		//if lead is less than cost to build cheapest building, can't build
		if(lead < RobotType.WATCHTOWER.buildCostLead)
			return;
    	
    	//get the lowest rubble surrounding mapLocation that is not occupied
		int lowestRubble = Integer.MAX_VALUE;
    	MapLocation bestLoc = null;
    	int tempRubble;
    	MapLocation tempLoc = rc.getLocation().translate(0,1);//north
    	if(rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.isLocationOccupied(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(1,1);//north east
    	if(rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.isLocationOccupied(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(1,0);//east
    	if(rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.isLocationOccupied(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(1,-1);//south east
    	if(rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.isLocationOccupied(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(0,-1);//south
    	if(rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.isLocationOccupied(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(-1,-1);//south west
    	if(rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.isLocationOccupied(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(-1,0);//west
    	if(rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.isLocationOccupied(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(-1,1);//north west
    	if(rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.isLocationOccupied(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
		
    	if(localInfo.nearestFR[RobotType.ARCHON.ordinal()] != null && bestLoc.isWithinDistanceSquared(localInfo.nearestFR[RobotType.ARCHON.ordinal()].location, 5)) {
    		
    	}
    	
		if(lead > RobotType.LABORATORY.buildCostLead) {
			double value = labWeights[1]*nearestCornerToSpawn.distanceSquaredTo(rc.getLocation()) + 
				labWeights[2]*localInfo.homeArchon.location.distanceSquaredTo(rc.getLocation()) +
				labWeights[3]*lead +
				labWeights[4]*gold + 
				labWeights[5]*(localInfo.getFriendlyDamagerCount()+localInfo.getFriendlyNonDamagerCount()) +
				labWeights[6]*lowestRubble;
			rc.setIndicatorString("Lab value: "+value);
			if(value > labWeights[0]) {
				tryBuild(RobotType.LABORATORY, rc.getLocation().directionTo(bestLoc));
				return;
			}
		}
		
		if(lead > RobotType.WATCHTOWER.buildCostLead) {
			localInfo.senseLead(false);
			double value =  
					wtWeights[1]*localInfo.homeArchon.location.distanceSquaredTo(rc.getLocation()) +
					wtWeights[2]*lead +
					wtWeights[3]*lowestRubble +
					wtWeights[4]*localInfo.totalLeadDeposits +
					wtWeights[5]*getNearestCorner().distanceSquaredTo(rc.getLocation());
			rc.setIndicatorString("WT value: "+value);
			if(value > wtWeights[0]) {
				tryBuild(RobotType.WATCHTOWER, rc.getLocation().directionTo(bestLoc));
				return;
			}
		}
		
		
	}

    private void tryBuild(RobotType type, Direction dir) throws GameActionException {
    	if(rc.canBuildRobot(type, dir)) {
    		rc.buildRobot(type, dir);
    	}
    }

	private void tryMutate(MapLocation location) throws GameActionException {
		if(rc.canMutate(location)) {
			rc.mutate(location);
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

    private void tryMoveInDanger() throws GameActionException {
		if(!rc.isMovementReady()) return;
		
		moveAway(localInfo.findNearestDamager());rc.setIndicatorString("run outnumbered");
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
