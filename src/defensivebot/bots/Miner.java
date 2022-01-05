package defensivebot.bots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

//import com.sun.tools.javac.util.List;

import battlecode.common.*;

public class Miner extends Robot{
    
	private MapLocation[] visableMapLocations;
	
	//private HashSet<MapLocation> resourcesFromComms;
	private HashSet<MapLocation> leadLocationsFromVision;
	private HashMap<MapLocation, Integer> leadAmountAtLocation;
	
	private MapLocation closestED; //map location of closest enemy robot capable of dealing damage
	private MapLocation closestLead; //map location of closest lead deposit
	private MapLocation closestFA; //map location of closest friendly Archon
	private MapLocation closestUnexplored; //map location of closest unexplored sector
	private MapLocation bestInVision; //map location of best square for mining in vision
	private int headingIndex = -1; // index in Constants.directions for heading
	private boolean parked = false;
	
	public Miner(RobotController rc) {
        super(rc);
        leadLocationsFromVision = new HashSet<MapLocation>();
        leadAmountAtLocation = new HashMap<MapLocation, Integer>();
    }
    
    
    
    @Override
    public void executeRole() throws GameActionException {
    	
    	//collect information
    	collectInformation();
    	
    	//take action
    	act();
    	 
    	
    	
    	//move
    	if(rc.isMovementReady()) {
    		/*
    		 * 1. move away from closest enemy damager in vision
    		 * If not parked:
    		 * 2. ***move toward closest gold in vision (then park)
    		 * 3. move toward closest lead in vision (then park)
    		 * 4. move toward closest resource from comms
    		 * 5. if damaged head toward friendly archon
    		 * 6. move toward center of sector with little comm info
    		 * 7. move with random heading
    		 * 
    		 * If Parked:
    		 * 2. move toward best available square in vision
    		 * 3. leave if two or more miners are parked at lead with <= 6 
    		 * 3. otherwise stay still
    		 * 
    		 */
    		
    		
    		
    	}
    	//distribute information
    	
    	
    }
    
    public void move() {
    	
    }
    
    public void collectInformation() throws GameActionException {
    	
    	System.out.println("Bytecode before list clears: " + Clock.getBytecodesLeft());
    	leadLocationsFromVision.clear();
    	leadAmountAtLocation.clear();
    	System.out.println("Bytecode after list clears: " + Clock.getBytecodesLeft());
    	
    	visableMapLocations = rc.getAllLocationsWithinRadiusSquared(currentLocation, RobotType.MINER.visionRadiusSquared);
    	//at most 73 visable maplocations
    	//5 bytecode per senseLead() = 365 bytecode total
    	int lead;
    	for(MapLocation ml : visableMapLocations) {
    		lead = rc.senseLead(ml);
    		if(lead > 0) {
    			leadLocationsFromVision.add(ml);
    			leadAmountAtLocation.put(ml, lead);
    			closestLead = getClosest(closestLead, ml);
    		}
    	}
    	
    	for(RobotInfo ri : sensedRobots) {
    		RobotType riType = ri.getType();
    		Team riTeam = ri.getTeam();
    		if(riTeam == team) {
    			switch(riType) {
				case ARCHON:
					//probably worth doing state check on buildings
					closestFA = getClosest(closestFA, ri.getLocation());
					break;
				case BUILDER:
				case LABORATORY:
				case MINER:
				case SAGE:
				case SOLDIER:
				case WATCHTOWER:
				default:
					break;
				}
    			continue;
    		}
    		
    		//enemy team robot sensed
    		switch(riType) {
			case ARCHON:
				break;
			case BUILDER:
				break;
			case LABORATORY:
				break;
			case MINER:
				break;
			case SAGE:
			case SOLDIER:
			case WATCHTOWER:
				closestED = getClosest(closestED, ri.getLocation());
				break;
			default:
				break;
			}
    		
    	}
    }
    
    /*
     * 
     */
    public void act() throws GameActionException {
    	if(rc.isActionReady()) {
    		System.out.println("Bytecode before creating mine locations: " + Clock.getBytecodesLeft());
	    	MapLocation[] mls = {	currentLocation.add(Direction.EAST), 
					                currentLocation.add(Direction.NORTH), 
					                currentLocation.add(Direction.NORTHEAST), 
					                currentLocation.add(Direction.NORTHWEST), 
					                currentLocation.add(Direction.SOUTH), 
					                currentLocation.add(Direction.SOUTHEAST), 
					                currentLocation.add(Direction.SOUTHWEST),
					                currentLocation.add(Direction.WEST), 
					                currentLocation};
	    	ArrayList<MapLocation> mineLocations = new ArrayList<MapLocation>(Arrays.asList(mls));
	    	System.out.println("Bytecode after creating mine locations: " + Clock.getBytecodesLeft());
	    	
	    	mineLead(mineLocations);
    	}
    }
    
    /*
     * 
     */
    public void mineLead(ArrayList<MapLocation> mineLocations) throws GameActionException {
    	/*
    	 * set this boolean to true based on factors such as ... 
    	 * if enemy miners are present and no friendly damage units are nearby (prevent them from mining too)
    	 * if enemy damage units are nearby (pack up and leave)
    	 * if enemy archons are closer then friendly archons (destroy this resource which is on their side of map)
    	 */
    	boolean mineGreedy = false;
    	
    	if(mineGreedy) {
    		for(MapLocation ml : mineLocations) {
    			while (rc.canMineLead(ml)) {
                    rc.mineLead(ml);
                }
    		}
    		return;
    	}
    	
    	//otherwise mine conservatively and let lead regenerate
    	for(MapLocation ml : mineLocations) {
    		if(leadAmountAtLocation.containsKey(ml)) {
            	int amt = leadAmountAtLocation.get(ml);
            	while (amt > 1 && rc.canMineLead(ml)) {
            		rc.mineLead(ml);
            		amt--;
            	}
            }
		}
    }
}
