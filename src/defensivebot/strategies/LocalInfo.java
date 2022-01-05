package defensivebot.strategies;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import defensivebot.enums.CommInfoBlock;

import static defensivebot.utils.Constants.UNITS_AVAILABLE;

public class LocalInfo {

    private RobotController rc;
    private Comms comms;
    public int[] friendlyUnitCounts;
    public int[] enemyUnitCounts;
    public int nearestEnemyDist,nearestFriendDist;
    public RobotInfo nearestEnemy,nearestFriend;


    public LocalInfo(RobotController rc,Comms comms){
        this.rc=rc;
        this.comms=comms;
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

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        MapLocation loc = rc.getLocation();
        for(RobotInfo robot: nearbyRobots){
            // we need enemy units, friendly unit counts etc.
            MapLocation robLoc = robot.getLocation();
            int distToMe = loc.distanceSquaredTo(robLoc);
            if(robot.getTeam() == rc.getTeam()){
                friendlyUnitCounts[robot.getType().ordinal()]++;

                if(distToMe<nearestFriendDist){
                    nearestFriendDist = distToMe;
                    nearestFriend = robot;
                }
            }else{
                enemyUnitCounts[robot.getType().ordinal()]++;

                if(distToMe<nearestEnemyDist){
                    nearestEnemyDist = distToMe;
                    nearestEnemy = robot;
                }
            }


        }
    }

    // need to be careful while sensing terrain. the way I see it, we don't need to sense terrain again and again
    public void senseTerrain() throws GameActionException {
        MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(),rc.getType().visionRadiusSquared);
        for(MapLocation location:locations){

            int lead = rc.senseLead(location);
            // queue bulk update for lead in Comms
            if(lead > 0){
                int val=1;
                if(lead>20)val = 2;
                comms.queueDenseMatrixUpdate(location.x, location.y, val, CommInfoBlock.LEAD_MAP);
            }
            // can also sense other things
        }

        comms.processUpdateQueue();

    }


}
