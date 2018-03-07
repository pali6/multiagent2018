import java.util.LinkedList;
import java.util.Queue;

class Path {
    public LinkedList<NavPoint> queue;

    public Path(LinkedList<NavPoint> queue){
        this.queue = queue;
    }
}