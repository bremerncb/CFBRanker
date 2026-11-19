import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the compact FBS schedules from FbsSchedules2026.csv.
 *
 * CSV columns: team_index, opponent_index, location, result, team_score,
 * opponent_score
 *
 * Each returned game is: {opponentIndex, location, result, teamScore,
 * opponentScore}
 *
 * location: -1 = home, 0 = neutral, 1 = away result: 1 = win, -1 = loss, 0 =
 * tie or not yet played opponentIndex: -1 = opponent is not in the FBS teams
 * array
 */
public final class FbsScheduleLoader {

    private FbsScheduleLoader() {
    }

    /**
     * Reads the CSV file and returns one schedule for each team in
     * FbsSchedules2026Compact.fbsTeams.
     *
     * The CSV file must be in the program's working directory, or csvFile must
     * contain its full/relative path.
     *
     * @param csvFile
     *            path to FbsSchedules2026.csv
     * @return schedules indexed in the same order as fbsTeams
     * @throws IOException
     *             if the file cannot be read
     */
    public static int[][][] loadSchedules(String csvFile) throws IOException {
        int teamCount = FbsSchedules2026Compact.fbsTeams.length;

        List<List<int[]>> schedules = new ArrayList<List<int[]>>();
        for (int i = 0; i < teamCount; i++) {
            schedules.add(new ArrayList<int[]>());
        }

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(csvFile),
                StandardCharsets.UTF_8)) {

            String line = reader.readLine(); // Skip CSV header.
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] values = line.split(",");
                if (values.length != 6) {
                    throw new IOException("Invalid CSV row: " + line);
                }

                int teamIndex = Integer.parseInt(values[0]);
                int[] game = { Integer.parseInt(values[1]), Integer.parseInt(values[2]),
                        Integer.parseInt(values[3]), Integer.parseInt(values[4]),
                        Integer.parseInt(values[5]) };

                if (teamIndex < 0 || teamIndex >= teamCount) {
                    throw new IOException("Invalid team index: " + teamIndex);
                }

                schedules.get(teamIndex).add(game);
            }
        }

        int[][][] result = new int[teamCount][][];
        for (int i = 0; i < teamCount; i++) {
            List<int[]> teamSchedule = schedules.get(i);
            result[i] = teamSchedule.toArray(new int[teamSchedule.size()][]);
        }

        return result;
    }
}
