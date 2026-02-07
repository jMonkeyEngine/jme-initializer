package com.jmonkeyengine.jmeinitializer.libraries;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.jmonkeyengine.jmeinitializer.deployment.DeploymentOption;
import com.jmonkeyengine.jmeinitializer.dto.DeploymentOptionDto;
import com.jmonkeyengine.jmeinitializer.uisupport.CategoryAndLibrariesDto;
import com.jmonkeyengine.jmeinitializer.uisupport.CategoryDto;
import com.jmonkeyengine.jmeinitializer.uisupport.LibraryDto;
import com.jmonkeyengine.jmeinitializer.uisupport.PlatformDto;
import com.jmonkeyengine.jmeinitializer.uisupport.UiLibraryDataDto;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Log4j2
public class LibraryService {

    /**
     * Libraries the initialiser should never show as options
     * (Usually because they have been put into the templates in
     * annother way)
     */
    private static final Set<String> SUPPRESSED_LIBRARIES = Set.of(
            "TAMARIN" //is handled within the VR templates
    );

    private final RestTemplate restTemplate = new RestTemplate();

    private Map<String, Library> currentAvailableLibraries = new HashMap<>();
    private Multimap<LibraryCategory, Library> currentAvailableLibraryByCategory ;
    private List<Library> jmeLibraries;
    private List<Library> nonJmeLibraries;

    /**
     * This is what is used by the front end to render the available libraries. Because it only changes when new
     * libraries come available it is cached here
     */
    @Getter
    private UiLibraryDataDto uiLibraryDataDto;

    /**
     * This is the URL that will be polled for a json file containing all the libraries the initializer should offer
     * as an option
     */
    private final String fetchUrl;

    public LibraryService ( @Value("${libraries.fetchUrl}" ) String fetchUrl) throws MalformedURLException{
        this.fetchUrl=fetchUrl;
    }

    public void fetchNewLibraries(){
        log.info("fetching new libraries");
        if (fetchUrl.equals("")){
            //this is for developer use, fetch from local file system
            File internalLocation = new File(System.getProperty("user.dir"), "libraries.json");
            try{
                updateLibrariesBasedOnJson(Files.readString(internalLocation.toPath()));
            } catch(IOException e){
                throw new RuntimeException(e);
            }
        }else{
            ResponseEntity<String> apiResponse = restTemplate.getForEntity(fetchUrl, String.class);

            if(apiResponse.getStatusCode().is2xxSuccessful()){
                    updateLibrariesBasedOnJson(apiResponse.getBody());
            } else{
                log.warn("Failed to fetch libraries, received " + apiResponse);
            }
        }
    }

    public void updateLibrariesBasedOnJson( String jsonString ){
        Map<String, Library> newAvailableLibraries = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        try{
            Library[] libraries = mapper.readValue(jsonString, Library[].class);
            for( Library library : libraries){
                //at present the data we're getting is just claiming that specialisedToPlatforms are required platforms (which is breaking Tamarin)
                library.setSpecialisedToPlatforms(
                        Stream.concat(library.getSpecialisedToPlatforms().stream(), library.getRequiredPlatforms().stream()).collect(Collectors.toList()));
                if(SUPPRESSED_LIBRARIES.contains(library.getKey())){
                    library.setCategory(LibraryCategory.HIDDEN);
                }

                newAvailableLibraries.put(library.getKey(), library);
            }
        } catch(JsonProcessingException e){
            log.warn("Json received, could not parse:" + jsonString);
            throw new RuntimeException("Could not pass libraries json", e);
        }

        Multimap<LibraryCategory, Library> newAvailableLibraryByCategory = ArrayListMultimap.create();
        newAvailableLibraries
                .values()
                .stream()
                .forEach(l -> newAvailableLibraryByCategory.put(l.getCategory(), l));
        List<Library> newNonJmeLibraries = newAvailableLibraryByCategory
                .values()
                .stream()
                .filter(l -> !l.isUsesJmeVersion())

                .collect(Collectors.toList());
        List<Library> newJmeLibraries = newAvailableLibraryByCategory
                .values()
                .stream()
                .filter(Library::isUsesJmeVersion)
                .collect(Collectors.toList());

        //I don't think it matters if stale data is briefly presented, but do the swap over quickly nonetheless
        currentAvailableLibraries = newAvailableLibraries;
        currentAvailableLibraryByCategory = newAvailableLibraryByCategory;
        nonJmeLibraries = newNonJmeLibraries;
        jmeLibraries = newJmeLibraries;
        createUiLibraryDataDto();
    }

    private void createUiLibraryDataDto(){
        Collection<String> allPlatformKeys = librariesOfCategory(LibraryCategory.JME_PLATFORM).stream().map(Library::getKey).collect(Collectors.toList());
        Collection<String> allDeploymentKeys = Arrays.stream(DeploymentOption.values()).map(Enum::name).collect(Collectors.toList());

        List<CategoryAndLibrariesDto> specialCategories = new ArrayList<>();
        Arrays.stream(LibraryCategory.values())
                .filter(c -> c != LibraryCategory.JME_PLATFORM && c != LibraryCategory.HIDDEN)
                .forEach(c -> specialCategories.add(new CategoryAndLibrariesDto(
                        new CategoryDto(c),
                        librariesOfCategory(c).stream().map(l -> LibraryDto.libraryDtoFromLibrary(l, allPlatformKeys, allDeploymentKeys)).collect(Collectors.toList()),
                        defaultLibraryInExclusiveCategory(c).map(Library::getKey).orElse(null))
                ));

        //don't present empty categories to the UI for rendering. This is for graceful upgrades with new categories or removal of old categories
        specialCategories.removeIf(c -> c.getLibraries().isEmpty());

        List<PlatformDto> platforms = Stream.of(JmePlatform.values())
                .map(p ->
                    new PlatformDto(
                            p.name(),
                            p.getHumanReadableName(),
                            p.getDescriptionText(),
                            p == JmePlatform.DESKTOP)
                ).toList();

        uiLibraryDataDto = new UiLibraryDataDto(
                platforms,
                DeploymentOptionDto.wrapAsDto(DeploymentOption.values()),
                specialCategories
        );
    }


    public List<Library> nonJmeLibraries(){
        return nonJmeLibraries;
    }

    public Collection<Library> librariesOfCategory(LibraryCategory category){
        return currentAvailableLibraryByCategory.get(category);
    }

    /**
     * This is intended for {@link LibraryCategory}s which have either 1 or zero defaults (so are represented by radios.
     * This returns the library that is the default for that category
     */
    public Optional<Library> defaultLibraryInExclusiveCategory(LibraryCategory category){
        if (!category.isOnlyOneAllowed()){
            return Optional.empty();
        }

        List<Library> defaultsInCategory = librariesOfCategory(category).stream().filter(Library::isDefaultSelected).collect(Collectors.toList());

        if (defaultsInCategory.size() > 1){
            throw new RuntimeException(category.name() + " has more than one default");
        }

        return defaultsInCategory.isEmpty() ? Optional.empty() : Optional.of(defaultsInCategory.get(0));
    }

    public Optional<Library> getLibraryFromKey (String key){
        return Optional.ofNullable(currentAvailableLibraries.get(key));
    }

}
