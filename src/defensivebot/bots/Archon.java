package defensivebot.bots;

import java.util.Random;

import battlecode.common.*;
import defensivebot.datasturctures.CustomSet;
import defensivebot.enums.SparseSignalType;
import defensivebot.models.SparseSignal;
import defensivebot.utils.*;


import static defensivebot.utils.Constants.UNITS_AVAILABLE;

public class Archon extends Robot{

    public static final Random rng = new Random(6147);

    static int[] unitCounts = new int[UNITS_AVAILABLE];
    private boolean enemySpotted = false;

    // build order
    final int INITIAL_MINERS_TO_BUILD_ROUNDS;
    final int EARLY_GAME_ROUNDS;

    MapLocation nearestCorner;
    private int tempCounter = 0;

    public Archon(RobotController rc) throws GameActionException  {
        super(rc);
        // TODO: decide based on map dimensions?
        INITIAL_MINERS_TO_BUILD_ROUNDS = 5;
        EARLY_GAME_ROUNDS = 50;

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
    public void sense() throws GameActionException {
        // TODO: check bytecode. This should not be required all the time
        localInfo.senseRobots(false);
        localInfo.senseLead(false);
//        localInfo.senseTerrain();
    }

    @Override
    public void executeRole() throws GameActionException {
        // for debugging

//        if(rc.getRoundNum()>80)rc.resign();
        Direction dir = Constants.directions[rng.nextInt(Constants.directions.length)];
        RobotType toBuild = RobotType.MINER;
//        printDebugLog("exploration index: "+comms.explorationIndex());

        CustomSet<SparseSignal> sparseSignals = comms.querySparseSignals();

        sparseSignals.initIteration();
        SparseSignal next = sparseSignals.next();
        while (next != null){
            if(next.type == SparseSignalType.ENEMY_SPOTTED)enemySpotted = true;
            next = sparseSignals.next();
        }

        // act as if enemy is spotted
        if(roundNum > 1000)enemySpotted = true;

        if(enemySpotted && tempCounter%5 != 0){
            toBuild = RobotType.SOLDIER;
        }

        if(rc.getMapHeight() > 40 && rc.getMapWidth() > 40 && Math.random() < 0.05) toBuild = RobotType.BUILDER;

        // testing this strat
//        if(turnCount<INITIAL_MINERS_TO_BUILD_ROUNDS){
//            toBuild = RobotType.MINER;
//        }else if(turnCount<EARLY_GAME_ROUNDS){
//            if(turnCount%2==0)toBuild = RobotType.MINER;
//            else toBuild = RobotType.SOLDIER;
//        }else{
//            // decide based on unit counts and resources
//            if(rc.getTeamLeadAmount(team)>200 && unitCounts[RobotType.BUILDER.ordinal()]<2){
//                toBuild = RobotType.BUILDER;
//            }else if(unitCounts[RobotType.WATCHTOWER.ordinal()]<4){
//                comms.signalUnitSubType(DroidSubType.BUILDER_FOR_WATCHTOWER,getLocationForWatchTower());
//            }
//        }

//        if(unitCounts[RobotType.MINER.ordinal()] > 50)toBuild = RobotType.SOLDIER;

//        if(unitCounts[RobotType.SOLDIER.ordinal()] > 30)toBuild = RobotType.SAGE;

        // testing
//        comms.signalUnitSubType(DroidSubType.MINER_ECO,rc.getLocation());

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

    @Override
    public void move() throws GameActionException {

    }


}
