package assignment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class GameDictionary implements BoggleDictionary {
    private Trie dictionary = new Trie();

    @Override
    public boolean contains(String word) {
        // TODO - implement me!
        return false;
    }

    @Override
    public boolean isPrefix(String prefix) {
        // TODO - implement me!
        return false;
    }

    @Override
    public void loadDictionary(String filename) throws IOException {
        String path = filename;
        try (BufferedReader bfro = new BufferedReader(new FileReader(path))) {
            String st;
            TrieNode curr = dictionary.root;
            while ((st = bfro.readLine()) != null) {
                for (char c : st.toCharArray()) {
                    if (curr.children[c - 'a'] == null) {
                        curr.children[c - 'a'] = new TrieNode();
                    }
                    curr = curr.children[c - 'a'];
                }
                wordList.add(st)
                curr.isWholeWord = true;
            }
        }  
    }

    @Override
    public Iterator<String> iterator() {
        return;
    }
}

class Trie {
    TrieNode root;

    public Trie() { 
        root = new TrieNode(); 
    }

    public boolean search(String key)
    {
        TrieNode curr = root;
        for (char c : key.toCharArray()) {
            if (curr.children[c - 'a'] == null) {
                return false;
            }
            curr = curr.children[c - 'a'];
        }
        return curr.isWholeWord;
    }

    public boolean isPrefix(String prefix)
    {
        TrieNode curr = root;
        for (char c : prefix.toCharArray()) {
            if (curr.children[c - 'a'] == null) {
                return false;
            }
            curr = curr.children[c - 'a'];
        }
        return true;
    }
}

class TrieNode {
    TrieNode[] children;
    boolean isWholeWord;

    TrieNode() {
        children = new TrieNode[26];
        isWholeWord = false;
    }
}