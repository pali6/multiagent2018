import bc.*;

public class RocketAgent extends UnitAgent {
    public int nInside = 0;

    public RocketAgent(int id, Central central) {
        super(id, central);
    }

    public void prepareTurn() {}

    public void doTurn() {
        if(central.gc.planet() == Planet.Mars) {
            for(int i = 0; i < Direction.values().length; i++)
                if(central.gc.canUnload(id, Direction.values()[i]))
                    central.gc.unload(id, Direction.values()[i]);
        }
        else {
            if(central.gc.unit(id).structureMaxCapacity() == nInside || central.gc.unit(id).health() < 10 || central.turnNumber > 725) {
                MapLocation landingLoc;
                do {
                    landingLoc = new MapLocation(Planet.Mars,
                            central.rng.nextInt((int)central.gc.startingMap(Planet.Mars).getWidth()),
                            central.rng.nextInt((int)central.gc.startingMap(Planet.Mars).getHeight()));
                } while(central.gc.startingMap(Planet.Mars).isPassableTerrainAt(landingLoc) == 0);
                if(central.gc.canLaunchRocket(id, landingLoc))
                    central.gc.launchRocket(id, landingLoc);
            }
        }
    }
}
