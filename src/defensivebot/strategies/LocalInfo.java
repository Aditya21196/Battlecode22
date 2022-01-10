package defensivebot.strategies;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import defensivebot.enums.CommInfoBlockType;
import defensivebot.enums.SparseSignalType;
import defensivebot.models.SparseSignal;

import static defensivebot.bots.Robot.roundNum;
import static defensivebot.bots.Robot.turnCount;
import static defensivebot.utils.Constants.UNITS_AVAILABLE;
import static defensivebot.utils.LogUtils.printDebugLog;

public class LocalInfo {

    private final RobotController rc;
    private final Comms comms;
    private final int MIN_LEAD_PASSIVE = 5;
    
    //Robot Info gathered
    public RobotInfo[] nearestFR; //nearest friendly robots of each type
    public int[] nearestFRDist; //nearest friendly robots' distances(of each type)
    public RobotInfo[] nearestER; //nearest enemy robots of each type
    public int[] nearestERDist; //nearest enemy robots' distances(of each type)
    public int[] friendlyUnitCounts;
    public int[] enemyUnitCounts;
    // just for debugging
    public RobotInfo nearestEnemy;
    public int nearestEnemyDist;

    public RobotInfo homeArchon;

    
    //additional Robot Info (for attacking)
    public RobotInfo[] weakestER; //weakest(lowest health) enemy robots of each type
    public int[] weakestERHealth; //weakest enemy robots' health (of each type)

    public int leadSensedLastRound = -1,robotsSensedLastRound = -1;


    
    //Lead Info gathered
    public MapLocation nearestLeadLoc;
    public int nearestLeadDist;
    public int totalLead;
    public int totalLeadDeposits;
    
    //Gold Info gathered
    public MapLocation nearestGoldLoc;
    public int nearestGoldDist;
    public int totalGoldDeposits;
    
    //Rubble Info gathered
    public MapLocation lowestRubbleLoc;
    public int lowestRubble;
    
    
    
    public LocalInfo(RobotController rc,Comms comms){
        this.rc=rc;
        this.comms=comms;
    }

    private void reset(){

    }

//    public RobotInfo getNearestEnemy(){
//
//    }

    public void senseRobots(boolean forAttack){

        if(robotsSensedLastRound == turnCount)return;
        else robotsSensedLastRound = turnCount;

    	friendlyUnitCounts = new int[UNITS_AVAILABLE]; 
        enemyUnitCounts = new int[UNITS_AVAILABLE];


        // for debugging
        nearestEnemy = null;
        nearestEnemyDist = Integer.MAX_VALUE;


        nearestFR = new RobotInfo[UNITS_AVAILABLE];
        nearestFRDist = new int[UNITS_AVAILABLE];
        nearestER = new RobotInfo[UNITS_AVAILABLE];
        nearestERDist = new int[UNITS_AVAILABLE];


        if(forAttack){
            //additional info gathered not in senseRobots()
            weakestER = new RobotInfo[UNITS_AVAILABLE];
            weakestERHealth = new int[UNITS_AVAILABLE];
        }


        for(int i = nearestFRDist.length; --i>=0;) {
        	nearestFRDist[i] = Integer.MAX_VALUE;
        	nearestERDist[i] = Integer.MAX_VALUE;
            if(forAttack)weakestERHealth[i] = Integer.MAX_VALUE;
        }
        
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();

        if(nearbyRobots.length>15)nearbyRobots = rc.senseNearbyRobots(10);

        MapLocation loc = rc.getLocation();
        for(int i = nearbyRobots.length; --i>=0;){
        	MapLocation robLoc = nearbyRobots[i].getLocation();
            int distToMe = loc.distanceSquaredTo(robLoc);
            int typeOrdinal = nearbyRobots[i].getType().ordinal();
            if(nearbyRobots[i].getTeam() == rc.getTeam()){
                friendlyUnitCounts[typeOrdinal]++;
                if(distToMe < nearestFRDist[typeOrdinal]) {
                	nearestFR[typeOrdinal] = nearbyRobots[i];
                	nearestFRDist[typeOrdinal] = distToMe;
                }
                if(homeArchon == null && typeOrdinal == RobotType.ARCHON.ordinal()) {
                	homeArchon = nearbyRobots[i];
                }

                
            }else{

                // for debugging
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
            }
        }
    }


    public void senseLead(boolean forPassive) throws GameActionException {

        if(leadSensedLastRound == turnCount)return;
        else leadSensedLastRound = turnCount;

    	nearestLeadDist = Integer.MAX_VALUE;
    	nearestLeadLoc = null;
    	totalLead=0;

        MapLocation loc = rc.getLocation();

        int totalLeadInSector=0;
        int xSector = loc.x/comms.xSectorSize, ySector = loc.y/comms.ySectorSize;


	    MapLocation[] locations = rc.senseNearbyLocationsWithLead(20);

        if(locations.length>15)rc.senseNearbyLocationsWithLead(10);

        boolean isDenseUpdateAllowed = comms.isDenseUpdateAllowed();
        for(int i = locations.length; --i >= 0;){
        	int lead = rc.senseLead(locations[i]);
        	totalLead += lead;
            if(isDenseUpdateAllowed && locations[i].x/comms.xSectorSize == xSector && locations[i].y/comms.ySectorSize == ySector)
                totalLeadInSector += lead;

            if(forPassive && lead<MIN_LEAD_PASSIVE)continue;

            int distToMe = loc.distanceSquaredTo(locations[i]);

        	if(distToMe < nearestLeadDist) {
            	nearestLeadLoc = locations[i];
            	nearestLeadDist = distToMe;
            }
        }
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
    
    // 
    public void senseRubble(MapLocation location) throws GameActionException {

    	lowestRubble = Integer.MAX_VALUE;
    	lowestRubbleLoc = null;
        //boolean isDenseUpdateAllowed = comms.isDenseUpdateAllowed();
	    MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(location, 2);
        for(int i = locations.length; --i >= 0;){
        	if(rc.canSenseLocation(locations[i])) {
        		int rubble = rc.senseRubble(locations[i]);
	        	if(rubble < lowestRubble) {
	            	lowestRubble = rubble;
	            	lowestRubbleLoc = locations[i];
	            }
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
    
	/*
	 * sets lowestRubbleLoc to lowest rubble MapLocation that this robot
	 * can move to or stay on that can still attack target
	 * null if all are occupied or out of range of target
	 */
	public void senseRubbleForAttack(MapLocation target) throws GameActionException {
		lowestRubble = Integer.MAX_VALUE;
    	lowestRubbleLoc = null;
    	MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2);
        for(int i = locations.length; --i >= 0;){
        	if(locations[i].isWithinDistanceSquared(target, rc.getType().actionRadiusSquared)){
        		int rubble = rc.senseRubble(locations[i]);
	        	if(rubble < lowestRubble && (rc.getLocation().equals(locations[i]) || !rc.isLocationOccupied(locations[i]))) {
	        		lowestRubble = rubble;
        			lowestRubbleLoc = locations[i];
	            }
        	}
        }
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
}
