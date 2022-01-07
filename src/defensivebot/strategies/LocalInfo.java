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
    
    public RobotInfo[] nearestFR; //nearest friendly robots of each type
    public int[] nearestFRDist; //nearest friendly robots' distances(of each type)s
    public RobotInfo[] nearestER; //nearest enemy robots of each type
    public int[] nearestERDist; //nearest enemy robots' distances(of each type)
    public int[] friendlyUnitCounts;
    public int[] enemyUnitCounts;
    
    public int[][] lead2d;
    
    public MapLocation nearestLead;
    public int nearestLeadDist;
    
    public RobotInfo homeArchon;
    
    public LocalInfo(RobotController rc,Comms comms){
        this.rc=rc;
        this.comms=comms;
        
        //initialize once to save bytecode
        nearestFR = new RobotInfo[UNITS_AVAILABLE];
        nearestFRDist = new int[UNITS_AVAILABLE];
        nearestER = new RobotInfo[UNITS_AVAILABLE];
        nearestERDist = new int[UNITS_AVAILABLE];
    }

    private void reset(){

    }

    public void senseRobots(){
    	//overhead = 250 bytecode
        friendlyUnitCounts = new int[UNITS_AVAILABLE]; 
        enemyUnitCounts = new int[UNITS_AVAILABLE];

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
                if(homeArchon == null && typeOrdinal == RobotType.ARCHON.ordinal())homeArchon = nearbyRobots[i];
                if(distToMe < nearestFRDist[typeOrdinal]) {
                	nearestFR[typeOrdinal] = nearbyRobots[i];
                	nearestFRDist[typeOrdinal] = distToMe;
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

    // need to be careful while sensing terrain. the way I see it, we don't need to sense terrain again and again
    public void senseTerrain() throws GameActionException {
    	nearestLeadDist = Integer.MAX_VALUE;
      MapLocation loc = rc.getLocation();
      boolean isDenseUpdateAllowed = comms.isDenseUpdateAllowed(loc);
        MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(),rc.getType().visionRadiusSquared);
        for(int i = locations.length; --i >= 0;){
        	int lead = rc.senseLead(locations[i]);
        	if(lead > 0){
                if(isDenseUpdateAllowed)comms.queueDenseMatrixUpdate(locations[i].x, locations[i].y, lead, CommInfoBlockType.LEAD_MAP);
        		    int distToMe = loc.distanceSquaredTo(locations[i]);

                if(distToMe < nearestLeadDist) {
                	nearestLead = locations[i];
                	nearestLeadDist = distToMe;
                }
          }
       }
    }
}
