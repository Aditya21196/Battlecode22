package defensivebot.bots;

import java.util.Random;

import battlecode.common.*;
import defensivebot.enums.DroidSubType;
import defensivebot.enums.SparseSignalType;
import defensivebot.models.SparseSignal;
import defensivebot.utils.*;


import static defensivebot.utils.Constants.UNITS_AVAILABLE;

public class Archon extends Robot{

    static final Random rng = new Random(6147);

    static int[] unitCounts = new int[UNITS_AVAILABLE];

    // build order
    final int INITIAL_MINERS_TO_BUILD_ROUNDS;
    final int EARLY_GAME_ROUNDS;

    MapLocation nearestCorner;


    public Archon(RobotController rc) throws GameActionException  {
        super(rc);
        // TODO: decide based on map dimensions?
        INITIAL_MINERS_TO_BUILD_ROUNDS = 5;
        EARLY_GAME_ROUNDS = 50;
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
    public void sense() throws GameActionException {
        // TODO: check bytecode. This should not be required all the time
        localInfo.senseRobots();
        localInfo.senseTerrain();

    }

    @Override
    public void executeRole() throws GameActionException {

        // for debugging
        if(rc.getRoundNum()>20)rc.resign();

        Direction dir = Constants.directions[rng.nextInt(Constants.directions.length)];
        RobotType toBuild = RobotType.SAGE;
        switch (0/*turnCount%2*/){
            case 1:
                toBuild = RobotType.SOLDIER;
                break;
            case 0:
                toBuild = RobotType.MINER;
                break;
        }

        // for testing
        //comms.queueSparseSignalUpdate(test);

        // testing this strat
        if(turnCount<INITIAL_MINERS_TO_BUILD_ROUNDS){
            toBuild = RobotType.MINER;
        }else if(turnCount<EARLY_GAME_ROUNDS){
            if(turnCount%2==0)toBuild = RobotType.MINER;
            else toBuild = RobotType.SOLDIER;
        }else{
            // decide based on unit counts and resources
            if(rc.getTeamLeadAmount(team)>200 && unitCounts[RobotType.BUILDER.ordinal()]<2){
                toBuild = RobotType.BUILDER;
            }else if(unitCounts[RobotType.WATCHTOWER.ordinal()]<4){
                comms.signalUnitSubType(DroidSubType.BUILDER_FOR_WATCHTOWER,getLocationForWatchTower());
            }
        }

        if(unitCounts[RobotType.MINER.ordinal()] > 50)toBuild = RobotType.SOLDIER;
//        if(unitCounts[RobotType.SOLDIER.ordinal()] > 30)toBuild = RobotType.SAGE;

        // testing
        comms.signalUnitSubType(DroidSubType.MINER_ECO,rc.getLocation());

        rc.setIndicatorString("Trying to build a: "+toBuild);
        if (rc.canBuildRobot(toBuild, dir)) {
            rc.buildRobot(toBuild, dir);
            unitCounts[toBuild.ordinal()]++;
        }
    }

    public MapLocation getLocationForWatchTower(){
        // if we know enemy Archon, act on that

        // or else
        Direction dir = currentLocation.directionTo(nearestCorner).opposite();

        // TODO: make this distance dynamic
        return new MapLocation(currentLocation.x + dir.dx*10,currentLocation.y + dir.dy*10);
    }

    @Override
    public void move() throws GameActionException {

    }


}
