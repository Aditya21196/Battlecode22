package defensivebot2.bots;


import battlecode.common.*;
import defensivebot2.utils.Constants;

public class Miner extends Robot{
    
	private MapLocation taskLoc = null;
	private boolean isMapExplored = false;
	private boolean tryLeadFromComms = true;

	public Miner(RobotController rc) throws GameActionException  {
        super(rc);
    }
    
	

    @Override
    public void executeRole() throws GameActionException {
    	
    	localInfo.senseRobots(false, false, false);
    	localInfo.senseGold();
    	localInfo.senseLead(true);

    	//movement priority 0: repel friends before charge anomaly (maybe if enemy sage is in range later)
    	AnomalyScheduleEntry  next = getNextAnomaly();
    	if(next != null && next.anomalyType == AnomalyType.CHARGE && next.roundNumber - rc.getRoundNum() < Constants.RUN_ROUNDS_BEFORE_CHARGE) {
    		tryMineGold();
			tryMineLead();
			tryMoveRepelFriends();
    	}
    	
		verbose("bytecode remaining after sensing: "+ Clock.getBytecodesLeft());

    	//movement priority 1: run from danger in area (in this case we should mine first if able)
    	if(localInfo.getEnemyDamagerCount() > localInfo.getFriendlyDamagerCount()) {
    		tryMineGold();
			tryMineLead();
			tryMoveInDanger();
    	}
    	
    	//movement priority 2: prevent friendly miner clumping (in this case we should mine first if able)
    	if(localInfo.totalLead < (localInfo.friendlyUnitCounts[RobotType.MINER.ordinal()]-2)*Constants.MINES_PER_ROUND*(GameConstants.GAME_MAX_NUMBER_OF_ROUNDS - rc.getRoundNum())) {
    		tryMineGold();
			tryMineLead();
    		//if miner has a task, prioritize existing task for movement, other wise assume this is your spot to mine
    		tryMoveOnTask();
    	}
    	
    	//movement priority 3: get to local gold/lead
    	tryMoveToGold();
    	tryMineGold();
    	tryMoveToLead();
    	tryMineLead();
    	
    	//movement priority 4: continue or get a task
    	tryMoveOnTask();
    	tryMoveNewTask();
    	
    	//movement priority 5: full miner repulsion
    	//TODO: consider adding edge repulsion
    	tryMoveRepelMiners();
    	
    }
    

	private void tryMoveRepelMiners() throws GameActionException {
		if(!rc.isMovementReady() || localInfo.nearestFR[RobotType.MINER.ordinal()] == null) return;
		
		moveAway(localInfo.nearestFR[RobotType.MINER.ordinal()].location);rc.setIndicatorString("repel from: "+localInfo.nearestFR[RobotType.MINER.ordinal()].location);
		
	}
	
	private void tryMoveRepelFriends() throws GameActionException {
		if(!rc.isMovementReady() || localInfo.nearestFriend == null) return;
		
		moveAway(localInfo.nearestFriend.location);rc.setIndicatorString("full friend repel, from: "+localInfo.nearestFriend.location);
		
	}

	//strategy is to only read from comms one piece of info per turn
	private void tryMoveNewTask() throws GameActionException {
		if(!rc.isMovementReady()) return;
		
		//got lead from comms last time you check, therefore try again
		if(tryLeadFromComms) {
			taskLoc = comms.getNearestLeadLoc();
			tryLeadFromComms = taskLoc != null;
		}
		
		//did not get lead from comms last time, try to get an exploration task
		else if(!isMapExplored) {
			//reset lead found state to look for lead next time
			tryLeadFromComms = true;
			taskLoc = comms.getNearbyUnexplored();
			if(taskLoc == null) {
				isMapExplored = true; // assume map is fully explored when BFS25 yields no result
			}
		}
		
		else {
			//reset lead found state to look for lead next time
			tryLeadFromComms = true;
		}
		
		tryMoveOnTask();
	}

	private void tryMoveInDanger() throws GameActionException {
		if(!rc.isMovementReady()) return;
		
		moveAway(localInfo.findNearestDamager());rc.setIndicatorString("run in danger");
	}

	private void tryMoveOnTask() throws GameActionException {
		if(!rc.isActionReady() || !rc.isMovementReady() || taskLoc == null) return;
		//arrived at task
		if(rc.getLocation().isWithinDistanceSquared(taskLoc, Constants.CLOSE_RADIUS)) {
			taskLoc = null;
			return;
		}
		
		moveToward(taskLoc);rc.setIndicatorString("best task loc: "+taskLoc);
		if(rc.isMovementReady()) {
			taskLoc = null;
		}
	}

	private void tryMoveToGold() throws GameActionException {
		if(!rc.isMovementReady()) return;
		
		if(localInfo.nearestGoldLoc != null) {
			MapLocation best = localInfo.getBestLocInRange(localInfo.nearestGoldLoc);
			if(best == null) {
				best = localInfo.nearestGoldLoc;
			}
			moveToward(best);rc.setIndicatorString("best gold loc: "+best);
		}
		
	}
	
	private void tryMoveToLead() throws GameActionException {
		if(!rc.isMovementReady()) return;
		
		if(localInfo.nearestLeadLoc != null) {
			MapLocation best = localInfo.getBestLocInRange(localInfo.nearestLeadLoc);
			if(best == null) {
				best = localInfo.nearestLeadLoc;
			}
			moveToward(best);rc.setIndicatorString("best lead loc: "+best);
		}
		
	}
   
    
    //mine lead if nearest lead is found and in range
	private void tryMineLead() throws GameActionException {
		if(!rc.isActionReady() || localInfo.nearestLeadLoc == null || localInfo.nearestLeadDist > RobotType.MINER.actionRadiusSquared) return;
		
		while(rc.canMineLead(localInfo.nearestLeadLoc)) {
    		rc.mineLead(localInfo.nearestLeadLoc);
    	}
    }
    
    
	//mine gold
	private void tryMineGold() throws GameActionException {
		if(!rc.isActionReady() || localInfo.nearestGoldLoc == null || localInfo.nearestGoldDist > RobotType.MINER.actionRadiusSquared) return;
		
		while(rc.canMineGold(localInfo.nearestGoldLoc)) {
    		rc.mineGold(localInfo.nearestGoldLoc);
    	}
    }
}
