package defensivebot.bots;


import battlecode.common.*;
import static defensivebot.utils.Constants.directions;

public class Miner extends Robot{
    
	private int headingIndex = -1; // index in Constants.directions for heading
	
	public Miner(RobotController rc) throws GameActionException  {
        super(rc);
    }
    
    
    
    public void sense() throws GameActionException{
		//sense
		localInfo.senseRobots();
		localInfo.senseLead();
    }

	/*
	 * current strat mine lead
	 */
    @Override
    public void executeRole() throws GameActionException {
    	sense();
		if(rc.isActionReady()) {
			mineLead();
		}
		move();
    }
    
    /*
     * MOVEMENT STRATS/IDEAS
     * 
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
    public void move() throws GameActionException {
    	if(!rc.isMovementReady())return;

		Direction bestDirection = null;
		
		//move away from nearest visible enemy watchtower
		if(localInfo.nearestER[RobotType.WATCHTOWER.ordinal()] != null) {
			bestDirection = getBestValidDirection(localInfo.nearestER[RobotType.WATCHTOWER.ordinal()].location.directionTo(currentLocation));
			rc.setIndicatorString("Trying to run from Watchtower.");
			headingIndex = -1;
		}

		//move away from nearest visible enemy soldier
		else if(localInfo.nearestER[RobotType.SOLDIER.ordinal()] != null) {
			bestDirection = getBestValidDirection(localInfo.nearestER[RobotType.SOLDIER.ordinal()].location.directionTo(currentLocation));
			rc.setIndicatorString("Trying to run from Soldier.");
			headingIndex = -1;
		}

		//move away from nearest visible enemy sage
		else if(localInfo.nearestER[RobotType.SAGE.ordinal()] != null) {
			bestDirection = getBestValidDirection(localInfo.nearestER[RobotType.SAGE.ordinal()].location.directionTo(currentLocation));
			rc.setIndicatorString("Trying to run from Sage.");
			headingIndex = -1;
		}

		//move toward gold would be nice, but currently not being sensed

		//move toward nearest visible lead
		else if(localInfo.nearestLeadLoc != null) {
			bestDirection = getBestValidDirection(currentLocation.directionTo(localInfo.nearestLeadLoc));
			rc.setIndicatorString("Trying to get to lead.");
			headingIndex = -1;
		}

		//move in random heading
		else if(headingIndex == -1){
			headingIndex = (int)(Math.random()*directions.length);
			bestDirection = directions[headingIndex];
			rc.setIndicatorString("Picking new direction to run.");
		}else {
			bestDirection = directions[headingIndex];
			rc.setIndicatorString("Running straight.");
		}


		//Finally, make move in best direction
		if(bestDirection != null && rc.canMove(bestDirection)) {
			rc.move(bestDirection);
		}else {
			headingIndex = -1;
		}
    		

    }

    
    /*
     * 
     */
    public void mineLead() throws GameActionException {
    	/*
    	 * set this boolean to true based on factors such as ... 
    	 * if enemy miners are present and no friendly damage units are nearby (prevent them from mining too)
    	 * if enemy damage units are nearby (pack up and leave)
    	 * if enemy archons are closer then friendly archons (destroy this resource which is on their side of map)
    	 */
    	boolean mineGreedy = true;
    	
    	if(mineGreedy) {
    		MapLocation me = rc.getLocation();
            for(int dx = 1; --dx >= -1;){
                for(int dy = 1; --dy >= -1;){
                	if(!rc.isActionReady())
                		return;
                	MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                    while(rc.canMineLead(mineLocation)) {
                        rc.mineLead(mineLocation);
                        rc.setIndicatorString("Mining Lead.");
                    }
                    
                }
            }
    		return;
    	}
//    	
//    	//otherwise mine conservatively and let lead regenerate
//    	MapLocation me = rc.getLocation();
//        for(int dx = 1; --dx >= -1;){
//            for(int dy = 1; --dy >= -1;){
//            	if(!rc.isActionReady())
//            		return;
//            	int x = me.x + dx, y = me.y + dy;
//            	MapLocation mineLocation = new MapLocation(x, y);
//                while(rc.canMineLead(mineLocation) && --localInfo.lead2d[x][y] >= 1) {
//                    rc.mineLead(mineLocation);
//                    rc.setIndicatorString("Mining Lead.");
//                }
//                
//            }
//        }
    }
    
    
    
}
