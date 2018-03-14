import bc.*;

class FactoryAgent extends UnitAgent {
	private Produce produce;

    public FactoryAgent(int id, Central central) {
        super(id, central);
        produce = new IsBuilt(id, central);
        System.out.println("created: round: " + central.turnNumber);
        
    }

    public void prepareTurn() {
    //	System.out.println("process");
    		if (produce instanceof Idl) {
    			produce = new Robot(central.needRobotType(), id);
    		}
    
    		produce = produce.processOccupation(central);
    }

    public void doTurn() {
        	//System.out.println("do");
    		produce = produce.doOccupation(central);
        //produceRobot(central.neededRobotType());
    }
}
	
abstract class Produce {
	int id;

	public Produce(int id) {
		this.id = id;
	}
	abstract public Produce processOccupation(Central central);
	abstract public Produce doOccupation(Central central);
}

class Nothing extends Produce {
	int timeToWait;
	
	public Nothing(int id, int time) {
		super(id);
		timeToWait = time;
	}

	public Produce processOccupation(Central central) {
		return this;
	}
	
	public Produce doOccupation(Central central) {
		timeToWait--;
		if (timeToWait <= 0) {
			Direction direction = null;
			Direction[] directions = Direction.values();
			for (int i=1; i<9; i++) {
				if (central.gc.canUnload(id, directions[i])) {
					direction = directions[i];
				}
			}
			if (direction != null) {
				//System.out.println("unloaded robot");
				central.gc.unload(id, direction);
				return new Idl(id);
			}
			//System.out.println("robot made");
			return this;
			
		}
		return this;
	}

}

class Idl extends Produce {

	public Idl(int id) {
		super(id);
	}	
	
	public Produce processOccupation(Central central) {
		//System.out.println("wasting rounds (process)");
		return this;
	}
	public Produce doOccupation(Central central) {
		//System.out.println("wasting rounds (do)");
		return this;
	}
}

class Robot extends Produce {
	private UnitType type;
	
	public Robot(UnitType t, int id) {
		super(id);
		type = t;
	}
	
	public Produce processOccupation(Central central) {
		
		if (central.tmpKarbonite  < 50) {
			//System.out.println("no karbonite");
			return new Idl(id);
		} else {
			central.tmpKarbonite -= 50;
			return this;
		}
	}
	public Produce doOccupation(Central central) {
		central.gc.produceRobot(id, type);
		//System.out.println("Producing robot");
		return new Nothing(id, 16);
	}

}
class IsBuilt extends Produce {
	Unit unit;

	public IsBuilt(int id, Central central) {
		super(id);
		unit = central.gc.unit(id);
	}	
	
	public Produce processOccupation(Central central) {
		//System.out.println("wasting rounds (process)");
		
		return this;
	}

	public Produce doOccupation(Central central) {
	      /*System.out.print("round: " + central.turnNumber);
		System.out.println(" structure: " +unit.structureIsBuilt());*/
		unit = central.gc.unit(id);
		if (unit.health() == 300) {
			//System.out.println("BUILT!");
			return new Idl(id);
		} 
		return this;
	}
}

