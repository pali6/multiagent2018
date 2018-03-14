import bc.*;
import java.util.*;


class Central {
    public GameController gc;
    HashMap<Integer, UnitAgent> unitAgents;
    HashMap<UnitType, Integer> numberOfUnits;
    Navigation navigation;
    int turnNumber = 0;
    long tmpKarbonite; //added for preparing turn(to get the karbonite that is ment to be spent in this turn, avoiding running aout of it during turn preparation
    int minersNeeded = 15; //open for changes
    int buildersNeeded = 7;	//open for changes
    Random rng;
    //MapLocation earthBase; //added for setting up the base of our team -> bulder workers will allways be in the base
    double[][] valuation;
    double[][] nearbyValuation;
    double[][] valuationPenalty;

    class Tile {
        boolean passable;
        double enemyPresence;
        double ourPresence;
        double teamControl;
        int lastKarbonite;
        int turnsSinceSeen;
        boolean hasEnemyFactory;
        boolean hasEnemyRocket;
    };

    public Tile[][] map;
    public int height, width;

    public Central() {
        rng = new Random();
        gc = new GameController();
        unitAgents = new HashMap<>();
        numberOfUnits = new HashMap<>();
        navigation = new Navigation(gc, gc.planet());
        tmpKarbonite = gc.karbonite(); // should be 100 at the start

        PlanetMap planetMap = gc.startingMap(gc.planet());
        height = (int) planetMap.getHeight();
        width = (int) planetMap.getWidth();

        valuation = new double[width][height];
        nearbyValuation = new double[width][height];
        valuationPenalty = new double[width][height];

        map = new Tile[width][height];
        for(int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                map[x][y] = new Tile();
                map[x][y].passable = planetMap.isPassableTerrainAt(new MapLocation(gc.planet(), x, y)) > 0;
                map[x][y].lastKarbonite = (int) planetMap.initialKarboniteAt(new MapLocation(gc.planet(), x, y));
                map[x][y].turnsSinceSeen = 0;
            }
        }

        gc.queueResearch(UnitType.Worker); // turn 25
        // maybe a few steps on the Worker tree?
        gc.queueResearch(UnitType.Ranger); // turn 50
        gc.queueResearch(UnitType.Ranger); // turn 150
        gc.queueResearch(UnitType.Ranger); // turn 350
        // ??? (~500 turns?) let's put in healers for now
        gc.queueResearch(UnitType.Rocket); //lets start building rockets erlier (lasts 100 rounds)
        
        gc.queueResearch(UnitType.Healer); // turn 375 + 100
        gc.queueResearch(UnitType.Healer); // turn 475	+ 100
        gc.queueResearch(UnitType.Healer); // turn 575 + 100

        // turn 625
        gc.queueResearch(UnitType.Rocket); // turn 725 
        gc.queueResearch(UnitType.Rocket); // turn 825 

        gc.queueResearch(UnitType.Mage); // random stuff
        gc.queueResearch(UnitType.Mage);
        gc.queueResearch(UnitType.Mage);
    }
    
    public UnitType needRobotType() {
    	//if (needWorkers()) return UnitType.Worker;
      return UnitType.Ranger;
    }
    
    public boolean needResources() { //open for changing
    		if (tmpKarbonite < 100) return true;
    		return false;
    }
    
    public boolean needWorkers() { //open for changing
    		int num_of_workers = numberOfUnits.get(UnitType.Worker);
    		if (num_of_workers < 20 ) return true;
    		return false;
    }
    
    public boolean needFactory() { //open for changing
    		int num_of_workers = numberOfUnits.get(UnitType.Worker);
    		Integer num_of_factories = numberOfUnits.get(UnitType.Factory);
    		if (num_of_workers > 10 &&  (num_of_factories == null || num_of_factories < 5)) return true;
    		return false;
    }
    
    public boolean needRocket() { //open for changing
    		if (turnNumber >= 450) return true;
    		return false;
    }
    

    public Path findPath(MapLocation from, MapLocation to) {
        return navigation.findPath(from, to);
    }

    public Direction nextStep(Path path, MapLocation me) {
        return navigation.getDirection(path, me);
    }

    public MapLocation findExactResources(MapLocation me) {
        return findResourcesInternal(me, 2.0, 1.0, 0.3, 0.0, true);
    }

    public MapLocation findResources(MapLocation me) {
        return findResourcesInternal(me, 1.0, 4.0, 1.0, 1.0, false);
    }

    public MapLocation findFight(MapLocation me) {
        double[][] distances = new double[width][height];
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                distances[x][y] = -1;
            }
        }

        class Entry implements Comparable<Entry> {
            public Double distance;
            public int x, y;

            public Entry(double distance, int x, int y) {
                this.distance = distance;
                this.x = x;
                this.y = y;
            }

            @Override
            public int compareTo(Entry other) {
                return this.distance.compareTo(other.distance);
            }
        }

        MapLocation defaultLoc = null;

        PriorityQueue<Entry> q = new PriorityQueue<>();
        q.add(new Entry(0, me.getX(), me.getY()));
        while(!q.isEmpty()) {
            Entry entry = q.poll();
            if(distances[entry.x][entry.y] != -1 && entry.distance >=  distances[entry.x][entry.y])
                continue;
            distances[entry.x][entry.y] = entry.distance;
            for(int dx = entry.x - 1; dx <= entry.x + 1; dx++)
                for(int dy = entry.y - 1; dy <= entry.y + 1; dy++){
                    MapLocation loc = new MapLocation(gc.planet(), dx, dy);
                    if(dx < 0 || dy < 0 || dx >= width || dy >= height || (dx == entry.x && dy == entry.y) || !map[dx][dy].passable)
                        continue;
                    if(gc.hasUnitAtLocation(loc)) {
                        Unit unit = gc.senseUnitAtLocation(loc);
                        if(unit.team() == gc.team())
                            continue;
                        else if(rng.nextInt(3) == 0) {
                            for (int ddx = dx - 1; ddx <= dx + 1; ddx++)
                                for (int ddy = dy - 1; ddy <= dy + 1; ddy++) {
                                    if (ddx < 0 || ddy < 0 || ddx >= width || ddy >= height || (ddx == dx && ddy == dy) || !map[ddx][ddy].passable ||
                                            gc.hasUnitAtLocation(new MapLocation(gc.planet(), ddx, ddy)))
                                        continue;
                                    return new MapLocation(gc.planet(), ddx, ddy);
                                }
                        }
                        else if(defaultLoc == null) {
                            for (int ddx = dx - 1; ddx <= dx + 1; ddx++)
                                for (int ddy = dy - 1; ddy <= dy + 1; ddy++) {
                                    if (ddx < 0 || ddy < 0 || ddx >= width || ddy >= height || (ddx == dx && ddy == dy) || !map[ddx][ddy].passable ||
                                            gc.hasUnitAtLocation(new MapLocation(gc.planet(), ddx, ddy)))
                                        continue;
                                    defaultLoc = new MapLocation(gc.planet(), ddx, ddy);
                                }
                        }
                    }
                    double dangerousness = -map[entry.x][entry.y].teamControl;
                    double neighDist = entry.distance + 1;
                    q.add(new Entry(neighDist, dx, dy));
                }
        }

        while(defaultLoc == null || !map[defaultLoc.getX()][defaultLoc.getY()].passable)
            defaultLoc = new MapLocation(gc.planet(), rng.nextInt(width), rng.nextInt(height));
        return defaultLoc;
    }

    public MapLocation findResourcesInternal(MapLocation me, double amountPriority, double nearPriority,
                                             double safetyPriority, double nearbyAmountPriority, boolean needKarbonite) {

        double[][] localValuation = new double[width][height];
        double[][] distances = new double[width][height];
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                distances[x][y] = -1;
                localValuation[x][y] = amountPriority * valuation[x][y]
                        + nearbyAmountPriority * nearbyValuation[x][y]
                        - valuationPenalty[x][y];
                if(!needKarbonite)
                    localValuation[x][y] -= valuationPenalty[x][y];
            }
        }

        class Entry implements Comparable<Entry> {
            public Double distance;
            public int x, y;

            public Entry(double distance, int x, int y) {
                this.distance = distance;
                this.x = x;
                this.y = y;
            }

            @Override
            public int compareTo(Entry other) {
                return this.distance.compareTo(other.distance);
            }
        }

        PriorityQueue<Entry> q = new PriorityQueue<>();
        q.add(new Entry(0, me.getX(), me.getY()));
        while(!q.isEmpty()) {
            Entry entry = q.poll();
            if(distances[entry.x][entry.y] != -1 && entry.distance >=  distances[entry.x][entry.y])
                continue;
            distances[entry.x][entry.y] = entry.distance;
            for(int dx = entry.x - 1; dx <= entry.x + 1; dx++)
                for(int dy = entry.y - 1; dy <= entry.y + 1; dy++){
                    if(dx < 0 || dy < 0 || dx >= width || dy >= height || (dx == entry.x && dy == entry.y) || !map[dx][dy].passable ||
                            gc.hasUnitAtLocation(new MapLocation(gc.planet(), dx, dy)))
                        continue;
                    double dangerousness = -map[entry.x][entry.y].teamControl;
                    if(dangerousness < 0)
                        dangerousness = 0;
                    double neighDist = entry.distance + nearPriority + dangerousness * safetyPriority;
                    q.add(new Entry(neighDist, dx, dy));
                }
        }

        double bestValue = 0;
        int bestX = 0, bestY = 0;
        for(int x = 0; x < width; x++)
            for(int y = 0; y < height; y++) {
                localValuation[x][y] = localValuation[x][y] - distances[x][y];
                MapLocation loc = new MapLocation(gc.planet(), x, y);
                if(needKarbonite) {
                    if (!gc.canSenseLocation(loc))
                        localValuation[x][y] = 0;
                    else if(gc.karboniteAt(loc) == 0)
                        localValuation[x][y] = 0;
                }
                if(localValuation[x][y] > bestValue) {
                    bestValue = localValuation[x][y];
                    bestX = x;
                    bestY = y;
                }
            }
        valuationPenalty[bestX][bestY] += 10.0;
        return new MapLocation(gc.planet(), bestX, bestY);
    }

    public UnitAgent createAgent(Unit unit) {
        switch(unit.unitType()) {
            case Factory:
                return new FactoryAgent(unit.id(), this);
            case Worker:
                return new WorkerAgent(unit.id(), this);
            case Ranger:
                return new RangerAgent(unit.id(), this);
            case Rocket:
                return new RocketAgent(unit.id(), this);
            default:
                return null;
        }
    }

    public interface AgentProcess {
        void call(UnitAgent agent);
    }

    void resolveValuation() {

        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                double karbonite;
                if(gc.canSenseLocation(new MapLocation(gc.planet(), x, y)))
                    karbonite = gc.karboniteAt(new MapLocation(gc.planet(), x, y));
                else {
                    karbonite = map[x][y].lastKarbonite;
                    if(map[x][y].teamControl < -0.1) // no idea what value to put here
                        karbonite *= Math.pow(0.99, map[x][y].turnsSinceSeen);
                }
                valuation[x][y] = karbonite;
                nearbyValuation[x][y] = karbonite;
                valuationPenalty[x][y] *= 0.98;
            }
        }

        for(int i = 0; i < 5; i++) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    for (int dx = x - 1; dx <= x + 1; dx++) {
                        for (int dy = y - 1; dy <= y + 1; dy++) {
                            if ((dx == x && dy == y) || dx < 0 || dy < 0 || dx >= width || dy >= height || !map[dx][dy].passable)
                                continue;
                            nearbyValuation[dx][dy] += 0.05 * nearbyValuation[x][y];
                        }
                    }
                }
            }
        }
    }

    public void applyToUnits(AgentProcess process)
    {
        VecUnit units = gc.myUnits();
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            UnitAgent agent = unitAgents.get(unit.id());
            if(agent == null) {
                unitAgents.put(unit.id(), createAgent(unit));
                agent = unitAgents.get(unit.id());
            }
            if(agent == null)
                System.out.printf("No agent for unit of type %s.", unit.unitType().toString());
            else {
                try {
                    process.call(agent);
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void resetStatistics(){
        numberOfUnits = new HashMap<>();
    }

    public void doStatistics(UnitAgent agent){
        UnitType type = agent.bcUnit().unitType(); 
        int currentCount = numberOfUnits.getOrDefault(type, 0);
        numberOfUnits.put(type, currentCount + 1);
    }

    public void updatePresences() {
        for(int x = 0; x < width; x++)
            for(int y = 0; y < height; y++) {
                Tile tile = map[x][y];
                if(!gc.canSenseLocation(new MapLocation(gc.planet(), x, y))) {
                    tile.teamControl += (tile.ourPresence - tile.enemyPresence) / 4.0;
                    for (int dx = x - 1; dx <= x + 1; dx++) {
                        for (int dy = y - 1; dy <= y + 1; dy++) {
                            if ((dx == x && dy == y) || dx < 0 || dy < 0 || dx >= width || dy >= height || !map[dx][dy].passable)
                                continue;
                            map[x][y].teamControl += 0.05 * tile.teamControl;
                        }
                    }
                    tile.teamControl *= 0.6;
                    tile.ourPresence *= 0.9;
                    tile.enemyPresence *= 0.9;
                    tile.turnsSinceSeen++;
                    continue;
                }
                tile.turnsSinceSeen = 0;
                tile.hasEnemyFactory = false;
                tile.hasEnemyRocket = false;
                tile.lastKarbonite = (int) gc.karboniteAt(new MapLocation(gc.planet(), x, y));
                if (gc.hasUnitAtLocation(new MapLocation(gc.planet(), x, y))) {
                    Unit unit = gc.senseUnitAtLocation(new MapLocation(gc.planet(), x, y));
                    if (unit.team() == gc.team())
                        tile.ourPresence += 1.0;
                    else {
                        tile.enemyPresence += 1.0;
                        if(unit.unitType() == UnitType.Factory)
                            tile.hasEnemyFactory = true;
                        if(unit.unitType() == UnitType.Rocket)
                            tile.hasEnemyRocket = true;
                    }
                }
                tile.teamControl += tile.ourPresence - tile.enemyPresence;
                for (int dx = x - 1; dx <= x + 1; dx++) {
                    for (int dy = y - 1; dy <= y + 1; dy++) {
                        if ((dx == x && dy == y) || dx < 0 || dy < 0 || dx >= width || dy >= height || !map[dx][dy].passable)
                            continue;
                        map[x][y].teamControl += 0.05 * tile.teamControl;
                    }
                }
                tile.teamControl *= 0.6;
                tile.ourPresence *= 0.9;
                tile.enemyPresence *= 0.9;
            }
    }

    public void doTurn() {
        tmpKarbonite = gc.karbonite(); //refreshing karbonite at each turn;
        if(turnNumber % 10 == 0)
            resolveValuation();
        updatePresences();
        resetStatistics();
        applyToUnits((agent) -> { doStatistics(agent); });
        applyToUnits((agent) -> { agent.prepareTurn(); });
        applyToUnits((agent) -> { agent.doTurn(); });
    }

    public void main()
    {
        if(gc.planet() != Planet.Earth) { // Mars placeholder
            while(true)
                gc.nextTurn();
        }
        while(true)
        {
            try{
                doTurn();
            }
            catch(Exception e){
                e.printStackTrace();
            }
            /*
		    if(turnNumber == 1) {
                for(int x = 0; x < width; x++)
                    for(int y = 0; y < height; y++) {
                        MapLocation target = findResources(new MapLocation(gc.planet(), x, y));
                        System.out.printf("%d %d: %d %d\n", x, y, target.getX(), target.getY());
                    }
            }
            */
            gc.nextTurn();
            turnNumber++;
        }
    }
}


