package offensivebot.bots;

import static offensivebot.bots.Archon.rng;
import static offensivebot.utils.Constants.directions;

import battlecode.common.*;
import offensivebot.datasturctures.CustomSet;

import static offensivebot.utils.LogUtils.printDebugLog;

public class Soldier extends Robot{
	
	private int headingIndex = -1; // index in Constants.directions for heading
	private MapLocation poi = null;
	
	//TODO:convert taskType to enum or try to sync with what is used in comms
	private int taskType = -1;
	private MapLocation taskLocation = null;
	private MapLocation headingTarget = null;
	private boolean randomMovementAllowed = false;
	private CustomSet<MapLocation> discoveredArchons = new CustomSet<>(5);
	
	
    public Soldier(RobotController rc) throws GameActionException  {
        super(rc);
    }

    @Override

    public void sense() throws GameActionException {
		localInfo.senseLead(false);
	}
    @Override
    public void move() throws GameActionException {}
    

    @Override
    public void act() throws GameActionException {
        //sense robots, track lowest hp by type as well
    	localInfo.senseRobots(true);
    	
    	//enemies that deal damage nearby?
		poi = localInfo.findNearestDamager();
		if(poi != null){
			enemyDamagerNearby();
			rc.setIndicatorLine(rc.getLocation(), poi, 255, 0, 0);
			rc.setIndicatorString("avoiding enemy");
		}
    	
    	//no enemy damagers nearby
    	//enemy non damager nearby?
		poi = localInfo.findNearestNondamager();
		if(poi != null){
			enemyNonDamagerNearby();
			rc.setIndicatorLine(rc.getLocation(), poi, 255, 0, 0);
			rc.setIndicatorString("enemy non damager");
		}


    	//no enemy non damager nearby
    	
    	//has task and taskLocation?
    	//currently does nothing ...
    	if(taskType == 0) {
    		//perform movement for task
    		trySenseResources();
    		return;
    	}
    	
    	//no task
    	
    	//TODO: add decision to look in comms for tasks (ie explore here, go to here, surround here)
		MapLocation loc = commsBestLocforSoldier();
		if(loc != null){
			// There can be a lot of back and forth because of this if not done right
			moveTo(loc);
			return;
		}
    	
    	//friendly soldier with higher id?
    	/*if(localInfo.highestIDFR[RobotType.SOLDIER.ordinal()] != null && localInfo.highestIDFR[RobotType.SOLDIER.ordinal()].getID() > rc.getID()) {
    		//follow him
    		moveTo(localInfo.highestIDFR[RobotType.SOLDIER.ordinal()].getLocation());
    		trySenseResources();
    		rc.setIndicatorString("following FS");
    		return;
    	}*/
    	
    	//no friendly soldier with higher id
    	//heading
		// TODO: We are wasting soldier potential here. We should do something
		moveHeading();
		rc.setIndicatorString("heading. no one is around");
    }

	private MapLocation commsBestLocforSoldier() throws GameActionException {
		MapLocation bestLoc = comms.getNearbyUnexplored();
		if(bestLoc != null)rc.setIndicatorString("unexplored area: "+bestLoc);
		else{
			bestLoc = comms.getClosestEnemyArchon(discoveredArchons);
			if(bestLoc!=null && currentLocation.distanceSquaredTo(bestLoc)<20){
				discoveredArchons.add(bestLoc);
			}
		}
		return bestLoc;
	}

	private void trySenseResources() throws GameActionException {
		if(Clock.getBytecodesLeft() > 1000) {
			localInfo.senseLead(false);
		}
		if(Clock.getBytecodesLeft() > 1000) {
			localInfo.senseGold();
		}
	}

	


	private void enemyDamagerNearby() throws GameActionException {
		int fdCount = localInfo.friendlyUnitCounts[RobotType.WATCHTOWER.ordinal()]+
				localInfo.friendlyUnitCounts[RobotType.SOLDIER.ordinal()]+
				localInfo.friendlyUnitCounts[RobotType.SAGE.ordinal()];
		int edCount = localInfo.enemyUnitCounts[RobotType.WATCHTOWER.ordinal()]+
				localInfo.enemyUnitCounts[RobotType.SOLDIER.ordinal()]+
				localInfo.enemyUnitCounts[RobotType.SAGE.ordinal()];
		//friendly damager count >= enemy damager count
		if(fdCount >= edCount) {
			//compute target
			
			//movement ready?
			if(rc.isMovementReady()) {
				MapLocation target = getBestTarget();
				if(currentLocation.distanceSquaredTo(target)>RobotType.SOLDIER.actionRadiusSquared)
					target = poi;
				localInfo.senseRubbleForAttack(target);
				int curRubble = rc.senseRubble(currentLocation);
				if(localInfo.lowestRubbleLoc != null && curRubble - localInfo.lowestRubble>3) {
					moveTo(localInfo.lowestRubbleLoc);
				}
				tryAttack(target);
				return;
			}
			
			//movement not ready
			
			//action ready?
			if(rc.isActionReady()) {
				MapLocation target = getBestTargetInRange();
				if(target != null) {
					tryAttack(target);
				}
				return;
			}
			//action not ready
			trySenseResources();
			return;
		}
		
		//enemy damager count is greater
		
		//can attack?
		if(rc.isActionReady()) {
			//calc best target in range
			MapLocation target = getBestTargetInRange();
			if(target != null) {
				tryAttack(target);
			}
			//move away from damager
			moveAway(poi);
			return;
		}
		//no one to attack, but still try to run
		moveAway(poi);
		return;
	}
	
	

	private void enemyNonDamagerNearby() throws GameActionException {
		//movement ready?
		if(rc.isMovementReady()) {
			MapLocation target = getBestTargetNonDamager();
			localInfo.senseRubbleForAttack(target);
			if(localInfo.lowestRubbleLoc != null) {
				moveTo(localInfo.lowestRubbleLoc);
				tryAttack(target);
				return;
			}
			
			//location doesn't exist to attack best target.
			//has action?
			if(rc.isActionReady()) {
				target = getBestTargetNonDamagerInRange();
				if(target != null) {
					tryAttack(target);
				}
				return;
			}
			return;
		}
		
		//movement not ready
		
		//has action?
		if(rc.isActionReady()) {
			MapLocation target = getBestTargetNonDamagerInRange();
			if(target != null) {
				tryAttack(target);
			}
			return;
		}
		
		//action not ready
		trySenseResources();
		return;
		
	}

	private MapLocation getBestTargetNonDamager() {
		if(localInfo.weakestER[RobotType.ARCHON.ordinal()] != null) {
    		return localInfo.weakestER[RobotType.ARCHON.ordinal()].location;
    	}else if(localInfo.weakestER[RobotType.MINER.ordinal()] != null) {
    		return localInfo.weakestER[RobotType.MINER.ordinal()].location;
    	}else if(localInfo.weakestER[RobotType.BUILDER.ordinal()] != null) {
    		return localInfo.weakestER[RobotType.BUILDER.ordinal()].location;
    	}else if(localInfo.weakestER[RobotType.LABORATORY.ordinal()] != null) {
    		return localInfo.weakestER[RobotType.LABORATORY.ordinal()].location;
    	}
		return null;
	}
	
	private MapLocation getBestTargetNonDamagerInRange() {
		if(localInfo.nearestER[RobotType.ARCHON.ordinal()] != null && 
				localInfo.nearestER[RobotType.ARCHON.ordinal()].getLocation().isWithinDistanceSquared(rc.getLocation(), rc.getType().actionRadiusSquared)) {
    		return localInfo.nearestER[RobotType.ARCHON.ordinal()].location;
    	}else if(localInfo.nearestER[RobotType.MINER.ordinal()] != null && 
				localInfo.nearestER[RobotType.MINER.ordinal()].getLocation().isWithinDistanceSquared(rc.getLocation(), rc.getType().actionRadiusSquared)) {
    		return localInfo.nearestER[RobotType.MINER.ordinal()].location;
    	}else if(localInfo.nearestER[RobotType.BUILDER.ordinal()] != null && 
				localInfo.nearestER[RobotType.BUILDER.ordinal()].getLocation().isWithinDistanceSquared(rc.getLocation(), rc.getType().actionRadiusSquared)) {
    		return localInfo.nearestER[RobotType.BUILDER.ordinal()].location;
    	}else if(localInfo.nearestER[RobotType.LABORATORY.ordinal()] != null && 
				localInfo.nearestER[RobotType.LABORATORY.ordinal()].getLocation().isWithinDistanceSquared(rc.getLocation(), rc.getType().actionRadiusSquared)) {
    		return localInfo.nearestER[RobotType.LABORATORY.ordinal()].location;
    	}
		return null;
	}

	private void tryAttack(MapLocation target) throws GameActionException {
		if(rc.canAttack(target)) {
			rc.attack(target);
		}
		
	}
	
	private void moveAway(MapLocation toAvoid) throws GameActionException {
    	if(rc.isMovementReady()) {
    		tryMove(getBestValidDirection(toAvoid.directionTo(rc.getLocation())));
    	}
	}
	private void moveTo(MapLocation target) throws GameActionException {
    	if(rc.isMovementReady() && !rc.getLocation().equals(target)) {
    		tryMove(getBestValidDirection(target));
		}
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

		if(headingTarget!=null){
			if(currentLocation.distanceSquaredTo(headingTarget)>10){
				headingTarget = new MapLocation(rng.nextInt(width),rng.nextInt(height));
			}

			if(headingIndex == -1) {
				headingIndex = rng.nextInt(directions.length);
			}

			tryMove(getBestValidDirection(directions[headingIndex]));
		}else headingTarget = new MapLocation(rng.nextInt(width),rng.nextInt(height));

	}

	// TODO: this should be only for robots in attack radius, not vision radius
	private MapLocation getBestTarget() {
		double highestDPH = Double.MIN_VALUE;
		int highestDPHIndex = 0;
		for(int i = localInfo.weakestER.length; --i>=0;) {
			if(localInfo.weakestER[i] != null) {
				double dph = localInfo.weakestER[i].getType().getDamage(localInfo.weakestER[i].getLevel()) / (double)localInfo.weakestER[i].getHealth();
				if(dph > highestDPH) {
					highestDPH = dph;
					highestDPHIndex = i;
				}
			}
			
		}
		return localInfo.weakestER[highestDPHIndex].getLocation();
	}

	private MapLocation getBestTargetInRange() {
		double highestDPH = Double.MIN_VALUE;
		int highestDPHIndex = -1;
		for(int i = localInfo.weakestER.length; --i>=0;) {
			if(localInfo.weakestER[i] != null && localInfo.weakestER[i].getLocation().isWithinDistanceSquared(rc.getLocation(), rc.getType().actionRadiusSquared)) {
				double dph = localInfo.weakestER[i].getType().getDamage(localInfo.weakestER[i].getLevel()) / (double)localInfo.weakestER[i].getHealth();
				if(dph > highestDPH) {
					highestDPH = dph;
					highestDPHIndex = i;
				}
			}
		}
		if(highestDPHIndex != -1) {
			return localInfo.weakestER[highestDPHIndex].getLocation();
		}
		for(int i = localInfo.nearestER.length; --i>=0;) {
			if(localInfo.weakestER[i] != null && localInfo.nearestER[i].getLocation().isWithinDistanceSquared(rc.getLocation(), rc.getType().actionRadiusSquared)) {
				double dph = localInfo.nearestER[i].getType().getDamage(localInfo.nearestER[i].getLevel()) / (double)localInfo.nearestER[i].getHealth();
				if(dph > highestDPH) {
					highestDPH = dph;
					highestDPHIndex = i;
				}
			}
		}
		if(highestDPHIndex != -1) {
			return localInfo.nearestER[highestDPHIndex].getLocation();
		}
		return null;
	}

    


}
