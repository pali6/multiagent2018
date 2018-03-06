import bc.*;

abstract class UnitAgent {
    int id;
    Central central;

    public UnitAgent(int id, Central central) {
        this.id = id;
        this.central = central;
    }

    public boolean canMove(Direction dir) {
        return central.gc.isMoveReady(id) && central.gc.canMove(id, dir);
    }

    public boolean move(Direction dir) {
        if(!canMove(dir))
            return false;
        central.gc.moveRobot(id, dir);
        return true;
    }

    abstract public void prepareTurn();
    abstract public void doTurn();
}