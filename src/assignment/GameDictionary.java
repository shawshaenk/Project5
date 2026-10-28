package assignment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class GameDictionary implements BoggleDictionary {
    Trie dictionary = new Trie();
    ArrayList<String> wordList = new ArrayList<String>();

    @Override
    public boolean contains(String word) {
        TrieNode curr = dictionary.root;
        for (char c : word.toLowerCase().toCharArray()) {
            if (curr.children[c - 'a'] == null) {
                return false;
            }
            curr = curr.children[c - 'a'];
        }

        return curr.isWholeWord;
    }

    @Override
    public boolean isPrefix(String prefix) {
        TrieNode curr = dictionary.root;
        for (char c : prefix.toLowerCase().toCharArray()) {
            if (curr.children[c - 'a'] == null) {
                return false;
            }
            curr = curr.children[c - 'a'];
        }
        
        return true;
    }

    @Override
    public void loadDictionary(String filename) throws IOException {
        String path = filename;
        try (BufferedReader bfro = new BufferedReader(new FileReader(path))) {
            String st;
            while ((st = bfro.readLine()) != null) {
                st = st.trim().toLowerCase();
                if (st.isEmpty()) {
                    continue;
                }
                TrieNode curr = dictionary.root;
                for (char c : st.toCharArray()) {
                    if (curr.children[c - 'a'] == null) {
                        curr.children[c - 'a'] = new TrieNode();
                    }
                    curr = curr.children[c - 'a'];
                }
                wordList.add(st);
                curr.isWholeWord = true;
            }
        }  
    }

    @Override
    public Iterator<String> iterator() {
        return new TrieIterator();
    }

    private class TrieIterator implements Iterator<String> {
        private final ArrayDeque<TrieNode> nodes = new ArrayDeque<>();
        private final ArrayDeque<String> prefixes = new ArrayDeque<>();
        private String nextWord;

        TrieIterator() {
            nodes.push(dictionary.root);
            prefixes.push("");
            advance();
        }

        private void advance() {
            nextWord = null;
            while (!nodes.isEmpty()) {
                TrieNode node = nodes.pop();
                String prefix = prefixes.pop();
                for (int i = 25; i >= 0; i--) {
                    if (node.children[i] != null) {
                        nodes.push(node.children[i]);
                        prefixes.push(prefix + (char) ('a' + i));
                    }
                }
                if (node.isWholeWord) {
                    nextWord = prefix;
                    return;
                }
            }
        }

        public boolean hasNext() {
            return nextWord != null;
        }

        public String next() {
            if (nextWord == null) {
                throw new NoSuchElementException();
            }
            String result = nextWord;
            advance();
            return result;
        }
    }
}

class Trie {
    TrieNode root;

    public Trie() { 
        root = new TrieNode(); 
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
