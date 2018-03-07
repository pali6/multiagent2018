import bc.MapLocation;
import bc.Planet;

public class NavPoint {

    private final int MAX_MAP_SIZE = 50;

    public final int x, y;

    public NavPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static NavPoint fromMapLocation(MapLocation mapLocation) {
        return new NavPoint(mapLocation.getX(), mapLocation.getY());
    }


    public MapLocation toMapLocation(Planet planet){
        return new MapLocation(planet, x, y);
    }

    @Override
    public int hashCode() {
        return x + y * (MAX_MAP_SIZE + 1);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NavPoint)) {
            return false;
        }
        NavPoint other = (NavPoint) obj;
        return other.x == this.x && other.y == this.y;
    }

    @Override
    public String toString() {
        return "NavPoint(" + x + "x" + y + ")";
    }
}
