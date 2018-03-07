import bc.*;

import java.util.*;

public class Navigation {

    private GameController gc;
    private MapLocation reusableMapLocation;
    private PlanetMap planetMap;

    public Navigation(GameController gc, Planet planet) {
        this.gc = gc;
        this.planetMap = gc.startingMap(planet);
        reusableMapLocation = new MapLocation(planet, 0, 0);
    }

    public Path findPath(MapLocation from, MapLocation to) {
        NavPoint start = NavPoint.fromMapLocation(from);
        NavPoint goal = NavPoint.fromMapLocation(to);

        Set<NavPoint> closed = new HashSet<>();
        Map<NavPoint, NavPoint> cameFrom = new HashMap<>();
        Map<NavPoint, Integer> fScore = new HashMap<>();
        Map<NavPoint, Integer> gScore = new HashMap<>();
        PriorityQueue<NavPoint> queue = new PriorityQueue<>(Comparator.comparingInt(fScore::get));

        gScore.put(start, 0);
        fScore.put(start, heuristic(start, goal));
        boolean found = false;
        queue.add(start);


        while (!queue.isEmpty()) {
            NavPoint current = queue.poll();
            if (current.equals(goal)) {
                found = true;
                break;
            }

            closed.add(current);

            for (NavPoint neighbour : getAdjacentNavPoints(current)) {
                if (closed.contains(neighbour)) {
                    continue;
                }

                int newGScore = gScore.get(current) + 1;
                if (gScore.containsKey(neighbour) && newGScore > gScore.get(neighbour)) {
                    continue;
                }

                cameFrom.put(neighbour, current);
                gScore.put(neighbour, newGScore);
                fScore.put(neighbour, newGScore + heuristic(neighbour, goal));
                if (!queue.contains(neighbour)) {
                    queue.add(neighbour);
                }
            }

        }
        if (!found) {
            return null;
        }

        return reconstructPath(cameFrom, goal);
    }

    public Direction getDirection(Path path, MapLocation mapLocation) {
        NavPoint nextNavPoint = path.queue.peek();
        if (nextNavPoint == null) {
            return Direction.Center;
        }
        boolean pathBlocked = false;
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                int nx = mapLocation.getX() + i;
                int ny = mapLocation.getY() + j;
                if (nextNavPoint.equals(new NavPoint(nx, ny))) {
                    if (!isLocationPassable(nx, ny)) {
                        break;
                    }
                    path.queue.poll();
                    return getDirectionFromCoords(i, j);
                }
            }
        }

        // if next direction couldn't be determined
        // (either the path is blocked or the unit si too far away)
        // we find new path
        Path newPath = findPath(mapLocation, path.queue.getLast().toMapLocation(gc.planet()));
        if(newPath == null){
            return null;
        }
        path.queue = newPath.queue;
        return getDirection(path, mapLocation);
    }

    private boolean isLocationPassable(int x, int y) {
        if (x < 0 || y < 0 || x >= planetMap.getWidth() || y >= planetMap.getHeight()) {
            return false;
        }
        reusableMapLocation.setX(x);
        reusableMapLocation.setY(y);
        return !gc.hasUnitAtLocation(reusableMapLocation) && planetMap.isPassableTerrainAt(reusableMapLocation) != 0;
    }

    private List<NavPoint> getAdjacentNavPoints(NavPoint navPoint) {
        List<NavPoint> result = new ArrayList<>(8);
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }
                int nx = navPoint.x + i;
                int ny = navPoint.y + j;

                if (isLocationPassable(nx, ny)) {
                    result.add(new NavPoint(nx, ny));
                }
            }
        }

        return result;
    }

    private Direction getDirectionFromCoords(int x, int y) {
        System.out.println(x + " x " + y);
        switch (x) {
            case -1:
                switch (y) {
                    case -1:
                        return Direction.Southwest;
                    case 0:
                        return Direction.West;
                    case 1:
                        return Direction.Northwest;
                }
            case 0:
                switch (y) {
                    case -1:
                        return Direction.South;
                    case 1:
                        return Direction.North;
                }
            case 1:
                switch (y) {
                    case -1:
                        return Direction.Southeast;
                    case 0:
                        return Direction.East;
                    case 1:
                        return Direction.Northeast;
                }
        }
        return null;
    }

    private int heuristic(NavPoint a, NavPoint b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    private Path reconstructPath(Map<NavPoint, NavPoint> cameFrom, NavPoint goal) {
        Stack<NavPoint> stack = new Stack<>();
        NavPoint current = goal;
        NavPoint from = null;
        while (cameFrom.containsKey(current)) {
            stack.push(current);
            current = cameFrom.get(current);
        }

        LinkedList<NavPoint> queue = new LinkedList<>();
        while (!stack.isEmpty()) {
            queue.add(stack.pop());
        }
        return new Path(queue);
    }
}
