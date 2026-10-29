package assignment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

// tests for the trie dictionary on its own, no board involved
@DisplayName("GameDictionary")
public class GameDictionaryTest {

    // small word list with a bunch of words that share starts, so prefix stuff actually gets tested
    private static final List<String> MINI_LEXICON = List.of(
            "river", "rive", "rivet", "riverbed", "stone", "stony", "sto", "owl");

    private GameDictionary miniDict;

    // loadDictionary wants a filename, so write the words to a temp file and hand back its path
    private static String writeTemp(List<String> lines) throws IOException {
        Path file = Files.createTempFile("dict-fixture", ".txt");
        Files.write(file, lines);
        file.toFile().deleteOnExit();
        return file.toString();
    }

    // runs the iterator all the way through and collects what it gave us
    private static Set<String> drain(Iterator<String> iter) {
        Set<String> result = new HashSet<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        return result;
    }

    // fresh dictionary before every test so one test can't mess up another
    @BeforeEach
    void init() throws IOException {
        miniDict = new GameDictionary();
        miniDict.loadDictionary(writeTemp(MINI_LEXICON));
    }

    @Test
    @DisplayName("contains finds exact words only")
    void containsExactWords() {
        assertTrue(miniDict.contains("river"));
        assertTrue(miniDict.contains("riverbed"));
        // caps shouldn't matter
        assertTrue(miniDict.contains("RiVeT"));
        // these are close to real words but aren't actually in the list
        assertFalse(miniDict.contains("rivers"));
        assertFalse(miniDict.contains("riv"));
        assertFalse(miniDict.contains("riverb"));
    }

    @Test
    @DisplayName("contains rejects null, empty, and non-letters")
    void containsRejectsJunk() {
        // none of these should crash, they should just say no
        assertFalse(miniDict.contains(null));
        assertFalse(miniDict.contains(""));
        assertFalse(miniDict.contains("riv3r"));
        assertFalse(miniDict.contains("ri ver"));
    }

    @Test
    @DisplayName("isPrefix accepts word starts and whole words")
    void prefixMatches() {
        assertTrue(miniDict.isPrefix("r"));
        assertTrue(miniDict.isPrefix("rive"));
        // "riverb" isn't a word but "riverbed" starts with it
        assertTrue(miniDict.isPrefix("riverb"));
        assertTrue(miniDict.isPrefix("OwL"));
        // a whole word counts as a prefix of itself
        assertTrue(miniDict.isPrefix("stony"));
    }

    @Test
    @DisplayName("isPrefix rejects unknown starts and junk")
    void prefixMisses() {
        assertFalse(miniDict.isPrefix("q"));
        // goes one letter past the end of "owl"
        assertFalse(miniDict.isPrefix("owls"));
        assertFalse(miniDict.isPrefix("r i"));
        assertFalse(miniDict.isPrefix(null));
    }

    @Test
    @DisplayName("iteration yields each word exactly once")
    void iterationCoversEverything() {
        // use a for-each loop since that's how the game actually goes through the dictionary
        List<String> produced = new ArrayList<>();
        for (String word : miniDict) {
            produced.add(word);
        }
        // same size and same words means nothing got skipped or repeated
        assertEquals(MINI_LEXICON.size(), produced.size());
        assertEquals(new HashSet<>(MINI_LEXICON), new HashSet<>(produced));
    }

    @Test
    @DisplayName("empty dictionary has nothing to iterate")
    void emptyDictionary() throws IOException {
        GameDictionary blank = new GameDictionary();
        blank.loadDictionary(writeTemp(List.of()));
        assertFalse(blank.contains("a"));
        Iterator<String> iter = blank.iterator();
        assertFalse(iter.hasNext());
        // next on an empty iterator is supposed to throw, not return null
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    @DisplayName("repeated hasNext does not skip, and next throws when done")
    void iteratorContract() {
        Iterator<String> iter = miniDict.iterator();
        // calling hasNext a bunch shouldn't move the iterator forward
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals(MINI_LEXICON.size(), drain(iter).size());
        // once it's used up it should stay used up
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    @DisplayName("loading normalizes case, trims, and skips bad lines")
    void loadingCleansLines() throws IOException {
        // three versions of ocean should turn into one, the blank line and wave5 get skipped
        GameDictionary messy = new GameDictionary();
        messy.loadDictionary(writeTemp(List.of("Ocean", "ocean", "", "OCEAN", "   tide   ", "wave5")));
        assertEquals(Set.of("ocean", "tide"), drain(messy.iterator()));
    }

    @Test
    @DisplayName("a second load adds to the first")
    void secondLoadAccumulates() throws IOException {
        // loading again should add words, not wipe out the old ones
        miniDict.loadDictionary(writeTemp(List.of("cat", "cats")));
        assertTrue(miniDict.contains("cats"));
        assertTrue(miniDict.contains("river"));
        assertEquals(MINI_LEXICON.size() + 2, drain(miniDict.iterator()).size());
    }

    @Test
    @DisplayName("missing file throws IOException")
    void missingFile() {
        // weird name so there's no way this file actually exists
        String path = new File("no_such_file_words_98765.txt").getAbsolutePath();
        assertThrows(IOException.class, () -> new GameDictionary().loadDictionary(path));
    }
}
