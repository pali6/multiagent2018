import bc.*;
import java.util.*;

class Central {
    public GameController gc;
    HashMap<Integer, UnitAgent> unitAgents;
    HashMap<UnitType, Integer> numberOfUnits;
    Navigation navigation;
    int turnNumber = 0;

    class Tile {
        boolean passable;
        double enemyPresence;
        double ourPresence;
        double teamControl;
        int lastKarbonite;
        int turnsSinceSeen;
    };

    public Tile[][] map;
    public int height, width;

    public Central() {
        gc = new GameController();
        unitAgents = new HashMap<>();
        numberOfUnits = new HashMap<>();
        navigation = new Navigation(gc, gc.planet());

        PlanetMap planetMap = gc.startingMap(gc.planet());
        height = (int) planetMap.getHeight();
        width = (int) planetMap.getWidth();
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
        gc.queueResearch(UnitType.Healer); // turn 375
        gc.queueResearch(UnitType.Healer); // turn 475
        gc.queueResearch(UnitType.Healer); // turn 575

        gc.queueResearch(UnitType.Rocket); // turn 625
        gc.queueResearch(UnitType.Rocket); // turn 725
        gc.queueResearch(UnitType.Rocket); // turn 825

        gc.queueResearch(UnitType.Mage); // random stuff
        gc.queueResearch(UnitType.Mage);
        gc.queueResearch(UnitType.Mage);
    }

    public Path findPath(MapLocation from, MapLocation to) {
        return navigation.findPath(from, to);
    }

    public Direction nextStep(Path path, MapLocation me) {
        return navigation.getDirection(path, me);
    }

    public MapLocation findExactResources(MapLocation me) {
        return findResourcesInternal(me, 2.0, 0.5, 0.3, 0.0, true);
    }

    public MapLocation findResources(MapLocation me) {
        return findResourcesInternal(me, 1.0, 0.5, 1.0, 1.0, false);
    }

    public MapLocation findResourcesInternal(MapLocation me, double amountPriority, double nearPriority,
                                             double safetyPriority, double nearbyAmountPriority, boolean needKarbonite) {

        double[][] valuation = new double[width][height];
        double[][] distances = new double[width][height];
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
                valuation[x][y] += karbonite * amountPriority;
                distances[x][y] = -1;
            }
        }

        for(int i = 0; i < 5; i++) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    for (int dx = x - 1; dx <= x + 1; dx++) {
                        for (int dy = y - 1; dy <= y + 1; dy++) {
                            if ((dx == x && dy == y) || dx < 0 || dy < 0 || dx >= width || dy >= height || !map[dx][dy].passable)
                                continue;
                            valuation[dx][dy] += 0.05 * valuation[x][y] * nearbyAmountPriority;
                        }
                    }
                }
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
            if(entry.distance >=  distances[entry.x][entry.y])
                continue;
            distances[entry.x][entry.y] = entry.distance;
            for(int dx = entry.x - 1; dx <= entry.x + 1; dx++)
                for(int dy = entry.y - 1; dy <= entry.y + 1; dy++){
                    if(dx < 0 || dy < 0 || dx >= width || dy >= height || (dx == entry.x && dy == entry.y) || !map[dx][dy].passable)
                        continue;
                    double dangerousness = -map[entry.x][entry.y].teamControl;
                    if(dangerousness < 0)
                        dangerousness = 0;
                    double neighDist = entry.distance + dangerousness * safetyPriority;
                    q.add(new Entry(neighDist, dx, dy));
                }
        }

        double bestValue = 0;
        int bestX = 0, bestY = 0;
        for(int x = 0; x < width; x++)
            for(int y = 0; y < height; y++) {
                valuation[x][y] = valuation[x][y] * (1 - nearPriority) + distances[x][y] * nearPriority;
                MapLocation loc = new MapLocation(gc.planet(), x, y);
                if(needKarbonite) {
                    if (!gc.canSenseLocation(loc))
                        valuation[x][y] = 0;
                    else if(gc.karboniteAt(loc) == 0)
                        valuation[x][y] = 0;
                }
                if(valuation[x][y] > bestValue) {
                    bestValue = valuation[x][y];
                    bestX = x;
                    bestY = y;
                }
            }

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
            default:
                return null;
        }
    }

    public interface AgentProcess {
        void call(UnitAgent agent);
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
                System.out.println("No agent for unit.");
            else
                process.call(agent);
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
                    // TODO
                    tile.turnsSinceSeen++;
                    continue;
                }
                tile.turnsSinceSeen = 0;
                tile.lastKarbonite = (int) gc.karboniteAt(new MapLocation(gc.planet(), x, y));
                if (gc.hasUnitAtLocation(new MapLocation(gc.planet(), x, y))) {
                    Unit unit = gc.senseUnitAtLocation(new MapLocation(gc.planet(), x, y));
                    if (unit.team() == gc.team())
                        tile.ourPresence += 1.0;
                    else
                        tile.enemyPresence += 1.0;
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
