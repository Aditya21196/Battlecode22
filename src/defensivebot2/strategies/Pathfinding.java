package defensivebot2.strategies;

import battlecode.common.*;
import static defensivebot2.utils.PathFindingConstants.*;

// TODO: needs path caching, backtracking prevention, bytecode checks, integration with local info, persistent pathing to avoid enemy damaging units for miners
public class Pathfinding {

    private RobotController rc;
    MapLocation prevTarget=null;
    MapLocation prevPrevTarget=null;
    boolean bugRight = true;
    int persistentTargeting=0;
    Direction persistentDirection=null;

    public Pathfinding(RobotController rc){
        // TODO: move all such things to local info
        this.rc=rc;
        if(rc.getID()%2==0)bugRight=false;
    }

    public void moveTowards(MapLocation target,boolean preventBacktracking) throws GameActionException {
        // check if path to target is cached which isn't more than 5 turns old
        // TODO: consider using LRU cache for more backtracking
        if(target == prevTarget){

        }
        if(target == prevPrevTarget){

        }

        if(preventBacktracking){
            // check if next movement is opposite to previous movement. If yes, use old target

        }

        // find angle to target. Then, find corresponding index in array
        MapLocation currentLocation = rc.getLocation();
        int itrIdx = (int)(Math.toDegrees(Math.atan2(target.y-currentLocation.y,target.x-currentLocation.x))/11.25)-8;
        if(itrIdx<0)itrIdx += 32;

        MapLocation localTarget = null;
        MapLocation consider;
        double dist;
        double minDist = Double.MAX_VALUE;
        for(int i=16;--i>=0;){
            if(itrIdx>=32)itrIdx -= 32;
            consider = currentLocation.translate(BORDER20[itrIdx][0],BORDER20[itrIdx][1]);
            // TODO: Should we ignore occupying unit? When?
            if(!rc.onTheMap(consider) || !rc.isLocationOccupied(consider))continue;
            dist = consider.distanceSquaredTo(consider) + RUBBLE_SCORE_MULTIPLIER*rc.senseRubble(consider);
            if(dist<minDist){
                localTarget = consider;
                minDist = dist;
            }
            // code to find local target
            itrIdx++;
        }

        // TODO: move this check inside function?
        if(Clock.getBytecodesLeft() > MIN_AFFORD_PATH_OPT){
            localTarget = findBetterLocalTarget(localTarget);
        }

        Direction dir;
        if(localTarget != null){
            dir = currentLocation.directionTo(localTarget);
        }else dir = currentLocation.directionTo(target);

        // greedily go to the target
        greedyMoveTowards(dir,target);
    }

    // unrolled loop.
    // pass target as null if it isn't a local target
    private void greedyMoveTowards(Direction dir,MapLocation target) throws GameActionException {

        MapLocation consider;
        MapLocation currentLocation = rc.getLocation();
        Direction considerDir = dir.rotateLeft();
        Direction idealDir = considerDir;
        int minRubble = Integer.MAX_VALUE;
        int rubble;

        if(rc.canMove(considerDir)){
            consider = currentLocation.add(considerDir);
            if(consider == target && rc.canMove(considerDir)){
                rc.move(considerDir);
                return;
            }
            minRubble = rc.senseRubble(consider);
//            idealDir = considerDir; // variable already assigned to this value
        }

        considerDir = considerDir.rotateRight();

        if(rc.canMove(considerDir)){
            consider = currentLocation.add(considerDir);
            if(consider == target && rc.canMove(considerDir)){
                rc.move(considerDir);
                return;
            }
            rubble = rc.senseRubble(consider);
            if(rubble<minRubble){
                minRubble = rubble;
                idealDir = considerDir;
            }
        }

        considerDir = considerDir.rotateRight();

        if(rc.canMove(considerDir)){
            consider = currentLocation.add(considerDir);
            if(consider == target && rc.canMove(considerDir)){
                rc.move(considerDir);
                return;
            }
            rubble = rc.senseRubble(consider);
            if(rubble<minRubble){
                minRubble = rubble;
                idealDir = considerDir;
            }
        }

        if(minRubble>0)rc.move(idealDir);
        else{
            // bug nav
            if(bugRight){
                considerDir = considerDir.rotateRight();
            }else{
                considerDir = dir.rotateLeft().rotateLeft();
            }
            if(rc.canMove(considerDir))rc.move(considerDir);
            else {
                // bugging in this direction has failed. change bug direction
                bugRight = !bugRight;
            }
        }
    }

    private MapLocation findBetterLocalTarget(MapLocation target) throws GameActionException {
        // check if it is a local target
        if (!rc.canSenseLocation(target)) {return target;}
        for (int i=0; i<NUM_BETTER_LOC_ITERATIONS; i++) {
            // TODO: decide bytecode limit
//            if (Clock.getBytecodesLeft()<1500) {break;}
            Direction bestDir = null;
            double bestCost = Integer.MAX_VALUE;
            for (Direction dir:Direction.values()) {
                MapLocation adjacent = target.add(dir);
                if (!rc.canSenseLocation(adjacent))continue;
                double cost = RUBBLE_SCORE_MULTIPLIER*rc.senseRubble(adjacent) + adjacent.distanceSquaredTo(target);
                if (cost<bestCost) {
                    bestDir = dir;
                    bestCost = cost;
                }
            }
            if(bestDir == null)return target;
            target = target.add(bestDir);
        }
        return target;
    }

    public void moveAwayFrom(MapLocation target,int threatLevel) throws GameActionException {
        // TODO: improve this to prevent backtracking and integrate threat level to improve run away
        greedyMoveTowards(target.directionTo(rc.getLocation()),null);
        // find tiles in direction opposite to target

        // find the one easiest to reach

    }

}

