package gabot3.strategies;

import battlecode.common.*;
import gabot3.datasturctures.LinkedList;

import static gabot3.utils.PathFindingConstants.*;

// TODO: needs path caching, backtracking prevention, bytecode checks, integration with local info, persistent pathing to avoid enemy damaging units for miners
public class Pathfinding {

    private final RobotController rc;
    MapLocation prevTarget=null,prevResponse=null;
    int prevTargetRubble = 100;
    LinkedList<MapLocation> prevTargetCache = null;
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
        MapLocation currentLocation = rc.getLocation();
        MapLocation localTarget = null;

        if(!rc.canSenseLocation(target)){

            if(target == prevTarget && prevResponse != null && rc.canSenseLocation(prevResponse)){
                localTarget = prevResponse;
            }else{
                // find angle to target. Then, find corresponding index in array
                int itrIdx = (int)(Math.toDegrees(Math.atan2(target.y-currentLocation.y,target.x-currentLocation.x))/11.25)-VISION_RADIUS;
                if(itrIdx<0)itrIdx += 32;
                MapLocation consider;
                double dist;
                double minDist = Double.MAX_VALUE;
                for(int i=2*VISION_RADIUS+VISION_RADIUS_BIAS;--i>=0;){
                    if(itrIdx>=32)itrIdx -= 32;
                    consider = currentLocation.translate(BORDER20[itrIdx][0],BORDER20[itrIdx][1]);
                    // TODO: Should we ignore occupying unit? When?
                    if(!rc.onTheMap(consider) || rc.canSenseRobotAtLocation(consider))continue;
                    dist = target.distanceSquaredTo(consider) + RUBBLE_SCORE_MULTIPLIER*rc.senseRubble(consider);
                    if(dist<minDist){
                        localTarget = consider;
                        minDist = dist;
                    }
                    // code to find local target
                    itrIdx++;
                }
                if(localTarget!=null && rc.senseRubble(localTarget)<CACHING_RUBBLE_LIMIT) {
                    prevTarget = target;
                    prevResponse = localTarget;
                }
            }
        }

        if(localTarget == null)localTarget = target;

        // TODO: move this check inside function?
        if(Clock.getBytecodesLeft() > MIN_AFFORD_PATH_OPT){
            localTarget = findBetterLocalTarget(localTarget);
        }

        greedyMoveTowards(localTarget);


        // greedily go to the target

    }

    // unrolled loop.
    // pass target as null if it isn't a local target
    private void greedyMoveTowards(MapLocation target) throws GameActionException {

        MapLocation currentLocation = rc.getLocation();
        Direction dir = currentLocation.directionTo(target);
        MapLocation consider;
        Direction idealDir=null;
        int minRubble = Integer.MAX_VALUE;
        int rubble;

//        int curD = 0;
        int curD = currentLocation.distanceSquaredTo(target);
        if(curD<=5){
            if(rc.canMove(dir))rc.move(dir);
            return;
        }
        // check the direction
        if(rc.canMove(dir)){
            consider = currentLocation.add(dir);
            if(consider.distanceSquaredTo(target)<curD){
                minRubble = rc.senseRubble(consider);
                if(consider == target || minRubble == 0){
                    rc.move(dir);
                    return;
                }
                idealDir = dir; // variable already assigned to this value
            }
        }


        // check left
        Direction considerDir = dir.rotateLeft();

        if(rc.canMove(considerDir)){
            consider = currentLocation.add(considerDir);
            // check only strictly closer targets
            if(consider.distanceSquaredTo(target)<curD){
                rubble = rc.senseRubble(consider);
                if(consider == target || rubble == 0){
                    rc.move(considerDir);
                    return;
                }
                if(rubble<minRubble){
                    minRubble = rubble;
                    idealDir = considerDir;
                }
            }
        }

        considerDir = dir.rotateRight();

        if(rc.canMove(considerDir)){
            consider = currentLocation.add(considerDir);
            if(consider.distanceSquaredTo(target)<curD){
                rubble = rc.senseRubble(consider);
                if(consider == target || rubble == 0){
                    rc.move(considerDir);
                    return;
                }
                if(rubble<minRubble){
                    minRubble = rubble;
                    idealDir = considerDir;
                }
            }
        }

        if(idealDir!=null)rc.move(idealDir);
        else{
            // bug nav
            if(bugRight){
                considerDir = dir.rotateRight().rotateRight();
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
        Direction dirFrom = null;
        MapLocation loc = rc.getLocation();
        for (int i=NUM_BETTER_LOC_ITERATIONS; --i>=0;) {
            if(target.distanceSquaredTo(loc)<=2)return target;
            // TODO: decide bytecode limit
//            if (Clock.getBytecodesLeft()<1500) {break;}
            Direction bestDir = null;
            double bestCost = Double.MAX_VALUE;
            for (Direction dir:Direction.values()) {
                if(dir == dirFrom)continue;
                MapLocation adjacent = target.add(dir);
                if (!rc.canSenseLocation(adjacent))continue;
                int d=adjacent.distanceSquaredTo(loc);
                double cost = RUBBLE_SCORE_LOCAL_MULTIPLIER*rc.senseRubble(adjacent) + adjacent.distanceSquaredTo(loc);
                if (cost<bestCost) {
                    bestDir = dir;
                    bestCost = cost;
                }
            }
            if(bestDir == null)return target;
            // no need to check this direction again
            dirFrom = bestDir.opposite();
            target = target.add(bestDir);
        }
        return target;
    }

    public void moveAwayFrom(MapLocation target,int threatLevel) throws GameActionException {
        // TODO: improve this to prevent backtracking and integrate threat level to improve run away
        MapLocation loc = rc.getLocation();
        greedyMoveTowards(target.translate(loc.x-target.x,loc.y-target.y));
        // find tiles in direction opposite to target

        // find the one easiest to reach

    }

}

