package assignment;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

// runs the actual game: builds the board, checks and scores words, and finds every word on the board
public class GameManager implements BoggleGame {
    char[][] board;
    int[] playerScores;
    String cubeFile;
    BoggleDictionary dictionary;
    private SearchTactic tactic = SEARCH_DEFAULT;
    // one set per player so we can tell if they already got a word
    private List<Set<String>> playerWords = new ArrayList<>();
    // the cells of the last word someone got, so the ui can highlight it
    private List<Point> lastPath;
    private List<String> cubes = new ArrayList<>();
    private Random random = new Random();

    @Override
    public int addWord(String word, int player) {
        // any bad input just scores 0 instead of crashing
        if (word == null) return 0;
        // no game yet or a player number that doesn't exist
        if (playerScores == null || player < 0 || player >= playerScores.length) return 0;
        // clean it up so "  CAT " and "cat" count as the same word
        word = word.trim().toLowerCase();
        // boggle rules say words need at least 4 letters
        if (word.length() < 4) return 0;
        // can't score the same word twice
        if (playerWords.get(player).contains(word)) return 0;
        if (!dictionary.contains(word)) return 0;
        // has to actually be traceable on the board too
        List<Point> path = findPath(word);
        if (path == null) return 0;

        // it passed everything so give the player credit
        playerWords.get(player).add(word);
        // 4 letters is 1 point, 5 is 2, and so on
        int points = word.length() - 3;
        playerScores[player] += points;
        // only save the path when the word actually counted
        lastPath = path;
        return points;
    }

    @Override
    public Collection<String> getAllWords() {
        // both ways should find the exact same words, they just go about it diff
        if (tactic == SearchTactic.SEARCH_DICT) {
            return searchDictionary();
        }
        return searchBoard();
    }

    @Override
    public char[][] getBoard() {
        // hand back a copy so whoever calls this can't mess with our real board
        char[][] copy = new char[board.length][];
        for (int r = 0; r < board.length; r++) {
            copy[r] = board[r].clone();
        }
        return copy;
    }

    @Override
    public List<Point> getLastAddedWord() {
        // null means nobody has gotten a word yet
        if (lastPath == null) return null;
        // copy it for the same reason as getBoard
        return new ArrayList<>(lastPath);
    }

    @Override
    public int[] getScores() {
        // copy again so nobody can just give themselves points
        return playerScores.clone();
    }

    @Override
    public void newGame(int size, int numPlayers, String cubeFile, BoggleDictionary dict) throws IOException {
        // stop bad setups early before we change anything
        if (size < 1) throw new IllegalArgumentException("Size must be at least 1");
        if (numPlayers < 1) throw new IllegalArgumentException("Must have at least one player");
        if (dict == null) throw new IllegalArgumentException("Dictionary cannot be null");

        // fresh scores and word lists for everyone
        this.playerScores = new int[numPlayers];
        this.cubeFile = cubeFile;
        this.dictionary = dict;
        this.playerWords = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            playerWords.add(new HashSet<>());
        }
        this.lastPath = null;

        loadCubes(cubeFile);
        // each spot gets its own cube, so we need at least size * size of them
        if (cubes.size() < size * size) {
            throw new IllegalArgumentException("Not enough cubes for a " + size + "x" + size + " board");
        }
        this.board = new char[size][size];
        fillBoard(size);
    }

    // each line in the cube file is one cube, and each letter on the line is one face
    private void loadCubes(String filename) throws IOException {
        cubes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // board letters are always capitalized
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

    // basically shaking the cubes in the box
    private void fillBoard(int size) {
        // shuffle so the cubes land in random spots
        List<String> pool = new ArrayList<>(cubes);
        Collections.shuffle(pool, random);
        int i = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                // each cube gets used once, and we pick a random face to land on top
                String cube = pool.get(i++);
                board[r][c] = cube.charAt(random.nextInt(cube.length()));
            }
        }
    }

    @Override
    public void setGame(char[][] board) {
        // need an actual board to work with
        if (board == null || board.length == 0) {
            throw new IllegalArgumentException("Board must be non-empty");
        }
        int n = board.length;
        this.board = new char[n][n];
        for (int r = 0; r < n; r++) {
            // every row has to be as long as the number of rows, or it's not square
            if (board[r] == null || board[r].length != n) {
                throw new IllegalArgumentException("Board must be square");
            }
            // copy each row so changing their array later doesn't change ours
            this.board[r] = board[r].clone();
        }
        // new board means starting over, but we keep the same players and dictionary
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
        // null isn't a real tactic so just go back to the default
        if (tactic == null) {
            this.tactic = SEARCH_DEFAULT;
        } else {
            this.tactic = tactic;
        }
    }

    // tries starting from every cell and follows paths as long as they could still turn into a word
    private Set<String> searchBoard() {
        // a set so a word we can make two diff ways only shows up once, and tree set keeps it sorted
        Set<String> found = new TreeSet<>();
        int n = board.length;
        boolean[][] visited = new boolean[n][n];
        // reuse one builder and add/remove letters as we go instead of making new strings
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

        // went off the edge of the board
        if (r < 0 || r >= n || c < 0 || c >= n) {
            return;
        }
        // can't use the same cube twice in one word
        if (visited[r][c]) {
            return;
        }

        current.append(Character.toLowerCase(board[r][c]));
        String word = current.toString();

        // if no word starts like this, going further is pointless, this is what keeps it fast
        if (!dictionary.isPrefix(word)) {
            current.deleteCharAt(current.length() - 1);
            return;
        }

        if (word.length() >= 4 && dictionary.contains(word)) {
            found.add(word);
        }

        // mark this cell used, then try all 8 neighbors
        visited[r][c] = true;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                // 0,0 would just be the same cell
                if (dr != 0 || dc != 0) {
                    dfs(r + dr, c + dc, visited, current, found);
                }
            }
        }

        // undo everything on the way back so other paths can use this cell
        visited[r][c] = false;
        current.deleteCharAt(current.length() - 1);
    }

    // the other way around: go through every dictionary word and check if it's on the board
    private Set<String> searchDictionary() {
        Set<String> found = new TreeSet<>();
        for (String word : dictionary) {
            if (word.length() >= 4 && findPath(word) != null) {
                found.add(word);
            }
        }
        return found;
    }

    // gives back the list of cells that spell the word, or null if it's not on the board
    private List<Point> findPath(String word) {
        if (word == null || word.isEmpty()) return null;
        word = word.toLowerCase();

        int n = board.length;
        boolean[][] visited = new boolean[n][n];
        List<Point> path = new ArrayList<>();

        // the word could start anywhere so try every cell
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                if (pathFrom(r, c, word, 0, visited, path)) {
                    return path;
                }
            }
        }
        return null;
    }

    // checks if the word can be finished starting with letter `index` at (r, c)
    private boolean pathFrom(int r, int c, String word, int index,
                             boolean[][] visited, List<Point> path) {
        int n = board.length;

        // off the board, already used, or the wrong letter means this path doesn't work
        if (r < 0 || r >= n || c < 0 || c >= n) return false;
        if (visited[r][c]) return false;
        if (Character.toLowerCase(board[r][c]) != word.charAt(index)) return false;

        visited[r][c] = true;
        path.add(new Point(r, c));

        // that was the last letter so we found the whole word
        if (index == word.length() - 1) return true;

        // look for the next letter in any of the 8 neighbors
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if ((dr != 0 || dc != 0)
                        && pathFrom(r + dr, c + dc, word, index + 1, visited, path)) {
                    return true;
                }
            }
        }

        // dead end, so take this cell back off and let the caller try somewhere else
        visited[r][c] = false;
        path.remove(path.size() - 1);
        return false;
    }
}
