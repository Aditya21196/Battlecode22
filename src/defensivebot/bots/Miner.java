package defensivebot.bots;


import battlecode.common.*;
import static defensivebot.utils.Constants.directions;

public class Miner extends Robot{
    
	private int headingIndex = -1; // index in Constants.directions for heading
	
	public Miner(RobotController rc) throws GameActionException  {
        super(rc);
    }
    
    
    
    @Override
    public void sense() throws GameActionException{
		//sense
		localInfo.senseRobots();
		localInfo.senseTerrain();
    }

	/*
	 * current strat mine lead
	 */
    @Override
    public void executeRole() throws GameActionException {
		if(rc.isActionReady()) {
			//mineLead();
		}
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

		//for testing search
//    	verbose("bytecode before search: "+ Clock.getBytecodesLeft());
//    	MapLocation best = getBestMiningLoc();
//    	if(best != null)
//    		rc.setIndicatorLine(best, currentLocation,0,0,255);
//    	verbose("bytecode after search: "+ Clock.getBytecodesLeft());
    	
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
		else if(localInfo.nearestLead != null) {
			bestDirection = getBestValidDirection(currentLocation.directionTo(localInfo.nearestLead));
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
    	
    	//otherwise mine conservatively and let lead regenerate
    	MapLocation me = rc.getLocation();
        for(int dx = 1; --dx >= -1;){
            for(int dy = 1; --dy >= -1;){
            	if(!rc.isActionReady())
            		return;
            	int x = me.x + dx, y = me.y + dy;
            	MapLocation mineLocation = new MapLocation(x, y);
                while(rc.canMineLead(mineLocation) && --localInfo.lead2d[x][y] >= 1) {
                    rc.mineLead(mineLocation);
                    rc.setIndicatorString("Mining Lead.");
                }
                
            }
        }
    }
    
    /*
     * return the best visible MapLocation for mining that is not currently occupied
     * Factors
     * 	-lead total within reach
     * 	-number of deposits within reach
     * 	-actions per round
     * 	-distance to get there?
     */
    public MapLocation getBestMiningLoc() {
    	
    	//get visible(7x7 centered around miner) and valid(on the map) x,y ranges
    	int xMin = currentLocation.x - 3 > 0 ? currentLocation.x - 3 : 0;
    	int yMin = currentLocation.y - 3 > 0 ? currentLocation.y - 3 : 0;
    	int xMax = currentLocation.x + 4 < rc.getMapWidth() ? currentLocation.x + 4 : rc.getMapWidth();
    	int yMax = currentLocation.y + 4 < rc.getMapHeight() ? currentLocation.y + 4 : rc.getMapHeight();
    	
    	//sum 3x1 strips in lead2d (sum deposits as well)
    	int[][] stripSumLead = new int[5][7];
    	int[][] stripSumDeposits = new int[5][7];
        for (int j=xMin; j<xMax; j++){
            int lead = 0;
            int deposits = 0;
            for (int i=yMin; i<yMin+3; i++) {
               lead += localInfo.lead2d[i][j];
               if(localInfo.lead2d[i][j] > 0)
            	   deposits++;
            	   
            }
            stripSumLead[0][j-xMin] = lead;
            stripSumDeposits[0][j-xMin] = deposits;
            
            for (int i=yMin+1; i<yMax-3+1; i++)
            {
            	int removeFromSum = localInfo.lead2d[i-1][j];
            	int addToSum = localInfo.lead2d[i+3-1][j];
	            lead += addToSum - removeFromSum;
	            stripSumLead[i-yMin][j-xMin] = lead;
	            if(addToSum > 0)
	            	deposits++;
	            if(removeFromSum > 0)
	            	deposits--;
	            stripSumDeposits[i-yMin][j-xMin] = deposits;
            }
       }
        
        //sum 3x1 into 3x3 sums from strips
        
        int[][] gridSumLead = new int[5][5];
        int[][] gridSumDeposits = new int[5][5];
        
        for (int i=0; i<(yMax-yMin)-3+1; i++)
        {
           int lead = 0;
           int deposits = 0;
           for (int j = 0; j<3; j++) {
                lead += stripSumLead[i][j];
                deposits += stripSumDeposits[i][j];
           }
           gridSumLead[i][0] = lead;
           gridSumDeposits[i][0] = deposits;
      
           for (int j=1; j<(xMax-xMin)-3+1; j++)
           {
              lead += (stripSumLead[i][j+3-1] - stripSumLead[i][j-1]);
              gridSumLead[i][j] = lead;
              deposits += (stripSumDeposits[i][j+3-1] - stripSumDeposits[i][j-1]);
              gridSumDeposits[i][j] = deposits;
           }
      
        }
        
        //compute location lead values
        int[][] values = new int[5][5];
        
        for(int i = 0; i < yMax-yMin-2;i++) {
        	for(int j = 0; j < xMax-xMin-2; j++) {
            	//gather data about location
		    	int leadDeposits = gridSumDeposits[j][i];//IOOB
		    	int currentLead = gridSumLead[j][i];
		    	int currentRubble = localInfo.rubble2d[j+xMin+1][i+yMin+1];
		    	
		    	//compute location value
		    	int roundsRemaining = GameConstants.GAME_MAX_NUMBER_OF_ROUNDS - rc.getRoundNum();
		    	int futureLeadPerDeposit = (roundsRemaining/GameConstants.ADD_LEAD_EVERY_ROUNDS)*GameConstants.ADD_LEAD;
		    	int totalLead = currentLead + leadDeposits*futureLeadPerDeposit;
		    	int cooldownPerAction = (int)((1 + (currentRubble/10.0)) * RobotType.MINER.actionCooldown);
		    	double leadPerRound = GameConstants.COOLDOWNS_PER_TURN / (double)cooldownPerAction; 
		    	
		    	int value = 0;
		    	if(roundsRemaining*leadPerRound < totalLead) {
		    		//if not occupied
		    		if(localInfo.robot2d[j+xMin+1][i+yMin+1] == 0) {
		    			value = (int) (roundsRemaining*leadPerRound);
		    		}
		    		//other wise value is 0
		    	}else {
		    		//if not occupied
		    		if(localInfo.robot2d[j+xMin+1][i+yMin+1] == 0) {
		    			value = totalLead;
		    		}
		    		//other wise value is 0
		    	}
		    	values[j][i] = value;
            }
        }
        
        int bestValue = 0;
        MapLocation best = null;
        
        for(int i = 0; i < 5;i++) {
        	for(int j = 0; j < 5; j++) {
            	if(values[j][i] > bestValue) {
            		bestValue = values[j][i];
            		best = new MapLocation(j+xMin+1, i+yMin+1);
            	}
            }
        }
        
		return best;
        
    }
    
}
