package jia;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;
import jason.environment.grid.Location;

import java.util.logging.Level;
import java.util.Random;

import arch.LocalWorldModel;
import arch.MinerArch;
import env.WorldModel;

import busca.Nodo;
import jia.Search;

/**
 * Gets the near least visited location.
 * Its is based on the agent's model of the world.
 *
 * @author jomi
 *
 */
public class near_least_visited extends DefaultInternalAction {

    static Random rnd;

    public boolean inBounds(LocalWorldModel model, int x, int y) {
        return x >= 0 && y >= 0 && x < model.getWidth() && y < model.getHeight();
    }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] terms) throws Exception {
        try {
            if(rnd == null)
                rnd = new Random();
            LocalWorldModel model = ((MinerArch)ts.getUserAgArch()).getModel();
            if (model == null) {
                ts.getLogger().log(Level.SEVERE, "no model to get near_least_visited!");
            } else {
                NumberTerm agx = (NumberTerm)terms[0];
                NumberTerm agy = (NumberTerm)terms[1];

                Location agloc = new Location((int)agx.solve(),(int)agy.solve());
                float[][] unvisNeigh = new float[model.getWidth()][model.getHeight()];
                for (int i = 0; i < model.getWidth(); i++) {
                    for (int j = 0; j < model.getHeight(); j++) {
                        if(model.getVisited(new Location(i, j)) == 0) {
                            for(int ii = i - 1; ii <= i + 1; ii++)
                                for(int jj = j - 1; jj <= j + 1; jj++)
                                    if((ii != i || jj != j) && inBounds(model, ii, jj))
                                        unvisNeigh[ii][jj] += 1;
                        }
                    }
                }
                float[][] unvis2Neigh = new float[model.getWidth()][model.getHeight()];
                for (int i = 0; i < model.getWidth(); i++) {
                    for (int j = 0; j < model.getHeight(); j++) {
                        for(int ii = i - 1; ii <= i + 1; ii++)
                            for(int jj = j - 1; jj <= j + 1; jj++)
                                if((ii != i || jj != j) && inBounds(model, ii, jj))
                                    unvis2Neigh[ii][jj] += unvisNeigh[i][j];
                    }
                }
                float bestUtility = -9999999;
                int bestX = rnd.nextInt() % model.getWidth();
                int bestY = rnd.nextInt() % model.getHeight();
                for (int y = 0; y < model.getHeight(); y++) {
                    for (int x = 0; x < model.getWidth(); x++) {
                        float unNeigh = unvisNeigh[x][y];
                        float un2Neigh = unvis2Neigh[x][y];
                        if(x == (int)agx.solve() && y == (int)agy.solve()) {
                            continue;
                        }
                        if(!model.isFree(x, y)) {
                            continue;
                        }
                        if(unNeigh == 0 && model.getVisited(new Location(x, y)) != 0) {
                            continue;
                        }
                        Nodo result;
                        try {
                            result = new Search(model, agloc, new Location(x, y)).search();
                        }
                        catch(Exception e) { // this hurts
                            continue;
                        }

                        if(result == null) {
                            continue;
                        }
                        int distance = result.getProfundidade();
                        
                        float utility = 200 - distance + unNeigh + 1/2 * un2Neigh;
                        utility += rnd.nextFloat() * 4;
                        if(model.hasObject(WorldModel.GOLD, x, y)) {
                            utility += 10;
                        }
                        if(utility > bestUtility) {
                            bestUtility = utility;
                            bestX = x;
                            bestY = y;
                        }
                    }
                }
                String log = "";
                for (int y = 0; y < model.getHeight(); y++) {
                    for (int x = 0; x < model.getWidth(); x++) {
                        if(!model.inGrid(new Location(x, y)))
                            continue;
                        float unNeigh = unvisNeigh[x][y];
                        float un2Neigh = unvis2Neigh[x][y];
                        if(x == bestX && y == bestY) {
                            log += "*** ";
                            continue;
                        }
                        if(x == (int)agx.solve() && y == (int)agy.solve()) {
                            log += "!!! ";
                            continue;
                        }
                        if(!model.isFree(x, y)) {
                            log += "### ";
                            continue;
                        }
                        if(unNeigh == 0 && model.getVisited(new Location(x, y)) != 0) {
                            log += "... ";
                            continue;
                        }
                        Nodo result;
                        try {
                            result = new Search(model, agloc, new Location(x, y)).search();
                        }
                        catch(Exception e) { // this hurts
                            continue;
                        }

                        if(result == null) {
                            log += "xxx ";
                            continue;
                        }
                        int distance = result.getProfundidade();
                        float utility = 200 - distance + unNeigh + 1/2 * un2Neigh;
                        log += (int)utility + " ";
                    }
                    ts.getLogger().info(log);
                    log = "";
                }
                ts.getLogger().info(bestX + " " + bestY + " " + bestUtility);
                

                un.unifies(terms[2], new NumberTermImpl(bestX));
                un.unifies(terms[3], new NumberTermImpl(bestY));
                //ts.getLogger().info("at "+agx+","+agy+" to "+n.x+","+n.y);
                return true;
            }
        } catch (Throwable e) {
            ts.getLogger().log(Level.SEVERE, "near_least_visited error: "+e, e);
        }
        return false;
    }
}
