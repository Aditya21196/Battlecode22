package gabot1.bots;

import static gabot1.utils.PathFindingConstants.SOLDIER_PATHFINDING_LIMIT;

import battlecode.common.*;
import gabot1.enums.TaskType;
import gabot1.models.SparseSignal;
import gabot1.models.Task;
import gabot1.strategies.Comms2;
import gabot1.utils.Constants;

public class Sage extends Robot{
	private boolean isMapExplored = false;//TODO:reset this to false every 100 rounds or so
	private boolean tryTargetFromComms = true;
	private Task task=null;
	
	public Sage(RobotController rc) throws GameActionException  {
        super(rc);
    }
    

    @Override
    public void executeRole() throws GameActionException {
        //sense robots, track lowest hp by type as well

    	localInfo.senseRobots(true, false, true);
    	
    	//movement priority 0: repel friends before charge anomaly (maybe if enemy sage is in range later)
    	AnomalyScheduleEntry  next = getNextAnomaly();
    	if(next != null && next.anomalyType == AnomalyType.CHARGE && next.roundNumber - rc.getRoundNum() < Constants.RUN_ROUNDS_BEFORE_CHARGE) {
    		tryMoveRepelFriends();
    	}
    	
    	verbose("bytecode remaining after sensing: "+ Clock.getBytecodesLeft());
    	//movement priority 1: run from danger in area if out numbered (in this case we should attack first if able)



    	if(localInfo.getEnemyDamagerCount() > localInfo.getFriendlyDamagerCount() || 
    			(localInfo.getEnemyDamagerCount() > 0 && !rc.isActionReady())) {
    		tryAttack();
			tryMoveInDanger();
    	}

    	tryAnomalyAndTryMove();
    	
    	//movement priority 2: move to best location to attack best target.
    	tryMoveAndAttackBestTarget();
    	
    	//movement priority 3: move to best location to attack a target.
    	tryMoveAndAttack();
    	tryAttack();
    	
    	//movement priority 4: continue or get a task
    	tryMoveOnTask();
    	tryMoveNewTask();
    	
    	//TODO: movement priority for after tasks (repel FArchons?)
    	
    	trySenseResources();
    }

    private void tryAnomalyAndTryMove() throws GameActionException {
		if(!rc.isActionReady()) return;
		
		if(localInfo.robotDamage > localInfo.buildingDamage && localInfo.robotDamage > RobotType.SAGE.damage) {
			if(rc.canEnvision(AnomalyType.CHARGE)) {
				rc.envision(AnomalyType.CHARGE);
				tryMoveInDanger();
			}
		}
		
		if(localInfo.buildingDamage > localInfo.robotDamage && localInfo.buildingDamage > RobotType.SAGE.damage) {
			if(rc.canEnvision(AnomalyType.FURY)) {
				rc.envision(AnomalyType.FURY);
				tryMoveInDanger();
			}
		}
		
	}


	private void tryMoveRepelFriends() throws GameActionException {
		if(!rc.isMovementReady() || localInfo.nearestFriend == null) return;
		
		moveAway(localInfo.nearestFriend.location);rc.setIndicatorString("full friend repel, from: "+localInfo.nearestFriend.location);
		
	}
    
    private void tryMoveOnTask() throws GameActionException {
		if(!rc.isMovementReady() || task == null) return;

		if(task.target != null){
			switch (task.type){
				case EXPLORE:
					if(rc.getLocation().isWithinDistanceSquared(task.target, Constants.CLOSE_RADIUS)){
						task = null;
						tryTargetFromComms = true;
						return;
					}
					break;
				case DEFEND_ARCHON:
					if(rc.getLocation().isWithinDistanceSquared(task.target, Constants.ARCHON_DEATH_CONFIRMATION)){
						RobotInfo fArchon = localInfo.nearestFR[RobotType.ARCHON.ordinal()];
						if(fArchon != null && !fArchon.location.equals(task.target)){
							task.target = fArchon.location;
						}else if(localInfo.nearestEnemy == null){
							Comms2.markTaskDone(task);
							task = null;
							tryTargetFromComms = true;
							return;
						}
					}
					break;
				case ATTACK_ARCHON:
					if(rc.getLocation().isWithinDistanceSquared(task.target, Constants.ARCHON_DEATH_CONFIRMATION)){
						RobotInfo eArchon = localInfo.nearestER[RobotType.ARCHON.ordinal()];
						if(eArchon == null){
							Comms2.markTaskDone(task);
							task = null;
							tryTargetFromComms = true;
							return;
						}
					}
			}

		}

		int bc = Clock.getBytecodesLeft();
		if(bc>SOLDIER_PATHFINDING_LIMIT){
			pathfinding.moveTowards(task.target,false);rc.setIndicatorString("best task loc: "+task.target);
		}else moveToward(task.target);rc.setIndicatorString("best task loc: "+task.target);

		if(rc.isMovementReady()) {
			task = null;
		}
	}

	//strategy is to only read from comms one piece of info per turn
	private void tryMoveNewTask() throws GameActionException {
		if(!rc.isMovementReady()) return;

		//got target from comms last time you checked, therefore try again
		if(tryTargetFromComms) {
			task = Comms2.getAttackTask();
			tryTargetFromComms = task != null;
		}

		//did not get target from comms last time, try to get an exploration task
		else if(!isMapExplored) {
			//reset lead found state to look for lead next time
			tryTargetFromComms = true;
			MapLocation loc = Comms2.getNearbyUnexplored();
			if(loc == null) {
				isMapExplored = true; // assume map is fully explored when BFS25 yields no result
			}else task = new Task(TaskType.EXPLORE,loc);
		}

		else {
			//reset found state to look for lead next time
			tryTargetFromComms = true;
		}

		tryMoveOnTask();
	}
    
    private void tryMoveInDanger() throws GameActionException {
		if(!rc.isMovementReady()) return;
		if(localInfo.findNearestDamager() != null) {
			moveAway(localInfo.findNearestDamager());rc.setIndicatorString("run outnumbered");
		}
	}

	private void trySenseResources() throws GameActionException {
		if(Clock.getBytecodesLeft() > 3000) {
			localInfo.senseLead(false,false);
		}
		if(Clock.getBytecodesLeft() > 2000) {
			localInfo.senseGold();
		}
	}
	

	//try to attack a target
	private void tryAttack() throws GameActionException {
		if(!rc.isActionReady()) return;
		
		MapLocation target = null;
		
		if(localInfo.nearestER[RobotType.SAGE.ordinal()] != null) {
			target = localInfo.nearestER[RobotType.SAGE.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), RobotType.SAGE.actionRadiusSquared)) {
				tryAttack(target);
				return;
			}	
		}
		
		if(localInfo.nearestER[RobotType.SOLDIER.ordinal()] != null) {
			target = localInfo.nearestER[RobotType.SOLDIER.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), RobotType.SAGE.actionRadiusSquared)) {
				tryAttack(target);
				return;
			}
		}
		
		if(localInfo.nearestER[RobotType.WATCHTOWER.ordinal()] != null) {
			target = localInfo.nearestER[RobotType.WATCHTOWER.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), RobotType.SAGE.actionRadiusSquared)) {
				tryAttack(target);
				return;
			}	
		}
		if(localInfo.nearestER[RobotType.ARCHON.ordinal()] != null) {
			target = localInfo.nearestER[RobotType.ARCHON.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), RobotType.SAGE.actionRadiusSquared)) {
				tryAttack(target);
				return;
			}
		}
		if(localInfo.nearestER[RobotType.BUILDER.ordinal()] != null) {
			target = localInfo.nearestER[RobotType.BUILDER.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), RobotType.SAGE.actionRadiusSquared)) {
				tryAttack(target);
				return;
			}
		}
		if(localInfo.nearestER[RobotType.MINER.ordinal()] != null) {
			target = localInfo.nearestER[RobotType.MINER.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), RobotType.SAGE.actionRadiusSquared)) {
				tryAttack(target);
				return;
			}
		}
		if(localInfo.nearestER[RobotType.LABORATORY.ordinal()] != null) {
			target = localInfo.nearestER[RobotType.LABORATORY.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), RobotType.SAGE.actionRadiusSquared)) {
				tryAttack(target);
				return;
			}
		}
		
	}
	
	//try to make an aggressive move, then try to attack a target
	private void tryMoveAndAttack() throws GameActionException {
		if(!rc.isMovementReady()) return;
		
		//MapLocation target = localInfo.getNearestTargetForSoldier();
		MapLocation target = null;
		MapLocation best = null;

		if(target != null){
			best = localInfo.getBestLocInRange(target);
			if(best != null) {
				int bc = Clock.getBytecodesLeft();
				if(bc>SOLDIER_PATHFINDING_LIMIT){
					pathfinding.moveTowards(best,false);rc.setIndicatorString("best loc: "+best);
				}else moveToward(best);rc.setIndicatorString("best loc: "+best);

				if(!rc.isActionReady())
					return;
				tryAttack(target);
			}
		}
	}


	//try to make an aggressive move, then try to attack the best target based on damage per turn per hp
	private void tryMoveAndAttackBestTarget() throws GameActionException {
		if(!rc.isMovementReady()) return;

		
		
		double highestDPH = Double.MIN_VALUE;
		MapLocation target = null;
		double dph;//damage per turn per hp
		if(localInfo.weakestER[RobotType.SOLDIER.ordinal()] != null) {
			dph = (RobotType.SOLDIER.damage / 2) / (double)localInfo.weakestER[RobotType.SOLDIER.ordinal()].getHealth();
			if(dph > highestDPH) {
				highestDPH = dph;
				target = localInfo.weakestER[RobotType.SOLDIER.ordinal()].getLocation();
			}
		}
		//as of 1-13-22 targeting level 1 WT 1st seems acceptable
		if(localInfo.weakestER[RobotType.WATCHTOWER.ordinal()] != null) {
			dph = (RobotType.WATCHTOWER.getDamage(localInfo.weakestER[RobotType.WATCHTOWER.ordinal()].getLevel()) / Constants.ROUNDS_PER_ACTION) / (double)localInfo.weakestER[RobotType.WATCHTOWER.ordinal()].getHealth();
			if(dph > highestDPH) {
				highestDPH = dph;
				target = localInfo.weakestER[RobotType.WATCHTOWER.ordinal()].getLocation();
			}
		}
		if(localInfo.weakestER[RobotType.SAGE.ordinal()] != null) {
			//assumes level 1 buildings
			int buildingDamage =
				localInfo.friendlyUnitCounts[RobotType.ARCHON.ordinal()]*RobotType.ARCHON.health/10 +
				localInfo.friendlyUnitCounts[RobotType.WATCHTOWER.ordinal()]*RobotType.WATCHTOWER.health/10 +
				localInfo.friendlyUnitCounts[RobotType.LABORATORY.ordinal()]*RobotType.LABORATORY.health/10;
			int robotDamage =
				(int)(localInfo.friendlyUnitCounts[RobotType.SOLDIER.ordinal()]*RobotType.SOLDIER.health*0.22 +
				localInfo.friendlyUnitCounts[RobotType.MINER.ordinal()]*RobotType.WATCHTOWER.health*0.22 +
				localInfo.friendlyUnitCounts[RobotType.SAGE.ordinal()]*RobotType.SAGE.health*0.22 +
				localInfo.friendlyUnitCounts[RobotType.BUILDER.ordinal()]*RobotType.BUILDER.health*0.22);

			int damage = RobotType.SAGE.damage;
			if(buildingDamage > damage)
				damage = buildingDamage;
			if(robotDamage > damage)
				damage = robotDamage;

			dph = (damage / (RobotType.SAGE.actionCooldown / GameConstants.COOLDOWNS_PER_TURN)) / (double)localInfo.weakestER[RobotType.SAGE.ordinal()].getHealth();
			if(dph > highestDPH) {
				highestDPH = dph;
				target = localInfo.weakestER[RobotType.SAGE.ordinal()].getLocation();
			}
		}

		//check non damagers
		if(target == null) {
			if(localInfo.weakestER[RobotType.ARCHON.ordinal()] != null) {
				target = localInfo.weakestER[RobotType.ARCHON.ordinal()].getLocation();
			}else if(localInfo.weakestER[RobotType.BUILDER.ordinal()] != null) {
				target = localInfo.weakestER[RobotType.BUILDER.ordinal()].getLocation();
			}else if(localInfo.weakestER[RobotType.MINER.ordinal()] != null) {
				target = localInfo.weakestER[RobotType.MINER.ordinal()].getLocation();
			}else if(localInfo.weakestER[RobotType.LABORATORY.ordinal()] != null) {
				target = localInfo.weakestER[RobotType.LABORATORY.ordinal()].getLocation();
			}
		}

		if(target == null) //no one to attack
			return;

		MapLocation best = localInfo.getBestLocInRange(target);

		if(best == null) //cannot move or stay to attack best target
			return;

//		moveToward(best); rc.setIndicatorString("best attack loc for best target: "+best);
		int bc = Clock.getBytecodesLeft();
		if(bc>SOLDIER_PATHFINDING_LIMIT){
			pathfinding.moveTowards(best,false);rc.setIndicatorString("best task loc: "+best);
		}else moveToward(best);rc.setIndicatorString("best task loc: "+best);


		if(!rc.isActionReady())
			return;
		
		tryAttack(target);
		
	}

	
	

    
    
    private void tryAttack(MapLocation target) throws GameActionException {
		if(rc.canAttack(target)) {
			rc.attack(target);
		}
		
	}
}
