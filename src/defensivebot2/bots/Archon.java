package defensivebot2.bots;

import java.util.Random;

import battlecode.common.*;
import defensivebot2.datasturctures.CustomSet;
import defensivebot2.enums.SparseSignalType;
import defensivebot2.models.SparseSignal;
import defensivebot2.utils.*;


import static defensivebot2.utils.Constants.UNITS_AVAILABLE;

public class Archon extends Robot{

    public static final Random rng = new Random(6147);

    static int[] unitCounts = new int[UNITS_AVAILABLE];
    private boolean enemySpotted = false;
    private boolean reportedCurrentLocation = false;
    private boolean reportedDangerFlag = false;

    // build order
//    final int INITIAL_MINERS_TO_BUILD_ROUNDS;
//    final int EARLY_GAME_ROUNDS;

    MapLocation nearestCorner;
    private int tempCounter = 0;

    public Archon(RobotController rc) throws GameActionException  {
        super(rc);
        // TODO: decide based on map dimensions?
//        INITIAL_MINERS_TO_BUILD_ROUNDS = 5;
//        EARLY_GAME_ROUNDS = 50;

    }

    public void initNearestCorner(){
        nearestCorner = bottomLeft;
        int minDist = bottomLeft.distanceSquaredTo(currentLocation);
        int dist = bottomRight.distanceSquaredTo(currentLocation);
        if(dist<minDist){
            nearestCorner = bottomRight;
            minDist = dist;
        }
        dist = topRight.distanceSquaredTo(currentLocation);
        if(dist<minDist){
            nearestCorner = topRight;
            minDist = dist;
        }dist = topLeft.distanceSquaredTo(currentLocation);
        if(dist<minDist){
            nearestCorner = topLeft;
        }
    }

    @Override
    public void executeRole() throws GameActionException {
        
    	localInfo.senseRobots(false,true);
        localInfo.senseLead(false);

        // TODO: test this
        if(!reportedCurrentLocation){
            comms.queueSparseSignalUpdate(
                    new SparseSignal(
                            SparseSignalType.ARCHON_LOCATION,
                            currentLocation,
                            -1,
                            0
                    )
            );
            reportedCurrentLocation = true;
        }

        boolean currentlyInDanger = rc.getHealth() < RobotType.ARCHON.getMaxHealth(rc.getLevel()) && localInfo.nearestEnemy != null;

        if(currentlyInDanger && !reportedDangerFlag){
            // signal for danger
            comms.queueSparseSignalUpdate(
                    new SparseSignal(
                            SparseSignalType.ARCHON_LOCATION,
                            currentLocation,
                            -1,
                            2
                    )
            );
            reportedDangerFlag = true;
        }else if(!currentlyInDanger && reportedDangerFlag){
            // signal I am okay
            comms.queueSparseSignalUpdate(
                    new SparseSignal(
                            SparseSignalType.ARCHON_LOCATION,
                            currentLocation,
                            -1,
                            0
                    )
            );
            reportedDangerFlag = false;
        }

//        if(rc.getRoundNum()>80)rc.resign();
        

       // CustomSet<SparseSignal> sparseSignals = comms.querySparseSignals();

//        sparseSignals.initIteration();
//        SparseSignal next = sparseSignals.next();
//        while (next != null){
//            if(next.type == SparseSignalType.ENEMY_SPOTTED)enemySpotted = true;
//            next = sparseSignals.next();
//        }
//
//        // act as if enemy is spotted
	        

        Direction dir = Constants.directions[rng.nextInt(Constants.directions.length)];
        RobotType toBuild; 
        if(tempCounter%10 < 5){
            toBuild = RobotType.MINER;
        }else if(tempCounter %10 < 9) {
        	toBuild = RobotType.SOLDIER;
        }else {
        	toBuild = RobotType.BUILDER;
        }
        if(roundNum > 400 && roundNum < 500) {
        	//rc.resign();
        	toBuild = null;
        }


        if (toBuild!=null && rc.canBuildRobot(toBuild, dir)) {
            rc.buildRobot(toBuild, dir);
            unitCounts[toBuild.ordinal()]++;
            tempCounter++;
        }
    }

    public MapLocation getLocationForWatchTower(){
        // if we know enemy Archon, act on that
        if(nearestCorner == null)initNearestCorner();
        // or else
        Direction dir = currentLocation.directionTo(nearestCorner).opposite();

        // TODO: make this distance dynamic
        return new MapLocation(currentLocation.x + dir.dx*10,currentLocation.y + dir.dy*10);
    }



}
