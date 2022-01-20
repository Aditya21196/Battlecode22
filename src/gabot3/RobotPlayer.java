package gabot3;

import battlecode.common.*;
import gabot3.bots.Robot;


public strictfp class RobotPlayer {

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!
//        System.out.println("I'm a " + rc.getType() + " and I just got created! I have health " + rc.getHealth());

        // You can also use indicators to save debug notes in replays.
//        rc.setIndicatorString("Hello world!");

        Robot robot = Robot.getRobot(rc);;

        while (true) {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                robot.runRobot();
            } catch (GameActionException e) {
                // critical exception
                System.out.println(rc.getType() + " Exception. Alert the Devs!");
                e.printStackTrace();

            } catch (Exception e) {
                // critical exception
                System.out.println(rc.getType() + " Exception. Fix!");
                e.printStackTrace();
            } finally {
//                System.out.println("Bytecode: "+Clock.getBytecodesLeft()+" unit: "+rc.getType());
                Clock.yield();
            }
        }
    }
}
