package defensivebot.bots;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.*;
import defensivebot.strategies.Comms;
import defensivebot.strategies.LocalInfo;
import defensivebot.utils.*;


import static defensivebot.utils.LogUtils.printDebugLog;
import static defensivebot.utils.LogUtils.printVerboseLog;

public abstract class Robot {

    public final RobotController rc;
    public Team team;
	public Team enemyTeam;
	public RobotType type;
	public RobotInfo[] sensedRobots;
    public int roundNum;
    public MapLocation currentLocation;
    public int turnCount = 0;
	protected LocalInfo localInfo;
	protected Comms comms;
    

    public Robot(RobotController rc) throws GameActionException{
        this.rc = rc;
        team = rc.getTeam();
		enemyTeam = team.opponent();
		type = rc.getType();
        //get and store anomaly schedule
		comms = new Comms(rc);
		localInfo = new LocalInfo(rc,comms);
    }

    // factory method for robots
    public static Robot getRobot(RobotController rc) throws GameActionException {
        switch (rc.getType()){
            case SAGE:return new Sage(rc);
            case MINER:return new Miner(rc);
            case SOLDIER:return new Soldier(rc);
            case BUILDER: return new Builder(rc);
            case ARCHON:return new Archon(rc);
            case LABORATORY: return new Lab(rc);
            case WATCHTOWER: return new WatchTower(rc);
            default: throw new RuntimeException("unidentified robot type");
        }
    }

    public void runRobot() throws GameActionException{
        // common code for all robots
        turnCount++;
		//sense();
        roundNum = rc.getRoundNum();
        sensedRobots = rc.senseNearbyRobots();
        currentLocation = rc.getLocation();
        executeRole();
        verbose("bytecode remaining: "+ Clock.getBytecodesLeft());
    }

	// sensing
	public void sense() throws GameActionException{
		localInfo.senseRobots();
		localInfo.senseTerrain();
	}
    
    /*
     * returns MapLocation which is closest to this robot and null if MapLocations are not valid.
     * 
     * TODO remove strict null testing is unnecessary 
     */
    public MapLocation getClosest(MapLocation a, MapLocation b) {
		if(a == null && b == null)
			return null;
		
		if(a == null)
			return b;
		
		if(b == null)
			return a;
		
		if(currentLocation.distanceSquaredTo(a) < currentLocation.distanceSquaredTo(b))
			return a;
		
		return b;
	}
    
    
    /*
     * returns a Direction with lowest rubble that moves this robot toward
     * 
     * TODO improve path-finding here
     * could calculate moves/round of each path and prefer center by about 1.4
     */
    public Direction getBestValidDirection(Direction toward) throws GameActionException{
		MapLocation ac = currentLocation.add(toward);
		MapLocation al = currentLocation.add(toward.rotateLeft());
		MapLocation ar = currentLocation.add(toward.rotateRight());
		int passC = 0;
		int passL = 0;
		int passR = 0;
		boolean canMoveC = rc.canMove(toward);
		boolean canMoveL = rc.canMove(toward.rotateLeft());
		boolean canMoveR = rc.canMove(toward.rotateRight());
		if(rc.canSenseLocation(ac)) {
			passC = (int)(100*rc.senseRubble(ac));
		}
		if(rc.canSenseLocation(al)) {
			passL = (int)(100*rc.senseRubble(al));
		}
		if(rc.canSenseLocation(ar)) {
			passR = (int)(100*rc.senseRubble(ar));
		}
		if(canMoveC && canMoveL && canMoveR){
			if(passC >= passL && passC >= passR) {
				return toward;
			}else if(passL >= passR) {
				return toward.rotateLeft();
			}
			return toward.rotateRight();
		}
		if(canMoveC && canMoveL){
			if(passC >= passL) {
				return toward;
			}
			return toward.rotateLeft();
		}
		if(canMoveC && canMoveR){
			if(passC >= passR) {
				return toward;
			}
			return toward.rotateRight();
		}
		if(canMoveR && canMoveL) {
			if(passL >= passR) {
				return toward.rotateLeft();
			}
			return toward.rotateRight();
		}
		if(canMoveC) {
			return toward;
		}
		if(canMoveL) {
			return toward.rotateLeft();
		}
		if(canMoveR) {
			return toward.rotateRight();
		}
		if(rc.canMove(toward.rotateRight().rotateRight())) {
			return toward.rotateRight().rotateRight();
		}
		if(rc.canMove(toward.rotateLeft().rotateLeft())) {
			return toward.rotateLeft().rotateLeft();
		}
		
		return null;
	}
    /*
     * returns a Direction with lowest rubble that moves this robot toward-ish the MapLocation target
     * 
     * TODO increase the depth of this search to include all of the robot's vision?
     */
	public Direction getBestValidDirection(MapLocation target) throws GameActionException{
		if(target == null) {
			double lowestRubble = 100;
			Direction lowestRubbleDirection = Direction.SOUTH;
			MapLocation tempLocation = null;
			for(Direction dir : Constants.directions) {
				tempLocation = currentLocation.add(dir);
				if(rc.canSenseLocation(tempLocation) && rc.senseRubble(tempLocation) > lowestRubble && rc.canMove(dir)) {
					lowestRubbleDirection = dir;
					lowestRubble = rc.senseRubble(tempLocation);
				}
			}
			return lowestRubbleDirection;
		}
		
		return getBestValidDirection(currentLocation.directionTo(target));
		
	}

    protected void debug(String msg){
        printDebugLog(rc,turnCount,msg);
    }



    protected void verbose(String msg){
        printVerboseLog(rc,turnCount,msg);
    }

    public abstract void executeRole() throws GameActionException;

}
