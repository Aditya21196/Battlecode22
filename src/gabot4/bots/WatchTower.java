package gabot4.bots;

import static gabot4.utils.PathFindingConstants.SOLDIER_PATHFINDING_LIMIT;

import battlecode.common.AnomalyScheduleEntry;
import battlecode.common.AnomalyType;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotMode;
import battlecode.common.RobotType;
import gabot4.models.SparseSignal;
import gabot4.strategies.Comms2;
import gabot4.utils.Constants;

public class WatchTower extends Robot{
	
	private MapLocation finalTarget;
	private MapLocation generalTarget;
	
	private int roundsUseless = 0;
	private int finalTargetRubble;
	
    public WatchTower(RobotController rc) throws GameActionException  {
        super(rc);
    }
    
    @Override
    public void executeRole() throws GameActionException {
        //sense robots, track lowest hp by type as well

    	localInfo.senseRobots(true, false, false);


    	tryAttackBestTarget();
    	
    	tryAttack();
    	
    	tryTransformPortable();

        tryTransformTurret();

        tryMove();
    	
    	trySenseResources();
    }

    private void tryTransformTurret() throws GameActionException {
    	if(!rc.isMovementReady() || rc.getMode() == RobotMode.TURRET) return;
    	
    	if(
				(finalTarget != null && finalTarget.equals(rc.getLocation()) && rc.canTransform())
				|| (
						localInfo.nearestEnemy != null
								&& currentLocation.isWithinDistanceSquared(localInfo.nearestEnemy.location,RobotType.WATCHTOWER.actionRadiusSquared)
								&& rc.canTransform()
				)
		) {
    		rc.transform();
    		finalTarget = null;
    	}
    	
		
	}

	private void tryMove() throws GameActionException {
    	if(!rc.isMovementReady() || rc.getMode() == RobotMode.TURRET) return;
		
    	//whenever final target is set
    	if(finalTarget != null) {
    		updateFinalTarget();
    		pathfinding.moveTowards(finalTarget, false);rc.setIndicatorString("ft: "+finalTarget); 
    		return;
    	}
    	
    	if(localInfo.getEnemyDamagerCount() > 0) {
			updateFinalTarget();
			return;
		}
    	
    	generalTarget = Comms2.getClosestArchon(false);
    	
    	//move toward general targets
    	if(generalTarget != null && generalTarget.isWithinDistanceSquared(rc.getLocation(), Constants.CLOSE_RADIUS)) {
    		updateFinalTarget();
    		return;
    	}
    		
    	
    	if(generalTarget != null) {
    		pathfinding.moveTowards(generalTarget, false);
    		return;
    	}
    	
	}

	private void updateFinalTarget() throws GameActionException {
		//final target is still good keep it
		if(finalTarget != null && !rc.canSenseRobotAtLocation(finalTarget) && finalTargetRubble < Constants.ARCHON_LOW_RUBBLE) {
			return;
		}
		
		//finalTarget is compromised or hasn't been searched for
		int lowRubble = Integer.MAX_VALUE;
		MapLocation lowLoc = null;
		
		MapLocation[] visable = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), rc.getType().visionRadiusSquared);
		
		int tempRubble;
		for(int i = 0; i < visable.length; i+=3) {
			if(rc.canSenseRobotAtLocation(visable[i]))
				continue;
			
			tempRubble = rc.senseRubble(visable[i]);
			if(tempRubble < lowRubble) {
				if(tempRubble < Constants.ARCHON_LOW_RUBBLE) {
					finalTarget = visable[i];
					finalTargetRubble = tempRubble;
					return;
				}
				lowLoc = visable[i];
				lowRubble = tempRubble;
			}
		}
		
		finalTarget = lowLoc;
		finalTargetRubble = lowRubble;
		
		return;
	}

	private void tryTransformPortable() throws GameActionException {
		//attacked previously
    	if(!rc.isActionReady()) {
    		roundsUseless = 0;
    		return;
    	}
		if(rc.getMode() == RobotMode.PORTABLE) return;
		
		roundsUseless++;
		
		rc.setIndicatorString(""+roundsUseless);
		
    	if(roundsUseless > 20) {
    		MapLocation target = Comms2.getClosestArchon(false);
    		if(target != null && rc.canTransform()) {
        		rc.transform();
        		generalTarget = target;
        		
        	}
    	}
    	
    	if(roundsUseless > 50) {
    		MapLocation target = Comms2.getClosestArchon(true);
    		if(target != null && rc.canTransform()) {
        		rc.transform();
        		generalTarget = new MapLocation(rc.getMapWidth() - target.x -1, rc.getMapHeight() - target.y -1);
        		
        	}
    	}
    	
	}

	private void trySenseResources() throws GameActionException {
		if(Clock.getBytecodesLeft() > 4000) {
			localInfo.senseLead(false,false);
		}
		if(Clock.getBytecodesLeft() > 3000) {
			localInfo.senseGold();
		}
	}
	

	//try to attack a target
	private void tryAttack() throws GameActionException {
		if(!rc.isActionReady()) return;
		
		MapLocation target = null;
		
		if(localInfo.nearestER[RobotType.SAGE.ordinal()] != null) {
			target = localInfo.nearestER[RobotType.SAGE.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), RobotType.WATCHTOWER.actionRadiusSquared)) {
				tryAttack(target);
				return;
			}	
		}
		
		if(localInfo.nearestER[RobotType.SOLDIER.ordinal()] != null) {
			target = localInfo.nearestER[RobotType.SOLDIER.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), RobotType.WATCHTOWER.actionRadiusSquared)) {
				tryAttack(target);
				return;
			}
		}
		
		if(localInfo.nearestER[RobotType.WATCHTOWER.ordinal()] != null) {
			target = localInfo.nearestER[RobotType.WATCHTOWER.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), RobotType.WATCHTOWER.actionRadiusSquared)) {
				tryAttack(target);
				return;
			}	
		}
		if(localInfo.nearestER[RobotType.ARCHON.ordinal()] != null) {
			target = localInfo.nearestER[RobotType.ARCHON.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), RobotType.WATCHTOWER.actionRadiusSquared)) {
				tryAttack(target);
				return;
			}
		}
		if(localInfo.nearestER[RobotType.BUILDER.ordinal()] != null) {
			target = localInfo.nearestER[RobotType.BUILDER.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), RobotType.WATCHTOWER.actionRadiusSquared)) {
				tryAttack(target);
				return;
			}
		}
		if(localInfo.nearestER[RobotType.MINER.ordinal()] != null) {
			target = localInfo.nearestER[RobotType.MINER.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), RobotType.WATCHTOWER.actionRadiusSquared)) {
				tryAttack(target);
				return;
			}
		}
		if(localInfo.nearestER[RobotType.LABORATORY.ordinal()] != null) {
			target = localInfo.nearestER[RobotType.LABORATORY.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), RobotType.WATCHTOWER.actionRadiusSquared)) {
				tryAttack(target);
				return;
			}
		}
		
	}
	
	


	//try to make an aggressive move, then try to attack the best target based on damage per turn per hp
	private void tryAttackBestTarget() throws GameActionException {
		if(!rc.isActionReady()) return;

		double highestDPH = Double.MIN_VALUE;
		MapLocation target = null;
		double dph;//damage per turn per hp
		if(localInfo.weakestER[RobotType.SOLDIER.ordinal()] != null && localInfo.weakestER[RobotType.SOLDIER.ordinal()].location.isWithinDistanceSquared(rc.getLocation(), RobotType.WATCHTOWER.actionRadiusSquared)) {
			dph = (RobotType.SOLDIER.damage / 2) / (double)localInfo.weakestER[RobotType.SOLDIER.ordinal()].getHealth();
			if(dph > highestDPH) {
				highestDPH = dph;
				target = localInfo.weakestER[RobotType.SOLDIER.ordinal()].getLocation();
			}
		}
		//as of 1-13-22 targeting level 1 WT 1st seems acceptable
		if(localInfo.weakestER[RobotType.WATCHTOWER.ordinal()] != null && localInfo.weakestER[RobotType.WATCHTOWER.ordinal()].location.isWithinDistanceSquared(rc.getLocation(), RobotType.WATCHTOWER.actionRadiusSquared)) {
			dph = (RobotType.WATCHTOWER.getDamage(localInfo.weakestER[RobotType.WATCHTOWER.ordinal()].getLevel()) / Constants.ROUNDS_PER_ACTION) / (double)localInfo.weakestER[RobotType.WATCHTOWER.ordinal()].getHealth();
			if(dph > highestDPH) {
				highestDPH = dph;
				target = localInfo.weakestER[RobotType.WATCHTOWER.ordinal()].getLocation();
			}
		}
		if(localInfo.weakestER[RobotType.SAGE.ordinal()] != null && localInfo.weakestER[RobotType.SAGE.ordinal()].location.isWithinDistanceSquared(rc.getLocation(), RobotType.WATCHTOWER.actionRadiusSquared)) {
			//assumes level 1 buildings
			int buildingDamage =
				localInfo.friendlyUnitCounts[RobotType.ARCHON.ordinal()]*RobotType.ARCHON.health/10 +
				localInfo.friendlyUnitCounts[RobotType.WATCHTOWER.ordinal()]*RobotType.WATCHTOWER.health/10 +
				localInfo.friendlyUnitCounts[RobotType.LABORATORY.ordinal()]*RobotType.LABORATORY.health/10;
			int robotDamage =
				localInfo.friendlyUnitCounts[RobotType.SOLDIER.ordinal()]*RobotType.SOLDIER.health/10 +
				localInfo.friendlyUnitCounts[RobotType.MINER.ordinal()]*RobotType.WATCHTOWER.health/10 +
				localInfo.friendlyUnitCounts[RobotType.SAGE.ordinal()]*RobotType.SAGE.health/10 +
				localInfo.friendlyUnitCounts[RobotType.BUILDER.ordinal()]*RobotType.BUILDER.health/10;

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
			if(localInfo.weakestER[RobotType.ARCHON.ordinal()] != null && localInfo.weakestER[RobotType.ARCHON.ordinal()].location.isWithinDistanceSquared(rc.getLocation(), RobotType.WATCHTOWER.actionRadiusSquared)) {
				target = localInfo.weakestER[RobotType.ARCHON.ordinal()].getLocation();
			}else if(localInfo.weakestER[RobotType.BUILDER.ordinal()] != null && localInfo.weakestER[RobotType.BUILDER.ordinal()].location.isWithinDistanceSquared(rc.getLocation(), RobotType.WATCHTOWER.actionRadiusSquared)) {
				target = localInfo.weakestER[RobotType.BUILDER.ordinal()].getLocation();
			}else if(localInfo.weakestER[RobotType.MINER.ordinal()] != null && localInfo.weakestER[RobotType.MINER.ordinal()].location.isWithinDistanceSquared(rc.getLocation(), RobotType.WATCHTOWER.actionRadiusSquared)) {
				target = localInfo.weakestER[RobotType.MINER.ordinal()].getLocation();
			}else if(localInfo.weakestER[RobotType.LABORATORY.ordinal()] != null && localInfo.weakestER[RobotType.LABORATORY.ordinal()].location.isWithinDistanceSquared(rc.getLocation(), RobotType.WATCHTOWER.actionRadiusSquared)) {
				target = localInfo.weakestER[RobotType.LABORATORY.ordinal()].getLocation();
			}
		}

		if(target == null) //no one to attack
			return;

		tryAttack(target);
		
	}

	private void tryAttack(MapLocation target) throws GameActionException {
		if(rc.canAttack(target)) {
			rc.attack(target);
		}
		
	}
}
