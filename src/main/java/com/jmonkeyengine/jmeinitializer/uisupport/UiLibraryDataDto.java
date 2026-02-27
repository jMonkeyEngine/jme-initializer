package com.jmonkeyengine.jmeinitializer.uisupport;

import com.jmonkeyengine.jmeinitializer.deployment.DeploymentOption;
import com.jmonkeyengine.jmeinitializer.dto.DeploymentOptionDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This is all the information the front end will need to render its options in a single packet.
 *
 * This dto represents json sent to the front end
 */
@Data
@AllArgsConstructor
@Schema(description = "The structure of json returned for use by front ends to describe available libraries etc")
public class UiLibraryDataDto {

    @Schema(description = "The platforms JME applications can run with, e.g. desktop")
    List<PlatformDto> jmePlatforms;

    @Schema(description = "The sub platform deployment targets e.g. windows. Typically used to include build scripts but can also be used to restrict libraries")
    List<DeploymentOptionDto> deploymentOptions;

    @Schema(description = "A series of categories and the libraries in that category")
    List<CategoryAndLibrariesDto> specialCategories;

    @Schema(example = "[\"TAMARIN\"]", description = "These are the libraries that are selected by default in jmeGeneralLibraries and generalLibraries (ones where you can choose as many as you like")
    List<String> defaultSelectedFreeChoiceLibraries;

    @Schema(example = "DESKTOP", description = "The platform that should be selected by default in the UI")
    List<String> defaultPlatform;

    @Schema(description = "This contains all the libraries in a map based on their keys, for convenience of the UI")
    Map<String, LibraryDto> allLibraries;

    public UiLibraryDataDto(List<PlatformDto> jmePlatforms, List<DeploymentOptionDto> deploymentOptions, List<CategoryAndLibrariesDto> specialCategories){
        this.jmePlatforms = jmePlatforms;
        this.deploymentOptions = deploymentOptions;
        this.specialCategories = specialCategories;


        this.defaultSelectedFreeChoiceLibraries = new ArrayList<>();
        specialCategories.forEach(categoryAndLibrariesDto -> {
            if (!categoryAndLibrariesDto.getCategory().isOnlyOneAllowed()){
                categoryAndLibrariesDto.getLibraries()
                        .stream()
                        .filter(l -> l.selectedByDefault)
                        .map(l -> l.key)
                        .forEach(l -> defaultSelectedFreeChoiceLibraries.add(l));
            }
        });


        this.defaultPlatform = jmePlatforms
                .stream()
                .filter(PlatformDto::isSelectedByDefault)
                .map(PlatformDto::getKey)
                .toList();

        allLibraries = new HashMap<>();

        Consumer<List<LibraryDto>> mergeIntoAllLibraries = list -> list.forEach(lib -> allLibraries.put(lib.key, lib));
        this.specialCategories.forEach(c -> mergeIntoAllLibraries.accept(c.getLibraries()));
    }
}
