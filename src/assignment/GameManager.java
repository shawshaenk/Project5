package assignment;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GameManager implements BoggleGame {
    String[][] board;
    int[] playerScores;
    String cubeFile;
    GameDictionary dictionary;

    @Override
    public int addWord(String word, int player) {
        // TODO - implement me!
        return 0;
    }

    @Override
    public Collection<String> getAllWords() {
        
        return null;
    }

    @Override
    public char[][] getBoard() {
        // TODO - implement me!
        return null;
    }

    @Override
    public List<Point> getLastAddedWord() {
        // TODO - implement me!
        return null;
    }

    @Override
    public int[] getScores() {
        // TODO - implement me!
        return null;
    }

    @Override
    public void newGame(int size, int numPlayers, String cubeFile, BoggleDictionary dict) throws IOException {
        this.board = new String[size][size];
        this.playerScores = new int[numPlayers];
        this.cubeFile = cubeFile;
        if (dict instanceof GameDictionary) {
            this.dictionary = (GameDictionary)dict;
        } else {
            System.err.println("Must be GameDictionary");
        }
    }

    @Override
    public void setGame(char[][] board) {
        // TODO - implement me!
    }

    @Override
    public void setSearchTactic(SearchTactic tactic) {
        // TODO - implement me!

    }

}
