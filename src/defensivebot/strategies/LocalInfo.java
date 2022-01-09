package defensivebot.strategies;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import defensivebot.enums.CommInfoBlockType;

import static defensivebot.bots.Robot.turnCount;
import static defensivebot.utils.Constants.UNITS_AVAILABLE;

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

    public RobotInfo[] highestIDFR; //highest id robots of each type
    
    //additional Robot Info (for attacking)
    public RobotInfo[] weakestER; //weakest(lowest health) enemy robots of each type
    public int[] weakestERHealth; //weakest enemy robots' health (of each type)

    public int leadSensedLastRound = -1;


    
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

    public void senseRobots(){
    	friendlyUnitCounts = new int[UNITS_AVAILABLE]; 
        enemyUnitCounts = new int[UNITS_AVAILABLE];


        // for debugging
        nearestEnemy = null;
        nearestEnemyDist = Integer.MAX_VALUE;


        nearestFR = new RobotInfo[UNITS_AVAILABLE];
        nearestFRDist = new int[UNITS_AVAILABLE];
        nearestER = new RobotInfo[UNITS_AVAILABLE];
        nearestERDist = new int[UNITS_AVAILABLE];

        highestIDFR = new RobotInfo[UNITS_AVAILABLE];

        for(int i = nearestFRDist.length; --i>=0;) {
        	nearestFRDist[i] = Integer.MAX_VALUE;
        	nearestERDist[i] = Integer.MAX_VALUE;
        }
        
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
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
                if(highestIDFR[typeOrdinal] != null && nearbyRobots[i].getID() > highestIDFR[typeOrdinal].getID()) {
                	highestIDFR[typeOrdinal] = nearbyRobots[i];
                }
                
            }else{

                // for debugging
                if(nearestEnemyDist<distToMe){
                    nearestEnemyDist = distToMe;
                    nearestEnemy = nearbyRobots[i];
                }

                enemyUnitCounts[typeOrdinal]++;
                if(distToMe < nearestERDist[typeOrdinal]) {
                	nearestER[typeOrdinal] = nearbyRobots[i];
                	nearestERDist[typeOrdinal] = distToMe;
                }
            }
        }
    }
    
    public void senseRobotsForAttack(){
    	friendlyUnitCounts = new int[UNITS_AVAILABLE]; 
        enemyUnitCounts = new int[UNITS_AVAILABLE];
        nearestFR = new RobotInfo[UNITS_AVAILABLE];
        nearestFRDist = new int[UNITS_AVAILABLE];
        nearestER = new RobotInfo[UNITS_AVAILABLE];
        nearestERDist = new int[UNITS_AVAILABLE];
        highestIDFR = new RobotInfo[UNITS_AVAILABLE];
        
        //additional info gathered not in senseRobots()
        weakestER = new RobotInfo[UNITS_AVAILABLE];
        weakestERHealth = new int[UNITS_AVAILABLE];
        
        for(int i = nearestFRDist.length; --i>=0;) {
        	nearestFRDist[i] = Integer.MAX_VALUE;
        	nearestERDist[i] = Integer.MAX_VALUE;
        	weakestERHealth[i] = Integer.MAX_VALUE;
        }
        
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
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
                if(highestIDFR[typeOrdinal] != null && nearbyRobots[i].getID() > highestIDFR[typeOrdinal].getID()) {
                	highestIDFR[typeOrdinal] = nearbyRobots[i];
                }
            }else{
                enemyUnitCounts[typeOrdinal]++;
                if(distToMe < nearestERDist[typeOrdinal]) {
                	nearestER[typeOrdinal] = nearbyRobots[i];
                	nearestERDist[typeOrdinal] = distToMe;
                }
                int hp = nearbyRobots[i].getHealth();
                if(hp < weakestERHealth[typeOrdinal]) {
                	weakestER[typeOrdinal] = nearbyRobots[i];
                	weakestERHealth[typeOrdinal] = hp;
                }
            }
        }
    }


    public void senseLead() throws GameActionException {

        if(leadSensedLastRound == turnCount)return;
        else leadSensedLastRound = turnCount;

    	nearestLeadDist = Integer.MAX_VALUE;
    	nearestLeadLoc = null;
    	totalLead=0;
	    MapLocation loc = rc.getLocation();
	    MapLocation[] locations = rc.senseNearbyLocationsWithLead(rc.getType().visionRadiusSquared);
        boolean isDenseUpdateAllowed = comms.isDenseUpdateAllowed();
        for(int i = locations.length; --i >= 0;){
        	int lead = rc.senseLead(locations[i]);
        	totalLead += lead;
        	int distToMe = loc.distanceSquaredTo(locations[i]);
        	if(distToMe < nearestLeadDist) {
            	nearestLeadLoc = locations[i];
            	nearestLeadDist = distToMe;
            }

	        if(isDenseUpdateAllowed) {
	        	comms.queueDenseMatrixUpdate(loc.x, loc.y, lead, CommInfoBlockType.LEAD_MAP);
	        }

        }
    }
    
    //designed to only report nearest lead deposit with greater than 5 lead.
    public void senseLeadForPassive() throws GameActionException {
    	nearestLeadDist = Integer.MAX_VALUE;
    	nearestLeadLoc = null;
    	totalLead=0;
	    MapLocation loc = rc.getLocation();
	    MapLocation[] locations = rc.senseNearbyLocationsWithLead(rc.getType().visionRadiusSquared);
        for(int i = locations.length; --i >= 0;){
        	int lead = rc.senseLead(locations[i]);
        	totalLead += lead;
        	int distToMe = loc.distanceSquaredTo(locations[i]);
        	if(distToMe < nearestLeadDist && lead > MIN_LEAD_PASSIVE) {
            	nearestLeadLoc = locations[i];
            	nearestLeadDist = distToMe;
            }
        }
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
        boolean isDenseUpdateAllowed = comms.isDenseUpdateAllowed();
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
        MapLocation loc = rc.getLocation();
        if(!comms.isDenseUpdateAllowed())return;
        if(turnCount == leadSensedLastRound){
            comms.queueDenseMatrixUpdate(loc.x,loc.y, 1, CommInfoBlockType.EXPLORATION);
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
    
}
