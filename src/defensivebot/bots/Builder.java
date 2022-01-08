package defensivebot.bots;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import defensivebot.enums.DroidSubType;

import static defensivebot.utils.LogUtils.printDebugLog;

public class Builder extends Robot{

    DroidSubType type = null;

    public Builder(RobotController rc) throws GameActionException  {
        super(rc);
    }

    @Override
    public void sense() throws GameActionException {
        localInfo.senseRobots();
        if(type == null){
            type = comms.getSubtypeFromSignal(localInfo.homeArchon);
            printDebugLog("I will build a filter id: "+rc.getID());
        }
//        CustomSet<SparseSignal> sparseSignals = comms.querySparseSignals();
//        sparseSignals.initIteration();
//        SparseSignal next = sparseSignals.next();
//        while(next!=null){
//            printDebugLog("Sparse Signal Found: "+next.type);
//            if(next.type == )
//        }
    }

    @Override
    public void executeRole() throws GameActionException {

    }

    @Override
    public void move() throws GameActionException {

    }

}
