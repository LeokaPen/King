import javax.swing.*;

public class App {
    public static void main(String[] args) throws Exception {
        int boardWidth = 600;
        int boardHeight = boardWidth;

        // Create the JFrame
        JFrame frame = new JFrame("Snake");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        // Create the SnakeGame instance
        SnakeGame snakeGame = new SnakeGame(boardWidth, boardHeight, frame); // Pass the frame to SnakeGame

        // Add the SnakeGame to the frame
        frame.add(snakeGame);
        frame.pack(); // Pack the frame to fit the preferred size of components
        frame.setLocationRelativeTo(null); // Center the frame on the screen
        frame.setVisible(true); // Finally, make the frame visible

        // Request focus for key events
        snakeGame.requestFocus();
    }
}