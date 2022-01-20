package gabot4.bots;

import battlecode.common.AnomalyScheduleEntry;
import battlecode.common.AnomalyType;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import gabot4.models.SparseSignal;
//import gabot4.enums.DroidSubType;
import gabot4.utils.Constants;

import static gabot4.bots.Archon.rng;
import static gabot4.utils.Constants.directions;
import static gabot4.utils.LogUtils.printDebugLog;
import static gabot4.utils.PathFindingConstants.SOLDIER_PATHFINDING_LIMIT;

import java.util.Random;

public class Builder extends Robot{

	private MapLocation taskLocLab = null;
	private MapLocation taskLocWT = null;
	
	private boolean isMapExplored = false;
	private boolean tryLeadFromComms = true;
	private MapLocation nearestCornerToSpawn;
	
	
	private int goldLastRound = 0;
	private int lead;
	private int gold;
	private int roundsWithoutProducingGold = 0;
	
	//public final Random rng = new Random(6147);

    public Builder(RobotController rc) throws GameActionException  {
        super(rc);
//        nearestCornerToSpawn = getNearestCorner();
    }

    

    @Override
    public void executeRole() throws GameActionException {
    	localInfo.senseRobots(false, true, false);
    	lead = rc.getTeamLeadAmount(rc.getTeam());
    	gold = rc.getTeamGoldAmount(rc.getTeam());
    	//movement priority 0: repel friends before charge anomaly (maybe if enemy sage is in range later)
    	AnomalyScheduleEntry  next = getNextAnomaly();
    	if(next != null && next.anomalyType == AnomalyType.CHARGE && next.roundNumber - rc.getRoundNum() < Constants.RUN_ROUNDS_BEFORE_CHARGE) {
    		tryMoveRepelFriends();
    	}
    	
    	//movement priority 1: repair damaged buildings
    	tryMoveAndRepair();
    	tryRepair();
    	
    	//movement priority 2: run from danger in area
    	if(localInfo.getEnemyDamagerCount() > localInfo.getFriendlyDamagerCount()) {
    		tryMoveInDanger();
    	}
    	
    	//movement priority 3: move for mutation. currently not trying to mutate laboratories
    	tryMoveForMutate();
    	tryMutate();
    	
    	//movement priority 4: move toward task location. build lab or watchtower
    	tryMoveAndBuildOnTask();
    	tryMoveNewTask();
    	
    	
    	//movement priority 5: 
    	tryMoveTowardBuildings();
    	
    	
    	
    	
    	
    	if(gold>goldLastRound || goldLastRound - gold > 19 || lead < 10) {
    		roundsWithoutProducingGold = 0;
    	}else {
    		roundsWithoutProducingGold++;
    	}
    	goldLastRound = gold;
	}
    
    private void tryMoveTowardBuildings() throws GameActionException {
    	if(!rc.isMovementReady()) return;
    	
    	if(localInfo.nearestFR[RobotType.WATCHTOWER.ordinal()] != null) {
			//ok to surround watchtowers with builders
			pathfinding.moveTowards(localInfo.nearestFR[RobotType.WATCHTOWER.ordinal()].location, false); rc.setIndicatorString("toward building WT");
			return;
		}
		if(localInfo.nearestFR[RobotType.ARCHON.ordinal()] != null) {
			if(!localInfo.nearestFR[RobotType.ARCHON.ordinal()].location.isWithinDistanceSquared(rc.getLocation(), 13)) {
				pathfinding.moveTowards(localInfo.nearestFR[RobotType.ARCHON.ordinal()].location, false); rc.setIndicatorString("toward building Archon");
				return;
			}else {
				pathfinding.moveAwayFrom(localInfo.nearestFR[RobotType.ARCHON.ordinal()].location, 0);
			}
		}
		
	}



	private void tryMoveAndBuildOnTask() throws GameActionException {
		if(!rc.isMovementReady()) return;
		//task build lab
		if(taskLocLab != null) {
			//arrived at task
			if(rc.getLocation().isWithinDistanceSquared(taskLocLab, Constants.CLOSE_RADIUS)) {
				//build Lab
				MapLocation best = getBuildLoc();
				if(best != null) {
					tryBuild(RobotType.LABORATORY, rc.getLocation().directionTo(best));
				}
				taskLocLab = null;
				return;
			}
			pathfinding.moveTowards(taskLocLab,false);rc.setIndicatorString("best spot to build a lab: "+taskLocLab);
			if(rc.isMovementReady()) {
				taskLocLab = null;
			}
		}
		//task build WT
		if(taskLocWT != null) {
			//arrived at task
			if(rc.getLocation().isWithinDistanceSquared(taskLocWT, Constants.CLOSE_RADIUS)) {
				//build WT
				MapLocation best = getBuildLoc();
				if(best != null) {
					tryBuild(RobotType.WATCHTOWER, rc.getLocation().directionTo(best));
				}
				taskLocWT = null;
				return;
			}
			pathfinding.moveTowards(taskLocWT,false);rc.setIndicatorString("best spot to build a WT: "+taskLocWT);
			if(rc.isMovementReady()) {
				taskLocWT = null;
			}
		}
		
	}
    
    private void tryMoveNewTask() throws GameActionException {
    	if(!rc.isMovementReady()) return;
    	
    	if(lead < RobotType.LABORATORY.buildCostLead)
			return;
    	
    	//System.out.println("loking for job");
    	
    	SparseSignal signal = comms.getClosestArchon();
    	//check if this builder thinks the team currently has a lab
    	if(roundsWithoutProducingGold > 20) {
    		//fixedBits == 0 means friendly archon not in threat (could be dead)
  			if(signal != null && signal.fixedBitsVal == 0){
				taskLocLab = getNearestCorner(signal.target);
				return;
			}
    		
    	}
    	
    	if(lead < RobotType.WATCHTOWER.buildCostLead)
			return;
    	
    	//fixedBits == 0b00 || == 0b10 means friendly archon
		if(signal != null && (signal.fixedBitsVal == 0 || signal.fixedBitsVal == 2)){
			MapLocation enemyLoc = comms.getNearestEnemyLoc();
			//System.out.println(enemyLoc);
			if(enemyLoc != null) {
				taskLocWT = new MapLocation(signal.target.x + (int)((enemyLoc.x-signal.target.x)*Constants.BUILDER_WATCHTOWER_FRACTION), 
										signal.target.y + (int)((enemyLoc.y-signal.target.y)*Constants.BUILDER_WATCHTOWER_FRACTION));
				//System.out.println("I should build a watch tower at: "+taskLocWT);
			}
		}
		
	}



	private void tryMoveForMutate() throws GameActionException {
    	if(!rc.isMovementReady()) return;
    	//we probably don't want to mutate when enemies are around because of 10 round cooldown
    	if(localInfo.getEnemyDamagerCount() > 0)
    		return;
    	
    	int lead = rc.getTeamLeadAmount(rc.getTeam());
		int gold = rc.getTeamGoldAmount(rc.getTeam());
		
		//current strat: mutate archons if able. Mutate watchtowers only if lead/gold is super high
		if(localInfo.nearestFR[RobotType.ARCHON.ordinal()] != null && 
				((lead > RobotType.ARCHON.getLeadMutateCost(2)+RobotType.ARCHON.getGoldMutateCost(2) && localInfo.nearestFR[RobotType.ARCHON.ordinal()].getLevel() == 1) ||
				(gold > RobotType.ARCHON.getLeadMutateCost(3)+RobotType.ARCHON.getGoldMutateCost(3) && localInfo.nearestFR[RobotType.ARCHON.ordinal()].getLevel() == 2)) ) {
			moveToward(localInfo.nearestFR[RobotType.ARCHON.ordinal()].location);rc.setIndicatorString("toward for mutation");
			//System.out.println(RobotType.ARCHON.getLeadMutateCost(3)+RobotType.ARCHON.getGoldMutateCost(3));
			return;
			
		}
		
		if(localInfo.nearestFR[RobotType.WATCHTOWER.ordinal()] != null && 
				((lead*2.5 > RobotType.WATCHTOWER.getLeadMutateCost(2)+RobotType.WATCHTOWER.getGoldMutateCost(2) && localInfo.nearestFR[RobotType.WATCHTOWER.ordinal()].getLevel() == 1) ||
				(gold*2.5 > RobotType.WATCHTOWER.getLeadMutateCost(3)+RobotType.WATCHTOWER.getGoldMutateCost(3) && localInfo.nearestFR[RobotType.WATCHTOWER.ordinal()].getLevel() == 2)) ) {
			moveToward(localInfo.nearestFR[RobotType.WATCHTOWER.ordinal()].location);rc.setIndicatorString("toward for mutation");
			return;
		}
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
				((lead > RobotType.ARCHON.getLeadMutateCost(2)+RobotType.ARCHON.getGoldMutateCost(2) && localInfo.nearestFR[RobotType.ARCHON.ordinal()].getLevel() == 1) ||
				(gold > RobotType.ARCHON.getLeadMutateCost(3)+RobotType.ARCHON.getGoldMutateCost(3) && localInfo.nearestFR[RobotType.ARCHON.ordinal()].getLevel() == 2)) &&
				localInfo.nearestFR[RobotType.ARCHON.ordinal()].location.isWithinDistanceSquared(rc.getLocation(), 2)) {
			tryMutate(localInfo.nearestFR[RobotType.ARCHON.ordinal()].location);
			return;
			
		}
		
		if(localInfo.nearestFR[RobotType.WATCHTOWER.ordinal()] != null && 
				((lead*2.5 > RobotType.WATCHTOWER.getLeadMutateCost(2)+RobotType.WATCHTOWER.getGoldMutateCost(2) && localInfo.nearestFR[RobotType.WATCHTOWER.ordinal()].getLevel() == 1) ||
				(gold*2.5 > RobotType.WATCHTOWER.getLeadMutateCost(3)+RobotType.WATCHTOWER.getGoldMutateCost(3) && localInfo.nearestFR[RobotType.WATCHTOWER.ordinal()].getLevel() == 2)) &&
				localInfo.nearestFR[RobotType.WATCHTOWER.ordinal()].location.isWithinDistanceSquared(rc.getLocation(), 2)) {
			tryMutate(localInfo.nearestFR[RobotType.WATCHTOWER.ordinal()].location);
			return;
		}
    }
    
    private MapLocation getBuildLoc() throws GameActionException {
    	//get the lowest rubble surrounding mapLocation that is not occupied
		int lowestRubble = Integer.MAX_VALUE;
    	MapLocation bestLoc = null;
    	int tempRubble;
    	MapLocation tempLoc = rc.getLocation().translate(0,1);//north
    	if(rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.canSenseRobotAtLocation(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(1,1);//north east
    	if(rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.canSenseRobotAtLocation(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(1,0);//east
    	if(rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.canSenseRobotAtLocation(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(1,-1);//south east
    	if(rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.canSenseRobotAtLocation(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(0,-1);//south
    	if(rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.canSenseRobotAtLocation(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(-1,-1);//south west
    	if(rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.canSenseRobotAtLocation(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(-1,0);//west
    	if(rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.canSenseRobotAtLocation(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(-1,1);//north west
    	if(rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.canSenseRobotAtLocation(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	
		return bestLoc;
		
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
			pathfinding.moveTowards(target, false); rc.setIndicatorString("best repair loc: "+target);
			if(!rc.isActionReady())
				return;
			tryRepair(target);
			return;
		}
		if(localInfo.nearestDamagedFR[RobotType.ARCHON.ordinal()] != null) {
			target = localInfo.nearestDamagedFR[RobotType.ARCHON.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), 1)) {
				pathfinding.moveAwayFrom(target, 0); rc.setIndicatorString("repair loc (trying to prevent crowding archon): "+target);
				if(!rc.isActionReady())
					return;
				tryRepair(target);
				return;
			}
			pathfinding.moveTowards(target, false); rc.setIndicatorString("best repair loc: "+target);
			if(!rc.isActionReady())
				return;
			tryRepair(target);
			return;
		}
		if(localInfo.nearestDamagedFR[RobotType.LABORATORY.ordinal()] != null) {
			target = localInfo.nearestDamagedFR[RobotType.LABORATORY.ordinal()].getLocation();
			//ok to surround laboratory with builders if they see it damaged
			pathfinding.moveTowards(target, false); rc.setIndicatorString("best repair loc: "+target);
			if(!rc.isActionReady())
				return;
			tryRepair(target);
			return;
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

    
    private void tryRepair(MapLocation location) throws GameActionException {
		if(rc.canRepair(location))
			rc.repair(location);
	}
    

	private void tryMoveRepelFriends() throws GameActionException {
		if(!rc.isMovementReady() || localInfo.nearestFriend == null) return;
		
		moveAway(localInfo.nearestFriend.location);rc.setIndicatorString("full friend repel, from: "+localInfo.nearestFriend.location);
		
	}
    
    
//    private void moveAwayInRadius(MapLocation toAvoid, int radius) throws GameActionException {
//    	if(rc.isMovementReady()) {
//			Direction dir = getBestValidDirection(toAvoid.directionTo(rc.getLocation()));
//			if(dir != null && rc.getLocation().add(dir).isWithinDistanceSquared(toAvoid, radius)) {
//				tryMove(dir);
//			}
//    	}
//	}
//
//    private void tryDeath() throws GameActionException {
//    	int leadHere = rc.senseLead(rc.getLocation());
//		if(leadHere == 0) {
//			rc.disintegrate();
//		}
//    }

	
    
}
