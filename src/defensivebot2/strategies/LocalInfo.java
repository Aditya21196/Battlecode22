package defensivebot2.strategies;

import battlecode.common.*;
import defensivebot2.datasturctures.CustomSet;
import defensivebot2.enums.CommInfoBlockType;
import defensivebot2.enums.SparseSignalType;
import defensivebot2.models.SparseSignal;
import defensivebot2.utils.Constants;

import static defensivebot2.bots.Robot.roundNum;
import static defensivebot2.bots.Robot.turnCount;
import static defensivebot2.utils.Constants.UNITS_AVAILABLE;
import static defensivebot2.utils.LogUtils.printDebugLog;

public class LocalInfo {

    private final RobotController rc;
    private final Comms comms;
    private final int MIN_LEAD_PASSIVE = 6;
    
    //Robot Info gathered
    public RobotInfo[] nearestFR; //nearest friendly robots of each type
    public int[] nearestFRDist; //nearest friendly robots' distances(of each type)
    public RobotInfo[] nearestER; //nearest enemy robots of each type
    public int[] nearestERDist; //nearest enemy robots' distances(of each type)
    public int[] friendlyUnitCounts;
    public int[] enemyUnitCounts;
    
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


    
    //Lead Info gathered
    public MapLocation nearestLeadLoc;
    public int nearestLeadDist;
    public int totalLead;
    public int totalLeadDeposits;
    
    //Gold Info gathered
    public MapLocation nearestGoldLoc;
    public int nearestGoldDist;
    public int numMinersInSector;
    
    //Rubble Info gathered
    //public MapLocation lowestRubbleLoc;
    //public int lowestRubble;
    
    
    public LocalInfo(RobotController rc,Comms comms){
        this.rc=rc;
        this.comms=comms;
    }
    
    public void senseRobots(boolean forAttack, boolean forRepair, boolean forAnomaly){

        if(robotsSensedLastRound == turnCount)return;
        else robotsSensedLastRound = turnCount;

    	friendlyUnitCounts = new int[UNITS_AVAILABLE]; 
        enemyUnitCounts = new int[UNITS_AVAILABLE];


        
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
		int xSector = loc.x/comms.xSectorSize, ySector = loc.y/comms.ySectorSize;
		boolean isDenseUpdateAllowed = comms.isDenseUpdateAllowed();

		RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
		if(nearbyRobots.length>20)nearbyRobots = rc.senseNearbyRobots(10);

        for(int i = nearbyRobots.length; --i>=0;){
        	MapLocation robLoc = nearbyRobots[i].getLocation();
            int distToMe = loc.distanceSquaredTo(robLoc);
            int typeOrdinal = nearbyRobots[i].getType().ordinal();
            if(nearbyRobots[i].getTeam() == rc.getTeam()){
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
						isDenseUpdateAllowed && robLoc.x/comms.xSectorSize == xSector &&
								robLoc.y/comms.ySectorSize == ySector
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
		if(Clock.getBytecodesLeft()>5000 && comms.isDenseUpdateAllowed()){
			int ed = getEnemyDamagerCount();
			int val=0;
			if(ed>1)val = 3;
			else if(ed == 1)val = 2;
			else if(getEnemyNonDamagerCount()>0)val = 1;
			comms.queueDenseMatrixUpdate(val,CommInfoBlockType.ENEMY_UNITS);
		}
    }


    
    
    public void senseLead(boolean forPassive) throws GameActionException {

        if(leadSensedLastRound == turnCount)return;
        else leadSensedLastRound = turnCount;

    	nearestLeadDist = Integer.MAX_VALUE;
    	nearestLeadLoc = null;
    	totalLead=0;
    	totalLeadDeposits = 0;

        MapLocation loc = rc.getLocation();
        
        MapLocation[] locations = getLeadLocations(forPassive);
        
        int totalLeadInSector=0;
        int xSector = loc.x/comms.xSectorSize, ySector = loc.y/comms.ySectorSize;
        
        boolean isDenseUpdateAllowed = comms.isDenseUpdateAllowed();
        for(int i = locations.length; --i >= 0;){
        	//we should consider not counting lead at all, just deposits
        	int lead = rc.senseLead(locations[i]);
        	totalLead += lead;
        	totalLeadDeposits++;
            if(isDenseUpdateAllowed && locations[i].x/comms.xSectorSize == xSector && locations[i].y/comms.ySectorSize == ySector)
                totalLeadInSector += lead;


            int distToMe = loc.distanceSquaredTo(locations[i]);

        	if(distToMe < nearestLeadDist) {
            	nearestLeadLoc = locations[i];
            	nearestLeadDist = distToMe;
            }
        }
		if(robotsSensedLastRound == turnCount)totalLeadInSector /= (numMinersInSector+1);
        if(isDenseUpdateAllowed)comms.queueDenseMatrixUpdate(totalLeadInSector, CommInfoBlockType.LEAD_MAP);
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
        if(!comms.isDenseUpdateAllowed())return;
        if(turnCount == leadSensedLastRound){
            comms.queueDenseMatrixUpdate( 1, CommInfoBlockType.EXPLORATION);
        }
    }

    
    public void checkEnemySpotted(){
        if(turnCount == robotsSensedLastRound && nearestEnemy!=null && roundNum<1000){
            comms.queueSparseSignalUpdate(new SparseSignal(SparseSignalType.ENEMY_SPOTTED,null,-1));
        }
    }
    
    public void checkArchonSpotted() {
        if(turnCount == robotsSensedLastRound && nearestER[RobotType.ARCHON.ordinal()] != null){
            comms.queueSparseSignalUpdate(
                    new SparseSignal(
                            SparseSignalType.ARCHON_LOCATION,
                            nearestER[RobotType.ARCHON.ordinal()].location,
                            -1,
                            3// first bit is on - means enemy archon. 2nd bit means its alive
                    )
            );
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
}
