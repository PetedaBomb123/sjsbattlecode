package sjsbattlecodeV110;
import battlecode.common.*;
import java.util.Random;

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
            	//Win Donation if possible
            	victoryPoints();
            	//Last round donate
            	if((rc.getRoundLimit()-10) < rc.getRoundNum()){
            		rc.donate(rc.getTeamBullets()-(rc.getTeamBullets()%10));
            	}

                // Generate a random direction
                Direction dir = randomDirection();

                // Randomly attempt to build a gardener in this direction
                if (rc.canHireGardener(dir) && Math.random() < .01) {
                    rc.hireGardener(dir);
                }

                // Move randomly
                tryMove(randomDirection());

                // Broadcast archon's location for other robots on the team to know
                MapLocation myLocation = rc.getLocation();
                rc.broadcast(0,(int)myLocation.x);
                rc.broadcast(1,(int)myLocation.y);

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
            	
            	//plantCircle();



                // Listen for home archon's location
                int xPos = rc.readBroadcast(0);
                int yPos = rc.readBroadcast(1);
                MapLocation archonLoc = new MapLocation(xPos,yPos);

                // Generate a random direction
                Direction dir = randomDirection();

                if(makeAScout == true) {
                    if (rc.canBuildRobot(RobotType.SCOUT, dir)) {
                        rc.buildRobot(RobotType.SCOUT, dir);
                    }

                    makeAScout = false;
                }

                // Randomly attempt to build a soldier or lumberjack in this direction
                if (rc.canBuildRobot(RobotType.SOLDIER, dir) && Math.random() < .01) {
                    rc.buildRobot(RobotType.SOLDIER, dir);
                } else if (rc.canBuildRobot(RobotType.LUMBERJACK, dir) && Math.random() < .01 && rc.isBuildReady()) {
                    rc.buildRobot(RobotType.LUMBERJACK, dir);
                }


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

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);
                TreeInfo[] trees = rc.senseNearbyTrees(GameConstants.LUMBERJACK_STRIKE_RADIUS);

                if(robots.length > 0 && !rc.hasAttacked()) {
                    // Use strike() to hit all nearby robots!
                    rc.strike();

                    //new method implementation
                } else if(trees.length > 0 && !rc.hasAttacked()) {
                    //moves toward the nearest tree
                    moveTowardObject(trees[0].location);
                    //performs tree related actions
                    lumberjackTrees(trees[0].location, trees[0].containedBullets);

                } else {
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(-1,enemy);

                    // If there is a robot, move towards it
                    if(robots.length > 0) {
                        //moves the robot toward the nearest robot
                        moveTowardObject(robots[0].getLocation());
                    } else {
                        //No close tree, so search for trees within sight radius
                        trees = rc.senseNearbyTrees(RobotType.LUMBERJACK.sensorRadius);
                        if(trees.length > 0) {
                            //Move toward trees.
                            moveTowardObject(trees[0].location);
                        } else {
                            // Move Randomly
                            tryMove(randomDirection());
                        }

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
    static void runScout() throws GameActionException {
        System.out.println("I'm a scout");

        while(true) {

            //the first thing that a out is going to do is check its radius to see if there is an enemy

            try {

                boolean isTrue = true;
                int counter = 0;




                //trying to send the scout in a certain direction
                while(isTrue) {

                    if(rc.canMove(directionToMove)) {
                        rc.move(directionToMove);

                        break;
                    }
                    else
                    {

                        directionToMove = directionToMove.rotateLeftRads((float)(Math.PI/2));
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
     * @version V3
     *
     */
    static void lumberjackTrees(MapLocation location, int containedBullets) throws GameActionException{
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


    	public static int determineGardenerStatus() throws GameActionException{
    		int status = 2;
    		rc.broadcast(rc.getID(), status);
    		return rc.readBroadcast(rc.getID());
    	}


    	/**
    	 * Method to show a gardener how to plant trees in a circle. 
    	 * 
    	 * @author Nathan Solomon
    	 * @version V1
    	 * @throws GameActionException
    	 */
		public static void plantCircle() throws GameActionException{
			if(determineGardenerStatus() == 1){
				//try present location (finishes method if can't or shouldn't plant)
				if(rc.isCircleOccupiedExceptByThisRobot(rc.getLocation(),(2*GameConstants.BULLET_TREE_RADIUS)+(2*RobotType.GARDENER.bodyRadius))){
					rc.plantTree(Direction.getEast());
					rc.broadcast(rc.getID(), 2);
					directionToMove = new Direction(0);
			
					
				}
			} else if(determineGardenerStatus() == 2) {
				for(int i = 1; i < 6; i++){
					if(rc.canPlantTree(directionToMove)){
						rc.plantTree(directionToMove);
						break;
					
					}
					else {
						directionToMove = directionToMove.rotateLeftRads((float)(2*Math.PI/5));
					}
				}
			}
		
			
	
		}
}
