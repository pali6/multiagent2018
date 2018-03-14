import bc.*;

class RangerAgent extends UnitAgent {
    Path currentPath;

    public RangerAgent(int id, Central central) {
        super(id, central);
    }

    public void prepareTurn() {
        
    }

    public MapLocation loc() {
        return central.gc.unit(id).location().mapLocation();
    }

    public Team enemyTeam() {
        if(central.gc.team() == Team.Blue)
            return Team.Red;
        return Team.Blue;
    }

    public Direction opposite(Direction dir) {
        switch(dir) {
            case Center:
                return Direction.Center;
            case North:
                return Direction.South;
            case South:
                return Direction.North;
            case Northeast:
                return Direction.Southwest;
            case Northwest:
                return Direction.Southeast;
            case West:
                return Direction.East;
            case East:
                return Direction.West;
            case Southeast:
                return Direction.Northwest;
            case Southwest:
                return Direction.Northeast;
        }
        return Direction.Center;
    }

    public void doCantAttack() {
        if(!central.gc.isMoveReady(id))
            return;
        //System.out.println("move ready");
        Unit closeTarget = null;
        VecUnit nearby = central.gc.senseNearbyUnitsByTeam(loc(), 50, enemyTeam());
        for(int i = 0; i < nearby.size(); i++) {
            Unit unit = nearby.get(i);
            int unitDist = (int) unit.location().mapLocation().distanceSquaredTo(loc());
            if(unitDist <= 10) {
                int closestDist = -1;
                if(closeTarget != null)
                    closestDist = (int) closeTarget.location().mapLocation().distanceSquaredTo(loc());
                if(closeTarget == null || unitDist > closestDist || (unitDist == closestDist && unit.id() < closeTarget.id()))
                    closeTarget = unit;
            }
        }
        if(closeTarget != null) {
            Direction wrongDir = loc().directionTo(closeTarget.location().mapLocation());
            Direction correctDir = opposite(wrongDir);
            if(central.gc.canMove(id, correctDir)) {
                central.gc.moveRobot(id, correctDir);
                //System.out.println("moving away");
                return;
            }
        }
        //System.out.println("1-");
        if(currentPath == null || central.turnNumber % 10 == 0)
            currentPath = central.findPath(loc(), central.findFight(loc()));
        int maxIter = 3;
        while(currentPath == null && maxIter-- > 0) // max iterations
            currentPath = central.findPath(loc(), central.findFight(loc()));
        if(currentPath == null) {
            Direction dir = Direction.values()[central.rng.nextInt(9)];
            //System.out.printf("dir %s\n", dir.toString());
            if(central.gc.canMove(id, dir))
                central.gc.moveRobot(id, dir);
            return;
        }
        //System.out.println("path");
        Direction dir = central.nextStep(currentPath, loc());
        //System.out.printf("path dir %s\n", dir.toString());
        if(dir != null && central.gc.canMove(id, dir)) {
            //System.out.println("actually moving");
            central.gc.moveRobot(id, dir);
        }
        else if(central.turnNumber % 5 == 0) {
            //System.out.println("failed moving");
            currentPath = central.findPath(loc(), central.findFight(loc()));
            if(currentPath != null)
                dir = central.nextStep(currentPath, loc());
            if (central.gc.canMove(id, dir)) {
                central.gc.moveRobot(id, dir);
                //System.out.println("backup moving");
            }
            //else //System.out.println("failed absolutely");
        }
    }

    public boolean snipeSomeRocket() {
        for(int x = 0; x < central.width; x++) {
            for(int y = 0; y < central.height; y++) {
                if(central.map[x][y].hasEnemyRocket) {
                    MapLocation factoryPos = new MapLocation(central.gc.planet(), x, y);
                    if(central.gc.canBeginSnipe(id, factoryPos)) {
                        central.gc.beginSnipe(id, factoryPos);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean snipeSomeFactory() {
        for(int x = 0; x < central.width; x++) {
            for(int y = 0; y < central.height; y++) {
                if(central.map[x][y].hasEnemyFactory) {
                    MapLocation factoryPos = new MapLocation(central.gc.planet(), x, y);
                    if(central.gc.canBeginSnipe(id, factoryPos)) {
                        central.gc.beginSnipe(id, factoryPos);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean doSnipe() {
        if(snipeSomeRocket())
            return true;
        if(snipeSomeFactory())
            return true;
        return false;
    }

    public boolean maybeEnterARocket() {
        if(central.numberOfUnits.containsKey(UnitType.Rocket) && central.numberOfUnits.get(UnitType.Rocket) > 0) {
            //System.out.println("ROCKET");
            VecUnit units = central.gc.myUnits();
            MapLocation targetLoc = null;
            for(int i = 0; i < units.size(); i++) {
                Unit unit = units.get(i);
                if(unit.unitType() == UnitType.Rocket && unit.rocketIsUsed() == 0) {
                    MapLocation rocketLoc = unit.location().mapLocation();
                    if(central.gc.canLoad(unit.id(), id)) {
                        //System.out.println("loading");
                        central.gc.load(unit.id(), id);
                        ((RocketAgent)central.unitAgents.get(unit.id())).nInside++;
                        return true;
                    }
                    /*
                    else if(central.gc.isMoveReady(id) && unit.location().isAdjacentTo(central.gc.unit(id).location())) { // temporary rocket launching
                        MapLocation landingLoc;
                        do {
                            landingLoc = new MapLocation(Planet.Mars,
                                    central.rng.nextInt((int)central.gc.startingMap(Planet.Mars).getWidth()),
                                    central.rng.nextInt((int)central.gc.startingMap(Planet.Mars).getHeight()));
                        } while(central.gc.startingMap(Planet.Mars).isPassableTerrainAt(landingLoc) == 0);
                        if(central.gc.canLaunchRocket(unit.id(), landingLoc))
                            central.gc.launchRocket(unit.id(), landingLoc);
                    }*/
                    for(int dx = rocketLoc.getX() - 1; dx <= rocketLoc.getX() + 1; dx++) {
                        for(int dy = rocketLoc.getY() - 1; dy <= rocketLoc.getY() + 1; dy++) {
                            MapLocation neighLoc = new MapLocation(central.gc.planet(), dx, dy);
                            if(dx < 0 || dy < 0 || dx >= central.width || dy >= central.height || (dx == rocketLoc.getX() && dy == rocketLoc.getY()) ||
                                    !central.map[dx][dy].passable || central.gc.hasUnitAtLocation(neighLoc))
                                continue;
                            if(targetLoc == null)
                                targetLoc = neighLoc;
                        }
                    }
                }
            }
            if(!central.gc.isMoveReady(id))
                return false;
            if(targetLoc != null)
                currentPath = central.findPath(loc(), targetLoc);
            if(currentPath == null)
                return false;
            Direction dir = central.nextStep(currentPath, loc());
            if(dir != null && central.gc.canMove(id, dir)) {
                central.gc.moveRobot(id, dir);
                return true;
            }
        }
        return false;
    }

    public void doTurn() {
        //System.out.println("1");
        if(!central.gc.unit(id).location().isOnMap() || central.gc.unit(id).location().isInGarrison() || central.gc.unit(id).location().isInSpace())
            return;
        //System.out.println("2");
        if(id % 2 == 0 && maybeEnterARocket()) // only some rangers go to a rocket
            return;
        //System.out.println("3");
        if(central.gc.isBeginSnipeReady(id) && central.rng.nextInt(5) == 0) {
            if(doSnipe()) {
                doCantAttack();
                return;
            }
        }
        //System.out.println("4");
        if(!central.gc.isAttackReady(id)) {
            doCantAttack();
            return;
        }
        //System.out.println("5");
        VecUnit nearby = central.gc.senseNearbyUnitsByTeam(loc(), 50, enemyTeam());
        for(int i = 0; i < nearby.size(); i++) {
            Unit unit = nearby.get(i);
            if(central.gc.canAttack(id, unit.id())) {
                //System.out.println("shoot");
                if(central.rng.nextInt(20) == 0) // TODO: remove
                    central.gc.attack(id, unit.id());
                doCantAttack(); // maybe?
                return;
            }
            //int unitDist = unit.location().mapLocation().distanceSquaredTo(loc());
        }
        //System.out.println("6");
        doCantAttack();
    }
}
