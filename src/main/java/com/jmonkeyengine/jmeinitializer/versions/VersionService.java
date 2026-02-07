package com.jmonkeyengine.jmeinitializer.versions;

import com.jmonkeyengine.jmeinitializer.libraries.Artifact;
import com.jmonkeyengine.jmeinitializer.libraries.Library;
import com.jmonkeyengine.jmeinitializer.libraries.LibraryService;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This uses the API documented at https://search.maven.org/classic/#api to determine the most up to date
 * stable releases of thing. Which are used as dependencies.
 */
@Service
@Log4j2
public class VersionService {

    /**
     * URL template for Maven Central repository metadata. Replace [GROUP_SLASH] and [ARTIFACT].
     * Example:
     * https://repo1.maven.org/maven2/com/onemillionworlds/tamarin/maven-metadata.xml
     */
    String mavenMetadataUrl = "https://repo1.maven.org/maven2/[GROUP_SLASH]/[ARTIFACT]/maven-metadata.xml";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Periodically the application will scan for the most recent version of a library and caches it here. Then
     * requests to build starter zips using that library will use this cached version
     */
    @Getter
    Map<String,String> versionCache = new ConcurrentHashMap<>();

    /**
     * Because many libraries use the same JME version this is a special case held separately
     */
    @Getter
    String jmeVersion = "[JME_VERSION_COULD_NOT_BE_DETERMINED]";

    private final LibraryService libraryService;

    public VersionService (LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    public void fetchNewVersions(){
        log.info("starting fetching new library versions");

        fetchMostRecentStableVersion("org.jmonkeyengine", "jme3-core", ".*-stable").ifPresent(newVersion -> this.jmeVersion=newVersion);

        for (Library library : libraryService.nonJmeLibraries()) {

            for (Artifact artifact : library.getArtifacts()) {
                String fullyQualifiedArtifactId = artifact.getGroupId() + ":" + artifact.getArtifactId();
                if (artifact.getPinVersionOpt().isEmpty()) {
                    fetchMostRecentStableVersion(artifact.getGroupId(), artifact.getArtifactId(), artifact.getLibraryVersionRegex())
                            .ifPresentOrElse(
                                    newVersion -> versionCache.put(fullyQualifiedArtifactId, newVersion),
                                    () -> versionCache.put(fullyQualifiedArtifactId, artifact.getFallbackVersion())
                            );
                }else{
                    versionCache.put(fullyQualifiedArtifactId, artifact.getPinVersionOpt().get());
                }
            }
        }

        log.info("completed fetching new library versions: " + versionCache);
    }

    /**
     * Will make an api call to attempt to get the most recent version of a library. If the api call fails Optional.empty will
     * be returned, in which case the old cached value should be retained.
     *
     * The acceptableLibraryRegex is used to determine if its a "release" version
     */
    public Optional<String> fetchMostRecentStableVersion(String group, String artifact, String acceptableLibraryRegex){
        return fetchRawVersionsForLibrary(group, artifact, acceptableLibraryRegex)
                .filter( listOfVersions -> !listOfVersions.isEmpty())
                .map( listOfVersions ->
                {
                    List<Version> listOfVersionObjects = new ArrayList<>();
                    listOfVersions.forEach(versionString -> listOfVersionObjects.add(new Version(versionString)));
                    return Collections.max(listOfVersionObjects).fullVersionString;
                });
    }

    /**
     * Will make an api call to attempt to get all the versions of a library. If the api call fails Optional.empty will
     * be returned, in which case the old cached value should be retained.
     */
    private Optional<List<String>> fetchRawVersionsForLibrary(String group, String artifact, String acceptableLibraryRegex){
        String groupSlash = group.replace('.', '/');
        String metadataUrl = mavenMetadataUrl
                .replace("[GROUP_SLASH]", groupSlash)
                .replace("[ARTIFACT]", artifact);

        ResponseEntity<String> apiResponse = restTemplate.getForEntity(metadataUrl, String.class);

        if (apiResponse.getStatusCode().is2xxSuccessful() && apiResponse.getBody() != null){
            try {
                String xml = apiResponse.getBody();

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(false);
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new InputSource(new StringReader(xml)));

                // Extract all <version> elements under <versions>
                NodeList versionsNodes = doc.getElementsByTagName("versions");
                List<String> versions = new ArrayList<>();
                if (versionsNodes.getLength() > 0){
                    Node versionsNode = versionsNodes.item(0);
                    NodeList children = versionsNode.getChildNodes();
                    for (int i = 0; i < children.getLength(); i++){
                        Node child = children.item(i);
                        if (child != null && "version".equals(child.getNodeName())){
                            String value = child.getTextContent();
                            if (value != null) {
                                versions.add(value.trim());
                            }
                        }
                    }
                }

                List<String> filtered = versions.stream()
                        .filter(v -> v != null && v.matches(acceptableLibraryRegex))
                        .collect(Collectors.toList());

                if(filtered.isEmpty()){
                    return Optional.empty();
                }

                return Optional.of(filtered);
            } catch (Exception e) {
                log.error("Failed parsing maven-metadata.xml for " + group + ":" + artifact, e);
                return Optional.empty();
            }
        }else{
            log.error("Failed to get a version response for " + group + ":" + artifact);
            log.error(apiResponse);
            return Optional.empty();
        }
    }


}
