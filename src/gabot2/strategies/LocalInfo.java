package gabot2.strategies;

import battlecode.common.*;
import gabot2.bots.Archon;
import gabot2.datasturctures.CustomSet;
import gabot2.enums.CommInfoBlockType;
import gabot2.enums.SparseSignalType;
import gabot2.models.SparseSignal;
import gabot2.utils.Constants;

import static gabot2.bots.Archon.rng;
import static gabot2.bots.Robot.roundNum;
import static gabot2.bots.Robot.turnCount;
import static gabot2.utils.Constants.*;


public class LocalInfo {

    private final RobotController rc;
    private final int MIN_LEAD_PASSIVE = 6;
	public RobotType selfType;
    
    //Robot Info gathered
    public RobotInfo[] nearestFR; //nearest friendly robots of each type
    public int[] nearestFRDist; //nearest friendly robots' distances(of each type)
    public RobotInfo[] nearestER; //nearest enemy robots of each type
    public int[] nearestERDist; //nearest enemy robots' distances(of each type)
    public int[] friendlyUnitCounts;
    public int[] enemyUnitCounts;
	public int[] leadInDirection;
	public MapLocation[] leadLocInDirection;
	public int[] minersInDirection;
    
    public RobotInfo nearestEnemy;
    public int nearestEnemyDist;
    public RobotInfo nearestFriend;
    public int nearestFriendDist;

    public RobotInfo homeArchon;
    
    //for anomaly info
    public int buildingDamage;
    public int robotDamage;

    
    //additional Robot Info (for attacking)
    public RobotInfo[] weakestER; //weakest(lowest health) enemy robots of each type
    public int[] weakestERHealth; //weakest enemy robots' health (of each type)
    
    //additional Robot Info (for repair)
    public RobotInfo[] nearestDamagedFR; //weakest(lowest health) enemy robots of each type
    public int[] nearestDamagedFRDist; //weakest enemy robots' health (of each type)
    
    public int leadSensedLastRound = -1,robotsSensedLastRound = -1;
	public int bestLeadScore;
	public MapLocation bestLeadLoc;

    
    //Lead Info gathered
    public MapLocation nearestLeadLoc;
    public int nearestLeadDist;
    public int totalLead;
    public int totalLeadDeposits;
    
    //Gold Info gathered
    public MapLocation nearestGoldLoc;
    public int nearestGoldDist;
    public int numMinersInSector;

	public CustomSet<MapLocation> friendlyArchons;
	public CustomSet<MapLocation> enemyArchons;

    //Rubble Info gathered
    //public MapLocation lowestRubbleLoc;
    //public int lowestRubble;
    
    
    public LocalInfo(RobotController rc){
        this.rc=rc;
		selfType=rc.getType();
    }

//	private static int getBestSectorSize(int dimension){
////        if(dimension%6 == 0)return 6;
////        return 5;
//		int dim7 = (int)Math.ceil(1.0*dimension/7);
//		int dim8 = (int)Math.ceil(1.0*dimension/8);
//		if(dim7 == dim8)return 7;
//		// more bits saved if we choose 8
//		return 8;
//	}
    
    public void senseRobots(boolean forAttack, boolean forRepair, boolean forAnomaly) throws GameActionException{

        if(robotsSensedLastRound == turnCount)return;
        else robotsSensedLastRound = turnCount;

		RobotType unitType = rc.getType();

		if(unitType == RobotType.MINER){
			minersInDirection = new int[9];
		}

    	friendlyUnitCounts = new int[UNITS_AVAILABLE]; 
        enemyUnitCounts = new int[UNITS_AVAILABLE];


		resetArchonLocations();
        nearestEnemy = null;
        nearestEnemyDist = Integer.MAX_VALUE;
        nearestFriend = null;
        nearestFriendDist = Integer.MAX_VALUE;
		numMinersInSector = 0;


        nearestFR = new RobotInfo[UNITS_AVAILABLE];
        nearestFRDist = new int[UNITS_AVAILABLE];
        nearestER = new RobotInfo[UNITS_AVAILABLE];
        nearestERDist = new int[UNITS_AVAILABLE];


        if(forAttack){
            //additional info gathered not in senseRobots()
            weakestER = new RobotInfo[UNITS_AVAILABLE];
            weakestERHealth = new int[UNITS_AVAILABLE];
        }
        if(forRepair){
            //additional info gathered not in senseRobots()
        	nearestDamagedFR = new RobotInfo[UNITS_AVAILABLE];
        	nearestDamagedFRDist = new int[UNITS_AVAILABLE];
        }
        if(forAnomaly) {
        	buildingDamage = 0;
        	robotDamage = 0;
        }

        for(int i = nearestFRDist.length; --i>=0;) {
        	nearestFRDist[i] = Integer.MAX_VALUE;
        	nearestERDist[i] = Integer.MAX_VALUE;
            if(forAttack)weakestERHealth[i] = Integer.MAX_VALUE;
            if(forRepair)nearestDamagedFRDist[i] = Integer.MAX_VALUE;
        }
        


		MapLocation loc = rc.getLocation();
		int xSector = loc.x/Comms2.xSectorSize, ySector = loc.y/Comms2.ySectorSize;
		boolean isDenseUpdateAllowed = Comms2.denseUpdateAllowed;

		RobotInfo[] nearbyRobots = getRobots(forAttack);
		Team team = rc.getTeam();
        for(int i = nearbyRobots.length; --i>=0;){
        	MapLocation robLoc = nearbyRobots[i].getLocation();
            int distToMe = loc.distanceSquaredTo(robLoc);
            int typeOrdinal = nearbyRobots[i].getType().ordinal();
            if(nearbyRobots[i].getTeam() == team){

				if(selfType == RobotType.MINER){
					Direction direction = loc.directionTo(robLoc);
					minersInDirection[direction.ordinal()]++;
				}

            	if(nearestFriendDist>distToMe){
            		nearestFriendDist = distToMe;
                    nearestFriend = nearbyRobots[i];
                }
            	
            	friendlyUnitCounts[typeOrdinal]++;
                if(distToMe < nearestFRDist[typeOrdinal]) {
                	nearestFR[typeOrdinal] = nearbyRobots[i];
                	nearestFRDist[typeOrdinal] = distToMe;
                }
                if(homeArchon == null && typeOrdinal == RobotType.ARCHON.ordinal()) {
                	homeArchon = nearbyRobots[i];
                }
                if(forRepair && distToMe < nearestDamagedFRDist[typeOrdinal] && 
            		nearbyRobots[i].getHealth() < nearbyRobots[i].getType().getMaxHealth(nearbyRobots[i].getLevel()) ){
                	nearestDamagedFRDist[typeOrdinal] = distToMe;
                	nearestDamagedFR[typeOrdinal] = nearbyRobots[i];
                }
				if(
						typeOrdinal == RobotType.MINER.ordinal() &&
						isDenseUpdateAllowed && robLoc.x/Comms2.xSectorSize == xSector &&
								robLoc.y/Comms2.ySectorSize == ySector
				)numMinersInSector++;


                
            }else{

                if(nearestEnemyDist>distToMe){
                    nearestEnemyDist = distToMe;
                    nearestEnemy = nearbyRobots[i];
                }

                enemyUnitCounts[typeOrdinal]++;
                if(distToMe < nearestERDist[typeOrdinal]) {
                	nearestER[typeOrdinal] = nearbyRobots[i];
                	nearestERDist[typeOrdinal] = distToMe;
                }
                if(forAttack){
                    int hp = nearbyRobots[i].getHealth();
                    if(hp < weakestERHealth[typeOrdinal]) {
                        weakestER[typeOrdinal] = nearbyRobots[i];
                        weakestERHealth[typeOrdinal] = hp;
                    }
                }
                if(forAnomaly && distToMe <= rc.getType().actionRadiusSquared) {
                	if(nearbyRobots[i].getType().isBuilding()) {
                		buildingDamage += (int)(nearbyRobots[i].getType().getMaxHealth(nearbyRobots[i].getLevel())*0.1);
                	}else {
                		robotDamage += (int)(nearbyRobots[i].getType().health*0.1);
                	}
                	
                }
            }
        }
		if(Clock.getBytecodesLeft()>5000 && Comms2.denseUpdateAllowed){
			int ed = getEnemyDamagerCount();
			int val=0;
			if(ed>3)val = 3;
			else if(ed > 1)val = 2;
			else if(ed == 1)val = 1;
			Comms2.queueDenseMatrixUpdate(val,CommInfoBlockType.ENEMY_UNITS,null);
		}

		// we no longer need this!
//		if(nearestER[RobotType.ARCHON.ordinal()] == null) {
//			Comms2.updateEnemyArchons(rc.getLocation());
//		}
		
		
    }


    
    
    public void senseLead(boolean forPassive,boolean forMining) throws GameActionException {

        if(leadSensedLastRound == turnCount)return;
        else leadSensedLastRound = turnCount;

		if(forMining){
			leadInDirection = new int[9];
			leadLocInDirection = new MapLocation[9];
		}

    	nearestLeadDist = Integer.MAX_VALUE;
    	nearestLeadLoc = null;
    	totalLead=0;
    	totalLeadDeposits = 0;
		bestLeadScore = 0;
		bestLeadLoc=null;

        MapLocation loc = rc.getLocation();
        
        MapLocation[] locations = getLeadLocations(forPassive);
        
        int totalLeadInSector=0;
        int xSector = loc.x/Comms2.xSectorSize, ySector = loc.y/Comms2.ySectorSize;
        
        boolean isDenseUpdateAllowed = Comms2.denseUpdateAllowed;
        for(int i = locations.length; --i >= 0;){
        	//we should consider not counting lead at all, just deposits
        	int lead = rc.senseLead(locations[i]);
        	totalLead += lead;
        	totalLeadDeposits++;
            if(isDenseUpdateAllowed && locations[i].x/Comms2.xSectorSize == xSector && locations[i].y/Comms2.ySectorSize == ySector)
                totalLeadInSector += lead;


            int distToMe = loc.distanceSquaredTo(locations[i]);

        	if(distToMe < nearestLeadDist) {
            	nearestLeadLoc = locations[i];
            	nearestLeadDist = distToMe;
            }

			if(forMining){
				Direction direction = loc.directionTo(locations[i]);
				leadInDirection[direction.ordinal()] += lead;
				// can improve to best location
				leadLocInDirection[direction.ordinal()] = locations[i];
			}
        }

		if(robotsSensedLastRound == turnCount)totalLeadInSector /= (numMinersInSector+1);
        if(isDenseUpdateAllowed)Comms2.queueDenseMatrixUpdate(totalLeadInSector, CommInfoBlockType.LEAD_MAP,null);
    }

    
    public void senseGold() throws GameActionException {
    	nearestGoldDist = Integer.MAX_VALUE;
    	nearestGoldLoc = null;
	    MapLocation loc = rc.getLocation();
	    MapLocation[] locations = rc.senseNearbyLocationsWithGold(rc.getType().visionRadiusSquared);
        for(int i = locations.length; --i >= 0;){
        	int distToMe = loc.distanceSquaredTo(locations[i]);
        	if(distToMe < nearestGoldDist) {
            	nearestGoldLoc = locations[i];
            	nearestGoldDist = distToMe;
            }
        }
    }
    

    
    public void checkExploration(){
        // if lead was checked, we mark as explored
        if(!Comms2.denseUpdateAllowed)return;
        if(turnCount == leadSensedLastRound){
			Comms2.queueDenseMatrixUpdate( 1, CommInfoBlockType.EXPLORATION,null);
        }
    }


    
	/*
	 * returns a surrounding, lowest rubble, and unoccupied location in action range of target
	 * if rubble is tied prefer current location
	 * returns null if all are occupied or out of range of target
	 */
	public MapLocation getBestLocInRange(MapLocation target) throws GameActionException {
		int lowestRubble = Integer.MAX_VALUE;
    	MapLocation bestLoc = null;
    	
    	MapLocation tempLoc = rc.getLocation();
    	int tempRubble = Integer.MAX_VALUE;
    	if(tempLoc.isWithinDistanceSquared(target, rc.getType().actionRadiusSquared)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(0,1);//north
    	if(tempLoc.isWithinDistanceSquared(target, rc.getType().actionRadiusSquared) && rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.isLocationOccupied(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(1,1);//north east
    	if(tempLoc.isWithinDistanceSquared(target, rc.getType().actionRadiusSquared) && rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.isLocationOccupied(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(1,0);//east
    	if(tempLoc.isWithinDistanceSquared(target, rc.getType().actionRadiusSquared) && rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.isLocationOccupied(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(1,-1);//south east
    	if(tempLoc.isWithinDistanceSquared(target, rc.getType().actionRadiusSquared) && rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.isLocationOccupied(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(0,-1);//south
    	if(tempLoc.isWithinDistanceSquared(target, rc.getType().actionRadiusSquared) && rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.isLocationOccupied(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(-1,-1);//south west
    	if(tempLoc.isWithinDistanceSquared(target, rc.getType().actionRadiusSquared) && rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.isLocationOccupied(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(-1,0);//west
    	if(tempLoc.isWithinDistanceSquared(target, rc.getType().actionRadiusSquared) && rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.isLocationOccupied(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	tempLoc = rc.getLocation().translate(-1,1);//north west
    	if(tempLoc.isWithinDistanceSquared(target, rc.getType().actionRadiusSquared) && rc.onTheMap(tempLoc)) {
    		tempRubble = rc.senseRubble(tempLoc);
    		if(tempRubble < lowestRubble && !rc.isLocationOccupied(tempLoc)) {
    			lowestRubble = tempRubble;
    			bestLoc = tempLoc;
    		}
    	}
    	
        return bestLoc;
	}

    public MapLocation findNearestDamager() {
        if(nearestER[RobotType.WATCHTOWER.ordinal()] != null)return nearestER[RobotType.WATCHTOWER.ordinal()].location;
        if(nearestER[RobotType.SOLDIER.ordinal()] != null)return nearestER[RobotType.SOLDIER.ordinal()].location;
        if(nearestER[RobotType.SAGE.ordinal()] != null)return nearestER[RobotType.SAGE.ordinal()].location;
        return null;
    }

    public MapLocation findNearestNondamager() {
        if(nearestER[RobotType.ARCHON.ordinal()] != null)return nearestER[RobotType.ARCHON.ordinal()].location;
        if(nearestER[RobotType.LABORATORY.ordinal()] != null)return nearestER[RobotType.LABORATORY.ordinal()].location;
        if(nearestER[RobotType.MINER.ordinal()] != null)return nearestER[RobotType.MINER.ordinal()].location;
        if(nearestER[RobotType.BUILDER.ordinal()] != null)return nearestER[RobotType.BUILDER.ordinal()].location;
        return null;
    }
    
    private RobotInfo[] getRobots(boolean forAttack) throws GameActionException {
    	
		int r = rc.getType().visionRadiusSquared;
    	RobotInfo[] robots = rc.senseNearbyRobots(r);
    	
    	//perform this check because most times there should be < 21 lead deposits and we should just proceed
        if(robots.length <= Constants.ROBOTS_UPPER_THRESHOLD_FOR_SENSING) {
        	return robots;
        }
        
        if(forAttack) {
        	return rc.senseNearbyRobots(r, rc.getTeam().opponent());
        }
        
        int rNew = (Constants.ROBOTS_UPPER_THRESHOLD_FOR_SENSING * r)/robots.length; //gets radius for finding lead upper threshold lead assuming uniform lead density
        
    	
		return rc.senseNearbyRobots(rNew);
        
    	
    }
    
    private MapLocation[] getLeadLocations(boolean forPassive) throws GameActionException {
    	
		int r = rc.getType().visionRadiusSquared;
    	MapLocation[] locations;
    	if(forPassive) 
    		locations = rc.senseNearbyLocationsWithLead(r, MIN_LEAD_PASSIVE);
    	else
    		locations = rc.senseNearbyLocationsWithLead(r);
        //perform this check because most times there should be < 21 lead deposits and we should just proceed
        if(locations.length <= Constants.LEAD_UPPER_THRESHOLD_FOR_SENSING) {
        	return locations;
        }
        
        int rNew = (Constants.LEAD_UPPER_THRESHOLD_FOR_SENSING * r)/locations.length; //gets radius for finding lead upper threshold lead assuming uniform lead density
        
        if(forPassive) 
    		return rc.senseNearbyLocationsWithLead(rNew, MIN_LEAD_PASSIVE);
    	
		return rc.senseNearbyLocationsWithLead(rNew);
        
    	
    }
    
    private MapLocation[] getLeadLocationsBinary(boolean forPassive) throws GameActionException {
    	
		int high = rc.getType().visionRadiusSquared;
    	MapLocation[] locations;
    	if(forPassive) 
    		locations = rc.senseNearbyLocationsWithLead(high,MIN_LEAD_PASSIVE);
    	else
    		locations = rc.senseNearbyLocationsWithLead(high);
        //perform this check because most times there should be < 21 lead deposits and we should just proceed
        if(locations.length <= Constants.LEAD_UPPER_THRESHOLD_FOR_SENSING) {
        	return locations;
        }
        
        //begin a binary search for radius with lead deposit counts within thresholds
    	high--;
    	int low = 0;
    	int mid = 0;
    	while(low < high) {
    		//sets mid to higher of two middle numbers when even number between low and high
        	mid = low + ((high - low + 1)/2);
        	if(forPassive) 
        		locations = rc.senseNearbyLocationsWithLead(mid,MIN_LEAD_PASSIVE);
        	else
        		locations = rc.senseNearbyLocationsWithLead(mid);
        	if(locations.length > Constants.LEAD_UPPER_THRESHOLD_FOR_SENSING) {
        		high = mid - 1;
        	}else if(locations.length < Constants.LEAD_LOWER_THRESHOLD_FOR_SENSING) {
        		low = mid;
        		continue;
        	}
        	//mid radius is within thresholds use these sensed locations
        	return locations;
        }

    	if(low == mid) {
    		return locations;
    	}
    	if(forPassive) 
    		return rc.senseNearbyLocationsWithLead(low,MIN_LEAD_PASSIVE);
    	
		return rc.senseNearbyLocationsWithLead(low);
    	
    }

    
    
    //returns the sum of the counts for enemy soldiers, sages, and watchtowers
    public int getEnemyDamagerCount() {
    	//make sure we have sensed robots at least once and therefore have initialized the enemy counts array.
    	if(robotsSensedLastRound != -1) {
    		return enemyUnitCounts[RobotType.WATCHTOWER.ordinal()] + enemyUnitCounts[RobotType.SOLDIER.ordinal()] + enemyUnitCounts[RobotType.SAGE.ordinal()];
    	}
    	return 0;
    }
    
  //returns the sum of the counts for enemy archons, builders, laboratories, and miners
    public int getEnemyNonDamagerCount() {
    	//make sure we have sensed robots at least once and therefore have initialized the enemy counts array.
    	if(robotsSensedLastRound != -1) {
    		return enemyUnitCounts[RobotType.ARCHON.ordinal()] + enemyUnitCounts[RobotType.BUILDER.ordinal()] + enemyUnitCounts[RobotType.LABORATORY.ordinal()] + enemyUnitCounts[RobotType.MINER.ordinal()];
    	}
    	return 0;
    }
    
  //returns the sum of the counts for enemy soldiers, sages, and watchtowers
    public int getFriendlyDamagerCount() {
    	//make sure we have sensed robots at least once and therefore have initialized the enemy counts array.
    	if(robotsSensedLastRound != -1) {
    		return friendlyUnitCounts[RobotType.WATCHTOWER.ordinal()] + friendlyUnitCounts[RobotType.SOLDIER.ordinal()] + friendlyUnitCounts[RobotType.SAGE.ordinal()];
    	}
    	return 0;
    }
    
  //returns the sum of the counts for enemy archons, builders, laboratories, and miners
    public int getFriendlyNonDamagerCount() {
    	//make sure we have sensed robots at least once and therefore have initialized the enemy counts array.
    	if(robotsSensedLastRound != -1) {
    		return friendlyUnitCounts[RobotType.ARCHON.ordinal()] + friendlyUnitCounts[RobotType.BUILDER.ordinal()] + friendlyUnitCounts[RobotType.LABORATORY.ordinal()] + friendlyUnitCounts[RobotType.MINER.ordinal()];
    	}
    	return 0;
    }

	public MapLocation getNearestTargetForSoldier() {
		if(nearestER[RobotType.SAGE.ordinal()] != null)return nearestER[RobotType.SAGE.ordinal()].location;
		if(nearestER[RobotType.SOLDIER.ordinal()] != null)return nearestER[RobotType.SOLDIER.ordinal()].location;
		if(nearestER[RobotType.WATCHTOWER.ordinal()] != null)return nearestER[RobotType.WATCHTOWER.ordinal()].location;
		if(nearestER[RobotType.ARCHON.ordinal()] != null)return nearestER[RobotType.ARCHON.ordinal()].location;
		if(nearestER[RobotType.BUILDER.ordinal()] != null)return nearestER[RobotType.BUILDER.ordinal()].location;
		if(nearestER[RobotType.MINER.ordinal()] != null)return nearestER[RobotType.MINER.ordinal()].location;
		if(nearestER[RobotType.LABORATORY.ordinal()] != null)return nearestER[RobotType.LABORATORY.ordinal()].location;
		return null;
	}

    public void resetArchonLocations() {
		friendlyArchons = new CustomSet<>(4);
		enemyArchons = new CustomSet<>(4);
    }

	public void addArchon(MapLocation target, boolean isFriendly) {
		if(isFriendly)friendlyArchons.add(target);
		else enemyArchons.add(target);
	}

	public MapLocation getBestLead() {
		int bestDir = -1;
		double bestLeadPerMiner = 0;
		for(int i=9;--i>=0;){
			int leadPerMiner = leadInDirection[i]/(minersInDirection[i]+1);
			if(leadPerMiner>bestLeadPerMiner){
				bestDir = i;
				bestLeadPerMiner = leadPerMiner;
			}
		}
		if(bestLeadPerMiner > LEAD_WORTH_PURSUING){
			return leadLocInDirection[bestDir];
		}
		return null;
	}
}
