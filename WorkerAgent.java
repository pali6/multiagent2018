import bc.*;

class WorkerAgent extends UnitAgent {
    	Occupation occupation;
    	//MapLocation worker_location;

    	public WorkerAgent(int id, Central central) {
    	    super(id, central);
    	    MapLocation worker_location = central.gc.unit(id).location().mapLocation();
    	    this.type = central.gc.unit(id).unitType();
    	    //change when dealing with workers that are not on earth
    	    occupation = new Idle(worker_location, id);
    	}

    	public void prepareTurn() {
    		long treshold = 100;
		if (occupation instanceof Idle ) {
			if (central == null) System.out.println("Central is null");
			if (central.gc == null) System.out.println("gc is null");
			//if (central.gc.round() == null) System.out.println("round is null");
			if (central.gc.round() == 1) {
				occupation = new Replicating(central.gc.unit(id).location().mapLocation(), id);
				System.out.println("replicating occupation created");
			} else if (central.tmpKarbonite >= 60){
				occupation = new Replicating(central.gc.unit(id).location().mapLocation(), id);
				central.tmpKarbonite -= 60;
				System.out.println("replicating occupation (2) created");
			} else if (central.tmpKarbonite < treshold) { //trashold can be changed
				//go harvest
				MapLocation my_loc = ((Idle)occupation).getLocation();
				MapLocation karbonite_loc = central.findResources(my_loc);
				//long res = central.gc.karboniteAt(karbonite_loc);
				//System.out.println("Karbonite resources at loc: " + res);
				Path p = central.findPath(my_loc, karbonite_loc);
				System.out.printf("X:%dY:%d%n", karbonite_loc.getX(), karbonite_loc);
				occupation = new Arriving(my_loc, id, karbonite_loc, new Harvesting(karbonite_loc, id, karbonite_loc), p);
			}
		}
		try {	
			occupation = occupation.processOccupation(central);
			//return true;
		} catch (Exception e) {
			System.out.println("Error: " + e.toString());
			//return false;
		}
    }

    public void doTurn() {
        try {

			occupation = occupation.doOccupation(central);
			//return true;
		} catch (Exception e) {
			System.out.println("Error: " + e.toString());
			//return false;
		}
    }
    /*
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
    }*/
    
}



abstract class Occupation {
	protected MapLocation worker_location;
	protected int worker_id;

	public Occupation(MapLocation wl, int id) {
		worker_location = wl;
		worker_id = id;
		//decide on occupation
	}
	abstract public Occupation processOccupation(Central central);
	abstract public Occupation doOccupation(Central central);
}

class Arriving extends Occupation {
	private MapLocation goal;
	private Occupation purpose;
	private Direction direction;
	private Path path;

	public Arriving(MapLocation wl, int id, MapLocation g, Occupation p, Path pth) {
		super(wl,id);
		goal = g;
		purpose = p;
		path = pth;
		direction = null;

	}
	public Arriving(MapLocation wl, int id,Occupation p) {
		super(wl,id);
		//if only one step is nessesary
		direction = null;
		path = null;
		goal = null;
	}

	public Occupation processOccupation(Central central) {
		//can move takes in account only map terrain
		if (goal == null) {
				//if only one step is necessary
				Direction[] directions = Direction.values();
		 		//find a feasable direction
				for (int i=1; i<9; i++) {
					if (central.gc.canMove(worker_id, directions[i])) {
						direction = directions[i];
						return this;
					} 
				}
			}
		else if (direction == null) {
			central.nextStep(path,worker_location);
		} 
		
		if (central.gc.canMove(worker_id, direction)) {
			//does not take heat into account -> takes map into account
			return this;
		} else {
			
			//TODO: handle unfeasible move
			return this;
		}
	}

	public Occupation doOccupation(Central central) {
		if (central.gc.canMove(worker_id, direction)) {
			//if mapLocation in direction is empy, we can try and make a move, if heat of worker is low enough;
			
	 		//isMoveReady takes heat into account
			if (goal == null) {
				//just doing one step
				if (central.gc.isMoveReady(worker_id)) {
					System.out.println("Doing a step - with no goal");
					central.gc.moveRobot(worker_id, direction);
					worker_location = worker_location.add(direction);
					return purpose;
				}
			}//part where only one step is neccesary to get avalible position to blueprint/replicate (stopped by return)
			
			//making a move, if possible
			if (central.gc.isMoveReady(worker_id)) {
				System.out.println("Doing a step - with goal");
				central.gc.moveRobot(worker_id, direction);
				worker_location = worker_location.add(direction);
				if (worker_location.equals(goal)) {
					System.out.println("Reached goal");
					//goal is reached, next step is to do what we came do to
					//not sure if this acctualy works - depends on how MapLocation is implemented
					return purpose;
				} else {
					System.out.println("goal not reached yet");
					System.out.printf("current loc: X:%d Y:%d", worker_location.getX(), worker_location.getY());
					//if goal is yet to be reached, next task is still moving, changing direction (roker_location was updated)
					direction = central.nextStep(path, worker_location);
					return this;
				}
			} else {
				//move was not possible in this turn, will happen next turn
				return this;
			}
		} else {
			//waiting another turn to get path freed
			System.out.println("Something is in the way");
			return this;
		}	

	}

}

class Harvesting extends Occupation {
	private MapLocation locationOfKarbonite;
	private Direction direction;


	public Harvesting(MapLocation wl, int id,MapLocation karbonite) {
		super(wl,id);
		locationOfKarbonite = karbonite;
		if (!worker_location.isAdjacentTo(locationOfKarbonite)) {
			//throw new Exception e();
		} else {
			direction = worker_location.directionTo(locationOfKarbonite);
		}
	}
	
	public Harvesting(MapLocation wl, int id, Direction karbonite) {
		super(wl,id);
		direction = karbonite;
		locationOfKarbonite = worker_location.add(direction);
		
	}
	


	public Occupation processOccupation(Central central) {
		if (central.gc.karboniteAt(locationOfKarbonite) <= 0) {
			//no karbon here left, should get new task
			return new Idle(worker_location, worker_id);
		} else {
			//still harvesting in next round
			return this;
		}

	}

	public Occupation doOccupation(Central central) {
		if (central.gc.canHarvest(worker_id, direction)) {
			//no action this round, if spot has karbonite
			central.gc.harvest(worker_id, direction);

		}
		if (central.gc.karboniteAt(locationOfKarbonite) <= 0) {
			//this can happen due to some other worker harvesting the same spot or we just exhausted the spot
			return new Idle(worker_location, worker_id);
		} else {
			//continue harvesting the same spot
			return this;
		}
		
	}
}



class Idle extends Occupation {

	public Idle(MapLocation wl, int id) {
		super(wl,id);
		//decide on occupation
	}
	public Occupation processOccupation(Central central) {
		//TODO: ask Central for another task
		//Occupation newTask = Central.getWorkerTask(locationOfWorker);
		//newTask.processOccupation();
		//return newTask;
		//best not to call
		System.out.println("CALLING (process) ON IDLE!!!");
		return this;
	}

	public Occupation doOccupation(Central central) {
		//should not be called EVER
		System.out.println("CALLING (do) ON IDLE!!!");
		return this;
	}
	
	public MapLocation getLocation() {
		return worker_location;
	}
}

class PlacingBlueprint extends Occupation {
	private Direction direction;
	private UnitType structure;

	public PlacingBlueprint(MapLocation wl, int id,UnitType s) {
		super(wl,id);
		structure = s;
		direction = null;
	}

	public PlacingBlueprint(MapLocation wl, int id, MapLocation blueprint, UnitType s) {
		super(wl,id);
		structure = s;
		direction = worker_location.directionTo(blueprint);

	}

	public Occupation processOccupation(Central central) {
		if (direction != null) {
			if (central.gc.canBlueprint(worker_id, structure, direction)) return this;
		}

		if (direction == null) {
			Direction[] directions = Direction.values();
	 		//find a feasable 
			for (int i=1; i<9; i++) {
				//canBlueprint takes into consideration Karbonite resources, heat, if rocket can be built
				if (central.gc.canBlueprint(worker_id, structure, directions[i])) {
					direction = directions[i];
					break;
				} 
			}
		}

		if(direction == null) {
			//no feasible direction to build found
			//go somewhere where you can build something (just try to move one spot)
			return new Arriving(worker_location, worker_id, this);
		} else {
			return this;
		}
	}

	public Occupation doOccupation(Central central) {
		if (central.gc.isOccupiable(worker_location.add(direction))>0) {
			//could be, that before making turn something gets in the way
			//we will just try waiting another round for now
			return this;
		}
		try {
			//maybe karbonite is too low
			central.gc.blueprint(worker_id, structure, direction);
			//I hope this will work!!
			int blueprint_id = (central.gc.senseUnitAtLocation(worker_location.add(direction))).id();
			//gets to build the blueprint BUT!! blueprint id is needed!! i hope bluprint is set up right away
			return new Building(worker_location, worker_id, blueprint_id);
		} catch (Exception e) {
			System.out.println(e.toString());
			//for some reason we could not blueprint
			//we will just try in the next round for now
			return this;
		}
	}

}

class Building extends Occupation {
	private int blueprint_id;


	public Building(MapLocation wl, int id, int idOfBlueprint) {
		super(wl,id);
		blueprint_id = idOfBlueprint;
	}

	public Occupation processOccupation(Central central) {
		//can build if adjacent and if worker hasnt made a move yet
		if ((central.gc.unit(blueprint_id)).health() >= (central.gc.unit(blueprint_id)).maxHealth()) {
			//is fully built -> get another task
			//TODO: call central to get task
			return new Idle(worker_location, worker_id);
		}
		if (central.gc.canBuild(worker_id, blueprint_id)) {
			return this;
		} else {
			//TODO: call Central for antoher task
			return new Idle(worker_location, worker_id);
		}
	}

	public Occupation doOccupation(Central central) {
		if (central.gc.canBuild(worker_id, blueprint_id)) {
			//throws exception if allready built
			central.gc.build(worker_id, blueprint_id);
			if ((central.gc.unit(blueprint_id)).health() >= (central.gc.unit(blueprint_id)).maxHealth()) {
				//is fully built -> get another task
				//TODO: call central to get task
				return new Idle(worker_location, worker_id);
			}
			return this;
		} else {
			//TODO: call Central for antoher task
			return new Idle(worker_location, worker_id);
		}
	}

}

class Replicating extends Occupation {
	private Direction direction;
	
	public Replicating(MapLocation wl, int id, Direction d) {
		super(wl,id);
		direction = d;
	}
	
	public Replicating(MapLocation wl, int id) {
		super(wl,id);
		direction = null;
	}
	
	public Occupation processOccupation(Central central) {
		if (direction == null) {
			Direction[] directions = Direction.values();
		 	//find a feasable direction
			for (int i=1; i<9; i++) {
				if (central.gc.isOccupiable(worker_location.add(directions[i]))>0) {
					direction = directions[i];
					return this;
				} 
			}
		} else {
			if (central.gc.isOccupiable(worker_location.add(direction))>0) {
				//if worker can replicate in direction
				return this;
			} else {
				Direction[] directions = Direction.values();
			 	//find a feasable direction
				for (int i=1; i<9; i++) {
					if (central.gc.isOccupiable(worker_location.add(directions[i]))>0) {
							direction = directions[i];
							return this;
						} 
					}
			}
			//if no feasible location was found
		}
		direction = Direction.values()[0];
		return this;
	
	}
	
	public Occupation doOccupation(Central central) {
		if (central.gc.canReplicate(worker_id, direction)) {
			System.out.println("Worker replicated");
			//takes into account karbonite, heat, location
			central.gc.replicate(worker_id, direction);
			return new Idle(worker_location, worker_id);
		} else {
			//do it next turn
			return this;
		}
	}

} 
