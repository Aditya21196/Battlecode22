package gabot1.bots;

import java.util.Random;

import battlecode.common.*;
import gabot1.datasturctures.CustomSet;
import gabot1.enums.SparseSignalType;
import gabot1.enums.TaskType;
import gabot1.models.SparseSignal;
import gabot1.strategies.Comms2;
import gabot1.utils.*;

import static gabot1.utils.Constants.*;

public class Archon extends Robot{

    public static final Random rng = new Random(6147);

//    static int[] unitCounts = new int[UNITS_AVAILABLE];
//    private boolean enemySpotted = false;
    
    
    private boolean reportedCurrentLocation = false;
    private boolean reportedDangerFlag = false;

    private Direction buildDir = Direction.NORTH;
    private MapLocation nearDamager;
	private int archonIdx=-1;
	private int teamLead;
    
    //comms locations
    MapLocation[] near = new MapLocation[4];
    double distFar = rc.getMapWidth()*rc.getMapWidth()+rc.getMapHeight()*rc.getMapHeight();

	//soldier threshold, distance to marked archon,distance to enemy,distance to lead
	// distance to unexplored, soldier count, miner count, team lead, nearestCorner
	private double[] soldierWeights ={-3.46,-5.35,-2.9,-1.5,-1.62,-1.77,-3.35,3.37,2.04};

	//miner threshold, distance to marked archon, distance to enemy, distance to lead
	// distance to unexplored, soldier count, miner count, team lead, nearestCorner
	private double[] minerWeights ={-3.39,-3.17,-1.8,6.15,-0.09,3.08,4.59,-1.75,0.92};


	// closest enemy archon, nearest enemy loc, robot count, non damagers, team lead, team gold
	// enemy team lead
	private final double[] phaseTwoWeights ={1.03,-4.66,6.64,-0.97,5.03,0.9,3.4};

	private final int randomMarkUnexplored=48;

    MapLocation nearestCorner;
    
    private boolean failedBuildAttempt = false;

	private MapLocation generalTarget = null;
	private MapLocation finalTarget;
	private int finalTargetRubble;
	private int lastTransformTurret=-100;

	private int phase = 1;
    

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

		if(roundNum % randomMarkUnexplored == 0){
			Comms2.markRandomSectorUnexplored();
		}
        
    	//sense
    	localInfo.senseRobots(false,true,false);
        localInfo.senseLead(true,false);

		teamLead = rc.getTeamLeadAmount(team);
        

        if(turnCount%4 == 0) {
			near[0] = Comms2.getClosestArchon(false);
        }else if(turnCount%4 == 1) {
        	near[1] = Comms2.getNearestEnemyLoc();
        }else if(turnCount%4 == 2) {
        	near[2] = Comms2.getNearestLeadLoc();
        }else if(turnCount%4 == 3) {
        	near[3] = Comms2.getNearbyUnexplored();
        }

        
        //self report location
        if(!reportedCurrentLocation && archonIdx == -1){
            archonIdx = Comms2.registerFriendlyArchon(rc.getLocation());
            reportedCurrentLocation = true;
        }else if(!reportedCurrentLocation) {
        	Comms2.updateFriendlyArchon(archonIdx, rc.getLocation());
            reportedCurrentLocation = true;
        }

        nearDamager = localInfo.findNearestDamager();

        if(nearDamager != null && !reportedDangerFlag){
            // signal for danger
            Comms2.registerGatherPoint(currentLocation, TaskType.DEFEND_ARCHON);
            reportedDangerFlag = true;
        }else if(nearDamager == null && reportedDangerFlag){
            // signal I am okay
//            Comms2.markLocationSafe(currentLocation);
            reportedDangerFlag = false;
        }

//		MapLocation loc = Comms2.getClosestArchon(false);
//		if(loc != null){
//			Comms2.registerGatherPoint(loc,TaskType.ATTACK_ARCHON);
//		}
        rc.setIndicatorString(Comms2.friendlyArchons[0]+", "+Comms2.friendlyArchons[1]+", "+Comms2.friendlyArchons[2]+", "+Comms2.friendlyArchons[3]);
        tryBuildLocal();
        
        tryRepair();
        
        tryTransformPortable();
        
        tryBuildFromComms();

        tryTransformTurret();

        tryMove();

        
        
    }
    
    private void tryTransformTurret() throws GameActionException {
    	if(!rc.isMovementReady() || rc.getMode() == RobotMode.TURRET) return;

		if(
				generalTarget != null
						&& generalTarget.distanceSquaredTo(currentLocation)<ARCHON_CLOSE_RADIUS
						&& rc.senseRubble(currentLocation)<ARCHON_LOW_RUBBLE
		){
			rc.transform();
			lastTransformTurret = roundNum;
			finalTarget = null;
			generalTarget = null;
			reportedCurrentLocation = false;
			return;
		}
    	
    	if(finalTarget != null && finalTarget.equals(rc.getLocation()) && rc.canTransform()) {
    		rc.transform();
			lastTransformTurret = roundNum;
    		finalTarget = null;
    		reportedCurrentLocation = false;
    	}
    	
    	if(finalTarget == null && generalTarget == null && rc.canTransform()) {
    		rc.transform();
			lastTransformTurret = roundNum;
    		reportedCurrentLocation = false;
    	}
		
	}

	private void tryMove() throws GameActionException {
    	if(!rc.isMovementReady() || rc.getMode() == RobotMode.TURRET) return;

		
    	//whenever final target is set
    	if(finalTarget != null) {
    		updateFinalTarget();
    		pathfinding.moveTowards(finalTarget, false);rc.setIndicatorString("ft: "+finalTarget); 
    		return;
    	}
    	
    	
    	//phase 1 set general movement
    	if(phase == 1) {

			if(localInfo.nearestFR[RobotType.ARCHON.ordinal()] != null){
				// stop here if can see friendly archon
				generalTarget = localInfo.nearestFR[RobotType.ARCHON.ordinal()].location;
			}else generalTarget = getFriendlyArchonMidpoint();

    	}
    	
    	
    	//phase 2 set general movement
    	if(phase ==2) {
    		//stop general movement if enemy is nearby
    		if(localInfo.getEnemyDamagerCount() > 0) {
    			updateFinalTarget();
    			Comms2.registerGatherPoint(rc.getLocation(),TaskType.DEFEND_ARCHON);
    			return;
    		}
    		generalTarget = Comms2.getClosestArchon(false);
    	}
    	
    	
    	//move toward general targets
    	if(generalTarget != null && generalTarget.isWithinDistanceSquared(rc.getLocation(), Constants.CLOSE_RADIUS)) {
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
		
		MapLocation[] visable = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 20);

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
    	if(
				!rc.isActionReady()
						|| rc.getMode() == RobotMode.PORTABLE
						|| localInfo.nearestEnemy != null
				|| roundNum - lastTransformTurret < 50
		) return;

		
    	if(phase== 1 && teamAdvanceToPhase2()) {
    		phase++;
    	}
    	//move for phase 1 : move farthest archon to nearest archon
    	if(
				phase == 1 && teamLead<LEAD_MOVE_THRESHOLD && isFarthestFriendlyArchon()&& rc.canTransform()
				&& localInfo.nearestFR[RobotType.ARCHON.ordinal()] == null
		) {
    		generalTarget = getFriendlyArchonMidpoint();
    		rc.transform();
    	}

    	//move for phase 2 : move toward rally point
    	
    	if(phase == 2 && shouldMovePhaseTwo()) {
    		MapLocation target = Comms2.getClosestArchon(false);
    		if(target != null && rc.canTransform()) {
        		rc.transform();
        		generalTarget = target;
        		Comms2.registerGatherPoint(target,TaskType.ATTACK_ARCHON);
        	}
    	}
    	
	}

	private boolean shouldMovePhaseTwo(){
		if(localInfo.getEnemyDamagerCount() != 0)return false;

		double weight = phaseTwoWeights[2]*rc.getRobotCount() +
				phaseTwoWeights[3]*localInfo.getFriendlyNonDamagerCount() +
				phaseTwoWeights[4]*teamLead +
				phaseTwoWeights[5]*rc.getTeamGoldAmount(team) +
				phaseTwoWeights[6]*rc.getTeamLeadAmount(enemyTeam);


		if(near[0]!=null) {
			weight += phaseTwoWeights[0]*(1-(near[0].distanceSquaredTo(rc.getLocation())/distFar));
		}
		if(near[1]!=null) {
			weight += phaseTwoWeights[1]*(1-(near[1].distanceSquaredTo(rc.getLocation())/distFar));
		}

		return weight>0;

	}
	
	private boolean teamAdvanceToPhase2() {
		if(rc.getArchonCount() == 1) return true;
		
		for(int i = 0; i < Comms2.friendlyArchons.length; i++) {
			if(Comms2.friendlyArchons[i] == null) continue;
			boolean archonGood = false;
			for(int j = 0; j < Comms2.friendlyArchons.length; j++) {
				if(i==j || Comms2.friendlyArchons[j] == null) continue;
				
	    		if(Comms2.friendlyArchons[i].isWithinDistanceSquared(Comms2.friendlyArchons[j], Constants.ARCHON_CLOSE_RADIUS)){
	    			archonGood = true;
	    			break;
	    		}
	    	}
			
			if(!archonGood) return false;
    	}
		
		return true;
	}

	private boolean isFarthestFriendlyArchon() {
		int highestDist = 0;
		int highestDistArchonIndex = -1;
		for(int i = 0; i < Comms2.friendlyArchons.length; i++) {
			if(Comms2.friendlyArchons[i] == null) continue;
			int tempDist = 0;
			for(int j = 0; j < Comms2.friendlyArchons.length; j++) {
				if(i==j || Comms2.friendlyArchons[j] == null) continue;
				
	    		tempDist += Comms2.friendlyArchons[i].distanceSquaredTo(Comms2.friendlyArchons[j]);
	    	}
			if(tempDist > highestDist) {
				highestDist = tempDist;
				highestDistArchonIndex = i;
			}
    	}
		return highestDistArchonIndex == archonIdx;
	}

    //return MapLocation of midpoint of other archons (not including self)
	private MapLocation getFriendlyArchonMidpoint() {
		int x=0,y=0,a = 0;
        for(int i=4;--i>=0;){
            if(i == archonIdx || Comms2.friendlyArchons[i] == null)continue;
            x+=Comms2.friendlyArchons[i].x;
            y+=Comms2.friendlyArchons[i].y;
            a++;
        }
        if(a==0) return null;
        
        return new MapLocation(x/a,y/a);
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
        
        if(rc.getTeamLeadAmount(rc.getTeam()) > BUILDER_LEAD_THRESH) {
        	failedBuildAttempt = !tryBuild(RobotType.BUILDER);
        }
    }
    
    /**
     *
     * @param  rt
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
