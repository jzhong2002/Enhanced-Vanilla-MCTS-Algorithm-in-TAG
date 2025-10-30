package players.heuristics;

import core.AbstractGameState;
import core.components.Deck;
import games.sushigo.SGGameState;
import core.interfaces.IStateHeuristic;
import games.sushigo.cards.SGCard;

public class SushiGoHeuristic implements IStateHeuristic {

    /**
     * This cares mostly about the raw game score - but will treat winning as a 50% bonus
     * and losing as halving it
     *
     * @param gameState - game gameState to evaluate and score.
     * @param playerId - player id
     * @return
     */

    // strategy weights (tune these to adjust play styles)
    private double COMBO_FOCUS = 1.0; // high = aggressive combo hunting
    private double SAFETY_FOCUS = 1.0; // high = safe immediate rewards
    private double DENIAL_FOCUS = 0.5; // high = block opponents
    private double MAKI_PRIORITY = 1.0; // high = compete hard for maki
    private double PUDDING_PRIORITY = 0.7; // high = value pudding more, better for late game

    @Override
    public double evaluateState(AbstractGameState gameState, int playerId) {

        // Cast to SushiGo specific game gameState
        SGGameState sgGameState = (SGGameState) gameState;

        double score = 0.0;

        // current actual score (most important)
        score += sgGameState.getGameScore(playerId);

        // set completion potential
        score += evaluateSetCompletion(sgGameState, playerId) * COMBO_FOCUS;

        // maki race positioning
        score += evaluateMakiRace(sgGameState, playerId) * SAFETY_FOCUS;

        // wasabi-nigiri combos
        score += evaluateWasabiNigiri(sgGameState, playerId) * DENIAL_FOCUS;

        // pudding positioning (end game score)
        score += evaluatePuddingScore(sgGameState, playerId) * MAKI_PRIORITY;

        // chopsticks value
        score += evaluateChopsticks(sgGameState, playerId) * PUDDING_PRIORITY;

        return score;

    }
    /*
    evaluate set completion values (Tempura, Sashimi, and Dumplings)
     */

    private double evaluateSetCompletion(SGGameState gameState, int playerId) {

        double score = 0.0;

        Deck<SGCard> playerBoard = gameState.getPlayedCards().get(playerId);

        // Count each card type
        int tempuraCount = 0;
        int sashimiCount = 0;
        int dumplingCount = 0;

        for (SGCard card : playerBoard.getComponents()) {
            String cardType = card.getComponentName();
            if (cardType.equals("Tempura"))  tempuraCount++;
            else if (cardType.equals("Sashimi")) sashimiCount++;
            else if (cardType.equals("Dumpling")) dumplingCount++;
        }

        // tempura needs x2 for 5 points
        if (tempuraCount == 1) {
            score += 2.5; // half value, needs completion
        }
        else if (tempuraCount >= 2) {
            score += 5; // 5 points per pair
        }

        // sushimi needs x3 for 10 points
        if (sashimiCount == 1) {
            score += 1.0; // Low value, hard to complete
        } else if (sashimiCount == 2) {
            score += 5.0; // High priority to complete!
        } else if (sashimiCount >= 3) {
            score += 10.0 * (sashimiCount / 3); // 10 points per triple
        }

        // dumplings has exponential value (1 = 1 point, 2 = 3 points, 3 = 6 points, 4 = 10 points, 5 = 15 points)
        if (dumplingCount == 1) score += 1.0;
        else if (dumplingCount == 2) score += 3.0;
        else if (dumplingCount == 3) score += 6.0;
        else if (dumplingCount == 4) score += 10.0;
        else if (dumplingCount >= 5) score += 15.0;

        return score;
    }

    /*
     evaluate maki rolls race Positioning
     */

    private double evaluateMakiRace(SGGameState gameState, int playerId) {
        int nPlayers = gameState.getNPlayers();
        int [] makiCounts = new int [nPlayers];

        // count maki rolls for each player
        for (int p = 0; p < nPlayers; p++) {
            makiCounts[p] = countMakiCounts(gameState, p);
        }

        int myMaki = makiCounts[playerId];
        int maxMaki = 0;
        int secondMaxMaki = 0;

        // find the highest and the second highest maki
        for (int p = 0; p < nPlayers; p++) {
            if (p == playerId) continue;
            if (makiCounts[p] > maxMaki) {
                secondMaxMaki = maxMaki;
                maxMaki = makiCounts[p];
            }
            else if (makiCounts[p] > secondMaxMaki) {
                secondMaxMaki = makiCounts[p];
            }
        }

        // award points based on position
        if (myMaki > maxMaki) {
            return 6.0; // first place with the most maki rolls
        } else if (myMaki == maxMaki && myMaki > 0) {
            return 4.5; // tied for first then split points
        } else if (myMaki > secondMaxMaki && myMaki > 0) {
            return 3.0; // second place gets 2 points
        } else if (myMaki < secondMaxMaki && myMaki > 0) {
            return 1.5; // tied for second
        }

        return 0.0; // out of contention when no maki rolls

    }

    /*
    count the total maki rolls for a player
     */

    private int countMakiCounts(SGGameState gameState, int playerId) {
        int count = 0;

        Deck<SGCard> playerBoard = gameState.getPlayedCards().get(playerId);

        for (SGCard card : playerBoard.getComponents()) {
            String cardType = card.getComponentName();
            if (cardType.equals("Maki Roll 1")) {
                count += 1;
            } else if (cardType.equals("Maki Roll 2")) {
                count += 2;
            } else if (cardType.equals("Maki Roll 3")) {
                count += 3;
            }
        }

        return count;
    }

    /*
    evaluate Wasabi and Nigiri combos
     */

    private double evaluateWasabiNigiri(SGGameState gameState, int playerId) {
        double score = 0.0;

        Deck<SGCard> playerBoard = gameState.getPlayedCards().get(playerId);

        int wasabiCount = 0;
        int usedWasabi = 0;
        int nigiriCount = 0;

        for (SGCard card : playerBoard.getComponents()) {
            String cardType = card.getComponentName();
            if (cardType.equals("Wasabi")) {
                wasabiCount++;
                // check if wasabi has nigiri on it (you'd need to check card gameState)
            } else if (cardType.contains("Nigiri")) {
                nigiriCount++;
            }
        }

        // unused wasabi ith potential is valuable
        int unUsedWasabi = wasabiCount - usedWasabi;

        // each used wasabi-nigiri combo adds extra value
        score += usedWasabi * 4.0;

        // unused wasabi is potential
        score += unUsedWasabi * 1.0;


        return score;
    }

    /*
    evaluate pudding positioning (critical for end-game)
     */

    private double evaluatePuddingScore(SGGameState gameState, int playerId) {
        int nPlayers = gameState.getNPlayers();
        int [] puddingCounts = new int[nPlayers];

        // count pudding for each player
        for (int p = 0; p < nPlayers; p++) {
            puddingCounts[p] = countPudding(gameState, p);
        }
        int myPudding = puddingCounts[playerId];
        int maxPudding = 0;
        int minPudding = Integer.MAX_VALUE;

        for (int p = 0; p < nPlayers; p++) {
            if (p == playerId) continue;
            maxPudding = Math.max(maxPudding, puddingCounts[p]);
            minPudding = Math.min(minPudding, puddingCounts[p]);
        }

        // scale pudding value by current round (important in late game)
        int currentRound = gameState.getRoundCounter();
//        double roundMultiplier = currentRound / 3.0; // Assuming 3 rounds

        // leading in pudding
        if (myPudding > maxPudding) {
            return 6.0;
        }

        // low pudding
        if (myPudding < minPudding || (myPudding == minPudding && minPudding == 0)) {
            return -6.0;
        }

        // middle safe ground
        return 0.0;
    }

    /*
    evaluate pudding cards for player
     */

    private int countPudding(SGGameState gameState, int playerId) {
        int count = 0;
        Deck<SGCard> playerBoard = gameState.getPlayedCards().get(playerId);

        for (SGCard card : playerBoard.getComponents()) {
            if (card.getComponentName().equals("Pudding")) {
                count++;
            }
        }

        return count;
    }

    /*
    evaluate chopsticks utility
     */

    private double evaluateChopsticks(SGGameState gameState, int playerId) {
        Deck<SGCard> playerBoard = gameState.getPlayedCards().get(playerId);
        int chopsticksCount = 0;

        for (SGCard card : playerBoard.getComponents()) {
            if (card.getComponentName().equals("Chopsticks")) {
                chopsticksCount++;
            }
        }

        // Chopsticks provide flexibility - more valuable early in round
        // This is a simplified check - adjust based on actual game gameState
        int cardsInHand = gameState.getPlayerHands().get(playerId).getSize();

        if (cardsInHand > 5) {
            return chopsticksCount * 2.0; // High value early
        } else if (cardsInHand > 2) {
            return chopsticksCount * 1.0; // Medium value
        } else {
            return chopsticksCount * -0.5; // Penalty if unused
        }
    }
    @Override
    public double minValue() {
        return Double.NEGATIVE_INFINITY;
    }
    @Override
    public double maxValue() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public String toString() {
        return "SushiGoHeuristic";
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof SushiGoHeuristic;
    }
    @Override
    public int hashCode() {
        return 3;
    }
}
