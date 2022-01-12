package offensivebot.bots;


import battlecode.common.*;

import static offensivebot.bots.Archon.rng;
import static offensivebot.utils.Constants.directions;

public class Miner extends Robot{

	int fx=0,fy=0,vx=0,vy=0;

	final int MINER_FORCE = 20,DAMAGER_FORCE = 10,LEAD_FORCE = 20,MINER_MASS = 1,MOVE_THRESH = 10,VEL_RED=40;


	public Miner(RobotController rc) throws GameActionException  {
        super(rc);
    }
    
    public void sense() throws GameActionException{
		RobotInfo[] robots = rc.senseNearbyRobots();

		Direction[] directions = Direction.values();
		int[] rubble = new int[directions.length];

		for(int i=directions.length;--i>=0;){
			MapLocation loc = currentLocation.add(directions[i]);
			if(rc.canMove(directions[i])){
				// can tweak this value
				rubble[i] = rc.senseRubble(loc)+1;
			}else rubble[i] = 50;;
		}
		fx=0;
		fy=0;
		for(int i=robots.length;--i>=0;){
			if(robots[i].getTeam() == team){
				if(robots[i].getType() == RobotType.MINER){
					MapLocation robLoc = robots[i].getLocation();
					int d = currentLocation.distanceSquaredTo(robLoc);
					// force from miner to me
					Direction dir = robLoc.directionTo(currentLocation);
					fx += dir.dx*MINER_FORCE/d*rubble[dir.ordinal()];
					fy += dir.dy*MINER_FORCE/d*rubble[dir.ordinal()];

					// also calculate miners in sector
				}
			}else{
				switch (robots[i].getType()) {
					case SAGE: case SOLDIER: case WATCHTOWER:
						MapLocation robLoc = robots[i].getLocation();
						int d = currentLocation.distanceSquaredTo(robLoc);
						// force from enemy damager to me
						Direction dir = robLoc.directionTo(currentLocation);
						fx += dir.dx * DAMAGER_FORCE/d*rubble[dir.ordinal()];
						fy += dir.dy * DAMAGER_FORCE/d*rubble[dir.ordinal()];
						break;
					default:
						break;
				}
			}
		}

		MapLocation[] leadLocations = rc.senseNearbyLocationsWithLead();
		if(leadLocations.length>30)leadLocations = rc.senseNearbyLocationsWithLead(10);
		else if(leadLocations.length>20)leadLocations = rc.senseNearbyLocationsWithLead(15);

		for(int i = leadLocations.length;--i>=0;){
			int lead = rc.senseLead(leadLocations[i]);
			int d = currentLocation.distanceSquaredTo(leadLocations[i])+1;
			Direction dir = currentLocation.directionTo(leadLocations[i]);
			// force from me to lead
			fx += (dir.dx*lead*LEAD_FORCE)/(rubble[dir.ordinal()]*d);
			fy += (dir.dy*lead*LEAD_FORCE)/(rubble[dir.ordinal()]*d);
			// also calculate lead in sector
		}
		vx += fx/MINER_MASS;
		vy += fy/MINER_MASS;
	}

    public void move() throws GameActionException {
		Direction idealDir = getIdealDir();
		if(rc.canMove(idealDir)){
			rc.move(idealDir);
		}
		vx -= Math.signum(vx)*VEL_RED;
		vy -= Math.signum(vy)*VEL_RED;
	}

    @Override
    public void act() throws GameActionException {

		for(Direction direction: Direction.allDirections()){
			MapLocation loc = currentLocation.add(direction);
			while(rc.canMineGold(loc))rc.mineGold(loc);
			while(rc.canMineLead(loc))rc.mineLead(loc);
		}

    }

	Direction getIdealDir(){
		if(vx>MOVE_THRESH){
			// NE, E, SE
			if(vy>MOVE_THRESH)return Direction.NORTHEAST;
			else if(vy<MOVE_THRESH)return Direction.SOUTHEAST;
			return Direction.EAST;
		}else if(vx<MOVE_THRESH){
			// NW, W, SW
			if(vy>MOVE_THRESH)return Direction.NORTHWEST;
			else if(vy<MOVE_THRESH)return Direction.SOUTHWEST;
			return Direction.WEST;
		}else{
			// N,S
			if(vy>MOVE_THRESH)return Direction.NORTH;
			else if(vy<MOVE_THRESH) return Direction.SOUTH;
			return Direction.CENTER;
		}
	}



	private MapLocation commsBestLocforMiner() throws GameActionException {
		// for now, I am only finding unexplored locations
		MapLocation bestLoc = comms.getNearestLeadLoc();
		if(bestLoc == null){
			bestLoc = comms.getNearbyUnexplored();
			if(bestLoc != null)rc.setIndicatorString("unexplored area: "+bestLoc);
		}rc.setIndicatorString("best mining loc: "+bestLoc);
		return bestLoc;
	}

}
