package assignment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// plays the actual console game like a real person would: types lines into System.in
// and reads back what it prints to System.out
// has to run from the project root cuz the game opens words.txt and cubes.txt from there
@DisplayName("Boggle (console game)")
public class BoggleTest {

    // the exact text the game prints, we wait for these to know when it's our turn to type
    private static final String WORD_PROMPT = "Enter a word (or a blank line to finish): ";
    private static final String AGAIN_PROMPT = "Play again? (y/n): ";
    private static final String GOODBYE = "Thanks for playing!";
    // if the game goes quiet this long, something's stuck so just fail
    private static final long TIMEOUT_MS = 15_000;

    private static GameDictionary dictionary;
    private static List<String> cubes;

    // save the real in/out so we can put them back after each test
    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;
    private Session session;

    // load the word list and cubes once, the tests use them to figure out the right answers
    @BeforeAll
    static void loadFixtures() throws IOException {
        // skip instead of fail if we're running from the wrong folder
        assumeTrue(Files.exists(Path.of("words.txt")) && Files.exists(Path.of("cubes.txt")),
                "run from the project root so words.txt and cubes.txt can be found");
        dictionary = new GameDictionary();
        dictionary.loadDictionary("words.txt");
        cubes = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of("cubes.txt"))) {
            if (!line.isBlank()) {
                cubes.add(line.trim().toUpperCase());
            }
        }
    }

    // every test gets its own fresh game running
    @BeforeEach
    void startGame() throws IOException {
        session = new Session();
    }

    @AfterEach
    void restoreStreams() throws InterruptedException {
        session.close();
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    // one copy of Boggle.main running in the background, hooked up so the test can type to it and read from it
    private static class Session {
        // whatever we write here shows up as typed input in the game
        private final PipedOutputStream keyboard;
        // everything the game prints ends up in here
        private final ByteArrayOutputStream screen = new ByteArrayOutputStream();
        private final Thread game;
        // volatile cuz the game thread sets it and the test thread reads it
        private volatile Throwable crash;
        // how much of the output we've already looked at
        private int readUpTo = 0;
        private boolean inputClosed = false;

        Session() throws IOException {
            // big buffer so typing never has to wait on the game to read
            PipedInputStream in = new PipedInputStream(1 << 16);
            keyboard = new PipedOutputStream(in);
            System.setIn(in);
            System.setOut(new PrintStream(screen, true, StandardCharsets.UTF_8));
            // main has to run on its own thread, otherwise it'd block waiting for input we haven't typed yet
            game = new Thread(() -> {
                try {
                    Boggle.main(new String[0]);
                } catch (Throwable t) {
                    crash = t;
                }
            });
            // daemon so a stuck game can't keep the test run from finishing
            game.setDaemon(true);
            game.start();
        }

        // same as typing a line and hitting enter
        void type(String line) throws IOException {
            keyboard.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            keyboard.flush();
        }

        // pretend the user hit ctrl-d
        void closeInput() throws IOException {
            if (!inputClosed) {
                inputClosed = true;
                keyboard.close();
            }
        }

        // waits until the game prints marker, then gives back everything it printed since last time
        String readThrough(String marker) throws InterruptedException {
            long deadline = System.currentTimeMillis() + TIMEOUT_MS;
            while (true) {
                String all = screen.toString(StandardCharsets.UTF_8);
                // only look at the new stuff so an old prompt doesn't count again
                int at = all.indexOf(marker, readUpTo);
                if (at >= 0) {
                    String chunk = all.substring(readUpTo, at + marker.length());
                    readUpTo = at + marker.length();
                    return chunk;
                }
                // if the game blew up, show the error and everything it printed before that
                if (crash != null) {
                    fail("Boggle.main threw " + crash + "\nOutput so far:\n" + all, crash);
                }
                if (!game.isAlive() || System.currentTimeMillis() > deadline) {
                    fail("never printed \"" + marker + "\"\nOutput so far:\n" + all);
                }
                // check again in a bit
                Thread.sleep(10);
            }
        }

        // makes sure main actually returned and didn't crash on the way out
        void awaitExit() throws InterruptedException {
            game.join(TIMEOUT_MS);
            assertFalse(game.isAlive(), "Boggle.main did not return");
            assertNull(crash, () -> "Boggle.main threw " + crash);
        }

        // closing the input makes the game wrap up on its own, then we wait for it to finish
        void close() throws InterruptedException {
            try {
                closeInput();
            } catch (IOException ignored) {
                // the game already stopped reading
            }
            game.join(TIMEOUT_MS);
        }
    }

    // finds the last board printed in the output, rows look like "A B C D"
    private static char[][] lastBoard(String chunk, int size) {
        List<String> rows = new ArrayList<>();
        for (String line : chunk.split("\\R")) {
            String trimmed = line.trim();
            // exactly size letters with spaces between them, so other output lines don't get picked up
            if (trimmed.matches("[A-Za-z]( [A-Za-z]){" + (size - 1) + "}")) {
                rows.add(trimmed.replace(" ", ""));
            }
        }
        assertTrue(rows.size() >= size, "no " + size + "x" + size + " board in:\n" + chunk);
        // there could be more than one board in the chunk, we want the newest one
        char[][] board = new char[size][];
        for (int r = 0; r < size; r++) {
            board[r] = rows.get(rows.size() - size + r).toCharArray();
        }
        return board;
    }

    // works out every word the game should accept on this board, separately from the console
    private static Set<String> wordsOn(char[][] board) throws IOException {
        GameManager oracle = new GameManager();
        oracle.newGame(board.length, 1, "cubes.txt", dictionary);
        // swap in the board the game printed so we're checking the same one
        oracle.setGame(board);
        return new TreeSet<>(oracle.getAllWords());
    }

    // boards are random, so keep playing new rounds until we get one with at least `needed` words
    private char[][] boardWithWords(int needed) throws Exception {
        for (int attempt = 0; attempt < 25; attempt++) {
            char[][] board = lastBoard(session.readThrough(WORD_PROMPT), 4);
            if (wordsOn(board).size() >= needed) {
                return board;
            }
            // not enough words, so end this round and say yes to another one
            session.type("");
            session.readThrough(AGAIN_PROMPT);
            session.type("y");
        }
        fail("no board with " + needed + " words after 25 games");
        return null;
    }

    private static String lineStartingWith(String chunk, String prefix) {
        for (String line : chunk.split("\\R")) {
            if (line.startsWith(prefix)) {
                return line;
            }
        }
        fail("no line starting with \"" + prefix + "\" in:\n" + chunk);
        return null;
    }

    // turns "Label (n): a, b, c" into a list of words, and checks that n matches how many there are
    private static List<String> wordList(String chunk, String label) {
        String line = lineStartingWith(chunk, label + " (");
        // the number is between "(" and ")"
        int count = Integer.parseInt(line.substring(label.length() + 2, line.indexOf(')')));
        String rest = line.substring(line.indexOf(": ") + 2).trim();
        // splitting an empty string would give [""], so handle no words separately
        List<String> words = rest.isEmpty() ? List.of() : Arrays.asList(rest.split(", "));
        assertEquals(count, words.size(), "count shown does not match list: " + line);
        return words;
    }

    // grabs the number after "Label: "
    private static int scoreOn(String chunk, String label) {
        String line = lineStartingWith(chunk, label + ": ");
        return Integer.parseInt(line.substring(label.length() + 2).trim());
    }

    // same scoring rule as the game
    private static int points(String word) {
        return word.length() - 3;
    }

    @Test
    @DisplayName("prints a 4x4 board of uppercase letters drawn from the cubes")
    void printsBoard() throws Exception {
        char[][] board = lastBoard(session.readThrough(WORD_PROMPT), 4);
        // every letter that shows up on any face of any cube
        Map<Character, Integer> available = new HashMap<>();
        for (String cube : cubes) {
            for (char face : cube.toCharArray()) {
                available.merge(face, 1, Integer::sum);
            }
        }
        for (char[] row : board) {
            assertEquals(4, row.length);
            for (char ch : row) {
                assertTrue(Character.isUpperCase(ch), "board letter not capitalized: " + ch);
                assertTrue(available.containsKey(ch), "letter " + ch + " is not on any cube");
            }
        }
    }

    @Test
    @DisplayName("a valid word scores length - 3 and shows the running total")
    void acceptsValidWord() throws Exception {
        char[][] board = boardWithWords(1);
        String word = wordsOn(board).iterator().next();

        session.type(word);
        String reply = session.readThrough(WORD_PROMPT);

        // check the exact message, including "point" vs "points"
        int earned = points(word);
        String unit = earned == 1 ? " point" : " points";
        assertTrue(reply.contains("+" + earned + unit + "  (total: " + earned + ")"),
                "unexpected reply to \"" + word + "\":\n" + reply);
    }

    @Test
    @DisplayName("after a valid word, the board is reprinted with exactly the path in lowercase")
    void highlightsPath() throws Exception {
        char[][] board = boardWithWords(1);
        String word = wordsOn(board).iterator().next();

        session.type(word);
        char[][] highlighted = lastBoard(session.readThrough(WORD_PROMPT), 4);

        int lowered = 0;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                // highlighting should only change the case, never the actual letter
                assertEquals(board[r][c], Character.toUpperCase(highlighted[r][c]),
                        "highlighting changed the letter at (" + r + ", " + c + ")");
                if (Character.isLowerCase(highlighted[r][c])) {
                    lowered++;
                }
            }
        }
        // a path never reuses a cell, so it should be one lowercase cell per letter
        assertEquals(word.length(), lowered, "highlighted cells for \"" + word + "\"");
    }

    @Test
    @DisplayName("the running total adds up across several words")
    void runningTotal() throws Exception {
        char[][] board = boardWithWords(3);
        int total = 0;
        for (String word : new ArrayList<>(wordsOn(board)).subList(0, 3)) {
            session.type(word);
            String reply = session.readThrough(WORD_PROMPT);
            // keep our own total and make sure the game's matches after each word
            total += points(word);
            assertTrue(reply.contains("(total: " + total + ")"), "after \"" + word + "\":\n" + reply);
        }
    }

    @Test
    @DisplayName("input is trimmed and case-insensitive")
    void normalizesInput() throws Exception {
        char[][] board = boardWithWords(1);
        String word = wordsOn(board).iterator().next();

        // all caps with extra spaces should still count
        session.type("   " + word.toUpperCase() + "   ");
        String reply = session.readThrough(WORD_PROMPT);

        assertTrue(reply.contains("+" + points(word)), "uppercase, padded input rejected:\n" + reply);
    }

    @Test
    @DisplayName("the same word twice is refused the second time")
    void rejectsRepeat() throws Exception {
        char[][] board = boardWithWords(1);
        String word = wordsOn(board).iterator().next();

        session.type(word);
        session.readThrough(WORD_PROMPT);
        session.type(word);
        String reply = session.readThrough(WORD_PROMPT);

        assertTrue(reply.contains("You already found \"" + word + "\"."), reply);
        // no + anywhere means no points were given out
        assertFalse(reply.contains("+"), "a repeat scored points:\n" + reply);
    }

    @Test
    @DisplayName("words under 4 letters are too short")
    void rejectsShortWord() throws Exception {
        session.readThrough(WORD_PROMPT);
        session.type("cat");
        String reply = session.readThrough(WORD_PROMPT);
        assertTrue(reply.contains("\"cat\" is too short. Words need at least 4 letters."), reply);
    }

    @Test
    @DisplayName("words missing from the dictionary are rejected")
    void rejectsNonWord() throws Exception {
        session.readThrough(WORD_PROMPT);
        // random letters that definitely aren't a word
        session.type("qxzvq");
        String reply = session.readThrough(WORD_PROMPT);
        assertTrue(reply.contains("\"qxzvq\" is not a word in the dictionary."), reply);
    }

    @Test
    @DisplayName("real words that cannot be traced are not on the board")
    void rejectsWordOffBoard() throws Exception {
        char[][] board = lastBoard(session.readThrough(WORD_PROMPT), 4);
        Set<String> traceable = wordsOn(board);
        // grab the first real word that's long enough but isn't on this board
        String missing = null;
        for (String word : dictionary) {
            if (word.length() >= 4 && !traceable.contains(word)) {
                missing = word;
                break;
            }
        }
        assertNotNull(missing);

        session.type(missing);
        String reply = session.readThrough(WORD_PROMPT);
        assertTrue(reply.contains("\"" + missing + "\" is not on the board."), reply);
    }

    @Test
    @DisplayName("the summary splits words between player and computer and scores both")
    void endOfRoundSummary() throws Exception {
        char[][] board = boardWithWords(2);
        Set<String> all = wordsOn(board);
        Iterator<String> it = all.iterator();
        // we find 2 words, the computer should get the rest
        List<String> mine = List.of(it.next(), it.next());

        for (String word : mine) {
            session.type(word);
            session.readThrough(WORD_PROMPT);
        }
        // blank line ends the round and shows the summary
        session.type("");
        String summary = session.readThrough(AGAIN_PROMPT);

        // figure out what the summary should say on our own
        Set<String> computer = new TreeSet<>(all);
        mine.forEach(computer::remove);
        int myScore = mine.stream().mapToInt(BoggleTest::points).sum();
        int computerScore = computer.stream().mapToInt(BoggleTest::points).sum();

        assertEquals(new TreeSet<>(mine), new TreeSet<>(wordList(summary, "Your words")));
        assertEquals(computer, new TreeSet<>(wordList(summary, "Computer's words")));
        assertEquals(myScore, scoreOn(summary, "Your score"));
        assertEquals(computerScore, scoreOn(summary, "Computer score"));
    }

    @Test
    @DisplayName("finishing with no words gives the computer everything")
    void emptyRound() throws Exception {
        char[][] board = lastBoard(session.readThrough(WORD_PROMPT), 4);
        Set<String> all = wordsOn(board);

        // quit right away without finding anything
        session.type("");
        String summary = session.readThrough(AGAIN_PROMPT);

        assertTrue(wordList(summary, "Your words").isEmpty());
        assertEquals(0, scoreOn(summary, "Your score"));
        assertEquals(all, new TreeSet<>(wordList(summary, "Computer's words")));
        assertEquals(all.stream().mapToInt(BoggleTest::points).sum(), scoreOn(summary, "Computer score"));
    }

    @Test
    @DisplayName("answering y starts a fresh round with scores reset")
    void playAgain() throws Exception {
        // score something in round 1 so we can tell if it carries over
        char[][] board = boardWithWords(1);
        String word = wordsOn(board).iterator().next();
        session.type(word);
        session.readThrough(WORD_PROMPT);
        session.type("");
        session.readThrough(AGAIN_PROMPT);

        // round 2: don't find anything and check the summary is all fresh
        session.type("y");
        char[][] next = lastBoard(session.readThrough(WORD_PROMPT), 4);
        session.type("");
        String summary = session.readThrough(AGAIN_PROMPT);

        assertTrue(wordList(summary, "Your words").isEmpty(), "words carried over:\n" + summary);
        assertEquals(0, scoreOn(summary, "Your score"));
        // computer's words should be for the new board, not the old one
        assertEquals(wordsOn(next), new TreeSet<>(wordList(summary, "Computer's words")));

        session.type("n");
        session.readThrough(GOODBYE);
        session.awaitExit();
    }

    @Test
    @DisplayName("answering n says goodbye and main returns")
    void quit() throws Exception {
        session.readThrough(WORD_PROMPT);
        session.type("");
        session.readThrough(AGAIN_PROMPT);
        session.type("n");
        session.readThrough(GOODBYE);
        session.awaitExit();
    }

    @Test
    @DisplayName("end of input mid-round still prints the summary and exits cleanly")
    void endOfInput() throws Exception {
        session.readThrough(WORD_PROMPT);
        // ctrl-d in the middle of a round, the game should still wrap up nicely instead of crashing
        session.closeInput();
        String rest = session.readThrough(GOODBYE);
        assertTrue(rest.contains("Your score: 0"), rest);
        assertTrue(rest.contains("Computer score: "), rest);
        session.awaitExit();
    }
}
