package sjsbattlecodeV111;
import battlecode.common.*;
import java.util.Random;
import java.lang.Integer;
import java.lang.Math;
import java.math.*;

@SuppressWarnings("unused")
public strictfp class RobotPlayer {
    static RobotController rc;

    //creating a Direction for the class which always holds the previous direction the robot was moving
    //please, whenever you make a robot move in a specific direction, change this direction
    private static Direction directionToMove;
    //this makes sure to choose a direction only the first time this bot is run
    private static boolean initialRound = true;

    //boolean to determine whether to make a scout
    private static boolean makeAScout = true;

    //boolean to determine whether it is the first time for a scout
    private static boolean firstTimeScout = true;


    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;



        //setting an inital direction for the robot to move
        if(initialRound == true) {
            Random generator = new Random();
            int i = generator.nextInt(4);

            if(i == 0) {
                directionToMove = new Direction((float)(Math.PI/2));
            }
            else if (i == 1) {
                directionToMove = new Direction(0);

            }
            else if (i == 2) {
                directionToMove = new Direction((float)(3*Math.PI/2));
            }
            else if (i == 4){
                directionToMove = new Direction((float)(Math.PI));
            }

            initialRound = false;

        }

        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SOLDIER:
                runSoldier();
                break;
            case LUMBERJACK:
                runLumberjack();
                break;
            case TANK:
                runTank();
                break;
            case SCOUT:
                runScout();
                break;
        }
    }

    static void runArchon() throws GameActionException {
        System.out.println("I'm an archon!");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                //First thing the archon does is broadcast its own location
                broadcastArchonLocation();
                
                //Win Donation if possible
                victoryPoints();
                //Last round donate
                if((rc.getRoundLimit()-10) < rc.getRoundNum()){
                    rc.donate(rc.getTeamBullets()-(rc.getTeamBullets()%10));
                }

                // Generate a random direction
                Direction dir = randomDirection();

                //Builds a gardener, if the conditions are correct
                createUnit();

                // Move randomly
                tryMove(randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }

    static void runGardener() throws GameActionException {
        System.out.println("I'm a gardener!");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                setGardenerStatus();
                plantCircle();



                // Listen for home archon's location
                int xPos = rc.readBroadcast(0);
                int yPos = rc.readBroadcast(1);
                MapLocation archonLoc = new MapLocation(xPos,yPos);

                // Generate a random direction
                Direction dir = randomDirection();

                if(makeAScout == true) {
                    if (rc.canBuildRobot(RobotType.SCOUT, dir)) {
                        rc.buildRobot(RobotType.SCOUT, dir);
                        int scoutCount=rc.readBroadcast(25);
                        scoutCount++;
                        rc.broadcast(25, scoutCount);
                    }

                    makeAScout = false;
                }

                //Creates a unit after checking conditions
                createUnit();


                // Move randomly
                tryMove(randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }

    static void runSoldier() throws GameActionException {
        System.out.println("I'm an soldier!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                // If there are some...
                if (robots.length > 0) {
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                }

                // Move randomly
                tryMove(randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }

    static void runLumberjack() throws GameActionException {
        System.out.println("I'm a lumberjack!");
        Team enemy = rc.getTeam().opponent();
        Team friendly = rc.getTeam();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);
                TreeInfo[] trees = rc.senseNearbyTrees(GameConstants.LUMBERJACK_STRIKE_RADIUS);
                System.out.println("1");
                if(robots.length > 0 && !rc.hasAttacked()) {
                    // Use strike() to hit all nearby robots!
                    rc.strike();
                    System.out.println("2");
                    //new method implementation
                } else if(trees.length > 0 && !rc.hasAttacked() && shouldAttack(trees[0].getTeam())) {
                    //performs tree related actions
                    System.out.println("3");
                    System.out.println("4");
                    lumberjackTrees(trees[0].location, trees[0].containedBullets);

                } else {
                    System.out.println("5");
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(-1,enemy);

                    // If there is a robot, move towards it
                    System.out.println("6");
                    if(robots.length > 0) {
                        System.out.println("7");
                        //moves the robot toward the nearest robot
                        moveTowardObject(robots[0].getLocation());
                    } else {
                        System.out.println("8");
                        // Move Randomly
                        moveTowardObject(rc.getInitialArchonLocations(enemy)[0]);

                    }
                }


                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();


            }
        }
    }
    static void runTank() throws GameActionException {
        System.out.println("I'm a tank!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                // If there are some...
                if (robots.length > 0) {
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                }

                // Move randomly
                tryMove(randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Tank Exception");
                e.printStackTrace();
            }
        }
    }
        /*THE SCOUT METHOD
    Basically, the scout will first sense all of the robots nearby it and get the number of enemy and
    friendly robots. Based on this information, it will prioritize what to do. The one overriding piece
    of logic the scout has is that if spots an enemy soldier or tank, it will record this soldier or tank
    on the broadcast, and then run away in the opposite direction. The next priority is if the scout sees
    another scout. If it sees another scout, it will fire a shot at the scout. If the scout is hit, we will move
    closer to the scout, if it is not, it will move perpendicularly to it. Next, if the scout senses a gardener,
    it will fire at the trees that the gardener is forming or shoot the gardener if it is doing anything else.
    Next, if the scout senses an archon, it will move one step closer depending on the distance it is away from the
    archon and add the archons location to the broadcast. There is currently no protocol on the lumberjack.




     */
    
    static void runScout() throws GameActionException {

        System.out.println("I'm a scout");

        //setting a new random generator for the scout
        Random scoutGenerator = new Random();

        while(true) {

            //the first thing that a scout is going to do is check its radius to see if there is an enemy

            try {

                boolean isTrue = true;
                int counter = 0;

                //first thing the scout does is find where our closest archon location is, and goes away from it
                //finds the closest archon - read findClosestArchon method
                if(firstTimeScout) {
                    int closestArchon = findClosestArchon();
                    MapLocation locationOfClosestArchon = new MapLocation((float) rc.readBroadcast(closestArchon), (float) rc.readBroadcast(closestArchon + 1));
                    Direction directionToArchon = new Direction(rc.getLocation(), locationOfClosestArchon);
                    directionToMove = directionToArchon.opposite();
                    firstTimeScout = false;
                }


                //returns an array of all the nearby robots
                RobotInfo[] nearbyRobots = rc.senseNearbyRobots();

                int numberOfEnemySoldiers = 0;
                int numberOfEnemyArchons = 0;
                int numberOfEnemyGardeners = 0;
                int numberOfEnemyScouts = 0;
                int numberOfEnemyTanks = 0;
                int numberOfEnemyLumberjacks = 0;
                int numberOfAllySoldiers = 0;
                int numberOfAllyArchons = 0;
                int numberOfAllyGardeners = 0;
                int numberOfAllyScouts = 0;
                int numberOfAllyTanks = 0;
                int numberOfAllyLumberjacks = 0;


                //incrementing the above variables if those types of robots are found
                for(int i = 0; i<nearbyRobots.length; i++)
                {
                    RobotInfo robotInRange = nearbyRobots[i];

                    if(robotInRange.getTeam() == rc.getTeam())
                    {
                        RobotType robotInRangeType = robotInRange.getType();

                        if(robotInRangeType == RobotType.ARCHON)
                        {
                            numberOfAllyArchons++;
                        }
                        else if(robotInRangeType == RobotType.GARDENER)
                        {
                            numberOfAllyGardeners++;
                        }
                        else if(robotInRangeType == RobotType.LUMBERJACK)
                        {
                            numberOfAllyLumberjacks++;
                        }
                        else if(robotInRangeType == RobotType.SCOUT)
                        {
                            numberOfAllyScouts++;
                        }
                        else if(robotInRangeType == RobotType.SOLDIER)
                        {
                            numberOfAllySoldiers++;
                        }
                        else if(robotInRangeType == RobotType.TANK)
                        {
                            numberOfAllyTanks++;
                        }

                    }
                    else
                    {
                        RobotType robotInRangeType = robotInRange.getType();

                        if(robotInRangeType == RobotType.ARCHON)
                        {
                            numberOfEnemyArchons++;
                        }
                        else if(robotInRangeType == RobotType.GARDENER)
                        {
                            numberOfEnemyGardeners++;
                        }
                        else if(robotInRangeType == RobotType.LUMBERJACK)
                        {
                            numberOfEnemyLumberjacks++;
                        }
                        else if(robotInRangeType == RobotType.SCOUT)
                        {
                            numberOfEnemyScouts++;
                        }
                        else if(robotInRangeType == RobotType.SOLDIER)
                        {
                            numberOfEnemySoldiers++;
                        }
                        else if(robotInRangeType == RobotType.TANK)
                        {
                            numberOfEnemyTanks++;
                        }

                    }

                }




                //going through the nearbyRobots array, and depending on the type of robot and number of enemy and ally robots,
                //this determines what the scout will do

                scoutDecisionLoop:
                for(int i = 0; i <nearbyRobots.length; i++)
                {
                    RobotInfo robotInRange = nearbyRobots[i];

                    //does nothing if scout senses a robot of its own team
                    if(robotInRange.getTeam() == rc.getTeam())
                    {

                    }
                    //if its the other team however...
                    else
                    {
                        //if the sensed robot is an archon- this scout will go to the archon (if there is nothing else nearby)
                        if(robotInRange.getType() == RobotType.ARCHON)
                        {
                            //broadcasting the archon's location

                            MapLocation locationOfArchon = robotInRange.getLocation();
                            int x = getX(locationOfArchon);
                            int y = getY(locationOfArchon);


                            //getting the number of archons
                            int numberOfArchons = rc.readBroadcast(0);

                            //if there is one archon, pretty easy, just rebroadcast it's position
                            if(numberOfArchons == 1)
                            {
                                rc.broadcast(17,x);
                                rc.broadcast(18,y);

                            }
                            //if there are two archons, this sets the x and y of the robot in range to the closer of the two archons from the broadcast
                            else if(numberOfArchons == 2)
                            {
                                int x1 = rc.readBroadcast(17);
                                int y1 = rc.readBroadcast(18);
                                int x2 = rc.readBroadcast(19);
                                int y2 = rc.readBroadcast(20);

                                double distance1 = Math.sqrt((x-x1)*(x-x1) + (y-y1)*(y-y1));
                                double distance2 = Math.sqrt((x-x2)*(x-x2) + (y-y2)*(y-y2));

                                if(distance1 < distance2)
                                {
                                    rc.broadcast(17,x);
                                    rc.broadcast(18,y);

                                }
                                else
                                {
                                    rc.broadcast(19,x);
                                    rc.broadcast(20,y);
                                }

                            }
                            //if there are three archons, this will broadcast the robotInRange x and y to the closest of the 6 coordinates
                            else if(numberOfArchons == 3)
                            {
                                int x1 = rc.readBroadcast(17);
                                int y1 = rc.readBroadcast(18);
                                int x2 = rc.readBroadcast(19);
                                int y2 = rc.readBroadcast(20);
                                int x3 = rc.readBroadcast(21);
                                int y3 = rc.readBroadcast(22);

                                double distance1 = Math.sqrt((x-x1)*(x-x1) + (y-y1)*(y-y1));
                                double distance2 = Math.sqrt((x-x2)*(x-x2) + (y-y2)*(y-y2));
                                double distance3 = Math.sqrt((x-x3)*(x-x3) + (y-y3)*(y-y3));

                                if(distance1 < distance2 && distance1 < distance3)
                                {
                                    rc.broadcast(17,x);
                                    rc.broadcast(18,y);

                                }
                                else if(distance2 < distance1 && distance2 < distance3)
                                {
                                    rc.broadcast(19,x);
                                    rc.broadcast(20,y);
                                }
                                else
                                {
                                    rc.broadcast(21,x);
                                    rc.broadcast(22,y);
                                }
                            }


                            //if there are no enemy soldiers, tanks, gardeners, and scouts
                            if(numberOfEnemySoldiers < 1 && numberOfEnemyTanks < 1 && numberOfEnemyGardeners<1 && numberOfEnemyScouts<1 ) {
                                //getting the distance between the scout and the archon
                                //DISTANCETO IS A GREAT METHOD USE IT- gets the distance between two map locations


                                float distance = rc.getLocation().distanceTo(robotInRange.getLocation());


                                //8 is an arbitrary number, it can be changed, however, if the scout is more than 8 away
                                // from the archon, it will move toward the archon
                                if (distance > 8) {

                                    //setting directionToMove to where the archon is- if no soldiers are detected,
                                    // the scout will head in this direction (if the for loop ends)
                                    directionToMove = new Direction(rc.getLocation(), robotInRange.getLocation());

                                }

                                break scoutDecisionLoop;
                            }

                        }
                        else if(robotInRange.getType() == RobotType.GARDENER)
                        {
                            //if there are no soldiers or tanks or scouts nearby
                            //right now, I am just making the scouts try to fire at enemy gardeners and go closer to them

                            if(numberOfEnemySoldiers < 1 && numberOfEnemyTanks < 1 && numberOfEnemyScouts < 1)
                            {
                                Direction directionToGardener = new Direction(rc.getLocation(), robotInRange.getLocation());

                                //fires a shot from the location of the scout to the location of the gardener
                                rc.fireSingleShot(directionToGardener);

                                //getting the distance between the scout and the gardener
                                float distance = rc.getLocation().distanceTo(robotInRange.getLocation());

                                if(distance > 4)
                                {
                                    directionToMove = directionToGardener;
                                }
                                else
                                {
                                    //if the distance between the scout and enemy gardener is less than 4, than it will pick a random direction perpendicular to the gardener
                                    int randomNumber = scoutGenerator.nextInt(2);

                                    if(randomNumber == 0)
                                    {
                                        directionToMove = directionToGardener.rotateLeftDegrees(90);
                                    }
                                    else
                                    {
                                        directionToMove = directionToGardener.rotateRightDegrees(90);
                                    }


                                }

                                break scoutDecisionLoop;
                            }

                        }
                        else if(robotInRange.getType() == RobotType.SOLDIER)
                        {
                            //getting the direction between the scout and soldier
                            Direction directionToSoldier = new Direction(rc.getLocation(), robotInRange.getLocation());
                            //getting the distance between teh scout and soldier
                            float distance = rc.getLocation().distanceTo(robotInRange.getLocation());

                            //if the scout is out of the viewing range of the soldier, it will walk perpendicular to it
                            if(distance > 7)
                            {
                                int randomNumber = scoutGenerator.nextInt(2);

                                if(randomNumber == 0)
                                {
                                    directionToMove = directionToSoldier.rotateLeftDegrees(90);
                                }
                                else
                                {
                                    directionToMove = directionToSoldier.rotateRightDegrees(90);
                                }
                            }
                            //if the scout is within the viewing range of the soldier, it goes in the opposite direction, but at an angle
                            //to avoid the bullets of the soldier
                            else
                            {
                                directionToMove = directionToSoldier.opposite();
                                int randomNumber = scoutGenerator.nextInt(2);

                                if(randomNumber == 0)
                                {
                                    directionToMove.rotateLeftDegrees(30);
                                }
                                else
                                {
                                    directionToMove.rotateRightDegrees(30);
                                }

                            }

                            break scoutDecisionLoop;

                        }
                        //literally does the exact same thing as the soldier class
                        else if(robotInRange.getType() == RobotType.TANK)
                        {
                            //getting the direction between the scout and tank
                            Direction directionToTank = new Direction(rc.getLocation(), robotInRange.getLocation());
                            //getting the distance between the scout and tank
                            float distance = rc.getLocation().distanceTo(robotInRange.getLocation());

                            //if the scout is out of the viewing range of the tank, it will walk perpendicular to it
                            if(distance > 7)
                            {
                                int randomNumber = scoutGenerator.nextInt(2);

                                if(randomNumber == 0)
                                {
                                    directionToMove = directionToTank.rotateLeftDegrees(90);
                                }
                                else
                                {
                                    directionToMove = directionToTank.rotateRightDegrees(90);
                                }
                            }
                            //if the scout is within the viewing range of the tank, it goes in the opposite direction, but at an angle
                            //to avoid the bullets of the tank
                            else
                            {
                                directionToMove = directionToTank.opposite();
                                int randomNumber = scoutGenerator.nextInt(2);

                                if(randomNumber == 0)
                                {
                                    directionToMove.rotateLeftDegrees(30);
                                }
                                else
                                {
                                    directionToMove.rotateRightDegrees(30);
                                }

                            }

                            break scoutDecisionLoop;
                        }


                    }



                }



                //trying to send the scout in a certain direction
                while(isTrue) {

                    if(rc.canMove(directionToMove)) {
                        rc.move(directionToMove);
                        isTrue = false;
                        break;
                    }
                    else
                    {


                        //if the scout is stopped for some reason, this gets the location of the scout
                        MapLocation location = rc.getLocation();
                        int x = getX(location);
                        int y = getY(location);
                        System.out.println(x + ", " + y);

                        //checking to see if the x or y are less or greater than the current map boundaries broadcast
                        //if they are, then this re broadcasts the current boundaries
                        if(x<rc.readBroadcast(13))
                        {
                            rc.broadcast(13,x);
                        }
                        else if(y>rc.readBroadcast(14))
                        {
                            rc.broadcast(14, y);
                        }
                        else if(x>rc.readBroadcast(15))
                        {
                            rc.broadcast(15, x);
                        }
                        else if(y<rc.readBroadcast(16))
                        {
                            rc.broadcast(16,y);
                        }

                        //making the scout choose a new random direction to move in

                        int newDirection = scoutGenerator.nextInt(5);
                        if(newDirection == 1)
                        {
                            directionToMove = directionToMove.rotateLeftDegrees(90);
                        }
                        else if(newDirection == 2)
                        {
                            directionToMove = directionToMove.rotateRightDegrees(90);
                        }
                        else if(newDirection == 3)
                        {
                            directionToMove = directionToMove.rotateLeftDegrees(135);
                        }
                        else
                        {
                            directionToMove = directionToMove.rotateRightDegrees(135);
                        }


                    }


                    counter++;
                    if(counter == 5) {
                        System.out.println("Scout cant move");
                        isTrue = false;
                    }

                }

                Clock.yield();

            }
            catch(Exception e) {
                System.out.println("Scout Exception");
                e.printStackTrace();
            }

        }




    }



    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }

    /**
     * Method that determines a lumberjacks tree related actions
     *
     * @param tree location from TreeInfo class. Array with information regarding nearby trees
     * @author Nathan Solomon
     * @version V4
     *
     */
    static void lumberjackTrees(MapLocation location, int containedBullets) throws GameActionException{
        //moves toward the nearest tree
        moveTowardObject(location);

        if(containedBullets > 0){
            //shake nearby tree
            rc.shake(location);

        } else {
            //chop nearby tree
            rc.chop(location);
        }

    }

    /**
     * Method that determines remaining victory points needed and other related statistics
     *
     * @author Nathan Solomon
     * @version V2
     *
     */
    public static void victoryPoints() throws GameActionException{
        int victoryPointsLeft = 1000 - rc.getTeamVictoryPoints();   //This calculates remaining points in the game.

        //is possible to win?
        int victoryPointsPossible = (int)(rc.getTeamBullets()/10); //calculates maximum donation
        if (victoryPointsLeft < victoryPointsPossible){
            rc.donate((float)(victoryPointsPossible*10));		//This may not need the float thing
        }
    }

    /**
     * Method that moves a unit toward a designated target
     *
     * @author Nathan Solomon
     * @version V1
     *
     */
    public static void moveTowardObject(MapLocation objectLocation) throws GameActionException{
        //Get my location
        MapLocation myLocation = rc.getLocation();
        //Get object location
        Direction toObject = myLocation.directionTo(objectLocation);
        //move to location
        tryMove(toObject);

    }

    /**This method returns the x value of a MapLocation
     *
     * @author Peter Buckman
     * @param map location of point that we need an x value for
     * @return the x coordinate of that map location
     */

    public static int getX(MapLocation location)
    {
        String location2 = location.toString();
        int stop = 0;

        for(int i = 0; i<location2.length(); i++)
        {
            if(location2.substring(i,i+1).equals("."))
            {
                stop = i;
                break;
            }
        }

        String xCoord = location2.substring(1, stop);

        int x = Integer.parseInt(xCoord);
        return x;
    }

    /**This method returns the y value of a MapLocation
     *
     * @author Peter Buckman
     * @param location
     * @return y value of the map location
     * @version V1
     */
    public static int getY(MapLocation location)
    {
        String location2 = location.toString();
        int stop = 0;
        int stop2 = 0;

        for(int i = 0; i<location2.length(); i++)
        {
            if(location2.substring(i,i+1).equals("."))
            {
                stop = i+9;
                break;
            }
        }

        for(int i = 8; i<location2.length(); i++)
        {
            if(location2.substring(i,i+1).equals("."))
            {
                stop2 = i;
                break;
            }
        }

        String yCoord = location2.substring(stop, stop2);

        int y = Integer.parseInt(yCoord);
        return y;



    }
    /**
     * creates broadcasting channels for individual robots (not archons) that have low likelyhood of glitching
     *
     * @return Robots designated broadcasting
     * @author Nathan Solomon
     * @version V1
     */
    public static int getRobotChannel(){
        return ((int)(rc.getID()/10)-500);
    }

    /**
     * determines gardener strategy
     *
     * @author Nathan Solomon
     * @version V1
     * @throws GameActionException
     */
    public static void setGardenerStatus() throws GameActionException{
        //tests if there is already a strategy
        if(getStatus() == 0){
            Random generator = new Random();
            int num = generator.nextInt(1000);
            //randomly assigns to strategy
            if(num < 500){
                int status = 1;
                rc.broadcast(getRobotChannel(), status);
            }
            else{
                int status = 2;
                rc.broadcast(getRobotChannel(), status);
            }
        }
    }

    /**
     * getter method to call the stored robots strategy
     *
     * @author Nathan Solomon
     * @version V1
     * @return Robot Strategy Value
     * @throws GameActionException
     */
    public static int getStatus() throws GameActionException{
        return (rc.readBroadcast(getRobotChannel()));
    }


    /**
     * Method to show a gardener how to plant trees in a circle.
     *
     * @author Nathan Solomon
     * @version V2
     * @throws GameActionException
     */
    public static void plantCircle() throws GameActionException{
        directionToMove = Direction.getEast();
        //gets the current strategy status of robot
        int status = getStatus();
        if(status == 1){
            for(int i = 1; i < 6; i++){
                if(rc.canPlantTree(directionToMove)){
                    rc.plantTree(directionToMove);
                    Clock.yield();

                }
                else {
                    //rotates to find perfect tree placement.
                    directionToMove = directionToMove.rotateLeftRads((float)(2*Math.PI/5));
                }
            }
            //waters nearby trees
            waterCircle();
            Clock.yield();

        }
    }
    /**
     * Method to show a gardener how to maintain a tree circle
     *
     * @author Nathan Solomon
     * @version V1
     * @throws GameActionException
     */
    public static void waterCircle() throws GameActionException {
        //finds nearby trees
        TreeInfo[] trees = rc.senseNearbyTrees(2);
        directionToMove = Direction.getEast();
        //index to water by turn number
        int directionMultiplier = rc.getRoundNum()%5;

        //shakes and waters surrounding trees
        rc.shake(trees[directionMultiplier].location);
        rc.water(trees[directionMultiplier].location);

    }
    public static boolean shouldAttack(Team Object) {
        if(Object == rc.getTeam()){
            return false;
        }
        else {
            return true;
        }
    }

    /**
     * Method so that the archon broadcasts its own location- can only be run by the archon
     *
     * @autho Peter Buckman
     * @version V1
     * @throws GameActionException
     */

    public static void broadcastArchonLocation() throws GameActionException
    {
        int numberOfArchons = rc.readBroadcast(0);
        MapLocation locationOfArchon = rc.getLocation();

        int x = getX(locationOfArchon);
        int y = getY(locationOfArchon);

        if(numberOfArchons == 1)
        {
            rc.broadcast(1, x);
            rc.broadcast(2,y);
        }
        else if(numberOfArchons == 2)
        {
            int x1 = rc.readBroadcast(1);
            int y1 = rc.readBroadcast(2);
            int x2 = rc.readBroadcast(3);
            int y2 = rc.readBroadcast(4);

            //if any of the 2 sets of broadcasts for the ally archons are 0 (less than 1), then automatically broadcast the archon location to there
            if(x1<1 && y1<1)
            {
                rc.broadcast(1, x);
                rc.broadcast(2,y);
                return;

            }
            else if(x2 < 1 && y2 < 1)
            {
                rc.broadcast(3, x);
                rc.broadcast(4,y);
                return;

            }

            //get the shortest distance between the x and y coordinate and the previous turns archon locations- broadcast to those channels
            double distance1 = Math.sqrt((x-x1)*(x-x1) + (y-y1)*(y-y1));
            double distance2 = Math.sqrt((x-x2)*(x-x2) + (y-y2)*(y-y2));

            if(distance1 < distance2)
            {
                rc.broadcast(1, x);
                rc.broadcast(2,y);
            }
            else
            {
                rc.broadcast(3, x);
                rc.broadcast(4,y);
            }

        }
        else if(numberOfArchons == 3)
        {
            int x1 = rc.readBroadcast(1);
            int y1 = rc.readBroadcast(2);
            int x2 = rc.readBroadcast(3);
            int y2 = rc.readBroadcast(4);
            int x3 = rc.readBroadcast(5);
            int y3 = rc.readBroadcast(6);


            //if any of the 3 sets of broadcasts for the ally archons are 0 (less than 1), then automatically broadcast the archon location to there
            if(x1<1 && y1<1)
            {
                rc.broadcast(1, x);
                rc.broadcast(2,y);
                return;
            }
            else if(x2 < 1 && y2 < 1)
            {
                rc.broadcast(3, x);
                rc.broadcast(4,y);
                return;
            }
            else if(x3 < 1 && y3 < 1)
            {
                rc.broadcast(5,x);
                rc.broadcast(6,y);
            }


            //get the shortest distance between the x and y coordinate and the previous turns archon locations- broadcast to those channels
            double distance1 = Math.sqrt((x-x1)*(x-x1) + (y-y1)*(y-y1));
            double distance2 = Math.sqrt((x-x2)*(x-x2) + (y-y2)*(y-y2));
            double distance3 = Math.sqrt((x-x3)*(x-x3) + (y-y3)*(y-y3));

            if(distance1 < distance2 && distance1 < distance3)
            {
                rc.broadcast(1, x);
                rc.broadcast(2,y);
            }
            else if(distance2 < distance1 && distance2 < distance3)
            {
                rc.broadcast(3, x);
                rc.broadcast(4,y);
            }
            else
            {
                rc.broadcast(5, x);
                rc.broadcast(6,y);
            }
        }
    }

    /**
     * Method that finds the closest ally archon to you, and returns the broadcast # of the x coordinate of the closest archon
     *
     * @author Peter Buckman
     * @version V1
     * @returns a 1,3, or 5 representing the x coordinate of the closest archon
     */

    public static int findClosestArchon() throws GameActionException
    {
        int numberOfArchons = rc.readBroadcast(0);

        //if there is only 1 archon, return that
        if(numberOfArchons == 1)
        {
            return 1;
        }
        else if(numberOfArchons == 2)
        {
            int x = getX(rc.getLocation());
            int y = getY(rc.getLocation());
            int x1 = rc.readBroadcast(1);
            int y1 = rc.readBroadcast(2);
            int x2 = rc.readBroadcast(3);
            int y2 = rc.readBroadcast(4);

            double distance1 = Math.sqrt((x-x1)*(x-x1) + (y-y1)*(y-y1));
            double distance2 = Math.sqrt((x-x2)*(x-x2) + (y-y2)*(y-y2));

            if(distance1 < distance2)
            {
                return 1;
            }
            else
            {
                return 3;
            }
        }
        //if there are 3 archons
        else
        {
            int x = getX(rc.getLocation());
            int y = getY(rc.getLocation());
            int x1 = rc.readBroadcast(1);
            int y1 = rc.readBroadcast(2);
            int x2 = rc.readBroadcast(3);
            int y2 = rc.readBroadcast(4);
            int x3 = rc.readBroadcast(5);
            int y3 = rc.readBroadcast(6);




            //get the shortest distance between the x and y coordinate and the previous turns archon locations- broadcast to those channels
            double distance1 = Math.sqrt((x-x1)*(x-x1) + (y-y1)*(y-y1));
            double distance2 = Math.sqrt((x-x2)*(x-x2) + (y-y2)*(y-y2));
            double distance3 = Math.sqrt((x-x3)*(x-x3) + (y-y3)*(y-y3));

            if(distance1 < distance2 && distance1 < distance3)
            {
                return 1;
            }
            else if(distance2 < distance1 && distance2 < distance3)
            {
                return 3;
            }
            else
            {
                return 5;
            }
        }


    }
	/**
	 * @author JohnBoom
	 * @return void
	 * @throws GameActionException
	 * @version V1
	 */
	
    
    public static void createUnit() throws GameActionException{
	int treeCount = rc.readBroadcast(29);
	int soldierCount = rc.readBroadcast(27);
	int lumberjackCount = rc.readBroadcast(26);
	int tankCount = rc.readBroadcast(28);
	int gardenerCount = rc.readBroadcast(24);
	
	if(rc.getType()==RobotType.ARCHON) {
		
		if(gardenerCount==0) {
			Direction buildDirection = randomDirection();
			if(rc.canBuildRobot(RobotType.GARDENER, buildDirection)) {
				rc.buildRobot(RobotType.GARDENER, buildDirection);
				gardenerCount++;
				rc.broadcast(24, gardenerCount);
				
			}
		} else if(treeCount/gardenerCount == 4){	
			Direction buildDirection = randomDirection();
			if(rc.canBuildRobot(RobotType.GARDENER, buildDirection)) {
				rc.buildRobot(RobotType.GARDENER, buildDirection);
				gardenerCount++;
				rc.broadcast(24, gardenerCount);
				
			}
		}
		if(treeCount ==0) {
			Direction buildDirection = randomDirection();
			if(rc.canBuildRobot(RobotType.GARDENER, buildDirection)) {
				rc.buildRobot(RobotType.GARDENER, buildDirection);
				gardenerCount++;
				rc.broadcast(24, gardenerCount);
			}
		}
	}
		if(rc.getType()==RobotType.GARDENER) {
			Direction buildDirection = findDirection();
			if(soldierCount==0){
				if(rc.canBuildRobot(RobotType.SOLDIER, buildDirection)) {
					rc.buildRobot(RobotType.SOLDIER, buildDirection);
				soldierCount++;
				rc.broadcast(27, soldierCount);
				}
			} else if(treeCount/soldierCount >=4) {
				if(rc.canBuildRobot(RobotType.SOLDIER, buildDirection)) {
					rc.buildRobot(RobotType.SOLDIER, buildDirection);
				soldierCount++;
				rc.broadcast(27, soldierCount);
				}
			}
			if (lumberjackCount==0) {
				if(rc.canBuildRobot(RobotType.LUMBERJACK, buildDirection)) {
					rc.buildRobot(RobotType.LUMBERJACK, buildDirection);
					lumberjackCount++;
					rc.broadcast(26, lumberjackCount);
					}
			} else if(treeCount/lumberjackCount>=10){
				if(rc.canBuildRobot(RobotType.LUMBERJACK, buildDirection)) {
				rc.buildRobot(RobotType.LUMBERJACK, buildDirection);
				lumberjackCount++;
				rc.broadcast(26, lumberjackCount);
				}
			
			
			}
			if(rc.getRoundNum()/rc.getRoundLimit()>=.5 && tankCount<2) {
				if(rc.canBuildRobot(RobotType.TANK, buildDirection)) {
				rc.buildRobot(RobotType.TANK, buildDirection);
				tankCount++;
				rc.broadcast(28, tankCount);
			}
		
		}
	
	}
	
	
	
	
	
    }
    
    /**
     * @author johnboom
     * @version V1
     * @return Direction
     * Finds a direction for gardeners to plant their trees
     */
    
    public static Direction findDirection() {
    	Direction buildDirection = Direction.getEast();

    	for(int i = 1; i < 6; i++){
            if(rc.canBuildRobot(RobotType.SOLDIER, buildDirection)){
              //ends the for loop, meaning whichever the direction is it no longer changes
               break;
            }
            else {
                //rotates to find perfect tree placement.
                buildDirection = buildDirection.rotateLeftRads((float)(2*Math.PI/5));
            }
            
    }
		return buildDirection;
    }
}


