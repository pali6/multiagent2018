import bc.*;

abstract class UnitAgent {
    int id;
    Central central;
    UnitType type;

    public UnitAgent(int id, Central central) {
        this.id = id;
        this.central = central;
        this.type = central.gc.unit(id).unitType();
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

    public Unit bcUnit() {
        return central.gc.unit(id);
    }

    abstract public void prepareTurn();
    abstract public void doTurn();
}