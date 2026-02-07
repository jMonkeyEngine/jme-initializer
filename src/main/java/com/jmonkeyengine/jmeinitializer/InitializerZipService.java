package com.jmonkeyengine.jmeinitializer;

import com.jmonkeyengine.jmeinitializer.libraries.JmePlatform;
import com.jmonkeyengine.jmeinitializer.libraries.Library;
import com.jmonkeyengine.jmeinitializer.libraries.LibraryCategory;
import com.jmonkeyengine.jmeinitializer.libraries.LibraryService;
import com.jmonkeyengine.jmeinitializer.versions.VersionService;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Given the user's choices produces a ByteArrayOutputStream that is a zip containing a starter project with those
 * choices
 */
@Service
@Log4j2
public class InitializerZipService {

    private static String templatePath = "/jmetemplate";
    private static Pattern unwantedPathRegex = Pattern.compile(".*" + Pattern.quote(templatePath) + "\\/");
    /**
     * Most file types are treated as text and scanned for merge fields, but for binary files thats "not a great idea"
     * those file types that shouldn't be scanned are listed here and they are included in the bundle as is
     */
    private final static Set<String> fileExtensionsToTreatAsBlobs = Set.of(".jar");

    private final VersionService versionService;

    private final LibraryService libraryService;
    
    /**
     * If true, then if libaries are requested that are inappropriate (or not available) an exception is thrown
     */
    @Setter
    private boolean strictValidate = false;

    public InitializerZipService (VersionService versionService, LibraryService libraryService) {
        this.versionService = versionService;
        this.libraryService = libraryService;
    }

    public Map<String, String> produceGradleFilePreview(String gameName, String packageName, List<String> requiredLibraryKeys, List<String> deploymentOptionKeys ){
        Collection<JmePlatform> requiredPlatforms = parsePlatforms(requiredLibraryKeys);
        Collection<Library> requiredLibraries = eliminateLibrariesOnUnsupportedPlatforms(requiredPlatforms, parseLibraryKeys(requiredLibraryKeys));
        Merger merger = new Merger(gameName, packageName, requiredPlatforms, requiredLibraries,  calculateAdditionalProfiles(requiredLibraries, deploymentOptionKeys), versionService.getJmeVersion(), versionService.getVersionCache(), new FragmentFetcher() );

        Map<String, String> gradleFiles = new HashMap<>();

        //find and merge all the build.gradle files (there may be more than one in a multimodule project
        for( Map.Entry<String,byte[]> templateFile : getRawTemplatePaths().entrySet()){
            if (merger.pathShouldBeAllowed(templateFile.getKey())) {
                String mergedPath = merger.mergePath(templateFile.getKey());
                if (mergedPath.endsWith("build.gradle")){
                    gradleFiles.put(mergedPath, new String(merger.mergeFileContents(templateFile.getValue()), StandardCharsets.UTF_8));
                }
            }
        }

        return gradleFiles;
    }

    public ByteArrayOutputStream produceZipInMemory(String gameName, String packageName, List<String> requiredLibraryKeys, List<String> deploymentOptionKeys ){

        Map<String,byte[]> baseTemplate = produceTemplate(gameName, packageName, requiredLibraryKeys, deploymentOptionKeys);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try(ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(byteArrayOutputStream)) {
            for( Map.Entry<String,byte[]> templateFile : baseTemplate.entrySet()){
                ZipArchiveEntry entry = new ZipArchiveEntry(templateFile.getKey());
                if (templateFile.getKey().endsWith(".sh") || templateFile.getKey().endsWith("gradlew")){
                    //noinspection OctalInteger this is traditionally shown in octal, its a chmod argument
                    entry.setUnixMode(0755); //set sh files as executable in the resultant zip archive
                }
                zipOutputStream.putArchiveEntry(entry);
                zipOutputStream.write(templateFile.getValue());

                zipOutputStream.closeArchiveEntry();
            }
        }catch(IOException ioe) {
            throw new RuntimeException("Exception while forming zip", ioe);
        }
        return byteArrayOutputStream;
    }
  
    /**
     * @param gameName the game name
     * @param packageName the package used by the game classes
     * @param requiredLibraryKeys the libraries required by the game
     * @return a map of paths to the file contents that should be created at that path
     */
    public Map<String,byte[]> produceTemplate(String gameName, String packageName, List<String> requiredLibraryKeys, List<String> deploymentOptionKeys){

        Collection<JmePlatform> requiredPlatforms = parsePlatforms(requiredLibraryKeys);
        if(requiredPlatforms.isEmpty()){
            throw new RuntimeException("No platforms were selected");
        }

        Collection<Library> requiredLibraries = eliminateLibrariesOnUnsupportedPlatforms(requiredPlatforms, parseLibraryKeys(requiredLibraryKeys));

        Merger merger = new Merger(gameName, packageName, requiredPlatforms, requiredLibraries, calculateAdditionalProfiles(requiredLibraries, deploymentOptionKeys), versionService.getJmeVersion(), versionService.getVersionCache(), new FragmentFetcher() );

        Map<String,byte[]> templateFiles = new HashMap<>();

        for( Map.Entry<String,byte[]> templateFile : getRawTemplatePaths().entrySet()){
            if (merger.pathShouldBeAllowed(templateFile.getKey())) {
                String mergedPath = merger.mergePath(templateFile.getKey());
                byte[] fileContents;
                if (fileExtensionsToTreatAsBlobs.stream().anyMatch(fe -> templateFile.getKey().contains(fe))) {
                    fileContents =templateFile.getValue();
                } else {
                    fileContents = merger.mergeFileContents(templateFile.getValue());
                }
                templateFiles.put(mergedPath, fileContents);
            }
        }


        for(Library lib: requiredLibraries){
            if(lib.getExamplesPath() == null) continue;
            String url = lib.getExamplesPath();
            String subPath = "/";
            int i = url.lastIndexOf(":");
            if(i != -1){
                subPath = url.substring(i+1);
                url = url.substring(0,i);
            }
            if(subPath.equals("/")||subPath.isBlank()) subPath=null;
            else if(subPath.startsWith("/")) subPath=subPath.substring(1);

            try{
                URL zipUrl=new URL(url);
                InputStream is=new BufferedInputStream(zipUrl.openStream());         
                ZipInputStream zis=new ZipInputStream(is);
                while(true){
                    ZipEntry entry = zis.getNextEntry();
                    if(entry == null) break;
                    if(entry.isDirectory()) continue;
                    if(subPath==null||entry.getName().startsWith(subPath)){
                        byte[] entryData=zis.readNBytes((int)entry.getSize());
                        String entryName = subPath==null?entry.getName():entry.getName().substring(subPath.length());                        
                        templateFiles.put(entryName,entryData);
                    }
                }
                zis.close();
                is.close();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        return templateFiles;
    }

    private List<JmePlatform> parsePlatforms(List<String> requiredLibraryKeys) {
        List<JmePlatform> platforms = new ArrayList<>();
        for(String requiredLibraryKey : requiredLibraryKeys){
            try {
                platforms.add(JmePlatform.valueOf(requiredLibraryKey));
            }catch(IllegalArgumentException e){
                // this is fine, just a non-platform library
            }
        }
        return platforms;
    }

    private Collection<Library> parseLibraryKeys(List<String> requiredLibraryKeys){
        return requiredLibraryKeys
                .stream()
                .flatMap(lk -> libraryService.getLibraryFromKey(lk).stream())
                .collect(Collectors.toSet());
    }

    /**
     * Given a raw list of libraries eliminates any who's required platform requirements aren't met
     * @param requiredPlatforms the platforms that the user has requested
     * @param unfilteredList the raw list of libraries
     */
    private Collection<Library> eliminateLibrariesOnUnsupportedPlatforms(Collection<JmePlatform> requiredPlatforms, Collection<Library> unfilteredList){
        Set<String> platformKeys = new HashSet<>();
        for(JmePlatform platform : requiredPlatforms){
            platformKeys.add(platform.name()); // what ideally the data would report as a required platform key
            platformKeys.add(platform.getAlsoKnownAs()); // what the data actually reports as a required platform key
        }

        Collection<Library> filtered =  unfilteredList.stream()
                .filter(l -> {
                    if (l.getRequiredPlatforms().isEmpty()){
                        return true;
                    }else{
                        return platformKeys.stream().anyMatch(matching -> l.getRequiredPlatforms().contains(matching));
                    }

                })
                .collect(Collectors.toSet());

        if ( strictValidate && filtered.size() != unfilteredList.size()){
            throw new RuntimeException("Illegal library requested and strictValidate is on");
        }

        return filtered;
    }

    /**
     * Returns a map of the file paths -> the file contents of the raw templates.
     * Note that the paths are within the template, not full paths so they can be used directly for where to put the
     * files in the produced zip
     */
    private Map<String,byte[]> getRawTemplatePaths(){

        Map<String,byte[]> templateFiles = new HashMap<>();
        try {
            Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(null).getResources("classpath:jmetemplate/**");
            for (Resource resource : resources) {
                if (resource.isReadable()){ //isReadable means "file, or file like thing". Not a directory
                    String pathWithStandardSlashes = resource.getURI().toString().replace("\\","/"); //change windows paths to linux paths

                    //we get things like %5b instead of [, this converts them back to their true form
                    pathWithStandardSlashes = URLDecoder.decode(pathWithStandardSlashes, StandardCharsets.UTF_8);

                    String withinTemplatePath = unwantedPathRegex.matcher(pathWithStandardSlashes).replaceAll("");
                    templateFiles.put(withinTemplatePath, resource.getInputStream().readAllBytes());
                }
            }

            return templateFiles;
        } catch (IOException e) {
            throw new RuntimeException("Exception during loading template", e);
        }
    }

    private Collection<String> calculateAdditionalProfiles(Collection<Library> requestedLibraries, List<String> deploymentOptionKeys){
        List<String> additionalProfiles = new ArrayList<>();

        long numberOfPlatforms = requestedLibraries.stream().filter(l -> l.getCategory() == LibraryCategory.JME_PLATFORM).count();

        if (numberOfPlatforms > 1) {
            additionalProfiles.add("MULTIPLATFORM");
        }else{
            additionalProfiles.add("SINGLEPLATFORM");
        }
        additionalProfiles.addAll(deploymentOptionKeys);

        return additionalProfiles;
    }

}
