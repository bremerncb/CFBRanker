/**
 * 2026 FBS schedules encoded as compact five-integer game records. Each game
 * is: {opponentIndex, location, result, teamScore, opponentScore}.
 *
 * location: -1 = home, 0 = neutral, 1 = away result: 1 = win, -1 = loss, 0 =
 * not played yet (or tie) opponentIndex: index in fbsTeams; -1 = opponent is
 * not in fbsTeams Unplayed games have scores 0, 0.
 */
public final class FbsSchedules2026Compact {
    private FbsSchedules2026Compact() {
    }

    public static final String[] fbsTeams = { "Air Force", "Akron", "Alabama",
            "Appalachian State", "Arizona", "Arizona State", "Arkansas", "Arkansas State",
            "Army", "Auburn", "Ball State", "Baylor", "Boise State", "Boston College",
            "Bowling Green", "Buffalo", "BYU", "California", "Central Michigan",
            "Charlotte", "Cincinnati", "Clemson", "Coastal Carolina", "Colorado",
            "Colorado State", "Delaware", "Duke", "East Carolina", "Eastern Michigan",
            "Florida Atlantic", "FIU", "Florida", "Florida State", "Fresno State",
            "Georgia", "Georgia Southern", "Georgia State", "Georgia Tech", "Hawaii",
            "Houston", "Illinois", "Indiana", "Iowa", "Iowa State", "Jacksonville State",
            "James Madison", "Kansas", "Kansas State", "Kennesaw State", "Kent State",
            "Kentucky", "Liberty", "Louisiana", "Louisiana Tech", "Louisville", "LSU",
            "Marshall", "Maryland", "Memphis", "Miami (FL)", "Miami (OH)", "Michigan",
            "Michigan State", "Middle Tennessee", "Minnesota", "Mississippi State",
            "Missouri", "Missouri State", "Navy", "NC State", "Nebraska", "Nevada",
            "New Mexico", "New Mexico State", "North Carolina", "North Dakota State",
            "North Texas", "Northern Illinois", "Northwestern", "Notre Dame", "Ohio",
            "Ohio State", "Oklahoma", "Oklahoma State", "Old Dominion", "Ole Miss",
            "Oregon", "Oregon State", "Penn State", "Pittsburgh", "Purdue", "Rice",
            "Rutgers", "Sacramento State", "Sam Houston", "San Diego State",
            "San Jose State", "SMU", "South Alabama", "South Carolina", "South Florida",
            "Southern Miss", "Stanford", "Syracuse", "TCU", "Temple", "Tennessee",
            "Texas", "Texas A&M", "Texas State", "Texas Tech", "Toledo", "Troy", "Tulane",
            "Tulsa", "UAB", "UCF", "UCLA", "UConn", "ULM", "UMass", "UNLV", "USC", "Utah",
            "Utah State", "UTEP", "UTSA", "Vanderbilt", "Virginia", "Virginia Tech",
            "Wake Forest", "Washington", "Washington State", "West Virginia",
            "Western Kentucky", "Western Michigan", "Wisconsin", "Wyoming" };

}
