
import java.io.IOException;
import java.util.Arrays;

/**
 * Calculates college football team rankings.
 *
 * @author Nicholas Bremer
 */
public final class Ranker {

    public static final int DEFAULT_ITERATIONS = 15;

    private Ranker() {
    }

    /**
     * Stores the final rankings and individual game impacts.
     */
    public static final class RankingResults {
        private final double[][] rankings;
        private final double[][] gameImpacts;

        private RankingResults(double[][] rankings, double[][] gameImpacts) {
            this.rankings = rankings;
            this.gameImpacts = gameImpacts;
        }

        public double[][] getRankings() {
            return this.rankings;
        }

        public double[][] getGameImpacts() {
            return this.gameImpacts;
        }
    }

    /**
     * Calculates rankings using the default settings.
     */
    public static RankingResults calculateResults(int[][][] schedules) {
        return calculateResults(schedules, DEFAULT_ITERATIONS, 0, 0, 0);
    }

    /**
     * Calculates rankings using the selected factors.
     */
    public static RankingResults calculateResults(int[][][] schedules, int iterations,
            double locationFactor, double marginFactor, double recencyFactor) {

        if (iterations < 1 || !Double.isFinite(locationFactor)
                || !Double.isFinite(marginFactor) || !Double.isFinite(recencyFactor)
                || locationFactor < 0 || locationFactor > 1 || marginFactor < 0
                || recencyFactor < 0) {
            throw new IllegalArgumentException("Invalid ranking settings.");
        }

        int teamCount = FbsSchedules2026Compact.fbsTeams.length;

        double[][] rankings = new double[teamCount][2];
        double[][] gameImpacts = new double[teamCount][];

        for (int i = 0; i < teamCount; i++) {
            rankings[i][1] = 1;
            gameImpacts[i] = new double[schedules[i].length];
        }

        for (int p = 0; p < iterations; p++) {

            for (int team = 0; team < teamCount; team++) {
                int[][] teamSchedule = schedules[team];
                double rating = 0;

                for (int i = 0; i < teamSchedule.length; i++) {
                    int[] game = teamSchedule[i];

                    int opponentIndex = game[0];
                    int location = game[1];
                    int result = game[2];
                    int teamScore = game[3];
                    int opponentScore = game[4];

                    double impact = 0;

                    if (opponentIndex != -1) {
                        if (result == -1) {
                            impact = -rankings[opponentIndex][1];
                        } else if (result == 1) {
                            impact = teamCount - rankings[opponentIndex][1];
                        }
                    } else if (result == -1) {
                        impact = -150;
                    }

                    if (result != 0) {
                        double locationMultiplier = 1
                                + locationFactor * result * location;

                        int margin = Math.abs(teamScore - opponentScore);
                        int marginTier = margin >= 4 ? (margin + 7) / 8 : 0;

                        double marginMultiplier = 1 + marginTier * marginFactor;

                        double recencyMultiplier = Math.pow(1 + recencyFactor, i);

                        impact *= locationMultiplier;
                        impact *= marginMultiplier;
                        impact *= recencyMultiplier;
                    }

                    gameImpacts[team][i] = impact;
                    rating += impact;
                }

                rankings[team][0] = rating;
            }

            Integer[] teamIndexes = new Integer[teamCount];

            for (int i = 0; i < teamCount; i++) {
                teamIndexes[i] = i;
            }

            Arrays.sort(teamIndexes,
                    (a, b) -> Double.compare(rankings[b][0], rankings[a][0]));

            for (int i = 0; i < teamCount; i++) {
                rankings[teamIndexes[i]][1] = i + 1;
            }
        }

        return new RankingResults(rankings, gameImpacts);
    }

    /**
     * Runs the rankings with the default settings and prints the results.
     */
    public static void main(String[] args) throws IOException {
        int[][][] schedules = FbsScheduleLoader
                .loadSchedules("data/FbsSchedules2026.csv");

        RankingResults results = calculateResults(schedules);
        double[][] rankings = results.getRankings();

        Integer[] teamIndexes = new Integer[rankings.length];

        for (int i = 0; i < teamIndexes.length; i++) {
            teamIndexes[i] = i;
        }

        Arrays.sort(teamIndexes,
                (a, b) -> Double.compare(rankings[a][1], rankings[b][1]));

        for (int i = 0; i < teamIndexes.length; i++) {
            int index = teamIndexes[i];
            System.out.println((i + 1) + ". " + FbsSchedules2026Compact.fbsTeams[index]
                    + " -> " + rankings[index][0]);
        }
    }
}
