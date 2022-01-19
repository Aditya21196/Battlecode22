package gabot2.bots;

import java.util.Random;

import battlecode.common.*;
import gabot2.datasturctures.CustomSet;
import gabot2.enums.SparseSignalType;
import gabot2.models.SparseSignal;
import gabot2.utils.*;


import static gabot2.utils.Constants.UNITS_AVAILABLE;

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
    private double[] soldierWeights ={-1.03,0.54,-2.75,-1.42,5.55,-0.12,2.5,2.88,-0.73};

	//miner threshold, distance to marked archon, distance to enemy, distance to lead
	// distance to unexplored, soldier count, miner count, team lead, nearestCorner
    private double[] minerWeights = {0.5, -1.0, -1.0, 1.0, 1.0, 1.0, -1.0, 0.01, -0.001};

    MapLocation nearestCorner;
    

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
        

    	localInfo.senseRobots(true,true,false);
        localInfo.senseLead(true);
        
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




        
    }
    private void tryBuildFromComms() throws GameActionException {
    	if(!rc.isActionReady()) return;
    	
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
        		tryBuild(RobotType.SAGE);
        		return;
        	}
        	if(rc.getTeamLeadAmount(rc.getTeam()) > RobotType.SOLDIER.buildCostLead) {
        		tryBuild(RobotType.SOLDIER);
        		return;
        	}
    	}
    	
    	if(minerValue > minerWeights[0] && minerValue > soldierValue) {
    		if(rc.getTeamLeadAmount(rc.getTeam()) > RobotType.MINER.buildCostLead) {
        		tryBuild(RobotType.MINER);
        		return;
        	}
    	}
    	
	}

	private void tryRepair() throws GameActionException {
    	if(!rc.isActionReady()) return;
		
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
        if(!rc.isActionReady()) return;
        
        int lead = rc.getTeamLeadAmount(rc.getTeam());
        int gold = rc.getTeamGoldAmount(rc.getTeam());
        
        
        if(localInfo.nearestER[RobotType.ARCHON.ordinal()] != null) {
        	if(gold > RobotType.SAGE.buildCostGold) {
        		buildDir = rc.getLocation().directionTo(localInfo.nearestER[RobotType.ARCHON.ordinal()].location).rotateRight();
        		tryBuild(RobotType.SAGE);
        	}
        	if(lead > RobotType.SOLDIER.buildCostLead) {
        		buildDir = rc.getLocation().directionTo(localInfo.nearestER[RobotType.ARCHON.ordinal()].location).rotateRight();
        		tryBuild(RobotType.SOLDIER);
        		
        	}
        	return;
        }
        //damager in vision build defensive troops
        if(nearDamager != null) {
        	if(gold > RobotType.SAGE.buildCostGold) {
        		buildDir = rc.getLocation().directionTo(nearDamager).rotateRight();
        		tryBuild(RobotType.SAGE);
        	}
        	if(lead > RobotType.SOLDIER.buildCostLead) {
        		buildDir = rc.getLocation().directionTo(nearDamager).rotateRight();
        		tryBuild(RobotType.SOLDIER);
        		
        	}
        	return;
        }
        
        if(rc.getHealth() < rc.getType().getMaxHealth(rc.getLevel()) && localInfo.nearestFR[RobotType.BUILDER.ordinal()] == null) {
        	buildDir = buildDir.opposite();
    		tryBuild(RobotType.BUILDER);
    		return;
        }
        
        if(localInfo.totalLead > 50) {
        	buildDir = rc.getLocation().directionTo(localInfo.nearestLeadLoc).rotateRight();
    		tryBuild(RobotType.MINER);
    		return;
        }
    }

    private void tryBuild(RobotType rt) throws GameActionException {
    	if(!rc.isActionReady()) return;
    	
    	for(int i = 8; i-- >= 0;) {
    		if (rc.canBuildRobot(rt, buildDir)) {
			    rc.buildRobot(rt, buildDir);
			    return;
			}
    		buildDir.rotateLeft();
    	}
    	
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
