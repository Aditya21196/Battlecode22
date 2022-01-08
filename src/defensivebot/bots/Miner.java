package defensivebot.bots;


import battlecode.common.*;
import static defensivebot.utils.Constants.directions;

public class Miner extends Robot{
    
	private int headingIndex = -1; // index in Constants.directions for heading
	private MapLocation poi = null;
	
	public Miner(RobotController rc) throws GameActionException  {
        super(rc);
    }
    
    public void sense() throws GameActionException{}
    public void move() throws GameActionException {}

	/*
	 * current strat mine lead
	 */
    @Override
    public void executeRole() throws GameActionException {
    	//sense robots
    	localInfo.senseRobots();
    	
    	//enemies that deal damage nearby?
    	if(localInfo.nearestER[RobotType.WATCHTOWER.ordinal()] != null) {
    		poi = localInfo.nearestER[RobotType.WATCHTOWER.ordinal()].location;
    		enemyDamagerNearby();
    		rc.setIndicatorLine(rc.getLocation(), poi, 255, 0, 0);
    		rc.setIndicatorString("avoiding enemy watchtower");
    		return;
    	}else if(localInfo.nearestER[RobotType.SOLDIER.ordinal()] != null) {
    		poi = localInfo.nearestER[RobotType.SOLDIER.ordinal()].location;
    		enemyDamagerNearby();
    		rc.setIndicatorLine(rc.getLocation(), poi, 255, 0, 0);
    		rc.setIndicatorString("avoiding enemy soldier");
    		return;
    	}else if(localInfo.nearestER[RobotType.SAGE.ordinal()] != null) {
    		poi = localInfo.nearestER[RobotType.SAGE.ordinal()].location;
    		enemyDamagerNearby();
    		rc.setIndicatorLine(rc.getLocation(), poi, 255, 0, 0);
    		rc.setIndicatorString("avoiding enemy sage");
    		return;
    	}
    	
    	//no enemy damager nearby
    	localInfo.senseGold();
    	
    	//found gold?
    	if(localInfo.nearestGoldLoc != null) {
    		localInfo.senseRubble(localInfo.nearestGoldLoc);
    		//move toward low rubble near gold
    		moveToward(localInfo.lowestRubbleLoc);
    		//mine gold
    		mineGold();
    		rc.setIndicatorLine(rc.getLocation(), localInfo.lowestRubbleLoc, 0, 255, 0);
    		rc.setIndicatorString("found gold, best mining loc identified.");
    		return;
    	}
    	
    	//no gold
    	//enemy miner or archon nearby
    	if(localInfo.nearestER[RobotType.MINER.ordinal()] != null || localInfo.nearestER[RobotType.ARCHON.ordinal()] != null) {
    		localInfo.senseLead();
    		//found lead?
    		if(localInfo.nearestLeadLoc != null) {
    			localInfo.senseRubble(localInfo.nearestLeadLoc);
    			moveToward(localInfo.lowestRubbleLoc);
    			mineLead();
    			rc.setIndicatorLine(rc.getLocation(), localInfo.lowestRubbleLoc, 0, 255, 0);
        		rc.setIndicatorString("enemy miner/archon near. Mine all lead found best mining loc.");
    			return;
    		}
    		
    		//TODO: scan comms for a target location to go toward
    		
    		//heading
    		moveHeading();
    		rc.setIndicatorString("heading. enemy miner/archon near .. whatever");
    		return;
    	}
    	
    	//no enemy miner or archon nearby
    	localInfo.senseLeadForPassive();
    	//found Lead for passive mining?
    	if(localInfo.nearestLeadLoc != null) {
    		localInfo.senseRubble(localInfo.nearestLeadLoc);
			moveToward(localInfo.lowestRubbleLoc);
			mineLead();
			rc.setIndicatorLine(rc.getLocation(), localInfo.lowestRubbleLoc, 0, 255, 0);
    		rc.setIndicatorString("mining passively cuz noone is around. found the best loc.");
			return;
    	}
    	
    	//no lead for passive mining
    	//TODO: scan comms for a target location to go toward
		
		//heading
		moveHeading();
		rc.setIndicatorString("heading. no one is around");
		return;
    }
    
    private void enemyDamagerNearby() throws GameActionException {
		localInfo.senseGold();
		//found gold?
		if(localInfo.nearestGoldLoc != null) {
    		mineGold();
    		moveAway(poi); //move away from enemy that deals damage
    		return;
    	}
		//no gold
		localInfo.senseLead();
		//found lead?
		if(localInfo.nearestLeadLoc != null) {
			mineLead();
			moveAway(poi); //move away from enemy that deals damage
    		return;
		}
		//no lead
		moveAway(poi); //move away from enemy that deals damage
		return;
	}
    
    private void tryMove(Direction dir) throws GameActionException {
    	if(dir!=null && rc.canMove(dir)) {
			rc.move(dir);
		}else {
			headingIndex = -1;
		}
    }
    
    private void moveHeading() throws GameActionException {
		if(!rc.isMovementReady()) return;
    	if(headingIndex == -1) {
			headingIndex = (int)(Math.random()*directions.length);
		}
    	tryMove(getBestValidDirection(directions[headingIndex]));
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
    
	
    private void mineLead() throws GameActionException {
    	while(rc.canMineLead(localInfo.nearestLeadLoc)) {
    		rc.mineLead(localInfo.nearestLeadLoc);
    	}
    }
    
    private void mineGold() throws GameActionException {
    	while(rc.canMineGold(localInfo.nearestGoldLoc)) {
    		rc.mineGold(localInfo.nearestGoldLoc);
    	}
	}
}
