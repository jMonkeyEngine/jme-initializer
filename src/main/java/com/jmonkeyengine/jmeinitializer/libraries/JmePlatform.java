package com.jmonkeyengine.jmeinitializer.libraries;

public enum JmePlatform {
    DESKTOP("Desktop", "Desktop Game development including Windows and Linux", "JME_DESKTOP"),
    ANDROID("Android", "Android mobile development", "JME_ANDROID"),

    PC_VR("Virtual reality (PCVR)","Virtual reality tethered to a PC", "JME_VR"),
    ANDROID_VR("Virtual reality (Android)", "Android VR development, e.g. for running natively on the quest", "JME_VR");

    private final String humanReadableName;
    private final String descriptionText;

    /**
     * What this platform was known as when JME libraries were used to represent platform.
     * <p>
     *     Are *not* required to be unique.
     * </p>
     */
    private final String alsoKnownAs;

    JmePlatform(String humanReadableName, String descriptionText, String alsoKnownAs) {
        this.humanReadableName = humanReadableName;
        this.descriptionText = descriptionText;
        this.alsoKnownAs = alsoKnownAs;
    }

    public String getDescriptionText() {
        return descriptionText;
    }

    public String getHumanReadableName() {
        return humanReadableName;
    }

    public String getAlsoKnownAs() {
        return alsoKnownAs;
    }
}
