package assignment;

import java.awt.Point;
import java.io.IOException;
import java.util.*;

public class Boggle {

    public static void main(String[] args) throws IOException {
        GameDictionary dictionary = new GameDictionary();
        dictionary.loadDictionary("words.txt");
        GameManager game = new GameManager();
        Scanner userInput = new Scanner(System.in);

        while (true) {
            game.newGame(4, 1, "cubes.txt", dictionary);
            Set<String> humanWords = new TreeSet<>();

            System.out.println();
            printBoard(game.getBoard());

            while (true) {
                System.out.print("\nEnter a word (or a blank line to finish): ");
                if (!userInput.hasNextLine()) {
                    break;
                }
                String word = userInput.nextLine().trim().toLowerCase();
                if (word.isEmpty()) {
                    break;
                }

                int points = game.addWord(word, 0);
                if (points > 0) {
                    humanWords.add(word);
                    System.out.println("+" + points + (points == 1 ? " point" : " points")
                            + "  (total: " + game.getScores()[0] + ")");
                    char[][] highlighted = game.getBoard();
                    List<Point> path = game.getLastAddedWord();
                    for (Point p : path) {
                        highlighted[p.x][p.y] = Character.toLowerCase(highlighted[p.x][p.y]);
                    }
                    printBoard(highlighted);
                } else if (word.length() < 4) {
                    System.out.println("\"" + word + "\" is too short. Words need at least 4 letters.");
                } else if (humanWords.contains(word)) {
                    System.out.println("You already found \"" + word + "\".");
                } else if (!dictionary.contains(word)) {
                    System.out.println("\"" + word + "\" is not a word in the dictionary.");
                } else {
                    System.out.println("\"" + word + "\" is not on the board.");
                }
            }

            Set<String> computerWords = new TreeSet<>(game.getAllWords());
            computerWords.removeAll(humanWords);
            int computerScore = 0;
            for (String w : computerWords) {
                computerScore += w.length() - 3;
            }

            System.out.println("\nYour words (" + humanWords.size() + "): " + String.join(", ", humanWords));
            System.out.println("Computer's words (" + computerWords.size() + "): " + String.join(", ", computerWords));
            System.out.println("\nYour score: " + game.getScores()[0]);
            System.out.println("Computer score: " + computerScore);

            System.out.print("\nPlay again? (y/n): ");
            if (!userInput.hasNextLine()) {
                break;
            }
            String answer = userInput.nextLine().trim().toLowerCase();
            if (!answer.startsWith("y")) {
                break;
            }
        }

        System.out.println("Thanks for playing!");
    }

    private static void printBoard(char[][] board) {
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                System.out.print(board[r][c] + " ");
            }
            System.out.println();
        }
    }
}