package com.jmonkeyengine.jmeinitializer.uisupport;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlatformDto {
    @Schema( example = "DESKTOP",  description = "A key for the platform")
    String key;

    @Schema( example = "Desktop", description ="A human readable short name for the platfrom")
    String platformName;

    @Schema( example = "Desktop Game development including Windows and Linux", description = "A longer piece of text (e.g. a sentence or two) describing the platform")
    String platformDescription;

    @Schema( description = "If this platform should be presented pre ticked in the UI")
    boolean selectedByDefault;
}
