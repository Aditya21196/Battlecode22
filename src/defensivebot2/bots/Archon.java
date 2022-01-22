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

    private Direction buildDir = Direction.NORTH;
    private MapLocation nearDamager;
    
    //comms locations
    MapLocation[] near = new MapLocation[4];
    double distFar = rc.getMapWidth()*rc.getMapWidth()+rc.getMapHeight()*rc.getMapHeight();

	//soldier threshold, distance to marked archon,distance to enemy,distance to lead
	// distance to unexplored, soldier count, miner count, team lead, nearestCorner
	private double[] soldierWeights ={-9.24,-1.07,-3.5,2.1,5.23,3.22,8.01,7.6,-1.1};

	//miner threshold, distance to marked archon, distance to enemy, distance to lead
	// distance to unexplored, soldier count, miner count, team lead, nearestCorner
	private double[] minerWeights ={6.62,-5.7,-1.88,-3.33,3.27,3.15,5.01,-0.39,-6.28};

    MapLocation nearestCorner;
    
    private boolean failedBuildAttempt = false;

	private MapLocation generalTarget = null;
	private MapLocation finalTarget;
	private int finalTargetRubble;

	private boolean settled = false;
    

    public Archon(RobotController rc) throws GameActionException  {
        super(rc);
        nearestCorner = getNearestCorner(rc.getLocation());//until archons move this is fine

    }

//    public void initNearestCorner(){
//        nearestCorner = bottomLeft;
//        int minDist = bottomLeft.distanceSquaredTo(currentLocation);
//        int dist = bottomRight.distanceSquaredTo(currentLocation);
//        if(dist<minDist){
//            nearestCorner = bottomRight;
//            minDist = dist;
//        }
//        dist = topRight.distanceSquaredTo(currentLocation);
//        if(dist<minDist){
//            nearestCorner = topRight;
//            minDist = dist;
//        }dist = topLeft.distanceSquaredTo(currentLocation);
//        if(dist<minDist){
//            nearestCorner = topLeft;
//        }
//    }

    @Override
    public void executeRole() throws GameActionException {
        
    	//sense
    	localInfo.senseRobots(false,true,false);
        localInfo.senseLead(true);
        
        
        //TODO replace with new comms?
        if(turnCount%4 == 0) {
        	SparseSignal ss = comms.getClosestArchonMarked();
        	if(ss != null) {
        		near[0] = ss.target;
        	}
        	
        }else if(turnCount%4 == 1) {
        	near[1] = comms.getNearestEnemyLoc();
        }else if(turnCount%4 == 2) {
        	near[2] = comms.getNearestLeadLoc();
        }else if(turnCount%4 == 3) {
        	near[3] = comms.getNearbyUnexplored();
        }

        
        //self report location
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

        nearDamager = localInfo.findNearestDamager();

        if(nearDamager != null && !reportedDangerFlag){
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
        }else if(nearDamager == null && reportedDangerFlag){
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
        
        tryBuildLocal();
        
        tryRepair();
        
        tryBuildFromComms();

        tryTransformPortable();
        
        tryTransformTurret();
        
        tryMove();

        
        
    }
    
    private void tryTransformTurret() throws GameActionException {
    	if(!rc.isMovementReady() || rc.getMode() == RobotMode.TURRET) return;
    	
    	if(finalTarget != null && finalTarget.equals(rc.getLocation()) && rc.canTransform()) {
    		rc.transform();
    		settled = true;
    	}
    	
		
	}

	private void tryMove() throws GameActionException {
    	if(!rc.isMovementReady() || rc.getMode() == RobotMode.TURRET) return;
		
    	if(finalTarget != null) {
    		updateFinalTarget();
    		pathfinding.moveTowards(finalTarget, false);rc.setIndicatorString("ft: "+finalTarget); 
    		return;
    	}
    	
    	generalTarget = getNearestFriendlyArchon();
    	
    	if(generalTarget.isWithinDistanceSquared(rc.getLocation(), Constants.CLOSE_RADIUS)) {
    		updateFinalTarget();
    		return;
    	}
    		
    	
    	if(generalTarget != null) {
    		pathfinding.moveTowards(generalTarget, false);
    		return;
    	}
    	
    	
	}

	private void updateFinalTarget() throws GameActionException {
		//final target is still good keep it
		if(finalTarget != null && !rc.canSenseRobotAtLocation(finalTarget) && finalTargetRubble < Constants.ARCHON_LOW_RUBBLE) {
			return;
		}
		
		//finalTarget is compromised or hasn't been searched for
		int lowRubble = Integer.MAX_VALUE;
		MapLocation lowLoc = null;
		
		MapLocation[] visable = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), rc.getType().visionRadiusSquared);
		
		int tempRubble;
		for(int i = 0; i < visable.length; i+=3) {
			if(rc.canSenseRobotAtLocation(visable[i]))
				continue;
			
			tempRubble = rc.senseRubble(visable[i]);
			if(tempRubble < lowRubble) {
				if(tempRubble < Constants.ARCHON_LOW_RUBBLE) {
					finalTarget = visable[i];
					finalTargetRubble = tempRubble;
					return;
				}
				lowLoc = visable[i];
				lowRubble = tempRubble;
			}
		}
		
		finalTarget = lowLoc;
		finalTargetRubble = lowRubble;
		
		return;
	}

	private void tryTransformPortable() throws GameActionException {
    	if(!rc.isActionReady() || rc.getMode() == RobotMode.PORTABLE || settled ) return;
		
    	//start moving archons together
        if(rc.getArchonCount() > 1 && failedBuildAttempt) {
        	MapLocation nearestFriendlyArchon = getNearestFriendlyArchon();
        	if(nearestFriendlyArchon != null && !nearestFriendlyArchon.isWithinDistanceSquared(rc.getLocation(), Constants.ARCHON_CLOSE_RADIUS) && rc.canTransform()) {
        		rc.transform();
        		generalTarget = nearestFriendlyArchon;
        	}
        }
        
	}

    //return MapLocation of nearest friendly archon (not including self)
	private MapLocation getNearestFriendlyArchon() {
		return new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
	}

	private void tryBuildFromComms() throws GameActionException {
    	if(!rc.isActionReady() || rc.getMode() == RobotMode.PORTABLE) return;
    	
    	double soldierValue = 
    			soldierWeights[5]*localInfo.getFriendlyDamagerCount() +
    			soldierWeights[6]*localInfo.getFriendlyNonDamagerCount() +
    			soldierWeights[7]*rc.getTeamLeadAmount(rc.getTeam()) +
    			soldierWeights[8]*nearestCorner.distanceSquaredTo(rc.getLocation());
    	double minerValue = 
    			minerWeights[5]*localInfo.getFriendlyDamagerCount() +
    			minerWeights[6]*localInfo.getFriendlyNonDamagerCount() +
    			minerWeights[7]*rc.getTeamLeadAmount(rc.getTeam()) +
    			minerWeights[8]*nearestCorner.distanceSquaredTo(rc.getLocation());
    	
    	
    	if(near[0]!=null) {
    		soldierValue += soldierWeights[1]*(1-(near[0].distanceSquaredTo(rc.getLocation())/distFar));
    		minerValue += minerWeights[1]*(1-(near[0].distanceSquaredTo(rc.getLocation())/distFar));
    	}
    	if(near[1]!=null) {
    		soldierValue += soldierWeights[2]*(1-(near[1].distanceSquaredTo(rc.getLocation())/distFar));
    		minerValue += minerWeights[2]*(1-(near[1].distanceSquaredTo(rc.getLocation())/distFar));
    	}
    	if(near[2]!=null) {
    		soldierValue += soldierWeights[3]*(1-(near[2].distanceSquaredTo(rc.getLocation())/distFar));
    		minerValue += minerWeights[3]*(1-(near[2].distanceSquaredTo(rc.getLocation())/distFar));
    	}
    	if(near[3]!=null) {
    		soldierValue += soldierWeights[4]*(1-(near[3].distanceSquaredTo(rc.getLocation())/distFar));
    		minerValue += minerWeights[4]*(1-(near[3].distanceSquaredTo(rc.getLocation())/distFar));
    	}
    	
    	
		
    	
    		
    	rc.setIndicatorString(soldierValue+", "+minerValue);
    	
    	if(soldierValue > soldierWeights[0] && soldierValue > minerValue) {
    		if(rc.getTeamGoldAmount(rc.getTeam()) > RobotType.SAGE.buildCostGold) {
    			failedBuildAttempt = !tryBuild(RobotType.SAGE);
        		return;
        	}
        	if(rc.getTeamLeadAmount(rc.getTeam()) > RobotType.SOLDIER.buildCostLead) {
        		failedBuildAttempt = !tryBuild(RobotType.SOLDIER);
        		return;
        	}
    	}
    	
    	if(minerValue > minerWeights[0] && minerValue > soldierValue) {
    		if(rc.getTeamLeadAmount(rc.getTeam()) > RobotType.MINER.buildCostLead) {
    			failedBuildAttempt = !tryBuild(RobotType.MINER);
        		return;
        	}
    	}
    	
	}

	private void tryRepair() throws GameActionException {
    	if(!rc.isActionReady() || rc.getMode() == RobotMode.PORTABLE) return;
		
		MapLocation target = null;
		if(localInfo.nearestDamagedFR[RobotType.SAGE.ordinal()] != null) {
			target = localInfo.nearestDamagedFR[RobotType.SAGE.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), rc.getType().actionRadiusSquared)) {
				tryRepair(target);
				return;
			}
		}
		if(localInfo.nearestDamagedFR[RobotType.SOLDIER.ordinal()] != null) {
			target = localInfo.nearestDamagedFR[RobotType.SOLDIER.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), rc.getType().actionRadiusSquared)) {
				tryRepair(target);
				return;
			}
		}
		if(localInfo.nearestDamagedFR[RobotType.BUILDER.ordinal()] != null) {
			target = localInfo.nearestDamagedFR[RobotType.BUILDER.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), rc.getType().actionRadiusSquared)) {
				tryRepair(target);
				return;
			}
		}
		if(localInfo.nearestDamagedFR[RobotType.MINER.ordinal()] != null) {
			target = localInfo.nearestDamagedFR[RobotType.MINER.ordinal()].getLocation();
			if(target.isWithinDistanceSquared(rc.getLocation(), rc.getType().actionRadiusSquared)) {
				tryRepair(target);
				return;
			}
		}
	}
    
    private void tryRepair(MapLocation location) throws GameActionException {
		if(rc.canRepair(location))
			rc.repair(location);
	}
    
    private void tryBuildLocal() throws GameActionException{
    	//build section
        if(!rc.isActionReady() || rc.getMode() == RobotMode.PORTABLE) return;
        
        if(localInfo.nearestER[RobotType.ARCHON.ordinal()] != null) {
        	buildDir = rc.getLocation().directionTo(localInfo.nearestER[RobotType.ARCHON.ordinal()].location).rotateRight();
        	failedBuildAttempt = !tryBuild(RobotType.SAGE);
        	if(failedBuildAttempt) {
        		failedBuildAttempt = !tryBuild(RobotType.SOLDIER);
        	}
        	return;
        }
        //damager in vision build defensive troops
        if(nearDamager != null) {
        	buildDir = rc.getLocation().directionTo(nearDamager).rotateRight();
        	failedBuildAttempt = !tryBuild(RobotType.SAGE);
        	if(failedBuildAttempt) {
        		failedBuildAttempt = !tryBuild(RobotType.SOLDIER);
        	}
        	return;
        }
        
        if(rc.getHealth() < rc.getType().getMaxHealth(rc.getLevel()) && localInfo.nearestFR[RobotType.BUILDER.ordinal()] == null) {
        	buildDir = buildDir.opposite();
        	failedBuildAttempt = !tryBuild(RobotType.BUILDER);
    		return;
        }
        
        if(localInfo.totalLead > 50) {
        	buildDir = rc.getLocation().directionTo(localInfo.nearestLeadLoc).rotateRight();
        	failedBuildAttempt = !tryBuild(RobotType.MINER);
    		return;
        }
    }
    
    /**
     *
     * @param RobotType rt
     * @return boolean buildSucceed
     * @throws GameActionException
     */
    
    private boolean tryBuild(RobotType rt) throws GameActionException {
    	if(!rc.isActionReady() ||
			rc.getTeamLeadAmount(rc.getTeam()) < rt.buildCostLead ||
			rc.getTeamGoldAmount(rc.getTeam()) < rt.buildCostGold) return false;

		if(buildDir == Direction.CENTER)buildDir = Direction.NORTH;
    	
    	for(int i = 8; i-- >= 0;) {
    		if (rc.canBuildRobot(rt, buildDir)) {
			    rc.buildRobot(rt, buildDir);
			    return true;
			}
    		buildDir.rotateLeft();
    	}
    	return false;
    }
    
//    public MapLocation getLocationForWatchTower(){
//        // if we know enemy Archon, act on that
//        if(nearestCorner == null)initNearestCorner();
//        // or else
//        Direction dir = currentLocation.directionTo(nearestCorner).opposite();
//
//        // TODO: make this distance dynamic
//        return new MapLocation(currentLocation.x + dir.dx*10,currentLocation.y + dir.dy*10);
//    }



}
