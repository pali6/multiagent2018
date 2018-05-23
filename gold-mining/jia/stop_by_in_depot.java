package jia;

import java.util.logging.Level;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;
import jason.environment.grid.Location;

public class stop_by_in_depot extends DefaultInternalAction {
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
                return false;
            }

            if (agentGoldDist <= 1) {
                return false;
            }
            if (agentDepotDist <= 1) {
                return true;
            }

            float diffGX = gX - agX;
            float diffGY = gY - agY;
            float sumDiffG = diffGX + diffGY;
            diffGX /= sumDiffG;
            diffGY /= sumDiffG;

            float diffDepX = depX - agX;
            float diffDepY = depY - agY;
            float sumDiffDep = diffDepX + diffDepY;
            diffDepX /= sumDiffDep;
            diffDepY /= sumDiffDep;

            if ((diffGX - diffDepX) * (diffGX - diffDepX) + (diffGY - diffDepY) * (diffGY - diffDepY) < 0.25f) {
                // gold and depot are in the same direction from agent
                return agentDepotDist < agentGoldDist;
            }

            // is it faster to unload gold in depot?
            return 2.5f * agentDepotDist * currentFatigue < agentGoldDist * currentFatigue + depotGoldDist + nextFatigue;


        } catch (Throwable e) {
            ts.getLogger().log(Level.SEVERE, "jia. error: " + e, e);
        }
        return false;
    }
}
