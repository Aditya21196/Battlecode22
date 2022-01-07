package defensivebot.bots;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Soldier extends Robot{
    public Soldier(RobotController rc) throws GameActionException  {
        super(rc);
    }

    @Override
    public void sense() throws GameActionException {
        localInfo.senseTerrain();
        localInfo.senseRobots();
    }

    @Override
    public void executeRole() throws GameActionException {
        if(rc.isActionReady())
            attack();
    }

    public void attack() throws GameActionException{
        if(localInfo.nearestEnemyDist <= RobotType.SOLDIER.actionRadiusSquared)
            rc.attack(localInfo.nearestEnemy.getLocation());
    }

    @Override
    public void move() throws GameActionException {
        // basic soldier: go to enemy, run from home archon
        Direction bestDirection = null;
        if(localInfo.nearestEnemy != null){
            bestDirection = getBestValidDirection(currentLocation.directionTo(localInfo.nearestEnemy.getLocation()));
        } else if(localInfo.homeArchon != null){
            bestDirection = getBestValidDirection(localInfo.homeArchon.getLocation().directionTo(currentLocation));
            rc.setIndicatorString("Trying to run from home archon.");

        }

        if(bestDirection != null && rc.canMove(bestDirection)) {
            rc.move(bestDirection);
        }

    }


}
