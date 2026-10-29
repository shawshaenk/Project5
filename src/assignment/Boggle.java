package assignment;

import java.awt.Point;
import java.io.IOException;
import java.util.*;

// the console version of the game: you vs the computer on a 4x4 board
public class Boggle {

    public static void main(String[] args) throws IOException {
        // load the dictionary once up front, it's the same for every round
        GameDictionary dictionary = new GameDictionary();
        dictionary.loadDictionary("words.txt");
        GameManager game = new GameManager();
        Scanner userInput = new Scanner(System.in);

        // each time through this loop is one round
        while (true) {
            // just 1 player (you), the computer's words get figured out at the end
            game.newGame(4, 1, "cubes.txt", dictionary);
            // tree set keeps your words sorted for the summary
            Set<String> humanWords = new TreeSet<>();

            System.out.println();
            printBoard(game.getBoard());

            // keep asking for words until you enter a blank line
            while (true) {
                System.out.print("\nEnter a word (or a blank line to finish): ");
                // input ran out (like ctrl-d), so treat it as done
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
                    // say "point" for 1 and "points" otherwise
                    System.out.println("+" + points + (points == 1 ? " point" : " points")
                            + "  (total: " + game.getScores()[0] + ")");
                    // show where the word was by making its letters lowercase
                    char[][] highlighted = game.getBoard();
                    List<Point> path = game.getLastAddedWord();
                    for (Point p : path) {
                        highlighted[p.x][p.y] = Character.toLowerCase(highlighted[p.x][p.y]);
                    }
                    printBoard(highlighted);
                // addWord just gives 0 for anything wrong, so we check again here to say what was wrong
                } else if (word.length() < 4) {
                    System.out.println("\"" + word + "\" is too short. Words need at least 4 letters.");
                } else if (humanWords.contains(word)) {
                    System.out.println("You already found \"" + word + "\".");
                } else if (!dictionary.contains(word)) {
                    System.out.println("\"" + word + "\" is not a word in the dictionary.");
                } else {
                    // it's a real word, so the only thing left is that it's not on the board
                    System.out.println("\"" + word + "\" is not on the board.");
                }
            }

            // the computer gets every word on the board that you didn't find
            Set<String> computerWords = new TreeSet<>(game.getAllWords());
            computerWords.removeAll(humanWords);
            // computer's words score the same way yours do
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
            // anything starting with y counts, so "yes" or "Y" works too
            String answer = userInput.nextLine().trim().toLowerCase();
            if (!answer.startsWith("y")) {
                break;
            }
        }

        System.out.println("Thanks for playing!");
    }

    // prints the board one row per line with spaces between letters
    private static void printBoard(char[][] board) {
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                System.out.print(board[r][c] + " ");
            }
            System.out.println();
        }
    }
}
