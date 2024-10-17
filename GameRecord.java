
public class GameRecord {
    private String snakeColor;
    private int score;
    private long time;

    // Constructor
    public GameRecord(String snakeColor, int score, long time) {
        this.snakeColor = snakeColor;
        this.score = score;
        this.time = time;
    }

    // Getters
    public String getSnakeColor() {
        return snakeColor;
    }

    public int getScore() {
        return score;
    }

    public long getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "Color: " + snakeColor + ", Score: " + score + ", Time: " + time + "ms";
    }
}
