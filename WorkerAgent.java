import bc.*;

class WorkerAgent extends UnitAgent {
    public WorkerAgent(int id, Central central) {
        super(id, central);
    }

    public void prepareTurn() {

    }

    public void doTurn() {
        move(Direction.North);
    }
}