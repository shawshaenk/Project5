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

@DisplayName("GameDictionary")
public class GameDictionaryTest {

    private static final List<String> MINI_LEXICON = List.of(
            "river", "rive", "rivet", "riverbed", "stone", "stony", "sto", "owl");

    private GameDictionary miniDict;

    private static String writeTemp(List<String> lines) throws IOException {
        Path file = Files.createTempFile("dict-fixture", ".txt");
        Files.write(file, lines);
        file.toFile().deleteOnExit();
        return file.toString();
    }

    private static Set<String> drain(Iterator<String> iter) {
        Set<String> result = new HashSet<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        return result;
    }

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
        assertTrue(miniDict.contains("RiVeT"));
        assertFalse(miniDict.contains("rivers"));
        assertFalse(miniDict.contains("riv"));
        assertFalse(miniDict.contains("riverb"));
    }

    @Test
    @DisplayName("contains rejects null, empty, and non-letters")
    void containsRejectsJunk() {
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
        assertTrue(miniDict.isPrefix("riverb"));
        assertTrue(miniDict.isPrefix("OwL"));
        assertTrue(miniDict.isPrefix("stony"));
    }

    @Test
    @DisplayName("isPrefix rejects unknown starts and junk")
    void prefixMisses() {
        assertFalse(miniDict.isPrefix("q"));
        assertFalse(miniDict.isPrefix("owls"));
        assertFalse(miniDict.isPrefix("r i"));
        assertFalse(miniDict.isPrefix(null));
    }

    @Test
    @DisplayName("iteration yields each word exactly once")
    void iterationCoversEverything() {
        List<String> produced = new ArrayList<>();
        for (String word : miniDict) {
            produced.add(word);
        }
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
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    @DisplayName("repeated hasNext does not skip, and next throws when done")
    void iteratorContract() {
        Iterator<String> iter = miniDict.iterator();
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals(MINI_LEXICON.size(), drain(iter).size());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    @DisplayName("loading normalizes case, trims, and skips bad lines")
    void loadingCleansLines() throws IOException {
        GameDictionary messy = new GameDictionary();
        messy.loadDictionary(writeTemp(List.of("Ocean", "ocean", "", "OCEAN", "   tide   ", "wave5")));
        assertEquals(Set.of("ocean", "tide"), drain(messy.iterator()));
    }

    @Test
    @DisplayName("a second load adds to the first")
    void secondLoadAccumulates() throws IOException {
        miniDict.loadDictionary(writeTemp(List.of("cat", "cats")));
        assertTrue(miniDict.contains("cats"));
        assertTrue(miniDict.contains("river"));
        assertEquals(MINI_LEXICON.size() + 2, drain(miniDict.iterator()).size());
    }

    @Test
    @DisplayName("missing file throws IOException")
    void missingFile() {
        String path = new File("no_such_file_words_98765.txt").getAbsolutePath();
        assertThrows(IOException.class, () -> new GameDictionary().loadDictionary(path));
    }
}