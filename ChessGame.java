import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class ChessGame {
    private Color lightSquareColor = new Color(240, 217, 183);
    private Color darkSquareColor = new Color(180, 136, 99);
    private boolean showCoordinates = false;
    private static final String TITLE = "Chess Game";
    private JFrame frame;
    private ChessBoard board;
    private JPanel boardPanel;
    private JLabel statusLabel;
    private JButton[][] squares;
    private JLabel whiteTimerLabel;
    private JLabel blackTimerLabel;
    private Timer whiteTimer;
    private Timer blackTimer;
    private int whiteTimeRemaining = 10 * 60; // 10 minutes in seconds
    private int blackTimeRemaining = 10 * 60; // 10 minutes in seconds
    private boolean isWhiteTurn = true;
    private boolean gameActive = false;
    private JButton selectedButton = null;
    private Position selectedPosition = null;
    private List<Position> possibleMoves = new ArrayList<>();
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChessGame().initializeGui());
    }
    
    private void updateBoardColors(Color lightColor, Color darkColor) {
        this.lightSquareColor = lightColor;
        this.darkSquareColor = darkColor;
        
        boolean white = true;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (white) {
                    squares[row][col].setBackground(lightColor);
                } else {
                    squares[row][col].setBackground(darkColor);
                }
                white = !white;
            }
            white = !white;
        }
        
        // Preserve selection highlighting
        if (selectedPosition != null) {
            squares[selectedPosition.row][selectedPosition.col].setBackground(new Color(173, 216, 230));
            
            for (Position move : possibleMoves) {
                squares[move.row][move.col].setBackground(new Color(144, 238, 144));
            }
        }
    }
    
    private void toggleBoardCoordinates() {
        showCoordinates = !showCoordinates;
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                JButton square = squares[row][col];
                ChessPiece piece = board != null ? board.getPieceAt(new Position(row, col)) : null;
                
                // Reset the button text based on what's on the square
                if (piece != null) {
                    square.setText(piece.getSymbol());
                } else if (showCoordinates) {
                    char file = (char)('a' + col);
                    char rank = (char)('8' - row);
                    square.setText(String.valueOf(file) + rank);
                    square.setFont(new Font("Sans-Serif", Font.PLAIN, 10));
                    square.setForeground(Color.GRAY);
                } else {
                    square.setText("");
                }
            }
        }
        
        // Ensure piece symbols are properly displayed after toggling coordinates
        if (board != null) {
            updateBoardDisplay();
        }
    }

    private void initializeGui() {
        frame = new JFrame(TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Setup menu bar
        JMenuBar menuBar = createMenuBar();
        frame.setJMenuBar(menuBar);
        
        // Main layout
        frame.setLayout(new BorderLayout(5, 5));
        
        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Welcome to Chess! Choose game type to start.");
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        
        // Timer panel
        JPanel timerPanel = new JPanel(new GridLayout(1, 2));
        whiteTimerLabel = new JLabel("White: 10:00", JLabel.CENTER);
        blackTimerLabel = new JLabel("Black: 10:00", JLabel.CENTER);
        timerPanel.add(whiteTimerLabel);
        timerPanel.add(blackTimerLabel);
        statusPanel.add(timerPanel, BorderLayout.SOUTH);
        
        frame.add(statusPanel, BorderLayout.SOUTH);
        
        // Initialize board panel
        boardPanel = new JPanel(new GridLayout(8, 8));
        squares = new JButton[8][8];
        
        // Create chess board squares
        boolean white = true;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                JButton square = new JButton();
                square.setMargin(new Insets(0, 0, 0, 0));
                square.setPreferredSize(new Dimension(60, 60));
                
                final int finalRow = row;
                final int finalCol = col;
                square.addActionListener(e -> handleSquareClick(new Position(finalRow, finalCol)));
                
                if (white) {
                    square.setBackground(new Color(240, 217, 183));
                } else {
                    square.setBackground(new Color(180, 136, 99));
                }
                
                squares[row][col] = square;
                boardPanel.add(square);
                white = !white;
            }
            white = !white;
        }
        
        frame.add(boardPanel, BorderLayout.CENTER);
        
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");
        JMenu optionsMenu = new JMenu("Options");
        
        // Game menu items
        JMenuItem newStandardGame = new JMenuItem("New Standard Game");
        newStandardGame.addActionListener(e -> startNewGame(false));
        
        JMenuItem newChess960 = new JMenuItem("New Chess960 Game");
        newChess960.addActionListener(e -> startNewGame(true));
        
        JMenuItem saveGame = new JMenuItem("Save Game");
        saveGame.addActionListener(e -> saveGame());
        
        JMenuItem loadGame = new JMenuItem("Load Game");
        loadGame.addActionListener(e -> loadGame());
        
        JMenuItem exitGame = new JMenuItem("Exit");
        exitGame.addActionListener(e -> System.exit(0));
        
        gameMenu.add(newStandardGame);
        gameMenu.add(newChess960);
        gameMenu.addSeparator();
        gameMenu.add(saveGame);
        gameMenu.add(loadGame);
        gameMenu.addSeparator();
        gameMenu.add(exitGame);
        
        // Options menu items
        JMenuItem timerSettings = new JMenuItem("Timer Settings");
        timerSettings.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(frame, 
                "Enter time in minutes for each player:", 
                "Timer Settings", 
                JOptionPane.QUESTION_MESSAGE);
            try {
                int minutes = Integer.parseInt(input);
                if (minutes > 0) {
                    whiteTimeRemaining = minutes * 60;
                    blackTimeRemaining = minutes * 60;
                    updateTimerLabels();
                    JOptionPane.showMessageDialog(frame, 
                        "Timer set to " + minutes + " minutes per player.",
                        "Timer Updated", 
                        JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame,
                    "Please enter a valid number of minutes.",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JMenu boardColorMenu = new JMenu("Board Colors");
        
        JMenuItem defaultColors = new JMenuItem("Default");
        defaultColors.addActionListener(e -> {
            updateBoardColors(new Color(240, 217, 183), new Color(180, 136, 99));
        });
        
        JMenuItem blueTheme = new JMenuItem("Blue Theme");
        blueTheme.addActionListener(e -> {
            updateBoardColors(new Color(220, 230, 245), new Color(75, 115, 153));
        });
        
        JMenuItem greenTheme = new JMenuItem("Green Theme");
        greenTheme.addActionListener(e -> {
            updateBoardColors(new Color(235, 240, 208), new Color(118, 150, 86));
        });
        
        boardColorMenu.add(defaultColors);
        boardColorMenu.add(blueTheme);
        boardColorMenu.add(greenTheme);
        
        JMenuItem toggleCoordinates = new JMenuItem("Show Coordinates");
        toggleCoordinates.addActionListener(e -> toggleBoardCoordinates());
        
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> {
            JOptionPane.showMessageDialog(frame,
                "Chess Game\nVersion 1.0\n\nSupports standard chess and Chess960 variants.",
                "About Chess Game",
                JOptionPane.INFORMATION_MESSAGE);
        });
        
        optionsMenu.add(timerSettings);
        optionsMenu.add(boardColorMenu);
        optionsMenu.add(toggleCoordinates);
        optionsMenu.addSeparator();
        optionsMenu.add(about);
        
        menuBar.add(gameMenu);
        menuBar.add(optionsMenu);
        
        return menuBar;
    }
    
    private void startNewGame(boolean isChess960) {
        stopTimers();
        board = new ChessBoard(isChess960);
        updateBoardDisplay();
        
        whiteTimeRemaining = 10 * 60;
        blackTimeRemaining = 10 * 60;
        updateTimerLabels();
        
        isWhiteTurn = true;
        gameActive = true;
        statusLabel.setText("White's turn");
        
        startTimer();
    }
    
private void updateBoardDisplay() {
    for (int row = 0; row < 8; row++) {
        for (int col = 0; col < 8; col++) {
            ChessPiece piece = board.getPieceAt(new Position(row, col));
            JButton square = squares[row][col];
            
            // Reset background color
            boolean isLightSquare = (row + col) % 2 == 0;
            square.setBackground(isLightSquare ? lightSquareColor : darkSquareColor);
            
            if (piece != null) {
                square.setText(piece.getSymbol());
                square.setForeground(piece.getColor() == ChessPiece.Color.WHITE ? Color.WHITE : Color.BLACK);
                Font font = new Font("Serif", Font.BOLD, 40);
                square.setFont(font);
            } else if (showCoordinates) {
                char file = (char)('a' + col);
                char rank = (char)('8' - row);
                square.setText(String.valueOf(file) + rank);
                square.setFont(new Font("Sans-Serif", Font.PLAIN, 10));
                square.setForeground(Color.GRAY);
            } else {
                square.setText("");
            }
        }
    }
    
    // Highlight selected square and possible moves
    if (selectedPosition != null) {
        squares[selectedPosition.row][selectedPosition.col].setBackground(new Color(173, 216, 230)); // Light blue
        
        for (Position move : possibleMoves) {
            squares[move.row][move.col].setBackground(new Color(144, 238, 144)); // Light green
        }
    }

        
        // Highlight selected square and possible moves
        if (selectedPosition != null) {
            squares[selectedPosition.row][selectedPosition.col].setBackground(new Color(173, 216, 230)); // Light blue
            
            for (Position move : possibleMoves) {
                squares[move.row][move.col].setBackground(new Color(144, 238, 144)); // Light green
            }
        }
    }
    
    private void handleSquareClick(Position position) {
        if (!gameActive) return;
        
        ChessPiece clickedPiece = board.getPieceAt(position);
        
        // If no piece is selected yet
        if (selectedPosition == null) {
            if (clickedPiece != null && clickedPiece.getColor() == (isWhiteTurn ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK)) {
                selectedPosition = position;
                possibleMoves = board.getLegalMoves(position);
                updateBoardDisplay();
            }
        } 
        // If a piece is already selected
        else {
            // If the clicked position is in possible moves
            if (possibleMoves.contains(position)) {
                // Execute the move
                boolean moveResult = board.movePiece(selectedPosition, position);
                if (moveResult) {
                    // Check for special moves like promotion, castling, etc.
                    handleSpecialMoves(selectedPosition, position);
                    
                    // Switch turn
                    isWhiteTurn = !isWhiteTurn;
                    statusLabel.setText((isWhiteTurn ? "White" : "Black") + "'s turn");
                    
                    // Reset selected position and possible moves
                    selectedPosition = null;
                    possibleMoves.clear();
                    
                    updateBoardDisplay();
                    
                    // Check for game ending conditions
                    checkGameEndingConditions();
                    
                    // Switch timer
                    switchTimer();
                }
            } else {
                // Deselect current piece if clicking on empty square or opponent's piece
                selectedPosition = null;
                possibleMoves.clear();
                
                // If clicking on own piece, select it
                if (clickedPiece != null && clickedPiece.getColor() == (isWhiteTurn ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK)) {
                    selectedPosition = position;
                    possibleMoves = board.getLegalMoves(position);
                }
                
                updateBoardDisplay();
            }
        }
    }
    
    private void handleSpecialMoves(Position from, Position to) {
        ChessPiece movedPiece = board.getPieceAt(to);
        
        // Handle pawn promotion
        if (movedPiece instanceof Pawn) {
            if ((movedPiece.getColor() == ChessPiece.Color.WHITE && to.row == 0) || 
                (movedPiece.getColor() == ChessPiece.Color.BLACK && to.row == 7)) {
                promotePawn(to);
            }
        }
        
        // Other special moves like castling and en passant are handled in the ChessBoard class
    }
    
    private void promotePawn(Position position) {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        int choice = JOptionPane.showOptionDialog(frame, "Choose promotion piece:", "Pawn Promotion",
                                                  JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        
        ChessPiece.Color color = board.getPieceAt(position).getColor();
        ChessPiece newPiece = null;
        
        switch (choice) {
            case 0: // Queen
                newPiece = new Queen(color);
                break;
            case 1: // Rook
                newPiece = new Rook(color);
                break;
            case 2: // Bishop
                newPiece = new Bishop(color);
                break;
            case 3: // Knight
                newPiece = new Knight(color);
                break;
            default: // Default to Queen if somehow no choice is made
                newPiece = new Queen(color);
        }
        
        board.setPieceAt(position, newPiece);
    }
    
    private void checkGameEndingConditions() {
        // Check for checkmate or stalemate
        boolean whiteHasMove = board.playerHasLegalMoves(ChessPiece.Color.WHITE);
        boolean blackHasMove = board.playerHasLegalMoves(ChessPiece.Color.BLACK);
        boolean whiteInCheck = board.isKingInCheck(ChessPiece.Color.WHITE);
        boolean blackInCheck = board.isKingInCheck(ChessPiece.Color.BLACK);
        
        if (isWhiteTurn && !whiteHasMove) {
            gameActive = false;
            if (whiteInCheck) {
                statusLabel.setText("Checkmate! Black wins.");
            } else {
                statusLabel.setText("Stalemate! The game is a draw.");
            }
            stopTimers();
        } else if (!isWhiteTurn && !blackHasMove) {
            gameActive = false;
            if (blackInCheck) {
                statusLabel.setText("Checkmate! White wins.");
            } else {
                statusLabel.setText("Stalemate! The game is a draw.");
            }
            stopTimers();
        } else if (whiteInCheck) {
            statusLabel.setText("White is in check!");
        } else if (blackInCheck) {
            statusLabel.setText("Black is in check!");
        }
    }
    
    private void startTimer() {
        if (whiteTimer != null) whiteTimer.cancel();
        if (blackTimer != null) blackTimer.cancel();
        
        whiteTimer = new Timer();
        blackTimer = new Timer();
        
        TimerTask whiteTask = new TimerTask() {
            @Override
            public void run() {
                if (isWhiteTurn && gameActive) {
                    whiteTimeRemaining--;
                    updateTimerLabels();
                    
                    if (whiteTimeRemaining <= 0) {
                        SwingUtilities.invokeLater(() -> handleTimeout(true));
                    }
                }
            }
        };
        
        TimerTask blackTask = new TimerTask() {
            @Override
            public void run() {
                if (!isWhiteTurn && gameActive) {
                    blackTimeRemaining--;
                    updateTimerLabels();
                    
                    if (blackTimeRemaining <= 0) {
                        SwingUtilities.invokeLater(() -> handleTimeout(false));
                    }
                }
            }
        };
        
        whiteTimer.scheduleAtFixedRate(whiteTask, 1000, 1000);
        blackTimer.scheduleAtFixedRate(blackTask, 1000, 1000);
    }
    
    private void updateTimerLabels() {
        whiteTimerLabel.setText("White: " + formatTime(whiteTimeRemaining));
        blackTimerLabel.setText("Black: " + formatTime(blackTimeRemaining));
    }
    
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }
    
    private void switchTimer() {
        // Timer switching is handled automatically in the timer tasks
    }
    
    private void stopTimers() {
        if (whiteTimer != null) whiteTimer.cancel();
        if (blackTimer != null) blackTimer.cancel();
    }
    
    private void handleTimeout(boolean isWhiteTimeout) {
        gameActive = false;
        if (isWhiteTimeout) {
            statusLabel.setText("White's time has expired. Black wins!");
        } else {
            statusLabel.setText("Black's time has expired. White wins!");
        }
    }
    
    private void saveGame() {
        if (!gameActive) {
            JOptionPane.showMessageDialog(frame, "No active game to save.", "Save Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(frame);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                GameState state = new GameState(
                    board,
                    isWhiteTurn,
                    whiteTimeRemaining,
                    blackTimeRemaining
                );
                oos.writeObject(state);
                statusLabel.setText("Game saved successfully!");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Error saving game: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void loadGame() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                GameState state = (GameState) ois.readObject();
                
                // Load game state
                board = state.board;
                isWhiteTurn = state.isWhiteTurn;
                whiteTimeRemaining = state.whiteTimeRemaining;
                blackTimeRemaining = state.blackTimeRemaining;
                
                // Reset UI state
                selectedPosition = null;
                possibleMoves.clear();
                gameActive = true;
                
                // Update display
                updateBoardDisplay();
                updateTimerLabels();
                statusLabel.setText((isWhiteTurn ? "White" : "Black") + "'s turn");
                
                // Restart timers
                startTimer();
                
            } catch (IOException | ClassNotFoundException e) {
                JOptionPane.showMessageDialog(frame, "Error loading game: " + e.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    // Position class to represent row and column coordinates
    public static class Position implements Serializable {
        private static final long serialVersionUID = 1L;
        
        final int row;
        final int col;
        
        public Position(int row, int col) {
            this.row = row;
            this.col = col;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Position position = (Position) obj;
            return row == position.row && col == position.col;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(row, col);
        }
        
        @Override
        public String toString() {
            return String.format("(%d, %d)", row, col);
        }
    }
    
    // GameState class for save/load functionality
    public static class GameState implements Serializable {
        private static final long serialVersionUID = 1L;
        
        final ChessBoard board;
        final boolean isWhiteTurn;
        final int whiteTimeRemaining;
        final int blackTimeRemaining;
        
        public GameState(ChessBoard board, boolean isWhiteTurn, int whiteTimeRemaining, int blackTimeRemaining) {
            this.board = board;
            this.isWhiteTurn = isWhiteTurn;
            this.whiteTimeRemaining = whiteTimeRemaining;
            this.blackTimeRemaining = blackTimeRemaining;
        }
    }
}

// ChessBoard class to handle the board state and rules
class ChessBoard implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private ChessPiece[][] board;
    private boolean isChess960;
    private boolean whiteCanCastleKingside = true;
    private boolean whiteCanCastleQueenside = true;
    private boolean blackCanCastleKingside = true;
    private boolean blackCanCastleQueenside = true;
    private ChessGame.Position enPassantTarget = null;
    private List<MoveRecord> moveHistory = new ArrayList<>();
    
    public ChessBoard(boolean isChess960) {
        this.isChess960 = isChess960;
        initializeBoard();
    }
    
    private void initializeBoard() {
        board = new ChessPiece[8][8];
        
        if (isChess960) {
            setupChess960();
        } else {
            setupStandardChess();
        }
    }
    
    private void setupStandardChess() {
        // Setup rooks
        board[0][0] = new Rook(ChessPiece.Color.BLACK);
        board[0][7] = new Rook(ChessPiece.Color.BLACK);
        board[7][0] = new Rook(ChessPiece.Color.WHITE);
        board[7][7] = new Rook(ChessPiece.Color.WHITE);
        
        // Setup knights
        board[0][1] = new Knight(ChessPiece.Color.BLACK);
        board[0][6] = new Knight(ChessPiece.Color.BLACK);
        board[7][1] = new Knight(ChessPiece.Color.WHITE);
        board[7][6] = new Knight(ChessPiece.Color.WHITE);
        
        // Setup bishops
        board[0][2] = new Bishop(ChessPiece.Color.BLACK);
        board[0][5] = new Bishop(ChessPiece.Color.BLACK);
        board[7][2] = new Bishop(ChessPiece.Color.WHITE);
        board[7][5] = new Bishop(ChessPiece.Color.WHITE);
        
        // Setup queens
        board[0][3] = new Queen(ChessPiece.Color.BLACK);
        board[7][3] = new Queen(ChessPiece.Color.WHITE);
        
        // Setup kings
        board[0][4] = new King(ChessPiece.Color.BLACK);
        board[7][4] = new King(ChessPiece.Color.WHITE);
        
        // Setup pawns
        for (int col = 0; col < 8; col++) {
            board[1][col] = new Pawn(ChessPiece.Color.BLACK);
            board[6][col] = new Pawn(ChessPiece.Color.WHITE);
        }
    }
    
    private void setupChess960() {
        Random random = new Random();
        
        // 1. Place bishops on opposite colored squares
        int bishop1Col = random.nextInt(4) * 2; // Even squares (0, 2, 4, 6)
        int bishop2Col = random.nextInt(4) * 2 + 1; // Odd squares (1, 3, 5, 7)
        
        board[0][bishop1Col] = new Bishop(ChessPiece.Color.BLACK);
        board[0][bishop2Col] = new Bishop(ChessPiece.Color.BLACK);
        board[7][bishop1Col] = new Bishop(ChessPiece.Color.WHITE);
        board[7][bishop2Col] = new Bishop(ChessPiece.Color.WHITE);
        
        // 2. Place queen in a random empty square
        int queenCol;
        do {
            queenCol = random.nextInt(8);
        } while (board[0][queenCol] != null);
        
        board[0][queenCol] = new Queen(ChessPiece.Color.BLACK);
        board[7][queenCol] = new Queen(ChessPiece.Color.WHITE);
        
        // 3. Place knights in random empty squares
        int knight1Col;
        do {
            knight1Col = random.nextInt(8);
        } while (board[0][knight1Col] != null);
        
        board[0][knight1Col] = new Knight(ChessPiece.Color.BLACK);
        board[7][knight1Col] = new Knight(ChessPiece.Color.WHITE);
        
        int knight2Col;
        do {
            knight2Col = random.nextInt(8);
        } while (board[0][knight2Col] != null);
        
        board[0][knight2Col] = new Knight(ChessPiece.Color.BLACK);
        board[7][knight2Col] = new Knight(ChessPiece.Color.WHITE);
        
        // 4. Place rooks and king (king must be between rooks)
        // Find the three remaining empty columns
        List<Integer> emptyCols = new ArrayList<>();
        for (int col = 0; col < 8; col++) {
            if (board[0][col] == null) {
                emptyCols.add(col);
            }
        }
        
        // Place rooks in the first and last empty columns
        int rook1Col = emptyCols.get(0);
        int rook2Col = emptyCols.get(2);
        int kingCol = emptyCols.get(1);
        
        board[0][rook1Col] = new Rook(ChessPiece.Color.BLACK);
        board[0][rook2Col] = new Rook(ChessPiece.Color.BLACK);
        board[0][kingCol] = new King(ChessPiece.Color.BLACK);
        
        board[7][rook1Col] = new Rook(ChessPiece.Color.WHITE);
        board[7][rook2Col] = new Rook(ChessPiece.Color.WHITE);
        board[7][kingCol] = new King(ChessPiece.Color.WHITE);
        
        // 5. Setup pawns
        for (int col = 0; col < 8; col++) {
            board[1][col] = new Pawn(ChessPiece.Color.BLACK);
            board[6][col] = new Pawn(ChessPiece.Color.WHITE);
        }
    }
    
    public ChessPiece getPieceAt(ChessGame.Position position) {
        return board[position.row][position.col];
    }
    
    public void setPieceAt(ChessGame.Position position, ChessPiece piece) {
        board[position.row][position.col] = piece;
    }
    
    public List<ChessGame.Position> getLegalMoves(ChessGame.Position position) {
        ChessPiece piece = getPieceAt(position);
        if (piece == null) return new ArrayList<>();
        
        List<ChessGame.Position> possibleMoves = new ArrayList<>();
        
        // Get all possible moves based on piece type
        List<ChessGame.Position> candidateMoves = piece.getPossibleMoves(position, this);
        
        // Filter moves that would put the king in check
        for (ChessGame.Position move : candidateMoves) {
            if (isLegalMove(position, move)) {
                possibleMoves.add(move);
            }
        }
        
        // Add special moves
        if (piece instanceof King) {
            addCastlingMoves(position, possibleMoves);
        } else if (piece instanceof Pawn) {
            addEnPassantMoves(position, possibleMoves);
        }
        
        return possibleMoves;
    }
    
    private void addCastlingMoves(ChessGame.Position kingPosition, List<ChessGame.Position> moves) {
        ChessPiece king = getPieceAt(kingPosition);
        if (!(king instanceof King)) return;
        
        ChessPiece.Color color = king.getColor();
        int row = (color == ChessPiece.Color.WHITE) ? 7 : 0;
        
        // Check if king is in check
        if (isKingInCheck(color)) return;
        
        // Handle Chess960 castling differently
        if (isChess960) {
            // Find king and rook positions
            int kingCol = -1;
            List<Integer> rookCols = new ArrayList<>();
            
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board[row][col];
                if (piece instanceof King && piece.getColor() == color) {
                    kingCol = col;
                } else if (piece instanceof Rook && piece.getColor() == color) {
                    rookCols.add(col);
                }
            }
            
            if (kingCol == -1 || rookCols.size() != 2) return;
            
            int leftRookCol = rookCols.get(0);
            int rightRookCol = rookCols.get(1);
            
            // Check queenside castling
            if ((color == ChessPiece.Color.WHITE && whiteCanCastleQueenside) || 
                (color == ChessPiece.Color.BLACK && blackCanCastleQueenside)) {
                boolean pathClear = true;
                
                // Check if path between king and left rook is clear
                int start = Math.min(kingCol, leftRookCol) + 1;
                int end = Math.max(kingCol, leftRookCol);
                for (int col = start; col < end; col++) {
                    if (board[row][col] != null) {
                        pathClear = false;
                        break;
                    }
                }
                
                // Check that squares king moves through are not under attack
                for (int col = Math.min(2, kingCol); col <= Math.max(2, kingCol); col++) {
                    if (isSquareAttacked(new ChessGame.Position(row, col), color)) {
                        pathClear = false;
                        break;
                    }
                }
                
                if (pathClear) {
                    moves.add(new ChessGame.Position(row, 2)); // King's destination for queenside castling
                }
            }
            
            // Check kingside castling
            if ((color == ChessPiece.Color.WHITE && whiteCanCastleKingside) || 
                (color == ChessPiece.Color.BLACK && blackCanCastleKingside)) {
                boolean pathClear = true;
                
                // Check if path between king and right rook is clear
                int start = Math.min(kingCol, rightRookCol) + 1;
                int end = Math.max(kingCol, rightRookCol);
                for (int col = start; col < end; col++) {
                    if (board[row][col] != null) {
                        pathClear = false;
                        break;
                    }
                }
                
                // Check that squares king moves through are not under attack
                for (int col = Math.min(6, kingCol); col <= Math.max(6, kingCol); col++) {
                    if (isSquareAttacked(new ChessGame.Position(row, col), color)) {
                        pathClear = false;
                        break;
                    }
                }
                
                if (pathClear) {
                    moves.add(new ChessGame.Position(row, 6)); // King's destination for kingside castling
                }
            }
        } else {
            // Standard chess castling
            // Check kingside castling
            if ((color == ChessPiece.Color.WHITE && whiteCanCastleKingside) || 
                (color == ChessPiece.Color.BLACK && blackCanCastleKingside)) {
                boolean pathClear = true;
                
                // Check if squares between king and rook are empty
                for (int col = 5; col < 7; col++) {
                    if (board[row][col] != null) {
                        pathClear = false;
                        break;
                    }
                }
                
                // Check if squares king moves through are not under attack
                for (int col = 4; col <= 6; col++) {
                    if (isSquareAttacked(new ChessGame.Position(row, col), color)) {
                        pathClear = false;
                        break;
                    }
                }
                
                if (pathClear) {
                    moves.add(new ChessGame.Position(row, 6));
                }
            }
            
            // Check queenside castling
            if ((color == ChessPiece.Color.WHITE && whiteCanCastleQueenside) || 
                (color == ChessPiece.Color.BLACK && blackCanCastleQueenside)) {
                boolean pathClear = true;
                
                // Check if squares between king and rook are empty
                for (int col = 1; col < 4; col++) {
                    if (board[row][col] != null) {
                        pathClear = false;
                        break;
                    }
                }
                
                // Check if squares king moves through are not under attack
                for (int col = 2; col <= 4; col++) {
                    if (isSquareAttacked(new ChessGame.Position(row, col), color)) {
                        pathClear = false;
                        break;
                    }
                }
                if (pathClear) {
                    moves.add(new ChessGame.Position(row, 2));
                }
            }
        }
    }
    
    private void addEnPassantMoves(ChessGame.Position pawnPosition, List<ChessGame.Position> moves) {
        if (enPassantTarget == null) return;
        
        ChessPiece pawn = getPieceAt(pawnPosition);
        if (!(pawn instanceof Pawn)) return;
        
        int row = pawnPosition.row;
        int col = pawnPosition.col;
        ChessPiece.Color color = pawn.getColor();
        
        // Check if the pawn can capture en passant
        if ((color == ChessPiece.Color.WHITE && row == 3) || (color == ChessPiece.Color.BLACK && row == 4)) {
            // Check if the pawn is adjacent to the en passant target column
            if ((col == enPassantTarget.col - 1 || col == enPassantTarget.col + 1) && 
                row == (color == ChessPiece.Color.WHITE ? 3 : 4)) {
                ChessGame.Position capturePosition = new ChessGame.Position(color == ChessPiece.Color.WHITE ? 2 : 5, enPassantTarget.col);
                if (isLegalMove(pawnPosition, capturePosition)) {
                    moves.add(capturePosition);
                }
            }
        }
    }
    
    public boolean isLegalMove(ChessGame.Position from, ChessGame.Position to) {
        ChessPiece piece = getPieceAt(from);
        if (piece == null) return false;
        
        // Make a hypothetical move and see if king would be in check
        ChessPiece capturedPiece = getPieceAt(to);
        ChessPiece.Color pieceColor = piece.getColor();
        
        // Temporarily make the move
        board[to.row][to.col] = piece;
        board[from.row][from.col] = null;
        
        // Check if king is in check after the move
        boolean kingInCheck = isKingInCheck(pieceColor);
        
        // Restore the board
        board[from.row][from.col] = piece;
        board[to.row][to.col] = capturedPiece;
        
        return !kingInCheck;
    }
    
    public boolean isKingInCheck(ChessPiece.Color kingColor) {
        // Find the king
        ChessGame.Position kingPosition = null;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board[row][col];
                if (piece instanceof King && piece.getColor() == kingColor) {
                    kingPosition = new ChessGame.Position(row, col);
                    break;
                }
            }
            if (kingPosition != null) break;
        }
        
        if (kingPosition == null) return false; // King not found (shouldn't happen in a valid game)
        
        // Check if any enemy piece can attack the king
        return isSquareAttacked(kingPosition, kingColor);
    }
    
    public boolean isSquareAttacked(ChessGame.Position square, ChessPiece.Color pieceColor) {
        ChessPiece.Color opponentColor = (pieceColor == ChessPiece.Color.WHITE) ? 
                                          ChessPiece.Color.BLACK : ChessPiece.Color.WHITE;
        
        // Check attacks from all directions
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessGame.Position from = new ChessGame.Position(row, col);
                ChessPiece piece = getPieceAt(from);
                
                if (piece != null && piece.getColor() == opponentColor) {
                    // Get raw moves (without checking if they'd put king in check)
                    List<ChessGame.Position> rawMoves = piece.getPossibleMoves(from, this);
                    
                    if (rawMoves.contains(square)) {
                        return true;
                    }
                    
                    // Special case for en passant
                    if (piece instanceof Pawn && enPassantTarget != null) {
                        if ((pieceColor == ChessPiece.Color.WHITE && square.row == 2) || 
                            (pieceColor == ChessPiece.Color.BLACK && square.row == 5)) {
                            if (square.col == enPassantTarget.col && 
                                (from.col == square.col - 1 || from.col == square.col + 1) &&
                                from.row == (pieceColor == ChessPiece.Color.WHITE ? 3 : 4)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    public boolean movePiece(ChessGame.Position from, ChessGame.Position to) {
        ChessPiece piece = getPieceAt(from);
        if (piece == null) return false;
        
        List<ChessGame.Position> legalMoves = getLegalMoves(from);
        if (!legalMoves.contains(to)) return false;
        
        // Handle castling
        if (piece instanceof King && Math.abs(from.col - to.col) > 1) {
            handleCastling(from, to);
            return true;
        }
        
        // Handle en passant capture
        if (piece instanceof Pawn && to.col != from.col && getPieceAt(to) == null) {
            // This is a diagonal move to an empty square, must be en passant
            ChessGame.Position capturedPawnPos = new ChessGame.Position(from.row, to.col);
            board[capturedPawnPos.row][capturedPawnPos.col] = null;
        }
        
        // Update en passant target
        if (piece instanceof Pawn && Math.abs(from.row - to.row) == 2) {
            // Pawn moved two squares, set en passant target
            enPassantTarget = new ChessGame.Position((from.row + to.row) / 2, to.col);
        } else {
            enPassantTarget = null;
        }
        
        // Update castling rights
        updateCastlingRights(from, to, piece);
        
        // Record the move
        moveHistory.add(new MoveRecord(from, to, piece, getPieceAt(to)));
        
        // Make the move
        board[to.row][to.col] = piece;
        board[from.row][from.col] = null;
        
        return true;
    }
    
    private void handleCastling(ChessGame.Position from, ChessGame.Position to) {
        int row = from.row;
        ChessPiece king = getPieceAt(from);
        
        // Determine rook positions based on standard chess or Chess960
        int rookFromCol, rookToCol;
        
        if (isChess960) {
            // In Chess960, find the rook on the appropriate side
            if (to.col == 6) { // Kingside castling
                rookFromCol = -1;
                for (int col = 7; col > from.col; col--) {
                    ChessPiece piece = board[row][col];
                    if (piece instanceof Rook && piece.getColor() == king.getColor()) {
                        rookFromCol = col;
                        break;
                    }
                }
                rookToCol = 5;
            } else { // Queenside castling
                rookFromCol = -1;
                for (int col = 0; col < from.col; col++) {
                    ChessPiece piece = board[row][col];
                    if (piece instanceof Rook && piece.getColor() == king.getColor()) {
                        rookFromCol = col;
                        break;
                    }
                }
                rookToCol = 3;
            }
        } else {
            // Standard chess
            if (to.col == 6) { // Kingside castling
                rookFromCol = 7;
                rookToCol = 5;
            } else { // Queenside castling
                rookFromCol = 0;
                rookToCol = 3;
            }
        }
        
        // Move the king
        board[to.row][to.col] = king;
        board[from.row][from.col] = null;
        
        // Move the rook
        ChessPiece rook = board[row][rookFromCol];
        board[row][rookToCol] = rook;
        board[row][rookFromCol] = null;
        
        // Update castling rights
        if (king.getColor() == ChessPiece.Color.WHITE) {
            whiteCanCastleKingside = false;
            whiteCanCastleQueenside = false;
        } else {
            blackCanCastleKingside = false;
            blackCanCastleQueenside = false;
        }
    }
    
    private void updateCastlingRights(ChessGame.Position from, ChessGame.Position to, ChessPiece piece) {
        // King moved
        if (piece instanceof King) {
            if (piece.getColor() == ChessPiece.Color.WHITE) {
                whiteCanCastleKingside = false;
                whiteCanCastleQueenside = false;
            } else {
                blackCanCastleKingside = false;
                blackCanCastleQueenside = false;
            }
            return;
        }
        
        // Rook moved or captured
        if (piece instanceof Rook) {
            if (piece.getColor() == ChessPiece.Color.WHITE) {
                if (from.row == 7) {
                    if (from.col == 0) {
                        whiteCanCastleQueenside = false;
                    } else if (from.col == (isChess960 ? findRookCol(ChessPiece.Color.WHITE, true) : 7)) {
                        whiteCanCastleKingside = false;
                    }
                }
            } else {
                if (from.row == 0) {
                    if (from.col == 0) {
                        blackCanCastleQueenside = false;
                    } else if (from.col == (isChess960 ? findRookCol(ChessPiece.Color.BLACK, true) : 7)) {
                        blackCanCastleKingside = false;
                    }
                }
            }
        }
        
        // Rook captured
        ChessPiece capturedPiece = getPieceAt(to);
        if (capturedPiece instanceof Rook) {
            if (capturedPiece.getColor() == ChessPiece.Color.WHITE) {
                if (to.row == 7) {
                    if (to.col == 0) {
                        whiteCanCastleQueenside = false;
                    } else if (to.col == (isChess960 ? findRookCol(ChessPiece.Color.WHITE, true) : 7)) {
                        whiteCanCastleKingside = false;
                    }
                }
            } else {
                if (to.row == 0) {
                    if (to.col == 0) {
                        blackCanCastleQueenside = false;
                    } else if (to.col == (isChess960 ? findRookCol(ChessPiece.Color.BLACK, true) : 7)) {
                        blackCanCastleKingside = false;
                    }
                }
            }
        }
    }
    
    private int findRookCol(ChessPiece.Color color, boolean kingside) {
        int row = (color == ChessPiece.Color.WHITE) ? 7 : 0;
        
        // Find king first
        int kingCol = -1;
        for (int col = 0; col < 8; col++) {
            ChessPiece piece = board[row][col];
            if (piece instanceof King && piece.getColor() == color) {
                kingCol = col;
                break;
            }
        }
        
        if (kingCol == -1) return -1;
        
        // Find the appropriate rook
        if (kingside) {
            for (int col = 7; col > kingCol; col--) {
                ChessPiece piece = board[row][col];
                if (piece instanceof Rook && piece.getColor() == color) {
                    return col;
                }
            }
        } else {
            for (int col = 0; col < kingCol; col++) {
                ChessPiece piece = board[row][col];
                if (piece instanceof Rook && piece.getColor() == color) {
                    return col;
                }
            }
        }
        
        return -1;
    }
    
    public boolean playerHasLegalMoves(ChessPiece.Color color) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = board[row][col];
                if (piece != null && piece.getColor() == color) {
                    List<ChessGame.Position> moves = getLegalMoves(new ChessGame.Position(row, col));
                    if (!moves.isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    // Record of moves for tracking game history
    private static class MoveRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        
        final ChessGame.Position from;
        final ChessGame.Position to;
        final ChessPiece piece;
        final ChessPiece capturedPiece;
        
        public MoveRecord(ChessGame.Position from, ChessGame.Position to, ChessPiece piece, ChessPiece capturedPiece) {
            this.from = from;
            this.to = to;
            this.piece = piece;
            this.capturedPiece = capturedPiece;
        }
    }
}

// Abstract base class for chess pieces
abstract class ChessPiece implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum Color { WHITE, BLACK }
    
    private final Color color;
    
    public ChessPiece(Color color) {
        this.color = color;
    }
    
    public Color getColor() {
        return color;
    }
    
    public abstract String getSymbol();
    
    public abstract List<ChessGame.Position> getPossibleMoves(ChessGame.Position position, ChessBoard board);
    
    // Helper method to check if a position is valid on the board
    protected boolean isValidPosition(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }
    
    // Helper method for sliding pieces (Queen, Rook, Bishop)
    protected List<ChessGame.Position> getSlidingMoves(ChessGame.Position position, ChessBoard board, int[][] directions) {
        List<ChessGame.Position> moves = new ArrayList<>();
        int row = position.row;
        int col = position.col;
        
        for (int[] direction : directions) {
            int dr = direction[0];
            int dc = direction[1];
            
            int r = row + dr;
            int c = col + dc;
            
            while (isValidPosition(r, c)) {
                ChessPiece pieceAtTarget = board.getPieceAt(new ChessGame.Position(r, c));
                
                if (pieceAtTarget == null) {
                    // Empty square, can move here
                    moves.add(new ChessGame.Position(r, c));
                } else {
                    // Square is occupied
                    if (pieceAtTarget.getColor() != this.color) {
                        // Can capture opponent's piece
                        moves.add(new ChessGame.Position(r, c));
                    }
                    break; // Can't move further in this direction
                }
                
                r += dr;
                c += dc;
            }
        }
        
        return moves;
    }
}

// King piece
class King extends ChessPiece {
    private static final long serialVersionUID = 1L;
    
    public King(Color color) {
        super(color);
    }
    
    @Override
    public String getSymbol() {
        return getColor() == Color.WHITE ? "♔" : "♚";
    }
    
    @Override
    public List<ChessGame.Position> getPossibleMoves(ChessGame.Position position, ChessBoard board) {
        List<ChessGame.Position> moves = new ArrayList<>();
        int row = position.row;
        int col = position.col;
        
        // King can move one square in any direction
        int[][] directions = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
        };
        
        for (int[] direction : directions) {
            int newRow = row + direction[0];
            int newCol = col + direction[1];
            
            if (isValidPosition(newRow, newCol)) {
                ChessPiece pieceAtTarget = board.getPieceAt(new ChessGame.Position(newRow, newCol));
                
                if (pieceAtTarget == null || pieceAtTarget.getColor() != this.getColor()) {
                    moves.add(new ChessGame.Position(newRow, newCol));
                }
            }
        }
        
        // Castling moves are handled separately in the ChessBoard class
        
        return moves;
    }
}

// Queen piece
class Queen extends ChessPiece {
    private static final long serialVersionUID = 1L;
    
    public Queen(Color color) {
        super(color);
    }
    
    @Override
    public String getSymbol() {
        return getColor() == Color.WHITE ? "♕" : "♛";
    }
    
    @Override
    public List<ChessGame.Position> getPossibleMoves(ChessGame.Position position, ChessBoard board) {
        // Queen can move like a rook or bishop combined
        int[][] directions = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
        };
        
        return getSlidingMoves(position, board, directions);
    }
}

// Rook piece
class Rook extends ChessPiece {
    private static final long serialVersionUID = 1L;
    
    public Rook(Color color) {
        super(color);
    }
    
    @Override
    public String getSymbol() {
        return getColor() == Color.WHITE ? "♖" : "♜";
    }
    
    @Override
    public List<ChessGame.Position> getPossibleMoves(ChessGame.Position position, ChessBoard board) {
        // Rook can move horizontally or vertically
        int[][] directions = {
            {-1, 0},
            {0, -1}, {0, 1},
            {1, 0}
        };
        
        return getSlidingMoves(position, board, directions);
    }
}

// Bishop piece
class Bishop extends ChessPiece {
    private static final long serialVersionUID = 1L;
    
    public Bishop(Color color) {
        super(color);
    }
    
    @Override
    public String getSymbol() {
        return getColor() == Color.WHITE ? "♗" : "♝";
    }
    
    @Override
    public List<ChessGame.Position> getPossibleMoves(ChessGame.Position position, ChessBoard board) {
        // Bishop can move diagonally
        int[][] directions = {
            {-1, -1}, {-1, 1},
            {1, -1},  {1, 1}
        };
        
        return getSlidingMoves(position, board, directions);
    }
}

// Knight piece
class Knight extends ChessPiece {
    private static final long serialVersionUID = 1L;
    
    public Knight(Color color) {
        super(color);
    }
    
    @Override
    public String getSymbol() {
        return getColor() == Color.WHITE ? "♘" : "♞";
    }
    
    @Override
    public List<ChessGame.Position> getPossibleMoves(ChessGame.Position position, ChessBoard board) {
        List<ChessGame.Position> moves = new ArrayList<>();
        int row = position.row;
        int col = position.col;
        
        // Knight moves in L-shape
        int[][] knightMoves = {
            {-2, -1}, {-2, 1},
            {-1, -2}, {-1, 2},
            {1, -2},  {1, 2},
            {2, -1},  {2, 1}
        };
        
        for (int[] move : knightMoves) {
            int newRow = row + move[0];
            int newCol = col + move[1];
            
            if (isValidPosition(newRow, newCol)) {
                ChessPiece pieceAtTarget = board.getPieceAt(new ChessGame.Position(newRow, newCol));
                
                if (pieceAtTarget == null || pieceAtTarget.getColor() != this.getColor()) {
                    moves.add(new ChessGame.Position(newRow, newCol));
                }
            }
        }
        
        return moves;
    }
}

// Pawn piece
class Pawn extends ChessPiece {
    private static final long serialVersionUID = 1L;
    
    public Pawn(Color color) {
        super(color);
    }
    
    @Override
    public String getSymbol() {
        return getColor() == Color.WHITE ? "♙" : "♟";
    }
    
    @Override
    public List<ChessGame.Position> getPossibleMoves(ChessGame.Position position, ChessBoard board) {
        List<ChessGame.Position> moves = new ArrayList<>();
        int row = position.row;
        int col = position.col;
        
        // Pawns move differently based on color
        int direction = (getColor() == Color.WHITE) ? -1 : 1;
        
        // Forward move
        int newRow = row + direction;
        if (isValidPosition(newRow, col) && board.getPieceAt(new ChessGame.Position(newRow, col)) == null) {
            moves.add(new ChessGame.Position(newRow, col));
            
            // Double move from starting position
            if ((getColor() == Color.WHITE && row == 6) || (getColor() == Color.BLACK && row == 1)) {
                newRow = row + 2 * direction;
                if (isValidPosition(newRow, col) && board.getPieceAt(new ChessGame.Position(newRow, col)) == null) {
                    moves.add(new ChessGame.Position(newRow, col));
                }
            }
        }
        
        // Diagonal captures
        for (int dc : new int[]{-1, 1}) {
            int newCol = col + dc;
            newRow = row + direction;
            
            if (isValidPosition(newRow, newCol)) {
                ChessPiece pieceAtTarget = board.getPieceAt(new ChessGame.Position(newRow, newCol));
                
                if (pieceAtTarget != null && pieceAtTarget.getColor() != this.getColor()) {
                    moves.add(new ChessGame.Position(newRow, newCol));
                }
            }
        }
        
        // En passant is handled separately in the ChessBoard class
        
        return moves;
    }
}