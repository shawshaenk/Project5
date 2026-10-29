package assignment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

// holds all the words in a trie so checking words and prefixes is super fast for the board search
public class GameDictionary implements BoggleDictionary {
    private Trie dictionary = new Trie();
    // plain list of every word we loaded, the trie is what actually gets searched tho
    ArrayList<String> wordList = new ArrayList<String>();

    @Override
    public boolean contains(String word) {
        // it only counts as a word if we land on a node that's marked as the end of one
        TrieNode node = findNode(word);
        return node != null && node.isWholeWord;
    }

    @Override
    public boolean isPrefix(String prefix) {
        // if we can follow the letters down the trie at all then some word starts with this
        return findNode(prefix) != null;
    }

    // walks down the trie one letter at a time and gives back wherever we end up, or null if we fall off
    private TrieNode findNode(String s) {
        if (s == null) {
            return null;
        }
        TrieNode curr = dictionary.root;
        // everything in the trie is lowercase so capital board letters still match
        for (char c : s.toLowerCase().toCharArray()) {
            // anything that isn't a-z would give a bad index, and it can't be in the trie anyway
            if (c < 'a' || c > 'z') {
                return null;
            }
            // no child for this letter means no word goes this way
            if (curr.children[c - 'a'] == null) {
                return null;
            }
            curr = curr.children[c - 'a'];
        }
        return curr;
    }

    // used when loading so lines with numbers or spaces in them get skipped
    private static boolean isAllLetters(String s) {
        for (char c : s.toCharArray()) {
            if (c < 'a' || c > 'z') {
                return false;
            }
        }
        return true;
    }

    @Override
    public void loadDictionary(String filename) throws IOException {
        String path = filename;
        // try-with-resources closes the file for us even if something goes wrong
        try (BufferedReader bfro = new BufferedReader(new FileReader(path))) {
            String st;
            while ((st = bfro.readLine()) != null) {
                // clean up the line first so stray spaces or caps don't mess up the trie
                st = st.trim().toLowerCase();
                if (st.isEmpty() || !isAllLetters(st)) {
                    continue;
                }
                // every word starts back at the root, otherwise it'd get tacked onto the last word
                TrieNode curr = dictionary.root;
                for (char c : st.toCharArray()) {
                    // make the node for this letter if no other word has made it yet
                    if (curr.children[c - 'a'] == null) {
                        curr.children[c - 'a'] = new TrieNode();
                    }
                    curr = curr.children[c - 'a'];
                }
                // only add to the list the first time so duplicate lines don't show up twice
                if (!curr.isWholeWord) {
                    wordList.add(st);
                }
                // mark the last letter so we know a full word ends here
                curr.isWholeWord = true;
            }
        }
    }

    @Override
    public Iterator<String> iterator() {
        return new TrieIterator();
    }

    // goes through the trie with a stack (basically dfs) and hands back words in abc order
    private class TrieIterator implements Iterator<String> {
        // two stacks that line up, the node and the letters it took to get there
        private final ArrayDeque<TrieNode> nodes = new ArrayDeque<>();
        private final ArrayDeque<String> prefixes = new ArrayDeque<>();
        // we always find the next word ahead of time so hasNext is just a null check
        private String nextWord;

        TrieIterator() {
            // start at the root with no letters yet
            nodes.push(dictionary.root);
            prefixes.push("");
            advance();
        }

        // keep popping nodes until we hit one that ends a word, or run out
        private void advance() {
            nextWord = null;
            while (!nodes.isEmpty()) {
                TrieNode node = nodes.pop();
                String prefix = prefixes.pop();
                // push z down to a so 'a' is on top and comes out first, that's what keeps it sorted
                for (int i = 25; i >= 0; i--) {
                    if (node.children[i] != null) {
                        nodes.push(node.children[i]);
                        prefixes.push(prefix + (char) ('a' + i));
                    }
                }
                // a word comes before any longer word that starts with it, so return it right away
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
            // the iterator rules say to throw this when there's nothing left
            if (nextWord == null) {
                throw new NoSuchElementException();
            }
            // hold onto the word we already found, then go find the one after it
            String result = nextWord;
            advance();
            return result;
        }
    }
}

// just a wrapper around the root node
class Trie {
    TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    // og prefix check, GameDictionary uses findNode now but this still works for lowercase input
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

// one letter spot in the trie
class TrieNode {
    // one slot per letter, index 0 is 'a' and index 25 is 'z'
    TrieNode[] children;
    // true if some word ends right at this node
    boolean isWholeWord;

    TrieNode() {
        children = new TrieNode[26];
        isWholeWord = false;
    }
}
