import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;

public class SnakeGame extends JPanel implements ActionListener, KeyListener {
    // Inner class representing a tile on the board
    private class Tile {
        int x;
        int y;

        Tile(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // Enum for Snake Colors
    enum SnakeType { 
        GREEN, BLUE, YELLOW 
    }

    // Enum for Difficulty Levels with corresponding speeds
    enum Difficulty {
        EASY(150), MEDIUM(100), HARD(50);

        int speed;

        Difficulty(int speed) {
            this.speed = speed;
        }
    }

    // Inner class for High Scores, implementing Serializable for persistence
    private class HighScore implements Comparable<HighScore>, Serializable {
        private static final long serialVersionUID = 1L;
        String name;
        int score;

        HighScore(String name, int score) {
            this.name = name;
            this.score = score;
        }

        @Override
        public int compareTo(HighScore other) {
            return other.score - this.score; // Descending order
        }
    }

    // Game Board Dimensions
    int boardWidth;
    int boardHeight;
    int tileSize = 25;

    // Snake Properties
    Tile snakeHead;
    ArrayList<Tile> snakeBody;
    Color snakeColor;
    SnakeType snakeType;
    boolean snakeTypeSelected = false;

    // Food Properties
    Tile food;
    Tile specialFood;
    Random random;

    // Game Logic Variables
    int velocityX;
    int velocityY;
    Timer gameLoop;
    long lastMoveTime = 0; // Delay between key presses

    boolean gameOver = false;
    boolean paused = false;
    boolean gameStarted = false;

    // Score and Time Tracking
    int score = 0;
    long startTime; // Time when the game starts
    long elapsedTime; // Time elapsed since the game started

    // Difficulty Level
    Difficulty difficulty;
    boolean difficultySelected = false;

    // Audio Clips
    Clip backgroundMusic;
    Clip eatSoundEffect; // Sound effect for eating food

    // Background Images
    Image mainMenuBackground;
    Image gameBackground;

    // JFrame Reference
    JFrame frame;

    // High Scores List and File Path
    private ArrayList<HighScore> highScores;
    private final String HIGH_SCORE_FILE = "highscores.dat";

    // Animation Variables for Scoreboard Fade-In
    float scoreboardOpacity = 0f;
    Timer animationTimer;

    // Flag to Indicate a New High Score
    private boolean isNewHighScore = false;

    // Constructor
    SnakeGame(int boardWidth, int boardHeight, JFrame frame) {
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.frame = frame;
        setPreferredSize(new Dimension(this.boardWidth, this.boardHeight));
        setBackground(Color.darkGray);
        addKeyListener(this);
        setFocusable(true);

        // Load Background Images
        mainMenuBackground = loadImage("image/background.jpg"); // Replace with your main menu background image path
        gameBackground = loadImage("image/game.jpg"); // Replace with your game background image path

        // Initialize Snake Attributes
        snakeType = SnakeType.GREEN;
        difficulty = Difficulty.MEDIUM;
        setSnakeAttributes(snakeType);

        // Initialize Food
        food = new Tile(10, 10);
        specialFood = new Tile(-1, -1); // Offscreen initially
        random = new Random();
        placeFood();

        // Initialize Snake Movement
        velocityX = 1;
        velocityY = 0;

        // Initialize Game Timer Based on Difficulty
        gameLoop = new Timer(difficulty.speed, this);

        // Load and Play Background Music
        loadMusic("audio/gameplay.wav");  // Replace with your music file path
        playMusic();

        // Load Sound Effect for Eating Food
        eatSoundEffect = loadSoundEffect("audio/eat.wav.wav"); // Replace with your sound effect file path

        // Initialize High Scores
        highScores = new ArrayList<>();
        loadHighScores();

        // Initialize Animation Timer for Scoreboard Fade-In
        animationTimer = new Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (scoreboardOpacity < 1f) {
                    scoreboardOpacity += 0.05f;
                    if (scoreboardOpacity >= 1f) {
                        scoreboardOpacity = 1f;
                        animationTimer.stop();
                    }
                    repaint();
                }
            }
        });
    }

    // Paint Component Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawBackground(g); // Draw appropriate background
        if (gameStarted) {
            draw(g);
        } else {
            drawStartScreen(g);
        }
    }

    // Draw Background Based on Game State
    public void drawBackground(Graphics g) {
        if (gameStarted) {
            g.drawImage(gameBackground, 0, 0, boardWidth, boardHeight, null);
        } else {
            g.drawImage(mainMenuBackground, 0, 0, boardWidth, boardHeight, null);
        }
    }

    // Draw Start Screen with Snake and Difficulty Selection
    public void drawStartScreen(Graphics g) {
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        drawStringWithShadow(g, "Select Your Snake", boardWidth / 2 - 160, boardHeight / 4, Color.white, Color.darkGray);

        g.setFont(new Font("Arial", Font.PLAIN, 24));

        // Draw Boxes Around Snake Options
        g.drawRect(boardWidth / 2 - 100, boardHeight / 2 - 60, 200, 40);
        g.drawRect(boardWidth / 2 - 100, boardHeight / 2 - 20, 200, 40);
        g.drawRect(boardWidth / 2 - 100, boardHeight / 2 + 20, 200, 40);

        // Draw Snake Options Text Inside Boxes
        drawStringWithShadow(g, "1. Green Snake", boardWidth / 2 - 80, boardHeight / 2 - 40, Color.white, Color.darkGray);
        drawStringWithShadow(g, "2. Blue Snake", boardWidth / 2 - 80, boardHeight / 2, Color.white, Color.darkGray);
        drawStringWithShadow(g, "3. Yellow Snake", boardWidth / 2 - 80, boardHeight / 2 + 40, Color.white, Color.darkGray);

        if (snakeTypeSelected) {
            drawStringWithShadow(g, "Select Difficulty", boardWidth / 2 - 120, boardHeight / 2 + 100, Color.white, Color.darkGray);

            // Draw Boxes Around Difficulty Options
            g.drawRect(boardWidth / 2 - 100, boardHeight / 2 + 120, 200, 40);
            g.drawRect(boardWidth / 2 - 100, boardHeight / 2 + 160, 200, 40);
            g.drawRect(boardWidth / 2 - 100, boardHeight / 2 + 200, 200, 40);

            // Draw Difficulty Options Text Inside Boxes
            drawStringWithShadow(g, "E. Easy", boardWidth / 2 - 80, boardHeight / 2 + 140, Color.white, Color.darkGray);
            drawStringWithShadow(g, "M. Medium", boardWidth / 2 - 80, boardHeight / 2 + 180, Color.white, Color.darkGray);
            drawStringWithShadow(g, "H. Hard", boardWidth / 2 - 80, boardHeight / 2 + 220, Color.white, Color.darkGray);
            drawStringWithShadow(g, "Press E, M, or H to choose Difficulty.", boardWidth / 2 - 160, boardHeight / 2 + 300, Color.white, Color.darkGray);
        } else {
            drawStringWithShadow(g, "Press 1, 2, or 3 to choose Snake.", boardWidth / 2 - 160, boardHeight / 2 + 260, Color.white, Color.darkGray);
        }
    }

    // Set Snake Color Based on Selection
    public void setSnakeAttributes(SnakeType type) {
        switch (type) {
            case GREEN:
                snakeColor = Color.green;
                break;
            case BLUE:
                snakeColor = Color.blue;
                break;
            case YELLOW:
                snakeColor = Color.yellow;
                break;
        }
    }

    // Start the Game
    public void startGame() {
        snakeHead = new Tile(5, 5);
        snakeBody = new ArrayList<Tile>();
        gameStarted = true;
        gameLoop.setDelay(difficulty.speed);
        gameLoop.start();
        startTime = System.currentTimeMillis(); // Record the start time
    }

    // Draw Game Elements
    public void draw(Graphics g) {
        // Draw Grid Lines
        g.setColor(Color.gray);
        for (int i = 0; i < boardWidth / tileSize; i++) {
            g.drawLine(i * tileSize, 0, i * tileSize, boardHeight);
            g.drawLine(0, i * tileSize, boardWidth, i * tileSize);
        }

        // Draw Food
        g.setColor(Color.red);
        g.fill3DRect(food.x * tileSize, food.y * tileSize, tileSize, tileSize, true);

        // Draw Special Food if Present
        if (specialFood.x != -1 && specialFood.y != -1) {
            g.setColor(Color.orange);
            g.fill3DRect(specialFood.x * tileSize, specialFood.y * tileSize, tileSize, tileSize, true);
        }

        // Draw Snake Head
        g.setColor(snakeColor);
        g.fill3DRect(snakeHead.x * tileSize, snakeHead.y * tileSize, tileSize, tileSize, true);

        // Draw Snake Body
        for (Tile snakePart : snakeBody) {
            g.fill3DRect(snakePart.x * tileSize, snakePart.y * tileSize, tileSize, tileSize, true);
        }

        // Draw Score
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.setColor(Color.white);
        g.drawString("Score: " + score, 10, 20);

        // Draw Pause Message if Paused
        if (paused) {
            drawStringWithShadow(g, "Paused", boardWidth / 2 - 70, boardHeight / 2, Color.yellow, Color.darkGray);
        }

        // Draw Game Over Screen
        if (gameOver) {
            // Calculate Dynamic Positions
            int centerX = boardWidth / 2;
            int centerY = boardHeight / 2;

            // Draw "Game Over" Message with Shadow
            drawStringWithShadow(g, "Game Over", centerX - 100, centerY - 80, Color.red, Color.darkGray);

            // Draw Semi-Transparent Panel for Scoreboard
            Graphics2D g2d = (Graphics2D) g;
            Composite original = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, scoreboardOpacity * 0.6f)); // Animated opacity
            g2d.setColor(Color.black);
            g2d.fillRoundRect(centerX - 200, centerY - 60, 400, 300, 25, 25); // Increased height for better spacing
            g2d.setComposite(original); // Reset to original opacity

            // Display Scoreboard with Shadow
            g.setFont(new Font("Arial", Font.BOLD, 24));
            int scoreboardX = centerX - 180;
            int scoreboardY = centerY - 30;

            drawStringWithShadow(g, "----- Scoreboard -----", scoreboardX, scoreboardY, Color.white, Color.darkGray);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            drawStringWithShadow(g, "Color Selected: " + snakeType.toString(), scoreboardX, scoreboardY + 30, Color.white, Color.darkGray);
            drawStringWithShadow(g, "Difficulty: " + difficulty.toString(), scoreboardX, scoreboardY + 60, Color.white, Color.darkGray);
            drawStringWithShadow(g, "Points: " + score, scoreboardX, scoreboardY + 90, Color.white, Color.darkGray);
            drawStringWithShadow(g, "Time Played: " + formatTime(elapsedTime), scoreboardX, scoreboardY + 120, Color.white, Color.darkGray);

            // Display "You Got a New High Score!" Message if Applicable
            if (isNewHighScore) {
                drawStringWithShadow(g, "You Got a New High Score!", centerX - 150, centerY - 120, Color.green, Color.darkGray);
            }

            // Display High Scores with Shadow
            g.setFont(new Font("Arial", Font.BOLD, 22));
            drawStringWithShadow(g, "----- High Scores -----", scoreboardX, scoreboardY + 160, Color.yellow, Color.darkGray);
            g.setFont(new Font("Arial", Font.PLAIN, 18));
            int highScoreY = scoreboardY + 190;
            for (int i = 0; i < highScores.size() && i < 5; i++) {
                HighScore hs = highScores.get(i);
                drawStringWithShadow(g, (i + 1) + ". " + hs.name + " - " + hs.score, scoreboardX, highScoreY, Color.white, Color.darkGray);
                highScoreY += 20;
            }

            // Display Restart Instructions Much Lower
            drawStringWithShadow(g, "Press 'R' to Restart or 'M' for Main Menu", centerX - 180, centerY + 250, Color.white, Color.darkGray); // Increased Y-coordinate

            gameLoop.stop();
        }
    }

    // Place Food at Random Location
    public void placeFood() {
        food.x = random.nextInt(boardWidth / tileSize);
        food.y = random.nextInt(boardHeight / tileSize);
    }

    // Place Special Food at Random Location
    public void placeSpecialFood() {
        specialFood.x = random.nextInt(boardWidth / tileSize);
        specialFood.y = random.nextInt(boardHeight / tileSize);
    }

    // Move the Snake
    public void moveSnake() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMoveTime < 100) return; // Prevent moving too fast
        lastMoveTime = currentTime;

        // Move Snake Body
        snakeBody.add(0, new Tile(snakeHead.x, snakeHead.y)); // Add new head
        snakeHead.x += velocityX;
        snakeHead.y += velocityY;
        if (snakeBody.size() > score) {
            snakeBody.remove(snakeBody.size() - 1); // Remove last segment
        }
    }

    // Check for Collisions (Walls or Self)
    public void checkCollisions() {
        boolean collisionDetected = false;

        // Check Wall Collisions
        if (snakeHead.x < 0 || snakeHead.x >= boardWidth / tileSize || 
            snakeHead.y < 0 || snakeHead.y >= boardHeight / tileSize) {
            collisionDetected = true;
        }

        // Check Self Collisions
        for (Tile tile : snakeBody) {
            if (tile.x == snakeHead.x && tile.y == snakeHead.y) {
                collisionDetected = true;
                break;
            }
        }

        // Handle Collision
        if (collisionDetected) {
            gameOver = true;
            calculateElapsedTime();
            checkHighScore();
            animationTimer.start(); // Start the fade-in animation
        }
    }

    // Check for Food Consumption
    public void checkFoodCollision() {
        if (snakeHead.x == food.x && snakeHead.y == food.y) {
            score++;
            placeFood();
            playSoundEffect(eatSoundEffect); // Play sound effect when eating food
        }

        // Check for Special Food
        if (specialFood.x == -1 && specialFood.y == -1 && random.nextInt(10) < 2) { // 20% chance to spawn special food
            placeSpecialFood();
        }

        if (snakeHead.x == specialFood.x && snakeHead.y == specialFood.y) {
            score += 5; // Increase score more for special food
            specialFood.x = -1; // Remove special food after eating
            specialFood.y = -1;
            playSoundEffect(eatSoundEffect); // Play sound effect when eating special food
        }
    }

    // Load Image with Enhanced Error Handling
    public Image loadImage(String filePath) {
        Image img = null;
        try {
            File imgFile = new File(filePath);
            if (!imgFile.exists()) {
                throw new IOException("Image file not found: " + filePath);
            }
            img = Toolkit.getDefaultToolkit().getImage(filePath);
            MediaTracker tracker = new MediaTracker(this);
            tracker.addImage(img, 0);
            tracker.waitForAll();
        } catch (Exception e) {
            System.err.println("Error loading image: " + filePath);
            e.printStackTrace();
            // Load a default placeholder image
            img = new BufferedImage(boardWidth, boardHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = ((BufferedImage) img).createGraphics();
            g2d.setColor(Color.pink);
            g2d.fillRect(0, 0, boardWidth, boardHeight);
            g2d.setColor(Color.black);
            g2d.drawString("Image Not Found", boardWidth / 2 - 50, boardHeight / 2);
            g2d.dispose();
        }
        return img;
    }

    // Load Music with Enhanced Error Handling
    public void loadMusic(String filePath) {
        try {
            File musicFile = new File(filePath);
            if (!musicFile.exists()) {
                throw new IOException("Music file not found: " + filePath);
            }
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(AudioSystem.getAudioInputStream(musicFile));
        } catch (Exception e) {
            System.err.println("Error loading music: " + filePath);
            e.printStackTrace();
            // Handle missing music gracefully
        }
    }

    // Play Background Music
    public void playMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    // Load Sound Effect with Enhanced Error Handling
    public Clip loadSoundEffect(String filePath) {
        Clip clip = null;
        try {
            File soundFile = new File(filePath);
            if (!soundFile.exists()) {
                throw new IOException("Sound file not found: " + filePath);
            }
            clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(soundFile));
        } catch (Exception e) {
            System.err.println("Error loading sound effect: " + filePath);
            e.printStackTrace();
            // Handle missing sound effect gracefully
        }
        return clip;
    }

    // Play Sound Effect
    public void playSoundEffect(Clip clip) {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }

    // Format Time from Milliseconds to MM:SS
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // Calculate Elapsed Time
    private void calculateElapsedTime() {
        elapsedTime = System.currentTimeMillis() - startTime;
    }

    // Handle Timer Events
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!paused && gameStarted) {
            moveSnake();
            checkCollisions();
            checkFoodCollision();
            // Update elapsed time
            elapsedTime = System.currentTimeMillis() - startTime;
            repaint();
        }
    }

    // Handle Key Press Events
    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_R) {
                restartGame();
            } else if (e.getKeyCode() == KeyEvent.VK_M) {
                showMainMenu();
            }
        } else if (gameStarted) {
            handleArrowKeys(e);
        } else {
            handleStartMenuKeys(e);
        }
    }

    // Handle Start Menu Key Presses
    public void handleStartMenuKeys(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_1) {
            snakeType = SnakeType.GREEN;
            snakeTypeSelected = true;
            setSnakeAttributes(snakeType);
            repaint();
        } else if (e.getKeyCode() == KeyEvent.VK_2) {
            snakeType = SnakeType.BLUE;
            snakeTypeSelected = true;
            setSnakeAttributes(snakeType);
            repaint();
        } else if (e.getKeyCode() == KeyEvent.VK_3) {
            snakeType = SnakeType.YELLOW;
            snakeTypeSelected = true;
            setSnakeAttributes(snakeType);
            repaint();
        }

        if (snakeTypeSelected) {
            if (e.getKeyCode() == KeyEvent.VK_E) {
                difficulty = Difficulty.EASY;
                startGame();
            } else if (e.getKeyCode() == KeyEvent.VK_M) {
                difficulty = Difficulty.MEDIUM;
                startGame();
            } else if (e.getKeyCode() == KeyEvent.VK_H) {
                difficulty = Difficulty.HARD;
                startGame();
            }
        }
    }

    // Handle Arrow Key Presses for Snake Movement
    public void handleArrowKeys(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP && velocityY == 0) {
            velocityX = 0;
            velocityY = -1;
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN && velocityY == 0) {
            velocityX = 0;
            velocityY = 1;
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT && velocityX == 0) {
            velocityX = -1;
            velocityY = 0;
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && velocityX == 0) {
            velocityX = 1;
            velocityY = 0;
        }
    }

    // Restart the Game
    public void restartGame() {
        score = 0;
        snakeBody.clear();
        snakeHead = new Tile(5, 5);
        placeFood();
        specialFood.x = -1; // Reset special food
        specialFood.y = -1;
        gameOver = false;
        startTime = System.currentTimeMillis(); // Reset start time
        elapsedTime = 0; // Reset elapsed time
        scoreboardOpacity = 0f; // Reset opacity for fade-in
        animationTimer.stop(); // Stop any ongoing animation
        isNewHighScore = false; // Reset the high score flag
        gameLoop.start();
        repaint();
    }

    // Show Main Menu
    public void showMainMenu() {
        gameStarted = false;
        gameOver = false;
        snakeTypeSelected = false;
        difficultySelected = false;

        // Reset Snake Attributes
        snakeBody.clear(); // Clear the body of the snake
        snakeHead = new Tile(5, 5); // Reset snake head position
        score = 0; // Reset score
        velocityX = 1; // Reset velocity
        velocityY = 0; // Reset velocity
        elapsedTime = 0; // Reset elapsed time
        scoreboardOpacity = 0f; // Reset opacity for fade-in
        animationTimer.stop(); // Ensure the animation timer is stopped
        isNewHighScore = false; // Reset the high score flag
        gameLoop.stop();
        repaint();
    }

    // Helper Method to Draw Strings with Shadow
    private void drawStringWithShadow(Graphics g, String text, int x, int y, Color textColor, Color shadowColor) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(shadowColor);
        g2d.drawString(text, x + 2, y + 2); // Shadow offset
        g2d.setColor(textColor);
        g2d.drawString(text, x, y);
    }

    // Load High Scores from File
    private void loadHighScores() {
        File file = new File(HIGH_SCORE_FILE);
        if (!file.exists()) {
            // Initialize with empty high scores
            highScores = new ArrayList<>();
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            highScores = (ArrayList<HighScore>) ois.readObject();
        } catch (Exception e) {
            System.err.println("Error loading high scores.");
            e.printStackTrace();
            highScores = new ArrayList<>();
        }
    }

    // Save High Scores to File
    private void saveHighScores() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(HIGH_SCORE_FILE))) {
            oos.writeObject(highScores);
        } catch (IOException e) {
            System.err.println("Error saving high scores.");
            e.printStackTrace();
        }
    }

    // Check and Update High Scores
    private void checkHighScore() {
        final int MAX_HIGH_SCORES = 5;

        // Determine if current score qualifies as a high score
        if (highScores.size() < MAX_HIGH_SCORES) {
            // Automatically qualifies if fewer than 5 high scores
            promptForHighScore();
        } else {
            // Check if current score is higher than the lowest existing high score
            HighScore lowestHighScore = highScores.get(highScores.size() - 1); // Since list is sorted descendingly
            if (score > lowestHighScore.score) {
                promptForHighScore();
            }
        }
    }

    // Prompt Player for High Score Entry
    private void promptForHighScore() {
        isNewHighScore = true; // Set the flag to display the message
        String name = JOptionPane.showInputDialog(this, "You Got a New High Score! Enter your name:", "High Score", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            highScores.add(new HighScore(name.trim(), score));
            Collections.sort(highScores);
            if (highScores.size() > 5) {
                highScores.remove(highScores.size() - 1); // Remove the lowest score if exceeding the limit
            }
            saveHighScores();
        }
    }

    // Main Method to Run the Game
    public static void main(String[] args) {
        JFrame frame = new JFrame("Snake Game");
        // Increased resolution to 1200x800
        SnakeGame snakeGame = new SnakeGame(1200, 800, frame);
        frame.add(snakeGame);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);
    }

    // Empty Implementations for KeyListener Interface Methods
    @Override
    public void keyReleased(KeyEvent e) {
        // Not used but must be implemented
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used but must be implemented
    }
}