package defensivebot;

import battlecode.common.*;
import defensivebot.bots.Robot;


/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!
        System.out.println("I'm a " + rc.getType() + " and I just got created! I have health " + rc.getHealth());

        // You can also use indicators to save debug notes in replays.
        rc.setIndicatorString("Hello world!");

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
                Clock.yield();
            }
        }
    }


}
