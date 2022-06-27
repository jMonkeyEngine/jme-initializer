package com.jmonkeyengine.jmeinitializer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;

import com.jmonkeyengine.jmeinitializer.libraries.Library;
import com.jmonkeyengine.jmeinitializer.libraries.LibraryCategory;
import com.jmonkeyengine.jmeinitializer.libraries.LibraryService;

/**
 * The merger is responsible for replacing merge fields in text documents and paths with their merged data.
 *
 * Merge fields are of the form
 *
 * [PROPERTY]
 *
 * For which the data for the merge field as specified in {@link MergeField}
 *
 * Or (in a file path)
 *
 * [IF=LIBRARY]
 *
 * In which case the file is only included if that library is active
 *
 * or
 *
 * [FRAGMENT=gradleDeployment.fragment]
 *
 * in which case that fragment (aka bit of text) is substituted into the template
 */
public class Merger {

    //the "anything but = is to avoid double ifs merging
    private Pattern mergeIfConditionPattern = Pattern.compile("\\[IF=([^\\]]*)]");

    private Pattern fragmentPattern = Pattern.compile("\\[FRAGMENT=([a-zA-Z0-9./]*)]");

    /**
     * After the allowed ifs have been processed this is used to eliminate forbidden ifs
     */
    private Pattern mergeIfInFileEliminationPattern = Pattern.compile("\\[IF=([^=]*)]");

    private final Map<MergeField, String> mergeData = new HashMap<>();

    Set<String> libraryKeysAndProfilesInUse;

    Function<String, String> fragmentSupplier;

    /**
     * Given the information provided by the user will evaluate merge fields in files and paths.
     *
     * libraryVersions is a map of a string of the form groupId:artifactId -> version
     *
     * additionalProfiles are things that can be used in [IF=] conditions in addition to libraries (things like MULTIPLATFORM)
     *
     * fragmentSupplier supplies other file's contents to add into the merged template
     */
    public Merger(String gameName, String gamePackage, List<Library> librariesRequired, Collection<String> additionalProfiles, String jmeVersion, Map<String,String> libraryVersions, Function<String, String> fragmentSupplier){
        mergeData.put(MergeField.GAME_NAME_FULL, gameName);
        mergeData.put(MergeField.GAME_NAME, sanitiseToJavaClass(gameName));

        String proposedPackage = sanitiseToPackage(gamePackage);
        if (proposedPackage.isBlank()){
            proposedPackage = sanitiseToPackage(mergeData.get(MergeField.GAME_NAME));
        }
        mergeData.put(MergeField.GAME_PACKAGE, proposedPackage);
        mergeData.put(MergeField.GAME_PACKAGE_FOLDER, convertPackageToFolder(mergeData.get(MergeField.GAME_PACKAGE)));
        mergeData.put(MergeField.JME_VERSION, jmeVersion);
        mergeData.put(MergeField.JME_DEPENDENCIES, formJmeRequiredLibrariesMergeField(librariesRequired));
        mergeData.put(MergeField.VR_SPECIALISED_DEPENDENCIES, formPlatformSpecialisedLibrariesMergeField(librariesRequired, libraryVersions, LibraryService.JME_VR));
        mergeData.put(MergeField.ANDROID_SPECIALISED_DEPENDENCIES, formPlatformSpecialisedLibrariesMergeField(librariesRequired, libraryVersions, LibraryService.JME_ANDROID));
        mergeData.put(MergeField.DESKTOP_SPECIALISED_DEPENDENCIES, formPlatformSpecialisedLibrariesMergeField(librariesRequired, libraryVersions, LibraryService.JME_DESKTOP));
        mergeData.put(MergeField.ALL_NON_JME_NON_SPECIALISED_DEPENDENCIES, formNonJmeNonSpecialised(librariesRequired, libraryVersions));
        mergeData.put(MergeField.ALL_NON_JME_DEPENDENCIES, eliminateEmptyLines(mergeData.get(MergeField.VR_SPECIALISED_DEPENDENCIES)+"\n"+mergeData.get(MergeField.ANDROID_SPECIALISED_DEPENDENCIES)+"\n"+mergeData.get(MergeField.DESKTOP_SPECIALISED_DEPENDENCIES)+"\n"+mergeData.get(MergeField.ALL_NON_JME_NON_SPECIALISED_DEPENDENCIES)));
        mergeData.put(MergeField.MAVEN_REPOS, formMavenRepos(librariesRequired));
        mergeData.put(MergeField.CSV_LIBRARIES, csvLibraires(librariesRequired));

        libraryKeysAndProfilesInUse = librariesRequired.stream().map(Library::getKey).collect(Collectors.toSet());
        libraryKeysAndProfilesInUse.addAll(additionalProfiles);

        this.fragmentSupplier = fragmentSupplier;
    }

    public boolean pathShouldBeAllowed(String pathTemplate){
        Matcher matcher = mergeIfConditionPattern.matcher(pathTemplate);

        while( matcher.find() ){
            String requiredLibrary = matcher.group(1);

            if (!libraryConditionStringPasses(requiredLibrary)){
                return false;
            }
        }

        return true;
    }

    public String mergePath (String pathTemplate){
        String path = pathTemplate;
        for(Map.Entry<MergeField, String> merges : mergeData.entrySet()){
            path = path.replace(merges.getKey().getMergeFieldInText(), merges.getValue());
        }
        //any "ifs" are removed from the path (they should have already been used to assess if the file should be included
        path = path.replaceAll("\\[IF=([^=]*)]", "");
        path = path.replaceAll("//+", "/"); //if the if is the entirety of the folder then redundant folder is collapsed
        path = path.replace(".jmetemplate", "");
        path = path.replaceAll("^/", ""); //empty conditional folders at the start of the path can cause a preceding / which makes the zips "all weird". This removes that
        path = path.replace("[DOT]", ".");
        return path;
    }

    /**
     * Treats the byte array as a UTF-8 String and merges it
     */
    public byte[] mergeFileContents(byte[] fileContents){
        String fileContentsAsString = new String(fileContents, StandardCharsets.UTF_8);

        //first add any fragments (as the fragments may themselves have merges or ifs in this
        Matcher matcher = fragmentPattern.matcher(fileContentsAsString);
        while(matcher.find()){
            String fragmentFile = matcher.group(1);
            String fragmentContent = fragmentSupplier.apply(fragmentFile);

            if (fragmentContent == null){
                throw new RuntimeException("Missing fragment " + fragmentFile);
            }
            fileContentsAsString = fileContentsAsString.replace(matcher.group(0), fragmentContent);
            matcher = fragmentPattern.matcher(fileContentsAsString);
        }

        for(Map.Entry<MergeField, String> merges : mergeData.entrySet()){
            String mergeKey = merges.getKey().getMergeFieldInText();
            fileContentsAsString = fileContentsAsString.lines()
                    .map( line -> {
                        if (line.contains(mergeKey)){
                            if (StringUtils.countMatches(line, mergeKey) == 1 ){
                                //try to match the indentation of the merge field (for multiline merge fields)
                                int indexation = line.indexOf(mergeKey);
                                String baseMergeData = merges.getValue();
                                String indentationString = " ".repeat(indexation);
                                String indentedMergeData = baseMergeData.lines().map(l -> indentationString + l ).collect(Collectors.joining("\n"));
                                return line
                                        .replace(indentationString+mergeKey, indentedMergeData)
                                        .replace(mergeKey, baseMergeData) //this is a catch-all in case the merge field wasn't indented but prefixed by normal text
                                        ;
                            }else{
                                //to complicated, hopefully just a single line merge field
                                return line.replace(mergeKey, merges.getValue());
                            }
                        }else{
                            return line;
                        }
                    }).collect(Collectors.joining("\n"));

        }

        /*
         * Now run the if statement merges. This is achieved by first removing all [IF] and [/IF] blocks for everything
         * that should be included, then eliminating everything thats left within an if block
         */
        for(String validProfile : libraryKeysAndProfilesInUse){
            fileContentsAsString = fileContentsAsString.replace("[IF=" + validProfile + "]", "");
            fileContentsAsString = fileContentsAsString.replace("[/IF=" + validProfile + "]", "");
        }

        fileContentsAsString = processIfStatements(fileContentsAsString);

        //always end with a new character because that will make git changes better in the future (plus the test want that)
        fileContentsAsString = fileContentsAsString+"\n";

        return fileContentsAsString.getBytes(StandardCharsets.UTF_8);
    }

    private String processIfStatements(String fileContentsAsString){
        Set<String> foundIfConditions = new HashSet<>();
        Matcher ifMatcher = mergeIfConditionPattern.matcher(fileContentsAsString);
        while(ifMatcher.find()){
            foundIfConditions.add(ifMatcher.group(1));
        }

        Set<String> failingIfConditions = new HashSet<>();
        for(String foundIfCondition : foundIfConditions){
            boolean ifConditionPasses = libraryConditionStringPasses(foundIfCondition);
            if(ifConditionPasses){
                fileContentsAsString = fileContentsAsString.replace("[IF=" + foundIfCondition + "]", "");
                fileContentsAsString = fileContentsAsString.replace("[/IF=" + foundIfCondition + "]", "");
            }else{
                failingIfConditions.add(foundIfCondition);
            }
        }

        //eliminate any remaining ifs and their contents
        //I suspect a single really advanced regex could do the below in one go, but its a painful double "not this" so I've gone for this probably less efficient approach

        for(int i = 0; i < 2; i++){
            for(String remainingIf : failingIfConditions){
                //this 2 step process of first eliminating down to a _eliminated_ string and then removing that is to get any whitespace eliminated nicely
                String regex = "\\[IF=" + Pattern.quote(remainingIf) + "]((?!IF=).)*\\[/IF=" + Pattern.quote(remainingIf) + "]";
                fileContentsAsString = Pattern.compile(regex, Pattern.DOTALL).matcher(fileContentsAsString).replaceAll("_eliminated_");
            }
            //kill including the new line after the closure, if thats the only thing on the line
            fileContentsAsString = fileContentsAsString.replaceAll("\\R *_eliminated_ *\\R", "\n");
            //then get rid of anything thats on a line with allowed text
            fileContentsAsString = fileContentsAsString.replace("_eliminated_", "");
        }
        return fileContentsAsString;
    }

    private boolean libraryConditionStringPasses(String condition){
        condition = condition.replace("_OR_", "|"); //| is not allowed in paths so accept _OR_ as well
        String[] libraries = condition.split("\\|");
        for(String library: libraries){
            if (libraryKeysAndProfilesInUse.contains(library)){
                return true;
            }
        }
        return false;
    }

    protected static String formJmeRequiredLibrariesMergeField(List<Library> librariesRequired){
        return librariesRequired.stream()
                .filter(Library::isUsesJmeVersion)
                .filter(l -> l.getCategory() != LibraryCategory.JME_PLATFORM) //platforms are hard coded into the templates to better support multimodule
                .flatMap(l ->
                    l.getArtifacts().stream()
                            .map(artifact -> "implementation '" + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":' + " + artifact.getPinVersionOpt().map(pv -> "'" + pv + "'").orElse( "jmonkeyengineVersion") )
                ).collect(Collectors.joining("\n"));

    }

    protected static String formNonJmeNonSpecialised(List<Library> librariesRequired, Map<String,String> libraryVersions){
        return librariesRequired.stream()
                .filter(l -> !l.isUsesJmeVersion())
                .filter(l -> l.getSpecialisedToPlatforms().isEmpty())
                .flatMap(l ->
                        l.getArtifacts().stream()
                                .map(artifact -> {
                                    String mavenCoordinate = artifact.getGroupId() + ":" + artifact.getArtifactId();
                                    return "implementation '" + mavenCoordinate + ":" + artifact.getPinVersionOpt().orElse(libraryVersions.getOrDefault(mavenCoordinate, artifact.getFallbackVersion()))  + "'";
                                })
                ).collect(Collectors.joining("\n"));
    }

    protected static String formMavenRepos(List<Library> librariesRequired){
        Set<String> mavenRepos = new HashSet<>();
        mavenRepos.add("mavenCentral()");
        mavenRepos.add("mavenLocal()");

        librariesRequired.forEach(l -> mavenRepos.addAll(l.getAdditionalMavenRepos()));

        return mavenRepos.stream()
                .sorted() //sorting them makes testing this easier
                .collect(Collectors.joining("\n"));
    }

    public static String csvLibraires(List<Library> librariesRequired){
        StringBuilder sb=new StringBuilder();
        for(Library l:librariesRequired){
            String key=l.getKey();
            if(sb.length()!=0)sb.append(",");
            try{
                sb.append(URLEncoder.encode(key,"UTF-8"));
            }catch(UnsupportedEncodingException e){
                e.printStackTrace();
            }            
        }
        
        return sb.toString();

    }

    protected static String formPlatformSpecialisedLibrariesMergeField(List<Library> librariesRequired, Map<String,String> libraryVersions, String platform){
        return librariesRequired.stream()
                .filter(l -> !l.isUsesJmeVersion())
                .filter(l -> l.getSpecialisedToPlatforms().contains(platform))
                .flatMap(l ->
                        l.getArtifacts().stream()
                                .map(artifact -> {
                                    String mavenCoordinate = artifact.getGroupId() + ":" + artifact.getArtifactId();
                                    return "implementation '" + mavenCoordinate + ":" + artifact.getPinVersionOpt().orElse(libraryVersions.getOrDefault(mavenCoordinate, artifact.getFallbackVersion()))  + "'";
                                })
                ).collect(Collectors.joining("\n"));
    }

    protected static String sanitiseToPackage(String proposedPackage){
        proposedPackage = proposedPackage.toLowerCase();
        proposedPackage = proposedPackage.replace(" ", ".");
        proposedPackage = proposedPackage.replaceAll("\\.\\.+", "."); //remove double dots or similar
        proposedPackage = proposedPackage.replaceAll("\\.$", ""); //remove trailing dots
        proposedPackage = proposedPackage.replaceAll("^\\.", ""); //remove prefix dots
        proposedPackage = proposedPackage.replaceAll("[^a-z.]", "");//remove illegal characters

        return proposedPackage;
    }

    protected static String convertPackageToFolder(String fullPackage){
        fullPackage = fullPackage.replace(".", "/");
        return fullPackage;
    }

    /**
     * Given a string that could be used as a java class name sanitises it so its a standard java
     * class name
     */
    protected static String sanitiseToJavaClass(String proposedName){
        //remove illegal characters (possibly a bit overaggressive, but whatever
        proposedName = proposedName.replaceAll("[^a-zA-Z ]", "");

        //in case the regex killed the whole string, produce a fall back
        if (proposedName.isBlank()){
            proposedName = "MyGame";
        }
        //camelcase a sentence
        if (proposedName.contains(" ") || Character.isLowerCase(proposedName.charAt(0))) {
            proposedName = CaseUtils.toCamelCase(proposedName, true, ' ', '_');
        }
        return proposedName;
    }

    public static String eliminateEmptyLines(String input){
        return input.lines().filter(l -> !l.isBlank()).collect(Collectors.joining("\n"));
    }

}
