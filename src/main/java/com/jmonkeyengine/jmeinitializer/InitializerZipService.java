package com.jmonkeyengine.jmeinitializer;

import com.jmonkeyengine.jmeinitializer.libraries.Library;
import com.jmonkeyengine.jmeinitializer.versions.VersionService;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
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
    private static Set<String> fileExtensionsToTreatAsBlobs = Set.of(".jar");

    VersionService versionService;

    public InitializerZipService (VersionService versionService) {
        this.versionService = versionService;
    }

    public ByteArrayOutputStream produceZipInMemory(String gameName, String packageName, List<String> requiredLibraryKeys ){

        List<Library> requiredLibraries = requiredLibraryKeys
                .stream()
                .flatMap(lk -> Library.tryValueOf(lk).stream())
                .collect(Collectors.toList());

        Merger merger = new Merger(gameName, packageName, requiredLibraries, versionService.getJmeVersion(), versionService.getVersionCache() );

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try(ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            for( Map.Entry<String,byte[]> templateFile : getRawTemplatePaths().entrySet()){
                ZipEntry entry = new ZipEntry(merger.mergePath(templateFile.getKey()));
                zipOutputStream.putNextEntry(entry);
                if (fileExtensionsToTreatAsBlobs.stream().anyMatch(fe -> templateFile.getKey().contains(fe))){
                    zipOutputStream.write(templateFile.getValue());
                }else{
                    zipOutputStream.write(merger.mergeFileContents(templateFile.getValue()));
                }

                zipOutputStream.closeEntry();
            }
        }catch(IOException ioe) {
            throw new RuntimeException("Exception while forming zip", ioe);
        }
        return byteArrayOutputStream;
    }

    /**
     * Returns a map of the file paths -> the file contents of the raw templates.
     * Note that the paths are within the template, not full paths so they can be used directly for where to put the
     * files in the produced zip
     */
    private Map<String,byte[]> getRawTemplatePaths(){

        Map<String,byte[]> templateFiles = new HashMap<>();
        URL resourceUrl = InitializerZipService.class.getResource(templatePath);
        if (resourceUrl == null){
            throw new RuntimeException("Resource at " + templatePath + " does not exist");
        }
        try {
            Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(null).getResources("classpath:jmetemplate/**");
            for (Resource resource : resources) {
                if (resource.isReadable()){ //isReadable means "file, or file like thing. Not a directory
                    String pathWithStandardSlashes = Paths.get(resource.getURI()).toString().replace("\\","/"); //change windows paths to linux paths
                    String withinTemplatePath = unwantedPathRegex.matcher(pathWithStandardSlashes).replaceAll("");
                    templateFiles.put(withinTemplatePath, resource.getInputStream().readAllBytes());
                }
            }

            return templateFiles;
        } catch (IOException e) {
            throw new RuntimeException("Exception during loading template", e);
        }
    }

}
