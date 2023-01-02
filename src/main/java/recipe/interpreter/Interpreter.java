package recipe.interpreter;

import java.io.IOException;

import recipe.analysis.NuXmvInteraction;

import recipe.lang.utils.Pair;

public class Interpreter {
    private recipe.lang.System sys;

    public Interpreter () {

    }

    public void init () throws IOException, Exception {
            assert sys != null;
            NuXmvInteraction nuxmv = new NuXmvInteraction(sys);
            // TODO feed user constraint
            Pair<Boolean, String> s0 = nuxmv.simulation_pick_init_state("");

            System.out.println(s0.toString());
    }



    public void setSystem(recipe.lang.System s) {
        sys = s;
    }
}
