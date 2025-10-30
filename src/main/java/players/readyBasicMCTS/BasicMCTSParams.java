package players.readyBasicMCTS;

import core.interfaces.IStateHeuristic;
import players.PlayerParameters;
import players.heuristics.SushiGoHeuristic;

import java.util.Arrays;


public class BasicMCTSParams extends PlayerParameters {

    public double K = Math.sqrt(2);
    public int rolloutLength = 10; // assuming we have a good heuristic
    public int maxTreeDepth = 100; // effectively no limit
    public double epsilon = 1e-6;
    public IStateHeuristic heuristic = new SushiGoHeuristic();

    // (1) Adding Heuristics Rollouts
    public boolean useHeuristicRollouts = true; // if true, the model uses greedy heuristic rollouts
    public double heuristicRolloutProbability = 0.9; // adds epsilon, probability of using heuristic action (0.0 = always random, 1.0 = always heuristic)

    /*
    1. Determin number of different determinizations to try
    2. Higher = more robust
     */
    // (2) Adding determinization
    public int nDeterminizations = 5; // default = 1 (no dertem). Set to 5 or 10 for better coverage

    public BasicMCTSParams() {
        addTunableParameter("K", Math.sqrt(2), Arrays.asList(0.0, 0.1, 1.0, Math.sqrt(2), 3.0, 10.0));
        addTunableParameter("rolloutLength", 10, Arrays.asList(0, 3, 10, 30, 100));
        addTunableParameter("maxTreeDepth", 100, Arrays.asList(1, 3, 10, 30, 100));
        addTunableParameter("epsilon", 1e-6);
        addTunableParameter("heuristic", (IStateHeuristic) new SushiGoHeuristic());
        addTunableParameter("useHeuristicRollouts", true);
        addTunableParameter("heuristicRolloutProbability", 0.9);
    }

    @Override
    public void _reset() {
        super._reset();
        K = (double) getParameterValue("K");
        rolloutLength = (int) getParameterValue("rolloutLength");
        maxTreeDepth = (int) getParameterValue("maxTreeDepth");
        epsilon = (double) getParameterValue("epsilon");
        heuristic = (IStateHeuristic) getParameterValue("heuristic");

        // Reset new params
        useHeuristicRollouts = (boolean) getParameterValue("useHeuristicRollouts");
        heuristicRolloutProbability = ((Number) getParameterValue("heuristicRolloutProbability")).doubleValue();
    }

    @Override
    protected BasicMCTSParams _copy() {
        // All the copying is done in TunableParameters.copy()
        // Note that any *local* changes of parameters will not be copied
        // unless they have been 'registered' with setParameterValue("name", value)
        return new BasicMCTSParams();
    }

    @Override
    public IStateHeuristic getStateHeuristic() {
        return heuristic;
    }

    @Override
    public BasicMCTSPlayer instantiate() {
        return new BasicMCTSPlayer((BasicMCTSParams) this.copy());
    }

}