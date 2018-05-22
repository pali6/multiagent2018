import bc.*;

import java.util.Map;
import java.util.Random;

class WorkerAgent extends UnitAgent {
    	Occupation occupation;
    	//MapLocation worker_location;
    	boolean isMiner;

    	public WorkerAgent(int id, Central central) {
    	    super(id, central);
    	    this.type = central.gc.unit(id).unitType();
    	    //change when dealing with workers that are not on earth
    	    occupation = new Idle(null, id);
    	    if (central.minersNeeded > 0) {
    	    		isMiner = true;
    	    		central.minersNeeded--;
    	    } else if (central.buildersNeeded > 0) {
    	    		isMiner = false;
    	    		central.buildersNeeded--;
    	    		MapLocation worker_location = central.gc.unit(id).location().mapLocation();
			//System.out.println("going harvesting");
			MapLocation karbonite_loc = central.findResources(worker_location);
			occupation = new Harvesting(worker_location, id, karbonite_loc);
    	    } else isMiner = true;
    	}

    	public void prepareTurn() {
    		if (central.turnNumber>450 && (new Random()).nextInt(3)==0) {
    			isMiner = false;
    			occupation = new Idle(null, id);
    		}
    		//System.out.println("id (process): " + id);
		if (occupation instanceof Idle ) {
			//System.out.println("giving occupation to idle worker");
			if (central.gc.round() == 1 && central.tmpKarbonite >= 60) {
				central.buildersNeeded--;
				isMiner = false;
				//central.earthBase = central.gc.unit(id).location().mapLocation();
				central.tmpKarbonite -= 60;
				
				occupation = new Replicating(central.gc.unit(id).location().mapLocation(), id);
			} else {
				Location loc = central.gc.unit(id).location();
				if (!loc.isInGarrison()) {
					if (isMiner) {
						
							if (central.needWorkers() && central.tmpKarbonite >= 60 && (new Random()).nextInt(3) == 0) {
								//System.out.println("Miner replicated");
								central.tmpKarbonite -= 60;
								occupation = new Replicating(central.gc.unit(id).location().mapLocation(), id);
							} else {
								
								//25 miners, they will mine
									MapLocation worker_location = central.gc.unit(id).location().mapLocation(); //even if not on earth
									//System.out.println("going harvesting");
									MapLocation karbonite_loc = central.findResources(worker_location);
									occupation = new Harvesting(worker_location, id, karbonite_loc).processOccupation(central);
							}
						
					} else {

						if (loc.isOnPlanet(Planet.Earth)) {
							MapLocation worker_location = loc.mapLocation();
							/*if (worker_location.distanceSquaredTo(central.earthBase) > 10) {
								//go to base:
								Path pth = central.findPath(worker_location, central.earthBase);
								occupation = new Arriving(worker_location, id, central.earthBase, new Idle(null, id), pth);
							}*/
							if (central.needFactory() && central.tmpKarbonite >= 200 ) {
								central.tmpKarbonite -= 200;
								occupation = new PlacingBlueprint(worker_location, id, UnitType.Factory);
							} else if (central.needRocket() && central.tmpKarbonite >= 150 ) {
								central.tmpKarbonite -= 150;
								occupation = new PlacingBlueprint(worker_location, id, UnitType.Rocket);
							} else if (central.needWorkers() && central.tmpKarbonite >= 60) {
								central.tmpKarbonite -= 60;
								occupation = new Replicating(central.gc.unit(id).location().mapLocation(), id);
							} /*else if (central.tmpKarbonite >= 500) {
								central.tmpKarbonite -= 60;
								occupation = new Replicating(central.gc.unit(id).location().mapLocation(), id);
							} */else {
								VecUnit rockets = central.gc.senseNearbyUnitsByType(worker_location, 1, UnitType.Rocket);
								if (rockets.size()>0) {
									occupation = new Building(worker_location, id, rockets.get(0).id());
								} else {
									VecUnit factoryes = central.gc.senseNearbyUnitsByType(worker_location,  1, UnitType.Factory);
									if (rockets.size()>0) {
										occupation = new Building(worker_location, id, factoryes.get(0).id());
									}
								}
								//possibly find a building that needs building
								//if there is nothig to do just do a random step
								//occupation = new Arriving(worker_location, id, new Idle(null, id));
							}

						}
					}
					if (occupation instanceof Idle ) new Arriving(central.gc.unit(id).location().mapLocation(), id, new Idle(null, id));
				}
			}
		}

		try {
			if (occupation == null) occupation = new Idle(null, id);	
			occupation = occupation.processOccupation(central);
			//return true;
		} catch (Exception e) {
			System.out.println("Error: " + e.toString());
			e.printStackTrace();
			//return false;
		}
    }

    public void doTurn() {
        try {
			//System.out.println("id (do): " + id);
			occupation = occupation.doOccupation(central);
			//return true;
		} catch (Exception e) {
			System.out.println("Error: " + e.toString());
			e.printStackTrace();
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
		Occupation ocp = new MineInBetween(worker_location,worker_id, this);
		ocp = ocp.processOccupation(central);
		if (!ocp.equals(this)) {
			return ocp;
		}
		//System.out.println("arriving (process)");
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
				if (direction == null) return new Idle(null, worker_id);
		}
		else if (direction == null) {
			if (path != null) {
				direction = central.nextStep(path,worker_location);
				return this;
			}
		} 
		if (path == null) return new Idle(null, worker_id);
		
		return this;
		
		/*if (direction != null && central.gc.canMove(worker_id, direction)) {
			//does not take heat into account -> takes map into account
			return this;
		} else {
			if (direction == null) {
				//System.out.println("direction is null (process)");
				if (purpose instanceof Harvesting) {
					MapLocation goal = central.findExactResources(worker_location);
					//maybe close enough to get exact resources position
					//System.out.printf("current loc: X:%d Y:%d%n", worker_location.getX(), worker_location.getY());
					//System.out.printf("goal loc: X:%d Y:%d%n", goal.getX(), goal.getY());
					return (new Harvesting(worker_location, worker_id, goal)).processOccupation(central);
				}
				System.out.println("cant move (process)");
				Direction[] directions = Direction.values();
		 		//find a feasable direction
				for (int i=1; i<9; i++) {
					if (central.gc.canMove(worker_id, directions[i])) {
						direction = directions[i];
						return this;
					}
				}
			}
			else if (central.gc.canMove(worker_id, direction)) {
				return this;
			} else {
				System.out.println("cant move (process)");
				Direction[] directions = Direction.values();
		 		//find a feasable direction
				for (int i=1; i<9; i++) {
					if (central.gc.canMove(worker_id, directions[i])) {
						direction = directions[i];
						return this;
					}
				}
				
			//TODO: handle unfeasible move
			//if not path around is found wait a few turns, o/w assign new occupation
			}
		}
		return new Idle(null, worker_id);*/
	}

	public Occupation doOccupation(Central central) {
		//System.out.println("arriving(do)");
		if (direction == null) {
			Direction[] directions = Direction.values();
			for (int i=1; i<9; i++) {
					if (central.gc.canMove(worker_id, directions[i])) {
						direction = directions[i];
					} 
				}
				if (direction == null) return new Idle(null, worker_id);
			//System.out.println("direction is null (do)");
		}
		if (central.gc.canMove(worker_id, direction)) {
			//if mapLocation in direction is empty, we can try and make a move, if heat of worker is low enough;
			
	 		//isMoveReady takes heat into account
			if (goal == null) {
				//just doing one step
				if (central.gc.isMoveReady(worker_id)) {
					//System.out.println("Doing a step - with no goal");
					central.gc.moveRobot(worker_id, direction);
					worker_location = worker_location.add(direction);
					return purpose;
				} return this;
			}//part where only one step is neccesary to get avalible position to blueprint/replicate (stopped by return)
			
			//making a move, if possible
			if (central.gc.isMoveReady(worker_id)) {
				//System.out.println("Doing a step - with goal");
				central.gc.moveRobot(worker_id, direction);
				worker_location = worker_location.add(direction);
				if (worker_location.equals(goal)) {
					//System.out.println("Reached goal");
					
					//goal is reached, next step is to do what we came do to
					//not sure if this acctualy works - depends on how MapLocation is implemented
					return purpose;
				} else {
					if (path == null) path = central.findPath(worker_location, goal); //trying to get a path from new position
					if (path == null) return new Idle(worker_location, worker_id); //if cannot find path, just go to idle
					//System.out.println("ROUND: " + central.gc.round() + " goal not reached yet");
					//System.out.printf("current loc: X:%d Y:%d%n", worker_location.getX(), worker_location.getY());
					//System.out.printf("goal loc: X:%d Y:%d%n", goal.getX(), goal.getY());
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
			//System.out.println("Something is in the way/overheated");
			return this;
		}	

	}

}

class Harvesting extends Occupation {
	private MapLocation locationOfKarbonite;
	private Direction direction;


	public Harvesting(MapLocation wl, int id, MapLocation karbonite) {
		super(wl,id);
		locationOfKarbonite = karbonite;
		/*if (!worker_location.isAdjacentTo(locationOfKarbonite)) {
			//throw new Exception e();
		} else {
			direction = worker_location.directionTo(locationOfKarbonite);
		}*/
	}
	
	public Harvesting(MapLocation wl, int id, Direction karbonite) {
		super(wl,id);
		direction = karbonite;
		locationOfKarbonite = worker_location.add(direction);
		
	}
	


	public Occupation processOccupation(Central central) {
		if (worker_location == null) {
			worker_location = central.gc.unit(worker_id).location().mapLocation();
		}
	
		//System.out.println("processing harvesting");
		if (worker_location.equals(locationOfKarbonite)) {
			direction = Direction.Center;
		} else  {
			if (!worker_location.isAdjacentTo(locationOfKarbonite)) {
				MapLocation tmp = worker_location;
				worker_location = null;
				//System.out.println("karbointe not adjacent");
				//System.out.printf("- my_loc: (%d,%d) karb_loc: (%d,%d)%n", tmp.getX(), tmp.getY(), locationOfKarbonite.getX(), locationOfKarbonite.getY());
				Path pth = central.findPath(tmp, locationOfKarbonite);
				if (pth == null) return (new Arriving(tmp, worker_id, this)).processOccupation(central);
				return (new Arriving(tmp, worker_id, locationOfKarbonite, this, pth)).processOccupation(central);
			} else {
				//System.out.println("karbonite adj, setting direction");
				direction = worker_location.directionTo(locationOfKarbonite);
			}
		}
		
		if (central.gc.karboniteAt(locationOfKarbonite) <= 0) {
			//return new Idle(worker_location, worker_id);
			/*if (!central.needResources()) {
				System.out.println("no need for resources rn");
				return new Idle(worker_location, worker_id);
			}*/   //TODO DECINE ON STRATEGY!!
			MapLocation tmp = worker_location;
			worker_location = null;
			//System.out.println("no karbonite left, finding new resources (process)");
			//no karbon here left, should get new task
			MapLocation new_loc = central.findExactResources(tmp);
			locationOfKarbonite = new_loc;
			//System.out.printf("no karbonite left, finding new resources, new_loc: (%d,%d)%n", locationOfKarbonite.getX(), locationOfKarbonite.getY());
			if (!tmp.isAdjacentTo(locationOfKarbonite)) {
				//System.out.println("finding path to new_loc");
				Path pth = central.findPath(tmp, locationOfKarbonite);
				if (pth == null) return new Idle(tmp,worker_id);
				return (new Arriving(tmp, worker_id, locationOfKarbonite, this, pth)).processOccupation(central);
			} else {
				worker_location = tmp;
				//if (locationOfKarbonite == null) System.out.println("locOfKarb je null");
				direction = worker_location.directionTo(locationOfKarbonite);
				return this;
			}
		} else {
			//System.out.println("everything fine, gonna harvest");
			//still harvesting in next round
			return this;
		}

	}

	public Occupation doOccupation(Central central) {
		//System.out.println("harvesting (do)");
		if (central.gc.canHarvest(worker_id, direction)) {
			//no action this round, if spot has karbonite
			//System.out.println("harvesting");
			central.gc.harvest(worker_id, direction);
			if (central.gc.karboniteAt(locationOfKarbonite) <= 0) {
				//TODO DECINE ON STRATEGY!!
				//return new Idle(worker_location, worker_id);
				//System.out.println("harvested all resources, tying to get loc of new karbonite");
				//if karbonite just went out
				MapLocation new_loc = central.findExactResources(worker_location);
				locationOfKarbonite = new_loc;
				return this;
				//this can happen due to some other worker harvesting the same spot or we just exhausted the spot
			} else {
				//continue harvesting the same spot
				return this;
			}

		} else {
			//System.out.printf("cant harvest- my_loc: (%d,%d) karb_loc: (%d,%d)%n", worker_location.getX(), worker_location.getY(), locationOfKarbonite.getX(), locationOfKarbonite.getY());
			//System.out.printf("resources at loc: %d%n, location: %s", central.gc.karboniteAt(locationOfKarbonite), direction);
			return this;
		}
		
		
	}
}



class Idle extends Occupation {
	int wait;
	Occupation after_waiting;

	public Idle(MapLocation wl, int id) {
		super(wl,id);
		//decide on occupation
		after_waiting = null;
		wait = -1;
	}
	
	public Idle(MapLocation wl, int id, int w, Occupation o) {
		super(wl, id);
		wait = w;
		after_waiting = o;
	}
	
	public Occupation processOccupation(Central central) {
		
		//TODO: ask Central for another task
		//Occupation newTask = Central.getWorkerTask(locationOfWorker);
		//newTask.processOccupation();
		//return newTask;
		//best not to call
		//System.out.println("CALLING (process) ON IDLE!!!");
		return this;
	}

	public Occupation doOccupation(Central central) {
		//should not be called EVER
		if (after_waiting != null) {
			wait --;
			if (wait == 0) {
				//System.out.println("from idle to occupation");
				return after_waiting;
			}
			
		}
		
		//System.out.println("CALLING (do) ON IDLE!!!");
		return this;
	}
	
}

class PlacingBlueprint extends Occupation {
	private Direction direction;
	private UnitType structure;
	private MapLocation blueprint_loc;

	public PlacingBlueprint(MapLocation wl, int id,UnitType s) {
		super(wl,id);
		structure = s;
		direction = null;
		blueprint_loc = null;
	}

	public PlacingBlueprint(MapLocation wl, int id, MapLocation blueprint, UnitType s) {
		super(wl,id);
		structure = s;
		blueprint_loc = blueprint;
	}

	public Occupation processOccupation(Central central) {
		//System.out.println("blueprinting (process)");
		if (worker_location == null) worker_location = central.gc.unit(worker_id).location().mapLocation();
	
		if (blueprint_loc != null) {
			MapLocation tmp = worker_location;
			worker_location = null;
			Path pth = central.findPath(tmp, blueprint_loc);
			if (pth != null) 
			return (new Arriving(tmp, worker_id, blueprint_loc, this, pth)).processOccupation(central);
		}
	
		if (direction != null) {
			if (central.gc.canBlueprint(worker_id, structure, direction)) return this;
		}

		if (blueprint_loc == null) {
			Direction[] directions = new Direction[4];
			directions[0] = Direction.Northeast;
			directions[2] = Direction.Northwest ;
			directions[3] = Direction.Southeast ;
			directions[1] = Direction.Southwest ;
	 		//find a feasable 
			for (int i=0; i<4; i++) {
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
		//System.out.println("blueprinting (do)");
		if (central.gc.hasUnitAtLocation(worker_location.add(direction)) ){
			Direction[] directions = Direction.values();
	 		//find a feasable 
			for (int i=1; i<9; i++) {
				//canBlueprint takes into consideration Karbonite resources, heat, if rocket can be built
				if (!central.gc.hasUnitAtLocation(worker_location.add(direction))) {
					direction = directions[i];
					break;
				} 
			}
			/*if (central.gc.isOccupiable(worker_location.add(direction))0) {
				//could be, that before making turn something gets in the way
				//we will just try waiting another round for now
				return new Idle(worker_location, worker_id, 1, this);
			}*/
		}
		try {
			//System.out.println("BLUPRIN PLACED");
			//maybe karbonite is too low
			if (central.gc.canBlueprint(worker_id, structure, direction)) {
				central.gc.blueprint(worker_id, structure, direction);
				if(structure == UnitType.Rocket)
					central.rocketsBuilt++;
				//System.out.println("placing blueprint");
				//I hope this will work!!
				int blueprint_id = (central.gc.senseUnitAtLocation(worker_location.add(direction))).id();
				//gets to build the blueprint BUT!! blueprint id is needed!! i hope bluprint is set up right away
				
				return new Building(worker_location, worker_id, blueprint_id);
			}
			return this;
		} catch (Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
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
		//System.out.println("building (process)");
		//can build if adjacent and if worker hasnt made a move yet
		if(!central.gc.canSenseUnit(blueprint_id))
			return new Idle(worker_location, worker_id);
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
		//System.out.println("building (do)");
		if (central.gc.canBuild(worker_id, blueprint_id)) {
			//System.out.println("building");
			//throws exception if allready built
			central.gc.build(worker_id, blueprint_id);
			if ((central.gc.unit(blueprint_id)).health() >= (central.gc.unit(blueprint_id)).maxHealth()) {
				System.out.println("Round: " + central.turnNumber + " BUILT (health:" + central.gc.unit(blueprint_id).health());
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
		//System.out.println("replicating (process)");
		if (direction == null) {
			Direction[] directions = Direction.values();
		 	//find a feasable direction
			for (int i=1; i<9; i++) {
				MapLocation mapLocation = worker_location.add(directions[i]);
				if (central.gc.startingMap(central.gc.planet()).onMap(mapLocation)
						&& central.gc.isOccupiable(mapLocation)>0) {
					direction = directions[i];
					return this;
				} 
			}
		} else {
			if (central.gc.canMove(worker_id, direction)) {
				//if worker can replicate in direction
				return this;
			} else {
				Direction[] directions = Direction.values();
			 	//find a feasable direction
				for (int i=1; i<9; i++) {
					if (central.gc.canMove(worker_id, directions[i])) {
							direction = directions[i];
							return this;
						} 
					}
			}
			//if no feasible location was found
		}
		direction = Direction.West;
		return this;
	
	}
	
	public Occupation doOccupation(Central central) {
		//System.out.println("replicationg (do)");
		if (central.gc.canReplicate(worker_id, direction)) {
			//System.out.println("Worker replicated");
			//takes into account karbonite, heat, location
			central.gc.replicate(worker_id, direction);
			return new Idle(worker_location, worker_id);
		} else {
			//do it next turn
			return this;
		}
	}

} 


class MineInBetween extends Occupation {
	private Direction direction;
	private Occupation occupation;
	
	public MineInBetween(MapLocation wl, int id, Occupation o) {
		super(wl,id);
		occupation = o;
	}
	
	
	public Occupation processOccupation(Central central) {
		if (central.gc.karboniteAt(worker_location) > 0) {
			return this;
		} 
		return occupation;
	
	}
	
	public Occupation doOccupation(Central central) {
		if (central.gc.karboniteAt(worker_location) <= 0) {
			return occupation;
		} 
		central.gc.harvest(worker_id, Direction.Center);
		if (central.gc.karboniteAt(worker_location) > 0) {
			return this;
		} 
		return occupation;
		
	}
		

} 



