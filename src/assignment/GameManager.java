package assignment;

import java.awt.Point;
import java.io.IOException;
import java.util.*;

public class GameManager implements BoggleGame {
    char[][] board;
    int[] playerScores;
    String cubeFile;
    BoggleDictionary dictionary;
    private SearchTactic tactic = SEARCH_DEFAULT;

    @Override
    public int addWord(String word, int player) {
        // TODO - implement me!
        return 0;
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
        // return a copy so callers can't modify our board
        char[][] copy = new char[board.length][];
        for (int r = 0; r < board.length; r++) {
            copy[r] = board[r].clone();
        }
        return copy;
    }

    @Override
    public List<Point> getLastAddedWord() {
        // TODO - implement me!
        return null;
    }

    @Override
    public int[] getScores() {
        return playerScores.clone();
    }

    @Override
    public void newGame(int size, int numPlayers, String cubeFile, BoggleDictionary dict) throws IOException {
        this.board = new char[size][size];
        this.playerScores = new int[numPlayers];
        this.cubeFile = cubeFile;
        this.dictionary = dict;
        // TODO - read cubes from cubeFile, shuffle, and fill the board
    }

    @Override
    public void setGame(char[][] board) {
        int n = board.length;
        this.board = new char[n][n];
        for (int r = 0; r < n; r++) {
            if (board[r].length != n) {
                throw new IllegalArgumentException("Board must be square");
            }
            this.board[r] = board[r].clone();
        }
        if (playerScores != null) {
            Arrays.fill(playerScores, 0);
        }
        // TODO - also clear each player's list of found words once you add those
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