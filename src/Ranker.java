import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Calculates the FBS team rankings using the project's rating algorithm.
 */
public final class Ranker {

    private static final int DEFAULT_ITERATIONS = 100;
    private static final int HISTORY_SIZE = 5;

    /**
     * Stores the rankings, game impacts, and iteration information.
     */
    public static final class RankingResults {
        private final int[][] rankings;
        private final int[][] gameImpacts;
        private final int[] bestRanks;
        private final int iterationsUsed;
        private final boolean repeated;

        private RankingResults(int[][] rankings, int[][] gameImpacts,
                int[] bestRanks, int iterationsUsed, boolean repeated) {
            this.rankings = rankings;
            this.gameImpacts = gameImpacts;
            this.bestRanks = bestRanks;
            this.iterationsUsed = iterationsUsed;
            this.repeated = repeated;
        }

        public int[][] getRankings() {
            return this.rankings;
        }

        public int[][] getGameImpacts() {
            return this.gameImpacts;
        }

        public int[] getBestRanks() {
            return this.bestRanks;
        }

        public int getIterationsUsed() {
            return this.iterationsUsed;
        }

        public boolean isRepeated() {
            return this.repeated;
        }
    }

    private Ranker() {
        // No instances needed.
    }

    /**
     * Runs the ranking calculation from the command line.
     *
     * @param args
     *            unused command-line arguments
     * @throws IOException
     *             if the schedule CSV cannot be read
     */
    public static void main(String[] args) throws IOException {
        int[][][] schedules = FbsScheduleLoader
                .loadSchedules("data/FbsSchedules2026.csv");
        RankingResults results = calculateResults(schedules);
        int[][] rankings = results.getRankings();

        Integer[] teamIndexes = new Integer[rankings.length];
        for (int i = 0; i < teamIndexes.length; i++) {
            teamIndexes[i] = i;
        }
        Arrays.sort(teamIndexes,
                (a, b) -> Integer.compare(rankings[a][1], rankings[b][1]));

        System.out.println("Iterations used: " + results.getIterationsUsed());
        System.out.println("Repeated ranking detected: " + results.isRepeated());
        System.out.println();

        for (int i = 0; i < teamIndexes.length; i++) {
            int team = teamIndexes[i];
            System.out.println((i + 1) + ". "
                    + FbsSchedules2026Compact.fbsTeams[team]
                    + " -> " + rankings[team][0]
                    + " (best cycle rank: " + results.getBestRanks()[team] + ")");
        }
    }

    /**
     * Calculates ratings, ranks, game impacts, and cycle information using
     * the default maximum of 100 iterations.
     *
     * @param schedules
     *            schedule data indexed by team
     * @return the ranking results
     */
    public static RankingResults calculateResults(int[][][] schedules) {
        return calculateResults(schedules, DEFAULT_ITERATIONS);
    }

    /**
     * Calculates ratings and ranks until a ranking repeats within the previous
     * five iterations or the maximum number of iterations is reached.
     *
     * @param schedules
     *            schedule data indexed by team
     * @param maxIterations
     *            maximum number of calculation passes
     * @return the ranking results and iteration information
     */
    public static RankingResults calculateResults(int[][][] schedules,
            int maxIterations) {
        return calculateResults(schedules, maxIterations, 0.0, 0.0, 0.0);
    }

    /**
     * Calculates rankings with location, recency, and margin adjustments.
     * Percentage inputs are expressed from 0 through 100.
     *
     * @param schedules
     *            schedule data indexed by team
     * @param maxIterations
     *            maximum number of calculation passes
     * @param locationPercent
     *            percentage used for home and away adjustments
     * @param recencyPercent
     *            percentage compounded by game number
     * @param marginPercent
     *            percentage used for each margin tier
     * @return the ranking results and iteration information
     */
    public static RankingResults calculateResults(int[][][] schedules,
            int maxIterations, double locationPercent, double recencyPercent,
            double marginPercent) {
        if (maxIterations < 1) {
            throw new IllegalArgumentException(
                    "The maximum number of iterations must be at least 1.");
        }
        validatePercentage(locationPercent, "Location");
        validatePercentage(recencyPercent, "Recency");
        validatePercentage(marginPercent, "Margin of victory");

        double locationFactor = locationPercent / 100.0;
        double recencyFactor = recencyPercent / 100.0;
        double marginFactor = marginPercent / 100.0;
        int teamCount = FbsSchedules2026Compact.fbsTeams.length;
        int[][] rankings = new int[teamCount][2];
        double[] currentRatings = new double[teamCount];
        int[] ranksUsedForFinalRating = new int[teamCount];
        List<int[]> previousRankings = new ArrayList<int[]>();
        int[] bestRanks = new int[teamCount];
        int iterationsUsed = 0;
        boolean repeated = false;

        for (int team = 0; team < teamCount; team++) {
            rankings[team][1] = 1;
        }

        for (int pass = 0; pass < maxIterations; pass++) {
            for (int team = 0; team < teamCount; team++) {
                ranksUsedForFinalRating[team] = rankings[team][1];
            }

            for (int team = 0; team < teamCount; team++) {
                double rating = 0.0;
                int[][] teamSchedule = schedules[team];

                for (int i = 0; i < teamSchedule.length; i++) {
                    rating += adjustedGameImpact(teamSchedule[i], i,
                            teamCount, ranksUsedForFinalRating, locationFactor,
                            recencyFactor, marginFactor);
                }

                currentRatings[team] = rating;
                rankings[team][0] = (int) Math.round(rating);
            }

            Integer[] teamIndexes = new Integer[teamCount];
            for (int i = 0; i < teamIndexes.length; i++) {
                teamIndexes[i] = i;
            }
            Arrays.sort(teamIndexes,
                    (a, b) -> Double.compare(currentRatings[b], currentRatings[a]));

            for (int i = 0; i < teamIndexes.length; i++) {
                rankings[teamIndexes[i]][1] = i + 1;
            }
            iterationsUsed++;

            int[] currentRanking = new int[teamCount];
            for (int team = 0; team < teamCount; team++) {
                currentRanking[team] = rankings[team][1];
            }

            int matchingIndex = -1;
            for (int i = 0; i < previousRankings.size(); i++) {
                if (Arrays.equals(currentRanking, previousRankings.get(i))) {
                    matchingIndex = i;
                }
            }

            if (matchingIndex >= 0) {
                repeated = true;
                for (int team = 0; team < teamCount; team++) {
                    bestRanks[team] = currentRanking[team];
                    for (int i = matchingIndex; i < previousRankings.size(); i++) {
                        bestRanks[team] = Math.min(bestRanks[team],
                                previousRankings.get(i)[team]);
                    }
                }
                break;
            }

            previousRankings.add(currentRanking);
            if (previousRankings.size() > HISTORY_SIZE) {
                previousRankings.remove(0);
            }
        }

        if (!repeated) {
            for (int team = 0; team < teamCount; team++) {
                bestRanks[team] = rankings[team][1];
            }
        }

        int[][] gameImpacts = new int[teamCount][];
        for (int team = 0; team < teamCount; team++) {
            int[][] teamSchedule = schedules[team];
            gameImpacts[team] = new int[teamSchedule.length];

            for (int gameNumber = 0; gameNumber < teamSchedule.length; gameNumber++) {
                int[] game = teamSchedule[gameNumber];
                int opponentIndex = game[0];
                int impact = (int) Math.round(adjustedGameImpact(game,
                        gameNumber, teamCount, ranksUsedForFinalRating,
                        locationFactor, recencyFactor, marginFactor));
                gameImpacts[team][gameNumber] = impact;
            }
        }

        return new RankingResults(rankings, gameImpacts, bestRanks,
                iterationsUsed, repeated);
    }

    private static double adjustedGameImpact(int[] game, int gameNumber,
            int teamCount, int[] ranks, double locationFactor,
            double recencyFactor, double marginFactor) {
        int opponentIndex = game[0];
        int location = game[1];
        int result = game[2];
        int margin = Math.abs(game[3] - game[4]);
        double impact = 0.0;

        if (result == 1) {
            impact = opponentIndex == -1 ? 0.0
                    : teamCount - ranks[opponentIndex];
        } else if (result == -1) {
            impact = opponentIndex == -1 ? -150.0 : -ranks[opponentIndex];
        }

        if (impact == 0.0) {
            return 0.0;
        }

        if ((location == -1 && result == 1)
                || (location == 1 && result == -1)) {
            impact *= 1.0 - locationFactor;
        } else if ((location == -1 && result == -1)
                || (location == 1 && result == 1)) {
            impact *= 1.0 + locationFactor;
        }

        impact *= Math.pow(1.0 + recencyFactor, gameNumber);

        if (margin > 3) {
            int marginTier = (margin + 7) / 8;
            impact *= 1.0 + marginTier * marginFactor;
        }

        return impact;
    }

    private static void validatePercentage(double percentage, String label) {
        if (!Double.isFinite(percentage) || percentage < 0.0
                || percentage > 100.0) {
            throw new IllegalArgumentException(
                    label + " percentage must be between 0 and 100.");
        }
    }

    /**
     * Calculates ratings and ranks for all FBS teams using the default limit.
     *
     * @param schedules
     *            schedule data indexed by team
     * @return a two-dimensional array where column 0 is rating and column 1
     *         is rank
     */
    public static int[][] calculateRankings(int[][][] schedules) {
        return calculateResults(schedules).getRankings();
    }
}

