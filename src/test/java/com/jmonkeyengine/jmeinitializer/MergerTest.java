package com.jmonkeyengine.jmeinitializer;

import com.jmonkeyengine.jmeinitializer.libraries.Artifact;
import com.jmonkeyengine.jmeinitializer.libraries.Library;
import com.jmonkeyengine.jmeinitializer.libraries.LibraryCategory;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class MergerTest {

    private Function<String,String> noOpFragmentSupplier = name -> null;

    @Test
    void mergePath(){
        Merger merger = new Merger("MyGame", "my.excellent.company", List.of(), List.of(), List.of(), "1", Map.of(), noOpFragmentSupplier);
        assertEquals("src/main/java/my/excellent/company/mygame/MyGame.java", merger.mergePath("src/main/java/[GAME_PACKAGE_FOLDER]/[GAME_NAME].java"));
        assertEquals("path/something.java", merger.mergePath("path/something.java.jmetemplate"));
        assertEquals(".gitignore",  merger.mergePath("[DOT]gitignore"));

    }

    @Test
    void mergeText(){
        Merger merger = new Merger("My Game!!", "my.excellent.company", List.of(), List.of(), List.of(), "1", Map.of(), noOpFragmentSupplier);

        String testString = """
                This is a test string for [GAME_NAME_FULL]. Open [GAME_NAME].java to start work.
                Also, the package is [GAME_PACKAGE], fyi
                """;

        String expectedString = """
                This is a test string for My Game!!. Open MyGame.java to start work.
                Also, the package is my.excellent.company.mygame, fyi
                """;

        assertEquals(expectedString, new String(merger.mergeFileContents(testString.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
    }

    @Test
    void mergeText_ifStatements(){
        Library testLibraryA = Library.builder("testLibraryA", "A test library",  LibraryCategory.GENERAL, "description").build();

        Library testLibraryB =  Library.builder("testLibraryB", "B test library",  LibraryCategory.GENERAL, "description").build();

        Merger merger = new Merger("My Game!!", "my.excellent.company", List.of(), List.of(testLibraryA, testLibraryB), List.of("SINGLEPLATFORM"), "1", Map.of(), noOpFragmentSupplier);

        String testString2 = """
                                 Bob
                             alice Bob
                             Sam
                             """;

        String testString = """
                [IF=testLibraryA][IF=testLibraryB]A test library and B test library[/IF=testLibraryB][/IF=testLibraryA]
                
                [IF=testLibraryA]A test library
                multiline[/IF=testLibraryA]
                
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
    void mergeText_ifStatementsWithOrs(){
        Library testLibraryA = Library.builder("testLibraryA", "A test library",  LibraryCategory.GENERAL, "description").build();

        Library testLibraryB =  Library.builder("testLibraryB", "B test library",  LibraryCategory.GENERAL, "description").build();

        Merger merger = new Merger("My Game!!", "my.excellent.company", List.of(), List.of(testLibraryA, testLibraryB), List.of("SINGLEPLATFORM"), "1", Map.of(), noOpFragmentSupplier);

        String testString =
            """
                [IF=testLibraryA|nonExistent]This should show A[/IF=testLibraryA|nonExistent]
                [IF=testLibraryA|testLibraryB]This should show B[/IF=testLibraryA|testLibraryB]
                [IF=nonExistentA|nonExistentB]This should not show[/IF=nonExistentA|nonExistentB]
                Normal text
            """;
        String expectedString =
            """
                This should show A
                This should show B
                Normal text
            """;
        assertEquals(expectedString, new String(merger.mergeFileContents(testString.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
    }

    @Test
    void mergeText_notStatements(){
        Library testLibraryA = Library.builder("testLibraryA", "A test library",  LibraryCategory.GENERAL, "description").build();

        Library testLibraryB =  Library.builder("testLibraryB", "B test library",  LibraryCategory.GENERAL, "description").build();

        Merger merger = new Merger("My Game!!", "my.excellent.company", List.of(), List.of(testLibraryA, testLibraryB), List.of("SINGLEPLATFORM"), "1", Map.of(), noOpFragmentSupplier);

        String testString =
                """
                    Normal Text
                    [NOT=testLibraryA]This should not show A[/NOT=testLibraryA]
                    [NOT=nonExistentC]This should show C[/NOT=nonExistentC]
                    [NOT=testLibraryB]This should not show B[/NOT=testLibraryB]
                    Normal text
                """;
        String expectedString =
                """
                    Normal Text
                    This should show C
                    Normal text
                """;
        assertEquals(expectedString, new String(merger.mergeFileContents(testString.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
    }

    @Test
    void pathShouldBeAllowed(){
        Library testLibraryA = Library.builder("testLibraryA", "A test library",  LibraryCategory.GENERAL, "description").build();
        Library testLibraryB = Library.builder("testLibraryB", "B test library",  LibraryCategory.GENERAL, "description").build();

        Merger merger = new Merger("My Game!!", "my.excellent.company", List.of(), List.of(testLibraryA, testLibraryB), List.of("SINGLEPLATFORM"), "1", Map.of(), noOpFragmentSupplier);

        assertTrue(merger.pathShouldBeAllowed("common/or/garden/path"));
        assertTrue(merger.pathShouldBeAllowed("path/[IF=testLibraryA]/path"));
        assertTrue(merger.pathShouldBeAllowed("path/[NOT=testLibraryC]/path"));
        assertFalse(merger.pathShouldBeAllowed("path/[NOT=testLibraryA]/path"));
        assertTrue(merger.pathShouldBeAllowed("path/something[IF=testLibraryA]/path/[IF=testLibraryB]/path"));
        assertFalse(merger.pathShouldBeAllowed("path/something[IF=testLibraryA]/path/[IF=testLibraryC]/path"));
        assertFalse(merger.pathShouldBeAllowed("path/[IF=testLibraryC]/path"));

        assertTrue(merger.pathShouldBeAllowed("path/[IF=SINGLEPLATFORM]/path"));

        assertTrue(merger.pathShouldBeAllowed("[IF=testLibraryA][IF=testLibraryB]/src/main/java/[GAME_PACKAGE_FOLDER]/[GAME_NAME].java"));
        assertTrue(merger.pathShouldBeAllowed("[IF=testLibraryA][NOT=testLibraryC][NOT=testLibraryD]/src/main/java/[GAME_PACKAGE_FOLDER]/[GAME_NAME].java"));
    }

    @Test
    void mergePath_librariesAndProfiles(){
        Library testLibraryA = Library.builder("testLibraryA", "A test library",  LibraryCategory.GENERAL, "description").build();
        Library testLibraryB = Library.builder("testLibraryB", "B test library",  LibraryCategory.GENERAL, "description").build();

        Merger merger = new Merger("My Game!!", "my.excellent.company", List.of(), List.of(testLibraryA, testLibraryB), List.of("SINGLEPLATFORM"), "1", Map.of(), noOpFragmentSupplier);

        assertEquals("common/or/garden/path", merger.mergePath("common/or/garden/path"));
        assertEquals("path/path", merger.mergePath("path/[IF=testLibraryA]/path"));
        assertEquals("path/path", merger.mergePath("path/[IF=testLibraryA]/[IF=testLibraryB]/path"));
        assertEquals("path/something/path/path", merger.mergePath("/path/something[IF=testLibraryA]/path/[IF=testLibraryB]/path"));
        assertEquals("path/path", merger.mergePath("path/[IF=SINGLEPLATFORM]/path"));
        assertEquals("path/path", merger.mergePath("path/[IF=SINGLEPLATFORM][IF=JME_DESKTOP]/path"));
        assertEquals("path1/path2", merger.mergePath("[IF=SINGLEPLATFORM][IF=JME_DESKTOP]/path1/path2"));
        assertEquals("path1/path2", merger.mergePath("[NOT=SINGLEPLATFORM][IF=JME_DESKTOP]/path1/path2[NOT=TAMARIN]"));
    }


    @Test
    void sanitiseToPackage () {
        assertEquals("mysuggestedpackage.game", Merger.sanitiseToPackage("mySuggestedPackage£$", "Game"));
        assertEquals("co.uk.company.game", Merger.sanitiseToPackage("co.uk.company","Game"));
        assertEquals("co.uk.company.game", Merger.sanitiseToPackage("Co.Uk.Company", "Game"));
        assertEquals("co.uk.company.game", Merger.sanitiseToPackage("..co..uk..company..","Game"));
        assertEquals("co.uk.company.game", Merger.sanitiseToPackage("co.uk.company.game","Game"));
        assertEquals("co.uk.company.game.mygame", Merger.sanitiseToPackage("co.uk.company.game","My Game"));
        assertEquals("co.uk.company.mygame", Merger.sanitiseToPackage("co.uk.company.mygame","My Game"));
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

    @Test
    void artifactsAddedCorrectly () {
        String testString = """
                                [ALL_NON_JME_NON_SPECIALISED_DEPENDENCIES]
                            """;

        String expectedString = """
                                    api libs.arta
                                    api libs.artb
                                """;

        Library testLibraryA = Library.builder("testLibraryA", "A test library",  LibraryCategory.GENERAL, "description").build();
        Artifact artifactA = new Artifact();
        artifactA.setGroupId("group");
        artifactA.setArtifactId("artA");
        artifactA.setPinVersion("1.2.3");
        Artifact artifactB = new Artifact();
        artifactB.setGroupId("group");
        artifactB.setArtifactId("artB");
        artifactB.setFallbackVersion("1.2.3");
        testLibraryA.setArtifacts(List.of(artifactA, artifactB));

        Merger merger = new Merger("", "", List.of(), List.of(testLibraryA), List.of("SINGLEPLATFORM"), "1", Map.of("group:artA", "1.2.4", "group:artB", "1.2.4"), noOpFragmentSupplier);
        assertEquals(expectedString.trim(), new String(merger.mergeFileContents(testString.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8).trim());
    }

    @Test
    void additionalRepositoriesAddedCorrectly() {
        String testString = """
                            buildscript {
                                repositories {
                                    [MAVEN_REPOS]
                                }
                            }
                            """;
        String expectedString = """
                            buildscript {
                                repositories {
                                    google()
                                    mavenCentral()
                                    mavenLocal()
                                    someRandomRepo()
                                }
                            }
                            """;

        Library testLibraryA = Library.builder("testLibraryA", "A test library",  LibraryCategory.GENERAL, "description").build();
        testLibraryA.setAdditionalMavenRepos(List.of("someRandomRepo()"));

        Merger merger = new Merger("", "", List.of(), List.of(testLibraryA), List.of("SINGLEPLATFORM"), "1", Map.of(), noOpFragmentSupplier);
        assertEquals(expectedString, new String(merger.mergeFileContents(testString.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
    }


    @Test
    void fragmentsMergedCorrectly() {
        Library testLibraryA = Library.builder("testLibraryA", "A test library",  LibraryCategory.GENERAL, "description").build();

        Function<String,String> fragmentSupplier = name -> {
            if (name.equals("fragA.fragment")){
                return """
                       [IF=testLibraryA]If works[/IF=testLibraryA]
                        """;
            }else if(name.equals("fragB.fragment")){
                return """
                       [GAME_NAME_FULL]
                        """;
            }else{
                return null;
            }
        };

        String testString = """
                            [FRAGMENT=fragA.fragment]
                            boo
                            [FRAGMENT=fragB.fragment]
                            """;
        String expectedString = """
                            If works
                            
                            boo
                            MyGame
                            """;
        Merger merger = new Merger("MyGame", "", List.of(), List.of(testLibraryA), List.of("SINGLEPLATFORM"), "1", Map.of(), fragmentSupplier);
        assertEquals(expectedString, new String(merger.mergeFileContents(testString.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
    }

    @Test
    void testEliminateEmptyLines(){
        String testString = """
                            test
                            
                            test""";

        String expectedString = """
                            test
                            test""";

        assertEquals(expectedString, Merger.eliminateEmptyLines(testString));
    }
}