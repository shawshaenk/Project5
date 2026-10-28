package assignment;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class GameManager implements BoggleGame {
    char[][] board;
    int[] playerScores;
    String cubeFile;
    BoggleDictionary dictionary;
    private SearchTactic tactic = SEARCH_DEFAULT;
    private List<Set<String>> playerWords = new ArrayList<>();
    private List<Point> lastPath;
    private List<String> cubes = new ArrayList<>();
    private Random random = new Random();

    @Override
    public int addWord(String word, int player) {
        if (word == null) return 0;
        if (playerScores == null || player < 0 || player >= playerScores.length) return 0;
        word = word.trim().toLowerCase();
        if (word.length() < 4) return 0;
        if (playerWords.get(player).contains(word)) return 0;
        if (!dictionary.contains(word)) return 0;
        List<Point> path = findPath(word);
        if (path == null) return 0;

        playerWords.get(player).add(word);
        int points = word.length() - 3;
        playerScores[player] += points;
        lastPath = path;
        return points;
    }

    @Override
    public Collection<String> getAllWords() {
        if (tactic == SearchTactic.SEARCH_DICT) {
            return searchDictionary();
        }
        return searchBoard();
    }

    @Override
    public char[][] getBoard() {
        char[][] copy = new char[board.length][];
        for (int r = 0; r < board.length; r++) {
            copy[r] = board[r].clone();
        }
        return copy;
    }

    @Override
    public List<Point> getLastAddedWord() {
        if (lastPath == null) return null;
        return new ArrayList<>(lastPath);
    }

    @Override
    public int[] getScores() {
        return playerScores.clone();
    }

    @Override
    public void newGame(int size, int numPlayers, String cubeFile, BoggleDictionary dict) throws IOException {
        if (size < 1) throw new IllegalArgumentException("Size must be at least 1");
        if (numPlayers < 1) throw new IllegalArgumentException("Must have at least one player");
        if (dict == null) throw new IllegalArgumentException("Dictionary cannot be null");

        this.playerScores = new int[numPlayers];
        this.cubeFile = cubeFile;
        this.dictionary = dict;
        this.playerWords = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            playerWords.add(new HashSet<>());
        }
        this.lastPath = null;

        loadCubes(cubeFile);
        if (cubes.size() < size * size) {
            throw new IllegalArgumentException("Not enough cubes for a " + size + "x" + size + " board");
        }
        this.board = new char[size][size];
        fillBoard(size);
    }

    private void loadCubes(String filename) throws IOException {
        cubes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim().toUpperCase();
                if (!line.isEmpty()) {
                    cubes.add(line);
                }
            }
        }
        if (cubes.isEmpty()) {
            throw new IOException("Cube file contains no cubes");
        }
    }

    private void fillBoard(int size) {
        List<String> pool = new ArrayList<>(cubes);
        Collections.shuffle(pool, random);
        int i = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                String cube = pool.get(i++);
                board[r][c] = cube.charAt(random.nextInt(cube.length()));
            }
        }
    }

    @Override
    public void setGame(char[][] board) {
        if (board == null || board.length == 0) {
            throw new IllegalArgumentException("Board must be non-empty");
        }
        int n = board.length;
        this.board = new char[n][n];
        for (int r = 0; r < n; r++) {
            if (board[r] == null || board[r].length != n) {
                throw new IllegalArgumentException("Board must be square");
            }
            this.board[r] = board[r].clone();
        }
        if (playerScores != null) {
            Arrays.fill(playerScores, 0);
        }
        for (Set<String> words : playerWords) {
            words.clear();
        }
        lastPath = null;
    }

    @Override
    public void setSearchTactic(SearchTactic tactic) {
        if (tactic == null) {
            this.tactic = SEARCH_DEFAULT;
        } else {
            this.tactic = tactic;
        }
    }

    private Set<String> searchBoard() {
        Set<String> found = new TreeSet<>();
        int n = board.length;
        boolean[][] visited = new boolean[n][n];
        StringBuilder current = new StringBuilder();

        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                dfs(r, c, visited, current, found);
            }
        }
        return found;
    }

    private void dfs(int r, int c, boolean[][] visited, StringBuilder current, Set<String> found) {
        int n = board.length;

        if (r < 0 || r >= n || c < 0 || c >= n) {
            return;
        }
        if (visited[r][c]) {
            return;
        }

        current.append(Character.toLowerCase(board[r][c]));
        String word = current.toString();

        if (!dictionary.isPrefix(word)) {
            current.deleteCharAt(current.length() - 1);
            return;
        }

        if (word.length() >= 4 && dictionary.contains(word)) {
            found.add(word);
        }

        visited[r][c] = true;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr != 0 || dc != 0) {
                    dfs(r + dr, c + dc, visited, current, found);
                }
            }
        }

        visited[r][c] = false;
        current.deleteCharAt(current.length() - 1);
    }

    private Set<String> searchDictionary() {
        Set<String> found = new TreeSet<>();
        for (String word : dictionary) {
            if (word.length() >= 4 && findPath(word) != null) {
                found.add(word);
            }
        }
        return found;
    }

    private List<Point> findPath(String word) {
        if (word == null || word.isEmpty()) return null;
        word = word.toLowerCase();

        int n = board.length;
        boolean[][] visited = new boolean[n][n];
        List<Point> path = new ArrayList<>();

        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                if (pathFrom(r, c, word, 0, visited, path)) {
                    return path;
                }
            }
        }
        return null;
    }

    private boolean pathFrom(int r, int c, String word, int index,
                             boolean[][] visited, List<Point> path) {
        int n = board.length;

        if (r < 0 || r >= n || c < 0 || c >= n) return false;
        if (visited[r][c]) return false;
        if (Character.toLowerCase(board[r][c]) != word.charAt(index)) return false;

        visited[r][c] = true;
        path.add(new Point(r, c));

        if (index == word.length() - 1) return true;

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if ((dr != 0 || dc != 0)
                        && pathFrom(r + dr, c + dc, word, index + 1, visited, path)) {
                    return true;
                }
            }
        }

        visited[r][c] = false;
        path.remove(path.size() - 1);
        return false;
    }
}