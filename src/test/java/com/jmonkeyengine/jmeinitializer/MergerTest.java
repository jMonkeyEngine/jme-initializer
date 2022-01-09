package com.jmonkeyengine.jmeinitializer;

import com.jmonkeyengine.jmeinitializer.libraries.Library;
import com.jmonkeyengine.jmeinitializer.libraries.LibraryCategory;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MergerTest {

    @Test
    void mergePath(){
        Merger merger = new Merger("MyGame", "my.excellent.company", List.of(), List.of(), "1", Map.of());
        assertEquals("/src/main/java/my/excellent/company/MyGame.java", merger.mergePath("/src/main/java/[GAME_PACKAGE_FOLDER]/[GAME_NAME].java"));
        assertEquals("path/something.java", merger.mergePath("path/something.java.jmetemplate"));
    }

    @Test
    void mergeText(){
        Merger merger = new Merger("My Game!!", "my.excellent.company", List.of(), List.of(), "1", Map.of());

        String testString = """
                This is a test string for [GAME_NAME_FULL]. Open [GAME_NAME].java to start work.
                Also, the package is [GAME_PACKAGE], fyi
                """;

        String expectedString = """
                This is a test string for My Game!!. Open MyGame.java to start work.
                Also, the package is my.excellent.company, fyi
                """;

        assertEquals(expectedString, new String(merger.mergeFileContents(testString.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
    }

    @Test
    void mergeText_ifStatements(){
        Library testLibraryA = new Library("testLibraryA", "A test library", "groupId", "artifactId", false, LibraryCategory.GENERAL, false, "description");
        Library testLibraryB = new Library("testLibraryB", "B test library", "groupId", "artifactId", false, LibraryCategory.GENERAL, false, "description");

        Merger merger = new Merger("My Game!!", "my.excellent.company", List.of(testLibraryA, testLibraryB), List.of("SINGLEPLATFORM"), "1", Map.of());


        String testString2 = """
                                 Bob
                             alice Bob
                             Sam
                             """;

        String out = testString2.replaceAll("^ *Bob\\R", "Bob");


        String testString = """
                [IF=testLibraryA][IF=testLibraryB]A test library and B test library[/IF=testLibraryB][/IF=testLibraryA]
                
                [IF=testLibraryA]A test library
                multiline[/IF=testLibraryB]
                
                [IF=nonExistent]This should not show[/IF=nonExistent]
                
                [IF=nonExistent]This should not show
                multiline
                [/IF=nonExistent]
                
                [IF=nonExistent]
                [IF=nonExistent2]
                
                This should not show
                
                [/IF=nonExistent2]
                [/IF=nonExistent]
                
                
                [IF=SINGLEPLATFORM]This text uses a profile rather than a library[/IF=SINGLEPLATFORM]
                
                [IF=nonExistent]This should not show[/IF=nonExistent][IF=testLibraryA]But this should[/IF=testLibraryA][IF=nonExistent]This should not show[/IF=nonExistent]
                """;

        String expectedString = """
                A test library and B test library
                
                A test library
                multiline
                
                
                
                
                
                This text uses a profile rather than a library
                
                But this should
                """;

        assertEquals(expectedString, new String(merger.mergeFileContents(testString.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
    }


    @Test
    void pathShouldBeAllowed(){
        Library testLibraryA = new Library("testLibraryA", "A test library", "groupId", "artifactId", false, LibraryCategory.GENERAL, false, "description");
        Library testLibraryB = new Library("testLibraryB", "B test library", "groupId", "artifactId", false, LibraryCategory.GENERAL, false, "description");

        Merger merger = new Merger("My Game!!", "my.excellent.company", List.of(testLibraryA, testLibraryB), List.of("SINGLEPLATFORM"), "1", Map.of());

        assertTrue(merger.pathShouldBeAllowed("/common/or/garden/path"));
        assertTrue(merger.pathShouldBeAllowed("/path/[IF=testLibraryA]/path"));
        assertTrue(merger.pathShouldBeAllowed("/path/something[IF=testLibraryA]/path/[IF=testLibraryB]/path"));
        assertFalse(merger.pathShouldBeAllowed("/path/something[IF=testLibraryA]/path/[IF=testLibraryC]/path"));
        assertFalse(merger.pathShouldBeAllowed("/path/[IF=testLibraryC]/path"));

        assertTrue(merger.pathShouldBeAllowed("/path/[IF=SINGLEPLATFORM]/path"));
    }

    @Test
    void mergePath_librariesAndProfiles(){
        Library testLibraryA = new Library("testLibraryA", "A test library", "groupId", "artifactId", false, LibraryCategory.GENERAL, false, "description");
        Library testLibraryB = new Library("testLibraryB", "B test library", "groupId", "artifactId", false, LibraryCategory.GENERAL, false, "description");

        Merger merger = new Merger("My Game!!", "my.excellent.company", List.of(testLibraryA, testLibraryB), List.of("SINGLEPLATFORM"), "1", Map.of());

        assertEquals("/common/or/garden/path", merger.mergePath("/common/or/garden/path"));
        assertEquals("/path/path", merger.mergePath("/path/[IF=testLibraryA]/path"));
        assertEquals("/path/path", merger.mergePath("/path/[IF=testLibraryA]/[IF=testLibraryB]/path"));
        assertEquals("/path/something/path/path", merger.mergePath("/path/something[IF=testLibraryA]/path/[IF=testLibraryB]/path"));
        assertEquals("/path/path", merger.mergePath("/path/[IF=SINGLEPLATFORM]/path"));
    }


    @Test
    void sanitiseToPackage () {
        assertEquals("mysuggestedpackage", Merger.sanitiseToPackage("mySuggestedPackage£$"));
        assertEquals("co.uk.company", Merger.sanitiseToPackage("co.uk.company"));
        assertEquals("co.uk.company", Merger.sanitiseToPackage("Co.Uk.Company"));
        assertEquals("co.uk.company", Merger.sanitiseToPackage("..co..uk..company.."));
    }

    @Test
    void convertPackageToFolder () {
        assertEquals("mysuggestedpackage", Merger.convertPackageToFolder("mysuggestedpackage"));
        assertEquals("my/suggested/package", Merger.convertPackageToFolder("my.suggested.package"));
    }

    @Test
    void sanitiseToJavaClass () {
        assertEquals("MyGame", Merger.sanitiseToJavaClass("!!!{}@~:@:@"));
        assertEquals("MyAmazingGame", Merger.sanitiseToJavaClass("%My Amazing Game!!"));
        assertEquals("MyGame", Merger.sanitiseToJavaClass(""));
        assertEquals("AlreadyCamelCase", Merger.sanitiseToJavaClass("AlreadyCamelCase"));
        assertEquals("LowerCaseSentence", Merger.sanitiseToJavaClass("lower case sentence"));
        assertEquals("Lowercaseword", Merger.sanitiseToJavaClass("lowercaseword"));
        assertEquals("SentenceWithExcessiveSpace", Merger.sanitiseToJavaClass("  Sentence  with   excessive space  "));
    }
}