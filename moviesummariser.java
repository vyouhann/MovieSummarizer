package movie;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.sql.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

public class Movie extends JFrame {
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USERNAME = "loginpage"; // Replace with your Oracle username
    private static final String PASSWORD = "123"; // Replace with your Oracle password

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField searchField;
    private JButton searchButton;
    private JButton addButton; // New button for adding movies
    private JPanel moviePanel;

    public Movie() {
        setTitle("Movie Viewer");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // Define gradient colors
                Color startColor = new Color(255, 0, 0); // Red
                Color endColor = new Color(0, 0, 0);      // Black

                // Define gradient direction
                Point2D startPoint = new Point2D.Float(0, 0);
                Point2D endPoint = new Point2D.Float(0, getHeight());

                // Create gradient paint
                GradientPaint gradient = new GradientPaint(startPoint, startColor, endPoint, endColor);

                // Set paint to graphics context
                g2d.setPaint(gradient);

                // Fill rectangle with gradient
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        Font labelFont = new Font("Arial", Font.BOLD, 16); // Example: Arial, Bold, Size 16
        JLabel userIconLabel = new JLabel();
        try {
            URL imageUrl = new URL("https://png.pngtree.com/png-clipart/20220613/original/pngtree-realistic-cinema-clapperboard-icon-on-red-background-png-image_8003407.png"); // Replace with your image URL
            ImageIcon userIcon = new ImageIcon(ImageIO.read(imageUrl).getScaledInstance(110, 100, Image.SCALE_SMOOTH));
            userIconLabel.setIcon(userIcon);
        } catch (IOException e) {
            e.printStackTrace();
        }
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(userIconLabel, gbc);

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setForeground(Color.WHITE);
        usernameLabel.setFont(labelFont);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(usernameLabel, gbc);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(Color.WHITE);
        passwordLabel.setFont(labelFont);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(passwordLabel, gbc);

        usernameField = new JTextField(15);
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(usernameField, gbc);

        passwordField = new JPasswordField(15);
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(passwordField, gbc);

        JButton loginButton = new JButton("Login");
          loginButton.setForeground(Color.WHITE);
        loginButton.setBackground(new Color(15, 30, 54)); // Different shade of blue
        loginButton.setFocusPainted(false);
        loginButton.setFont(new Font("Arial", Font.PLAIN, 14)); // Less bold font
        loginButton.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        loginButton.setPreferredSize(new Dimension(45, 35));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                if (authenticate(username, password)) {
                    JOptionPane.showMessageDialog(null, "Login successful. Retrieving movies...");
                    displayMoviesAndSearch();
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid username or password. Access denied.");
                }
            }
        });
        panel.add(loginButton, gbc);

        add(panel);
        setVisible(true);

        // Load the JDBC driver
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private boolean authenticate(String username, String password) {
        // Implement your authentication logic here
        // For simplicity, let's assume hardcoded username and password
        return USERNAME.equals(username) && PASSWORD.equals(password);
    }

    private void displayMoviesAndSearch() {
        moviePanel = new JPanel(new BorderLayout());

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel searchLabel = new JLabel("Search:");
        searchPanel.add(searchLabel);

        searchField = new JTextField(15);
        searchPanel.add(searchField);

        searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchTerm = searchField.getText();
                if (!searchTerm.isEmpty()) {
                    searchMovies(searchTerm);
                }
            }
        });
        searchPanel.add(searchButton);

        addButton = new JButton("Add"); // New button for adding movies
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addMovie();
            }
        });
        searchPanel.add(addButton); // Add the button to the search panel

        moviePanel.add(searchPanel, BorderLayout.NORTH);

        JTextArea textArea = new JTextArea(30, 60);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        moviePanel.add(scrollPane, BorderLayout.CENTER);

        updateMovieList(textArea);

        JFrame movieFrame = new JFrame("Movie Details");
        movieFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        movieFrame.add(moviePanel);
        movieFrame.pack();
        movieFrame.setLocationRelativeTo(null);
        movieFrame.setVisible(true);
    }

    private void updateMovieList(JTextArea textArea) {
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Movies")) {

            StringBuilder movies = new StringBuilder();
            movies.append("Movie ID\tTitle\tRelease Date\tGenre\tSummary\tImage URL\n");
            while (rs.next()) {
                int movieId = rs.getInt("movie_id");
                String title = rs.getString("title");
                String releaseDate = rs.getString("release_date");
                String genre = rs.getString("genre");
                String summary = rs.getString("summary");
                String imageUrl = rs.getString("image_url");

                movies.append(String.format("%d\t%s\t%s\t%s\t%s\t%s%n", movieId, title, releaseDate, genre, summary, imageUrl));
            }
            textArea.setText(movies.toString());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    private void searchMovies(String searchTerm) {
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Movies WHERE title LIKE ?")) {
            stmt.setString(1, "%" + searchTerm + "%");
            ResultSet rs = stmt.executeQuery();

            StringBuilder movieDetails = new StringBuilder();
            movieDetails.append("<html>"); // HTML formatting for multi-line display
            while (rs.next()) {
                int movieId = rs.getInt("movie_id");
                String title = rs.getString("title");
                String genre = rs.getString("genre");
                String summary = rs.getString("summary");
                String imageUrl = rs.getString("image_url");

                displayMovieDetails(movieId, title, null, genre, summary, imageUrl);
            }
            movieDetails.append("</html>");

            JFrame movieDetailsFrame = new JFrame("Movie Details");
            movieDetailsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            movieDetailsFrame.setLayout(new BorderLayout());

            JLabel movieDetailsLabel = new JLabel(movieDetails.toString());
            movieDetailsLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
            movieDetailsFrame.add(movieDetailsLabel, BorderLayout.CENTER);

            movieDetailsFrame.pack();
            movieDetailsFrame.setLocationRelativeTo(null);
            movieDetailsFrame.setVisible(true);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    private void addMovie() {
        String movieId = JOptionPane.showInputDialog(null, "Enter movie ID:");
        String title = JOptionPane.showInputDialog(null, "Enter title:");
        String releaseDateInput = JOptionPane.showInputDialog(null, "Enter release date (YYYY-MM-DD):");
        String genre = JOptionPane.showInputDialog(null, "Enter genre:");
        String summary = JOptionPane.showInputDialog(null, "Enter summary:");
        String imageUrl = JOptionPane.showInputDialog(null, "Enter image URL (optional):");

        if (movieId != null && title != null && releaseDateInput != null && genre != null && summary != null) {
            // Validate the release date format
            if (!isValidDateFormat(releaseDateInput)) {
                JOptionPane.showMessageDialog(null, "Invalid date format. Please enter date in YYYY-MM-DD format.");
                return;
            }

            try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO Movies (movie_id, title, release_date, genre, summary, image_url) VALUES (?, ?, TO_DATE(?, 'YYYY-MM-DD'), ?, ?, ?)")) {
                stmt.setString(1, movieId);
                stmt.setString(2, title);
                stmt.setString(3, releaseDateInput);
                stmt.setString(4, genre);
                stmt.setString(5, summary);
                stmt.setString(6, imageUrl);

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(null, "Movie added successfully.");
                    // Refresh the movie list
                    JTextArea textArea = (JTextArea) ((JScrollPane) moviePanel.getComponent(1)).getViewport().getView();
                    updateMovieList(textArea);
                } else {
                    JOptionPane.showMessageDialog(null, "Failed to add movie.");
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
            }
        }
    }

    private boolean isValidDateFormat(String date) {
        try {
            java.sql.Date.valueOf(date);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void displayMovieDetails(int movieId, String title, String releaseDate, String genre, String summary, String imageUrl) {
    JFrame detailsFrame = new JFrame(title);
    detailsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    JPanel detailsPanel = new JPanel(new BorderLayout());

    JLabel titleLabel = new JLabel(title);
    titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
    detailsPanel.add(titleLabel, BorderLayout.NORTH);

    JLabel imageLabel = new JLabel();
    if (imageUrl != null && !imageUrl.isEmpty()) {
        try {
            BufferedImage image = ImageIO.read(new URL(imageUrl));
            if (image != null) {
                Image scaledImage = image.getScaledInstance(300, 400, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaledImage));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Add padding to the left of the image
    imageLabel.setBorder(new EmptyBorder(0, 20, 0, 0));
    detailsPanel.add(imageLabel, BorderLayout.CENTER);

    // Create a panel for genre and summary labels
    JPanel genreSummaryPanel = new JPanel(new BorderLayout());

    JLabel genreLabel = new JLabel("Genre: " + genre);
    genreLabel.setFont(new Font("Arial", Font.PLAIN, 16));
    genreSummaryPanel.add(genreLabel, BorderLayout.NORTH);

    JLabel summaryLabel = new JLabel("<html><p style='font-size:12px'>" + summary + "</p></html>");
    summaryLabel.setBorder(new EmptyBorder(0, 0, 0, 20));
    genreSummaryPanel.add(summaryLabel, BorderLayout.CENTER);

    // Add the genreSummaryPanel to the detailsPanel
    detailsPanel.add(genreSummaryPanel, BorderLayout.SOUTH);

    // Create buttons panel
    JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton backButton = new JButton("Back");
    backButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Close the window
            Window window = SwingUtilities.getWindowAncestor(backButton);
            window.dispose();
        }
    });
    JButton deleteButton = new JButton("Delete");
    deleteButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            deleteMovie(movieId);
        }
    });
    buttonsPanel.add(backButton);
    buttonsPanel.add(deleteButton);

    // Add buttons panel to detailsPanel in the north-east corner
    detailsPanel.add(buttonsPanel, BorderLayout.NORTH);

    detailsFrame.add(detailsPanel);
    detailsFrame.pack();
    detailsFrame.setLocationRelativeTo(null);
    detailsFrame.setVisible(true);
}




    private void deleteMovie(int movieId) {
        int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete this movie?");
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM Movies WHERE movie_id = ?")) {
                stmt.setInt(1, movieId);
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(null, "Movie deleted successfully.");
                    // Refresh the movie list
                    JTextArea textArea = (JTextArea) ((JScrollPane) moviePanel.getComponent(1)).getViewport().getView();
                    updateMovieList(textArea);
                } else {
                    JOptionPane.showMessageDialog(null, "Failed to delete movie.");
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Movie();
            }
        });
    }
}
