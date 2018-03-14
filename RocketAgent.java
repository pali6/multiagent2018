import bc.*;

public class RocketAgent extends UnitAgent {
    public int nInside = 0;

    long marsWidth;
    long marsHeight;

    // how many turns rocket waited without anyone entering
    public int turnsWaiting = 0;
    public final int MAX_WAIT_FOR_TURNS = 100;

    public RocketAgent(int id, Central central) {
        super(id, central);
        marsHeight = central.gc.startingMap(Planet.Mars).getHeight();
        marsWidth = central.gc.startingMap(Planet.Mars).getWidth();
    }

    public void prepareTurn() {}

    public void doTurn() {
        if(central.gc.planet() == Planet.Mars) {
            for(int i = 0; i < Direction.values().length; i++)
                if(central.gc.canUnload(id, Direction.values()[i]))
                    central.gc.unload(id, Direction.values()[i]);
        }
        else {
            turnsWaiting++;
            if(central.gc.unit(id).structureMaxCapacity() == nInside
                    || central.gc.unit(id).health() < 10
                    || central.turnNumber > 725
                    || (nInside > 0 & turnsWaiting > MAX_WAIT_FOR_TURNS)
                    ) {
                MapLocation landingLoc;
                int locationsTried = 0;
                int minPassableNeighbours;
                do {
                    int x = central.rng.nextInt((int)central.gc.startingMap(Planet.Mars).getWidth());
                    int y = central.rng.nextInt((int)central.gc.startingMap(Planet.Mars).getHeight());
                    landingLoc = new MapLocation(Planet.Mars, x, y);
                    locationsTried++;
                    minPassableNeighbours = 8 - locationsTried/10;
                    if(getPassableNeighboursCount(x,y, central) < minPassableNeighbours) {
                        continue;
                    }
                } while(central.gc.startingMap(Planet.Mars).isPassableTerrainAt(landingLoc) == 0);
                if(central.gc.canLaunchRocket(id, landingLoc)) {
                    central.gc.launchRocket(id, landingLoc);
                    turnsWaiting = 0;
                }
            }
        }
    }

    private int getPassableNeighboursCount(int x, int y, Central central){
        int result = 0;
        MapLocation neighboringLocation = new MapLocation(Planet.Mars, x, y);
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if(i == 0 && j == 0) {
                    continue;
                }
                int nx = x + i;
                int ny = y + j;
                if (nx < 0 || ny < 0 || nx >= marsWidth || ny >= marsHeight) {
                    continue;
                }
                neighboringLocation.setX(nx);
                neighboringLocation.setY(ny);
                if(central.gc.startingMap(Planet.Mars).isPassableTerrainAt(neighboringLocation) != 0){
                    result++;
                }
            }
        }
        return result;
    }
}
