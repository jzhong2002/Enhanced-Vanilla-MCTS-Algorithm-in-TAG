package players.readyBasicMCTS;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * This is a simple version of MCTS that may be useful for newcomers to TAG and MCTS-like algorithms
 * It strips out some of the additional configuration of MCTSPlayer. It uses BasicTreeNode in place of
 * SingleTreeNode.
 */
public class BasicMCTSPlayer extends AbstractPlayer {

    public BasicMCTSPlayer() {
        this(System.currentTimeMillis());
    }

    public BasicMCTSPlayer(long seed) {
        super(new BasicMCTSParams(), " Improved BasicMCTS");
        // for clarity we create a new set of parameters here, but we could just use the default parameters
        parameters.setRandomSeed(seed);
        rnd = new Random(seed);

        // These parameters can be changed, and will impact the Basic MCTS algorithm
//        BasicMCTSParams params = getParameters();
//        params.K = Math.sqrt(2);
//        params.rolloutLength = 10;
//        params.maxTreeDepth = 5;
//        params.epsilon = 1e-6;

    }

    public BasicMCTSPlayer(BasicMCTSParams params) {
        super(params, " Improved BasicMCTS");
        rnd = new Random(params.getRandomSeed());
    }

    /*
    Each MCTS iteration:
        1. Copy game state with determinization
            → Randomly fill in opponent hands

        2. Search using this "guess" of the world
            → MCTS assumes this guess is correct

        3. Pick action that works best in this scenario
     */

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        BasicMCTSParams params = getParameters();

        // Track action visits counts across all determinizations
        Map<AbstractAction, Integer> totalVisits = new HashMap<>();
        for (AbstractAction action : actions) {
            totalVisits.put(action, 0);
        }

        // Run multiple determinizations
        for (int d = 0; d < params.nDeterminizations; d++) {
            // Create a new determinised state
            AbstractGameState determinState = gameState.copy(getPlayerID());

            // Search from the deterministic root
            BasicTreeNode root = new BasicTreeNode(this, null, determinState, rnd);
            root.mctsSearch();

            // Assumes visit counts for each action
            for (AbstractAction action : actions) {
                BasicTreeNode child = root.children.get(action);
                if (child != null) {
                    totalVisits.put(action, totalVisits.get(action) + child.nVisits);
                }
            }
        }

        // Return action with the most total visits across all determinizations
        AbstractAction bestAction = null;
        int maxVisits = -1;
        for (AbstractAction action : totalVisits.keySet()) {
            if (totalVisits.get(action) > maxVisits) {
                maxVisits = totalVisits.get(action);
                bestAction = action;
            }
        }

        // Return best action
        return bestAction;
    }

    @Override
    public BasicMCTSParams getParameters() {
        return (BasicMCTSParams) parameters;
    }

    public void setStateHeuristic(IStateHeuristic heuristic) {
        getParameters().heuristic = heuristic;
    }


    @Override
    public String toString() {
        return " Improved BasicMCTS";
    }

    @Override
    public BasicMCTSPlayer copy() {
        return new BasicMCTSPlayer((BasicMCTSParams) parameters.copy());
    }
}