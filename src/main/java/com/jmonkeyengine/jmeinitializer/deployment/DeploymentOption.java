package com.jmonkeyengine.jmeinitializer.deployment;

import com.jmonkeyengine.jmeinitializer.libraries.JmePlatform;
import com.jmonkeyengine.jmeinitializer.libraries.LibraryService;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public enum DeploymentOption{
    WINDOWS("Windows", JmePlatform.DESKTOP, JmePlatform.PC_VR),
    LINUX("Linux", JmePlatform.DESKTOP, JmePlatform.PC_VR),
    MACOS("MacOs", JmePlatform.DESKTOP, JmePlatform.PC_VR);

    /**
     * The human-readable name
     */
    String optionName;

    List<String> relevantToPlatforms;

    DeploymentOption(String optionName, JmePlatform... relevantToPlatforms){
        this.optionName = optionName;
        this.relevantToPlatforms = Arrays.stream(relevantToPlatforms).map(Enum::name).toList();
    }

    public static List<DeploymentOption> valuesOf(List<String> names){
        List<DeploymentOption> options = new ArrayList<>(names.size());
        for(String name : names){
            options.add(valueOf(name));
        }
        return options;
    }
}
