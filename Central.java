import bc.*;
import java.util.*;

class Central {
    GameController gc;
    HashMap<Integer, UnitAgent> unitAgents;

    public Central() {
        gc = new GameController();
        unitAgents = new HashMap<Integer, UnitAgent>();
    }

    public Path findPath(MapLocation from, MapLocation to) {
        return new Path();
    }

    public Direction nextStep(Path path, MapLocation me) {
        // placeholder
        return Direction.West;
    }

    public MapLocation findResources(MapLocation me) {
        // placeholder
        return new MapLocation(Planet.Earth, 0, 0);
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
        public void call(UnitAgent agent);
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

    public void doTurn() {
        /*VecUnit units = gc.myUnits();
        for (int i = 0; i < units.size(); i++) {
            Unit unit = units.get(i);
            UnitAgent agent = unitAgents.get(unit.id(()));
            if(agent == null) {
                unitAgents.put(createAgent(unit));
                agent = unitAgents.get(unit.id(()));
            }
            agent.prepareTurn();
        }*/

        applyToUnits((agent) -> { agent.prepareTurn(); });
        applyToUnits((agent) -> { agent.doTurn(); });
    }

    public void main()
    {
        while(true)
        {
            doTurn();
            gc.nextTurn();
        }
    }
}