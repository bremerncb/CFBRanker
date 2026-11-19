import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Small local web server for the CFB Rankings website.
 */
public final class CfbWebServer {
    private static final int PORT = 18766;
    private static final String CSV_FILE = "data/FbsSchedules2026.csv";
    private static final int DEFAULT_ITERATIONS = 100;

    private static int[][][] schedules;
    private static int[][] rankings;
    private static int[][] gameImpacts;
    private static int[] bestRanks;
    private static int iterationsUsed;
    private static boolean repeated;
    private static double locationPercent;
    private static double recencyPercent;
    private static double marginPercent;

    private CfbWebServer() {
    }

    public static void main(String[] args) throws IOException {
        schedules = FbsScheduleLoader.loadSchedules(CSV_FILE);
        recalculate(DEFAULT_ITERATIONS, 0.0, 0.0, 0.0);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", CfbWebServer::handleRequest);
        server.setExecutor(null);
        server.start();

        System.out.println("CFB Rankings website is running.");
        System.out.println("Open http://localhost:" + PORT + " in your browser.");
        System.out.println("Press Ctrl+C to stop the server.");
    }

    private static void recalculate(int maxIterations, double location,
            double recency, double margin) {
        Ranker.RankingResults results = Ranker.calculateResults(schedules,
                maxIterations, location, recency, margin);
        rankings = results.getRankings();
        gameImpacts = results.getGameImpacts();
        bestRanks = results.getBestRanks();
        iterationsUsed = results.getIterationsUsed();
        repeated = results.isRepeated();
        locationPercent = location;
        recencyPercent = recency;
        marginPercent = margin;
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("GET".equals(method) && "/".equals(path)) {
            sendFile(exchange, Paths.get("index.html"), "text/html; charset=utf-8");
        } else if ("GET".equals(method) && "/api/rankings".equals(path)) {
            sendJson(exchange, rankingsJson());
        } else if ("GET".equals(method) && "/api/status".equals(path)) {
            sendJson(exchange, statusJson());
        } else if ("GET".equals(method) && "/api/team".equals(path)) {
            sendTeam(exchange, exchange.getRequestURI().getRawQuery());
        } else if ("POST".equals(method) && "/api/recalculate".equals(path)) {
            recalculateFromRequest(exchange);
        } else {
            sendText(exchange, 404, "Not found");
        }
    }

    private static void recalculateFromRequest(HttpExchange exchange)
            throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
        int maxIterations = -1;
        double location = -1.0;
        double recency = -1.0;
        double margin = -1.0;

        for (String part : body.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2) {
                String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                try {
                    if ("iterations".equals(pair[0])) {
                        maxIterations = Integer.parseInt(value);
                    } else if ("location".equals(pair[0])) {
                        location = Double.parseDouble(value);
                    } else if ("recency".equals(pair[0])) {
                        recency = Double.parseDouble(value);
                    } else if ("margin".equals(pair[0])) {
                        margin = Double.parseDouble(value);
                    }
                } catch (NumberFormatException ignored) {
                    // Invalid values are rejected by the validation below.
                }
            }
        }

        if (maxIterations < 1 || maxIterations > 1000) {
            sendText(exchange, 400,
                    "Iterations must be a whole number between 1 and 1000.");
        } else if (!validPercentage(location) || !validPercentage(recency)
                || !validPercentage(margin)) {
            sendText(exchange, 400,
                    "Location, recency, and margin percentages must be between 0 and 100.");
        } else {
            recalculate(maxIterations, location, recency, margin);
            sendJson(exchange, statusJson());
        }
    }

    private static boolean validPercentage(double value) {
        return Double.isFinite(value) && value >= 0.0 && value <= 100.0;
    }

    private static String statusJson() {
        return "{\"iterations\":" + iterationsUsed
                + ",\"repeated\":" + repeated
                + ",\"location\":" + locationPercent
                + ",\"recency\":" + recencyPercent
                + ",\"margin\":" + marginPercent + "}";
    }

    private static String rankingsJson() {
        List<Integer> indexes = new ArrayList<Integer>();
        for (int i = 0; i < rankings.length; i++) {
            indexes.add(i);
        }
        indexes.sort(Comparator.comparingInt(i -> bestRanks[i]));

        StringBuilder json = new StringBuilder("[");
        for (int k = 0; k < indexes.size(); k++) {
            int index = indexes.get(k);
            if (k > 0) {
                json.append(',');
            }
            json.append("{\"index\":").append(index)
                    .append(",\"team\":\"")
                    .append(jsonEscape(FbsSchedules2026Compact.fbsTeams[index]))
                    .append("\",\"rating\":").append(rankings[index][0])
                    .append(",\"rank\":").append(bestRanks[index])
                    .append('}');
        }
        return json.append(']').toString();
    }

    private static void sendTeam(HttpExchange exchange, String query)
            throws IOException {
        int index = -1;
        if (query != null) {
            for (String part : query.split("&")) {
                String[] pair = part.split("=", 2);
                if (pair.length == 2 && "id".equals(pair[0])) {
                    try {
                        index = Integer.parseInt(
                                URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                    } catch (NumberFormatException ignored) {
                        index = -1;
                    }
                }
            }
        }

        if (index < 0 || index >= schedules.length) {
            sendText(exchange, 400, "Invalid team index");
        } else {
            int wins = 0;
            int losses = 0;
            int ties = 0;
            StringBuilder games = new StringBuilder("[");
            int[][] schedule = schedules[index];

            for (int i = 0; i < schedule.length; i++) {
                int[] game = schedule[i];
                int opponentIndex = game[0];
                int location = game[1];
                int result = game[2];
                int teamScore = game[3];
                int opponentScore = game[4];

                if (result == 1) {
                    wins++;
                } else if (result == -1) {
                    losses++;
                } else if (teamScore != 0 || opponentScore != 0) {
                    ties++;
                }

                if (i > 0) {
                    games.append(',');
                }
                String opponent = opponentIndex == -1 ? "FCS opponent"
                        : FbsSchedules2026Compact.fbsTeams[opponentIndex];
                String venue = location == -1 ? "Home"
                        : location == 1 ? "Away" : "Neutral site";
                String status = result == 1 ? "Win"
                        : result == -1 ? "Loss"
                                : (teamScore == 0 && opponentScore == 0
                                        ? "Upcoming / no result" : "Tie");

                games.append("{\"opponent\":\"")
                        .append(jsonEscape(opponent))
                        .append("\",\"location\":\"")
                        .append(jsonEscape(venue))
                        .append("\",\"status\":\"").append(status)
                        .append("\",\"result\":").append(result)
                        .append(",\"teamScore\":").append(teamScore)
                        .append(",\"opponentScore\":").append(opponentScore)
                        .append(",\"ratingImpact\":")
                        .append(gameImpacts[index][i]).append('}');
            }
            games.append(']');

            String json = "{\"index\":" + index
                    + ",\"team\":\""
                    + jsonEscape(FbsSchedules2026Compact.fbsTeams[index]) + "\""
                    + ",\"rank\":" + bestRanks[index]
                    + ",\"rating\":" + rankings[index][0]
                    + ",\"wins\":" + wins + ",\"losses\":" + losses
                    + ",\"ties\":" + ties + ",\"games\":" + games + "}";
            sendJson(exchange, json);
        }
    }

    private static void sendFile(HttpExchange exchange, Path path,
            String contentType) throws IOException {
        if (!Files.exists(path)) {
            sendText(exchange, 404, "Website file not found: " + path);
        } else {
            byte[] bytes = Files.readAllBytes(path);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        }
    }

    private static void sendJson(HttpExchange exchange, String json)
            throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type",
                "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void sendText(HttpExchange exchange, int status,
            String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type",
                "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
