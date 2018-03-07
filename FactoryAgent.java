import bc.*;

class FactoryAgent extends UnitAgent {
    public FactoryAgent(int id, Central central) {
        super(id, central);
    }

    public void prepareTurn() {

    }

    public boolean produceRobot(UnitType type) {
        if(!central.gc.canProduceRobot(id, type))
            return false;
        central.gc.produceRobot(id, type);
        return true;
    }

    public void doTurn() {
        //produceRobot(central.neededRobotType());
    }
}
