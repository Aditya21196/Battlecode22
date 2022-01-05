package defensivebot.strategies;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import defensivebot.enums.CommInfoBlock;

import static defensivebot.utils.Constants.UNITS_AVAILABLE;

public class LocalInfo {

    private final RobotController rc;
    private final Comms comms;
    public int[] friendlyUnitCounts;
    public int[] enemyUnitCounts;
    public int nearestEnemyDist,nearestFriendDist;
    public RobotInfo nearestEnemy,nearestFriend;
    public RobotInfo[] nearestRobots; //nearest robots of each type and on each team
    public int[] nearestDist; //nearest robots' distances to this rc (of each type and team)
    public int[][] lead2d;
    public int[][] rubble2d;
    public MapLocation nearestLead;
    public int nearestLeadDist;
    public RobotInfo homeArchon;
    
    public LocalInfo(RobotController rc,Comms comms){
        this.rc=rc;
        this.comms=comms;
        
        lead2d = new int[rc.getMapWidth()][rc.getMapHeight()];
        rubble2d = new int[rc.getMapWidth()][rc.getMapHeight()];
        for(int i = rubble2d.length; --i>=0;)
        	for(int j = rubble2d[0].length; --j>=0;)
        		rubble2d[i][j] = GameConstants.MAX_RUBBLE;
    }

    private void reset(){

    }

    public void senseRobots(){
        friendlyUnitCounts = new int[UNITS_AVAILABLE]; 
        enemyUnitCounts = new int[UNITS_AVAILABLE];
        nearestEnemyDist = Integer.MAX_VALUE;
        nearestFriendDist = Integer.MAX_VALUE;
        nearestEnemy = null;
        nearestFriend = null;

        // TODO: separate out into 2 different arrays
        nearestRobots = new RobotInfo[UNITS_AVAILABLE*2];
        nearestDist = new int[UNITS_AVAILABLE*2];
        for(int i = nearestDist.length; --i>=0;) nearestDist[i] = Integer.MAX_VALUE;
        

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        MapLocation loc = rc.getLocation();
        for(RobotInfo robot: nearbyRobots){
            // we need enemy units, friendly unit counts etc.
            MapLocation robLoc = robot.getLocation();
            int distToMe = loc.distanceSquaredTo(robLoc);
            int typeOrdinal = robot.getType().ordinal();
            if(robot.getTeam() == rc.getTeam()){
                friendlyUnitCounts[typeOrdinal]++;

                // find home archon upon spawning
                if(homeArchon == null && typeOrdinal == RobotType.ARCHON.ordinal())homeArchon = robot;

                if(distToMe<nearestFriendDist){
                    nearestFriendDist = distToMe;
                    nearestFriend = robot;
                }
                
                if(distToMe < nearestDist[typeOrdinal + UNITS_AVAILABLE]) {
                	nearestRobots[typeOrdinal + UNITS_AVAILABLE] = robot;
                	nearestDist[typeOrdinal + UNITS_AVAILABLE] = distToMe;
                }
                
            }else{
                enemyUnitCounts[robot.getType().ordinal()]++;

                if(distToMe<nearestEnemyDist){
                    nearestEnemyDist = distToMe;
                    nearestEnemy = robot;
                }
                
                if(distToMe < nearestDist[typeOrdinal]) {
                	nearestRobots[typeOrdinal] = robot;
                	nearestDist[typeOrdinal] = distToMe;
                }
            }


        }
    }

    // need to be careful while sensing terrain. the way I see it, we don't need to sense terrain again and again
    public void senseTerrain() throws GameActionException {
        MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(),rc.getType().visionRadiusSquared);
        nearestLeadDist = Integer.MAX_VALUE;
        nearestLead = null;
        MapLocation loc = rc.getLocation();
        for(MapLocation location:locations){
        	//lead: probably worth doing most if not every turn
            int lead = rc.senseLead(location);
            // queue bulk update for lead in Comms
            if(lead > 0){
                int val=1;
                if(lead>20)val = 2;
                comms.queueDenseMatrixUpdate(location.x, location.y, val, CommInfoBlock.LEAD_MAP);
                
                int distToMe = loc.distanceSquaredTo(location);
                if(distToMe < nearestLeadDist) {
                	nearestLead = location;
                	nearestLeadDist = distToMe;
                }
                lead2d[location.x][location.y] = lead;
            }
            // can also sense other things
            
            // rubble 
            //(should probably reduce frequency of this sensing to at most after this robot moves)
            //int rubble = rc.senseRubble(location);
            //rubble2d[location.x][location.y] = rubble;
            
            
            
        }

        comms.processUpdateQueue();

    }


}
