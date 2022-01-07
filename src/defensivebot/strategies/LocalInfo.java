package defensivebot.strategies;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import defensivebot.enums.CommInfoBlockType;

import static defensivebot.utils.Constants.UNITS_AVAILABLE;

public class LocalInfo {

    private final RobotController rc;
    private final Comms comms;
    
    //Robot Info gathered
    public RobotInfo[] nearestFR; //nearest friendly robots of each type
    public int[] nearestFRDist; //nearest friendly robots' distances(of each type)s
    public RobotInfo[] nearestER; //nearest enemy robots of each type
    public int[] nearestERDist; //nearest enemy robots' distances(of each type)
    public int[] friendlyUnitCounts;
    public int[] enemyUnitCounts;
    public RobotInfo homeArchon;
    
    //Lead Info gathered
    public MapLocation nearestLeadLoc;
    public MapLocation passiveMiningLoc;
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

    public void senseRobots(){
    	//overhead = 250 bytecode
        friendlyUnitCounts = new int[UNITS_AVAILABLE]; 
        enemyUnitCounts = new int[UNITS_AVAILABLE];
        nearestFR = new RobotInfo[UNITS_AVAILABLE];
        nearestFRDist = new int[UNITS_AVAILABLE];
        nearestER = new RobotInfo[UNITS_AVAILABLE];
        nearestERDist = new int[UNITS_AVAILABLE];
        for(int i = nearestFRDist.length; --i>=0;) {
        	nearestFRDist[i] = Integer.MAX_VALUE;
        	nearestERDist[i] = Integer.MAX_VALUE;
        }
        
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        MapLocation loc = rc.getLocation();
        //w/o comms per robot sensed = 40 bytecode
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
                enemyUnitCounts[typeOrdinal]++;
                if(distToMe < nearestERDist[typeOrdinal]) {
                	nearestER[typeOrdinal] = nearbyRobots[i];
                	nearestERDist[typeOrdinal] = distToMe;
                }
            }
        }
    }

    public void senseLead() throws GameActionException {
    	nearestLeadDist = Integer.MAX_VALUE;
    	nearestLeadLoc = null;
    	passiveMiningLoc = null;
    	totalLead=0;
	    MapLocation loc = rc.getLocation();
	    MapLocation[] locations = rc.senseNearbyLocationsWithLead(rc.getType().visionRadiusSquared);
        for(int i = locations.length; --i >= 0;){
        	int lead = rc.senseLead(locations[i]);
        	totalLead += lead;
        	int distToMe = loc.distanceSquaredTo(locations[i]);
        	if(distToMe < nearestLeadDist) {
            	nearestLeadLoc = locations[i];
            	nearestLeadDist = distToMe;
            	if(passiveMiningLoc != null && locations[i].isWithinDistanceSquared(loc, 2) && lead > 5) {
            		passiveMiningLoc = locations[i];
            	}
            }
        	/*
	        boolean isDenseUpdateAllowed = comms.isDenseUpdateAllowed(loc);
	        if(isDenseUpdateAllowed) {
	        	comms.queueDenseMatrixUpdate(loc.x, loc.y, lead, CommInfoBlockType.LEAD_MAP);
	        }
	        */
        }
        
    }
    
    public void senseGold() throws GameActionException {
    	nearestGoldDist = Integer.MAX_VALUE;
    	nearestGoldLoc = null;
	    MapLocation loc = rc.getLocation();
	    MapLocation[] locations = rc.senseNearbyLocationsWithGold(rc.getType().visionRadiusSquared);
        for(int i = locations.length; --i >= 0;){
        	//int gold = rc.senseGold(locations[i]);
        	int distToMe = loc.distanceSquaredTo(locations[i]);
        	if(distToMe < nearestGoldDist) {
            	nearestGoldLoc = locations[i];
            	nearestGoldDist = distToMe;
            }
        }
        /*
        boolean isDenseUpdateAllowed = comms.isDenseUpdateAllowed(loc);
        if(isDenseUpdateAllowed && locations.length > 1) {
        	comms.queueDenseMatrixUpdate(loc.x, loc.y, locations.length, CommInfoBlockType.GOLD_MAP);
        }
        */
    }
    
    //We can change this later, but for now 
    public void senseRubble(MapLocation location) throws GameActionException {
    	lowestRubble = Integer.MAX_VALUE;
    	lowestRubbleLoc = null;
	    MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(location, 2);
        for(int i = locations.length; --i >= 0;){
        	int rubble = rc.senseRubble(locations[i]);
        	if(rubble < lowestRubble) {
            	lowestRubble = rubble;
            	lowestRubbleLoc = locations[i];
            }
        }
        /*
        boolean isDenseUpdateAllowed = comms.isDenseUpdateAllowed(loc);
        if(isDenseUpdateAllowed) {
        	comms.queueDenseMatrixUpdate(loc.x, loc.y, locations.length, CommInfoBlockType.GOLD_MAP);
        }
        */
    }
    
}
