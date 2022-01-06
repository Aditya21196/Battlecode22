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
    public RobotInfo nearestEnemy, nearestFriend;
    public RobotInfo[] nearestFR; //nearest friendly robots of each type
    public int[] nearestFRDist; //nearest friendly robots' distances(of each type)
    public RobotInfo[] nearestER; //nearest enemy robots of each type
    public int[] nearestERDist; //nearest enemy robots' distances(of each type)
    public int[][] lead2d;
    public int[][] rubble2d;
    
    public int[][] enemy2d;
    public int[][] friend2d;
    public int[][] robot2d;
    
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

        nearestFR = new RobotInfo[UNITS_AVAILABLE];
        nearestFRDist = new int[UNITS_AVAILABLE];
        nearestER = new RobotInfo[UNITS_AVAILABLE];
        nearestERDist = new int[UNITS_AVAILABLE];
        for(int i = nearestFRDist.length; --i>=0;) {
        	nearestFRDist[i] = Integer.MAX_VALUE;
        	nearestERDist[i] = Integer.MAX_VALUE;
        }
        enemy2d = new int[rc.getMapWidth()][rc.getMapHeight()];
        friend2d = new int[rc.getMapWidth()][rc.getMapHeight()];
        robot2d = new int[rc.getMapWidth()][rc.getMapHeight()];

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        MapLocation loc = rc.getLocation();
        for(RobotInfo robot: nearbyRobots){
            // we need enemy units, friendly unit counts etc.
            MapLocation robLoc = robot.getLocation();
            int distToMe = loc.distanceSquaredTo(robLoc);
            int typeOrdinal = robot.getType().ordinal();
            robot2d[robLoc.x][robLoc.y] = typeOrdinal+1;
            if(robot.getTeam() == rc.getTeam()){
                friendlyUnitCounts[typeOrdinal]++;
                friend2d[robLoc.x][robLoc.y] = typeOrdinal+1;
                // find home archon upon spawning
                if(homeArchon == null && typeOrdinal == RobotType.ARCHON.ordinal())homeArchon = robot;

                if(distToMe<nearestFriendDist){
                    nearestFriendDist = distToMe;
                    nearestFriend = robot;
                }
                
                if(distToMe < nearestFRDist[typeOrdinal]) {
                	nearestFR[typeOrdinal] = robot;
                	nearestFRDist[typeOrdinal] = distToMe;
                }
                
            }else{
                enemyUnitCounts[robot.getType().ordinal()]++;
                enemy2d[robLoc.x][robLoc.y] = typeOrdinal+1;
                if(distToMe<nearestEnemyDist){
                    nearestEnemyDist = distToMe;
                    nearestEnemy = robot;
                }
                
                if(distToMe < nearestERDist[typeOrdinal]) {
                	nearestER[typeOrdinal] = robot;
                	nearestERDist[typeOrdinal] = distToMe;
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
            lead2d[location.x][location.y] = lead;
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
