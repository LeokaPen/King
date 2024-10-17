import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

public class LoadingScreen extends JPanel {

    private int progress = 0;
    private Timer loadingTimer;

    public LoadingScreen() {
        setPreferredSize(new Dimension(500, 500));
        setBackground(Color.black);
        
        // Timer to simulate loading progress
        loadingTimer = new Timer();
        loadingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                progress += 5;
                if (progress >= 100) {
                    progress = 100;
                    loadingTimer.cancel(); // Stop the timer when loading is done
                    // Here you can switch to the main game screen
                }
                repaint();
            }
        }, 0, 100); // Update every 100 milliseconds
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw loading text
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Loading...", 200, 200);

        // Draw progress bar
        g.setColor(Color.green);
        g.fillRect(100, 250, 300 * progress / 100, 30);

        // Draw progress percentage
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString(progress + "%", 230, 270);
        
        // Draw a sample image (replace this with your image path)
        // ImageIcon imageIcon = new ImageIcon("path/to/your/image.png");
        // Image image = imageIcon.getImage();
        // g.drawImage(image, 150, 300, this);
    }

    public boolean isLoadingComplete() {
        return progress >= 100;
    }
}
