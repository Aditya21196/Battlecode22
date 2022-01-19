package gabot1.bots;

import battlecode.common.*;
import gabot1.strategies.Comms;
import gabot1.strategies.LocalInfo;
import gabot1.strategies.Pathfinding;
import gabot1.utils.*;


import static gabot1.utils.Constants.EXTRA_BYTECODE_FOR_COMMS_CLEANUP;
import static gabot1.utils.LogUtils.printVerboseLog;
import static gabot1.utils.PathFindingConstants.SOLDIER_PATHFINDING_LIMIT;

public abstract class Robot {

    public final RobotController rc;
    public Team team;
	public Team enemyTeam;
	public RobotType type;
    public static int roundNum;
    public MapLocation currentLocation;
    public static int turnCount;
	protected LocalInfo localInfo;
	protected Comms comms;
	MapLocation bottomLeft;
	MapLocation bottomRight;
	MapLocation topLeft;
	MapLocation topRight;
	protected int height;
	protected int width;

	public AnomalyScheduleEntry[] anomalies;
	public int anomalyIndex;

	protected Pathfinding pathfinding;


    

    public Robot(RobotController rc) throws GameActionException{
        this.rc = rc;
        team = rc.getTeam();
		enemyTeam = team.opponent();
		type = rc.getType();
        anomalies = rc.getAnomalySchedule();
        anomalyIndex = 0;
		comms = new Comms(rc);
		localInfo = new LocalInfo(rc,comms);
		turnCount = 0;
		height = rc.getMapHeight();
		width = rc.getMapWidth();
		bottomLeft = new MapLocation(0,0);
		bottomRight = new MapLocation(0,width-1);
		topLeft = new MapLocation(height-1,0);
		topRight = new MapLocation(height-1,width-1);
		pathfinding = new Pathfinding(rc);
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
        roundNum = rc.getRoundNum();
        currentLocation = rc.getLocation();

		executeRole();
		verbose("bytecode remaining after acting: "+ Clock.getBytecodesLeft());

		//verbose("lead count: "+rc.getTeamLeadAmount(team));

		localInfo.checkExploration();
//		localInfo.checkEnemySpotted();

		localInfo.checkArchonSpotted();
		comms.processUpdateQueues();

		verbose("bytecode remaining after comms: "+ Clock.getBytecodesLeft());

		// TODO: decide byte code limit for cleaning dynamically?
		//if(comms.isSignalArrayFull && Clock.getBytecodesLeft()<EXTRA_BYTECODE_FOR_COMMS_CLEANUP)comms.cleanComms();
      	// TODO: more stuff for spare bytecode utilization?
    }

    public MapLocation getNearestCorner(MapLocation loc){
        MapLocation nearestCorner = bottomLeft;
        int minDist = bottomLeft.distanceSquaredTo(loc);
        int dist = bottomRight.distanceSquaredTo(loc);
        if(dist<minDist){
            nearestCorner = bottomRight;
            minDist = dist;
        }
        dist = topRight.distanceSquaredTo(loc);
        if(dist<minDist){
            nearestCorner = topRight;
            minDist = dist;
        }dist = topLeft.distanceSquaredTo(loc);
        if(dist<minDist){
            nearestCorner = topLeft;
        }
        return nearestCorner;
    }
    
    public AnomalyScheduleEntry getNextAnomaly() {
    	while(anomalyIndex < anomalies.length) {
    		if(anomalies[anomalyIndex].roundNumber > rc.getRoundNum()) {
    			return anomalies[anomalyIndex];
    		}
    		anomalyIndex++;
    	}
    	return null;
    }
    public void tryMove(Direction dir) throws GameActionException {
    	if(dir!=null && rc.canMove(dir)) {
			rc.move(dir);
		}
    }
    
    public void moveToward(MapLocation target) throws GameActionException {
    	if(rc.isMovementReady() && !rc.getLocation().equals(target)) {
			int bc = Clock.getBytecodesLeft();
			if(bc>SOLDIER_PATHFINDING_LIMIT){
				pathfinding.moveTowards(target,false);rc.setIndicatorString("best task loc: "+target);
			}else moveToward(target);rc.setIndicatorString("best task loc: "+target);

		}
    }
    
    public void moveAway(MapLocation toAvoid) throws GameActionException {
    	if(rc.isMovementReady()) {
    		tryMove(getBestValidDirection(toAvoid.directionTo(rc.getLocation())));
    	}
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
     * TODO: observation seems to tell me this function (or the random heading) pull units north east ...
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

		// temporary fix
		if(rc.canSenseLocation(ac)) {
			passC = 1000/(rc.senseRubble(ac)+1);
		}
		if(rc.canSenseLocation(al)) {
			passL = 1000/(rc.senseRubble(al)+1);
		}
		if(rc.canSenseLocation(ar)) {
			passR = 1000/(rc.senseRubble(ar)+1);
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

    protected void verbose(String msg){
        printVerboseLog(msg);
    }

    public abstract void executeRole() throws GameActionException;

}
