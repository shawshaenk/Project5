package assignment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameManager")
public class GameManagerTest {

    private static final char[][] GRID = {
            {'S', 'T', 'O', 'P'},
            {'L', 'A', 'N', 'E'},
            {'I', 'R', 'E', 'D'},
            {'M', 'U', 'S', 'K'}
    };

    private static final Set<String> TRACEABLE = Set.of(
            "stop", "salt", "slat", "star", "tarn", "tone", "stone",
            "open", "pond", "lair", "mire", "ruse", "used", "toner");

    private static final List<String> NOT_TRACEABLE = List.of(
            "top", "quiz", "mask", "rare", "lump", "dune");

    private GameDictionary fullDict;
    private GameManager manager;
    private String uniformCubes;

    private static String writeTemp(List<String> lines) throws IOException {
        Path file = Files.createTempFile("game-fixture", ".txt");
        Files.write(file, lines);
        file.toFile().deleteOnExit();
        return file.toString();
    }

    private static String nonexistentPath(String name) {
        return new File("no_such_file_" + name + "_98765.txt").getAbsolutePath();
    }

    private static void checkPath(char[][] grid, String word, List<Point> path) {
        assertNotNull(path, "no path recorded for " + word);
        assertEquals(word.length(), path.size(), "path length for " + word);
        Set<Point> visited = new HashSet<>();
        Point previous = null;
        for (int i = 0; i < path.size(); i++) {
            Point cell = path.get(i);
            char onBoard = Character.toLowerCase(grid[cell.x][cell.y]);
            assertEquals(Character.toLowerCase(word.charAt(i)), onBoard, "letter " + i + " of " + word);
            assertTrue(visited.add(cell), "reused a cell in " + word);
            if (previous != null) {
                int rowGap = Math.abs(previous.x - cell.x);
                int colGap = Math.abs(previous.y - cell.y);
                assertTrue(rowGap <= 1 && colGap <= 1, "non-adjacent step in " + word);
            }
            previous = cell;
        }
    }

    @BeforeEach
    void init() throws IOException {
        List<String> lexicon = new ArrayList<>(TRACEABLE);
        lexicon.addAll(NOT_TRACEABLE);
        fullDict = new GameDictionary();
        fullDict.loadDictionary(writeTemp(lexicon));

        uniformCubes = writeTemp(Collections.nCopies(25, "LMNOPQ"));
        manager = new GameManager();
        manager.newGame(4, 2, uniformCubes, fullDict);
        manager.setGame(GRID);
    }

    @Nested
    @DisplayName("Game setup")
    class Setup {

        @Test
        @DisplayName("newGame builds a square board from cube letters")
        void boardShapeAndLetters() throws IOException {
            manager.newGame(5, 1, uniformCubes, fullDict);
            char[][] grid = manager.getBoard();
            assertEquals(5, grid.length);
            for (char[] row : grid) {
                assertEquals(5, row.length);
                for (char ch : row) {
                    assertTrue("LMNOPQ".indexOf(Character.toUpperCase(ch)) >= 0, "unexpected " + ch);
                }
            }
        }

        @Test
        @DisplayName("each cube lands on the board once")
        void everyCubeUsedOnce() throws IOException {
            String solidCubes = writeTemp(List.of("WWWWWW", "XXXXXX", "YYYYYY", "ZZZZZZ"));
            manager.newGame(2, 1, solidCubes, fullDict);
            Set<Character> seen = new HashSet<>();
            for (char[] row : manager.getBoard()) {
                for (char ch : row) {
                    seen.add(Character.toUpperCase(ch));
                }
            }
            assertEquals(Set.of('W', 'X', 'Y', 'Z'), seen);
        }

        @Test
        @DisplayName("newGame clears scores, found words, and last path")
        void newGameStartsFresh() throws IOException {
            assertEquals(1, manager.addWord("stop", 0));
            manager.newGame(4, 3, uniformCubes, fullDict);
            assertArrayEquals(new int[3], manager.getScores());
            assertNull(manager.getLastAddedWord());
            manager.setGame(GRID);
            assertEquals(1, manager.addWord("stop", 0));
        }

        @Test
        @DisplayName("newGame rejects bad sizes, players, and cube files")
        void newGameValidation() throws IOException {
            String tooFew = writeTemp(Collections.nCopies(3, "LMNOPQ"));
            String absent = nonexistentPath("cubes");
            assertThrows(IllegalArgumentException.class, () -> manager.newGame(4, 0, uniformCubes, fullDict));
            assertThrows(IllegalArgumentException.class, () -> manager.newGame(0, 1, uniformCubes, fullDict));
            assertThrows(IllegalArgumentException.class, () -> manager.newGame(2, 1, tooFew, fullDict));
            assertThrows(IOException.class, () -> manager.newGame(4, 1, absent, fullDict));
        }

        @Test
        @DisplayName("board is copied both into setGame and out of getBoard")
        void boardIsDefensivelyCopied() {
            char[][] snapshot = manager.getBoard();
            for (int r = 0; r < GRID.length; r++) {
                assertArrayEquals(GRID[r], snapshot[r], "row " + r);
            }
            snapshot[1][1] = 'Q';
            assertEquals('A', manager.getBoard()[1][1]);

            char[][] tiny = {{'G', 'H'}, {'J', 'K'}};
            manager.setGame(tiny);
            tiny[1][0] = 'Q';
            assertEquals('J', manager.getBoard()[1][0]);
        }

        @Test
        @DisplayName("setGame rejects null, empty, ragged, and non-square boards")
        void setGameValidation() {
            assertThrows(IllegalArgumentException.class, () -> manager.setGame(null));
            assertThrows(IllegalArgumentException.class, () -> manager.setGame(new char[0][0]));
            assertThrows(IllegalArgumentException.class, () -> manager.setGame(new char[][]{{'A', 'B'}, {'C'}}));
            assertThrows(IllegalArgumentException.class, () -> manager.setGame(new char[][]{{'A', 'B', 'C'}, {'D', 'E', 'F'}}));
        }

        @Test
        @DisplayName("setGame clears scores, found words, and last path")
        void setGameStartsFresh() {
            assertEquals(1, manager.addWord("stop", 0));
            assertEquals(2, manager.addWord("stone", 1));
            manager.setGame(GRID);
            assertArrayEquals(new int[2], manager.getScores());
            assertNull(manager.getLastAddedWord());
            assertEquals(1, manager.addWord("stop", 0));
        }

        @Test
        @DisplayName("a lowercase board plays the same as uppercase")
        void lowercaseBoard() {
            char[][] lower = new char[GRID.length][];
            for (int r = 0; r < GRID.length; r++) {
                lower[r] = String.valueOf(GRID[r]).toLowerCase().toCharArray();
            }
            manager.setGame(lower);
            assertEquals(1, manager.addWord("stop", 0));
            assertEquals(TRACEABLE, new HashSet<>(manager.getAllWords()));
        }
    }

    @Nested
    @DisplayName("Adding words")
    class AddingWords {

        @Test
        @DisplayName("each word scores its length minus three")
        void scoring() {
            int expectedTotal = 0;
            for (String word : TRACEABLE) {
                int earned = manager.addWord(word, 1);
                assertEquals(word.length() - 3, earned, word);
                expectedTotal += earned;
            }
            assertArrayEquals(new int[]{0, expectedTotal}, manager.getScores());
        }

        @Test
        @DisplayName("invalid words and players earn nothing")
        void rejections() {
            assertEquals(0, manager.addWord("top", 0));
            assertEquals(0, manager.addWord("", 0));
            assertEquals(0, manager.addWord(null, 0));
            assertEquals(0, manager.addWord("pots", 0));
            assertEquals(0, manager.addWord("quiz", 0));
            assertEquals(0, manager.addWord("mask", 0));
            assertEquals(0, manager.addWord("lump", 0));
            assertEquals(0, manager.addWord("rare", 0));
            assertEquals(0, manager.addWord("stop", -1));
            assertEquals(0, manager.addWord("stop", 2));
            assertArrayEquals(new int[2], manager.getScores());
        }

        @Test
        @DisplayName("same player cannot score a word twice, in any case")
        void noDoubleScoring() {
            assertEquals(1, manager.addWord("tarn", 0));
            assertEquals(0, manager.addWord("tarn", 0));
            assertEquals(0, manager.addWord("TARN", 0));
            assertArrayEquals(new int[]{1, 0}, manager.getScores());
        }

        @Test
        @DisplayName("getScores returns a copy")
        void scoresAreCopied() {
            manager.addWord("tarn", 0);
            int[] leaked = manager.getScores();
            leaked[0] = 999;
            assertArrayEquals(new int[]{1, 0}, manager.getScores());
        }

        @Test
        @DisplayName("players are tracked independently; case and spacing ignored")
        void independentPlayers() {
            assertEquals(1, manager.addWord("pond", 0));
            assertEquals(1, manager.addWord("pond", 1));
            assertEquals(2, manager.addWord("ToNeR", 0));
            assertEquals(1, manager.addWord("  lair  ", 1));
            assertArrayEquals(new int[]{3, 2}, manager.getScores());
        }

        @Test
        @DisplayName("last added word is always a legal path")
        void pathsAreLegal() {
            for (String word : TRACEABLE) {
                assertTrue(manager.addWord(word, 0) > 0, word);
                checkPath(GRID, word, manager.getLastAddedWord());
            }
        }

        @Test
        @DisplayName("last added word has exact (row, col) points and ignores failures")
        void exactPathSurvivesFailures() {
            assertNull(manager.getLastAddedWord());
            manager.addWord("pond", 0);
            List<Point> expected = List.of(new Point(0, 3), new Point(0, 2), new Point(1, 2), new Point(2, 3));
            assertEquals(expected, manager.getLastAddedWord());
            manager.addWord("quiz", 0);
            manager.addWord("pond", 0);
            assertEquals(expected, manager.getLastAddedWord());
        }
    }

    @Nested
    @DisplayName("Finding all words")
    class FindingAll {

        @Test
        @DisplayName("both tactics and null find every word with no duplicates")
        void allTacticsMatch() {
            manager.addWord("stop", 0);
            manager.addWord("stone", 1);
            assertEquals(TRACEABLE, new HashSet<>(manager.getAllWords()));
            BoggleGame.SearchTactic[] tactics = {
                    BoggleGame.SearchTactic.SEARCH_DICT, BoggleGame.SearchTactic.SEARCH_BOARD, null};
            for (BoggleGame.SearchTactic tactic : tactics) {
                manager.setSearchTactic(tactic);
                Collection<String> found = manager.getAllWords();
                assertEquals(TRACEABLE, new HashSet<>(found), "tactic " + tactic);
                assertEquals(TRACEABLE.size(), found.size(), "duplicates under " + tactic);
            }
        }

        @Test
        @DisplayName("tiny boards with no words")
        void barrenBoards() {
            for (BoggleGame.SearchTactic tactic : BoggleGame.SearchTactic.values()) {
                manager.setSearchTactic(tactic);
                manager.setGame(new char[][]{{'Q', 'Q'}, {'Q', 'Q'}});
                assertTrue(manager.getAllWords().isEmpty(), tactic.name());
                manager.setGame(new char[][]{{'S'}});
                assertTrue(manager.getAllWords().isEmpty(), tactic.name());
                assertEquals(0, manager.addWord("stop", 0));
            }
        }

        @Test
        @DisplayName("tactics agree on random boards, and every found word can be added")
        void randomBoardsAgree() throws IOException {
            String mixedCubes = writeTemp(Collections.nCopies(25, "ADEILMNOPRSTU"));
            for (int round = 0; round < 20; round++) {
                manager.newGame(round % 2 == 0 ? 5 : 4, 1, mixedCubes, fullDict);
                manager.setSearchTactic(BoggleGame.SearchTactic.SEARCH_DICT);
                Set<String> viaDict = new HashSet<>(manager.getAllWords());
                manager.setSearchTactic(BoggleGame.SearchTactic.SEARCH_BOARD);
                assertEquals(viaDict, new HashSet<>(manager.getAllWords()), "round " + round);
                for (String word : viaDict) {
                    assertTrue(manager.addWord(word, 0) > 0, word + " found but not addable");
                }
            }
        }
    }
}