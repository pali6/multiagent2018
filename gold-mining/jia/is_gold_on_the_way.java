package jia;

import java.util.logging.Level;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;
import jason.environment.grid.Location;

public class is_gold_on_the_way extends DefaultInternalAction {
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] terms) throws Exception {
        try {
            env.WorldModel model = ((arch.MinerArch) ts.getUserAgArch()).getModel();
            System.out.println(model.toString());
            int gX = (int) ((NumberTerm) terms[0]).solve();
            int gY = (int) ((NumberTerm) terms[1]).solve();
            int agentDepotDist = (int) ((NumberTerm) terms[2]).solve();
            int agentGoldDist = (int) ((NumberTerm) terms[3]).solve();
            int depotGoldDist = (int) ((NumberTerm) terms[4]).solve();
            int agentId = arch.MinerArch.getAgId(ts.getUserAgArch().getAgName());
            Location depotLoc = model.getDepot();
            int depX = depotLoc.x;
            int depY = depotLoc.y;
            Location agentLoc = model.getAgPos(agentId);
            int agX = agentLoc.x;
            int agY = agentLoc.y;
            int golds = model.getGoldsWithAg(agentId);
            double currentFatigue = 1 + model.getAgFatigue(agentId);
            double nextFatigue = 1 + model.getAgFatigue(agentId, golds + 1);

            if (golds <= 1) {
                return true;
            }

            if (agentGoldDist <= 1) {
                return true;
            }
            if (agentDepotDist <= 1) {
                return false;
            }

            return agentDepotDist > agentGoldDist;
        } catch (Throwable e) {
            ts.getLogger().log(Level.SEVERE, "jia. error: " + e, e);
        }
        return false;
    }
}
