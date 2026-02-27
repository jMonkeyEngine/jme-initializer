package com.jmonkeyengine.jmeinitializer;

/**
 * These are the supported merge fields that the {@link Merger} will insert information into. They allow fields
 * with these names (represented by [NAME]) in both paths and file contents in the template to be replaced by
 * dynamic content
 */
public enum MergeField {

    /**
     * The game name as it appears in Class names and gradle artifact. A sanitised version of the true name
     * e.g. MyFirstGame
     */
    GAME_NAME,

    /**
     * The game name as it appears in marketing material. Can contain spaces etc
     *
     * e.g. My First Game
     */
    GAME_NAME_FULL,

    /**
     * The games package in "dot style". E.g. com.myexcellentgame
     */
    GAME_PACKAGE,

    /**
     * The games package in folder style. E.g. com/myexcellentgame
     * Note the no trailing slash and no preceding slash
     */
    GAME_PACKAGE_FOLDER,

    /**
     * The version of JME that is being used.
     *
     * This isn't based on use input, but is a merge field so it can be updated easily without editing the template.
     *
     * E.g. 3.4.0-stable
     */
    JME_VERSION,

    /**
     * Dependencies selected by the user that are not mandatory.
     * This is expected to be a multiline, indented merge field (I.e. each line needs 4 spaces at the front) and refer
     * to the jmonkeyengineVersion parameter for its version.
     * E.g:
     *     implementation 'org.jmonkeyengine:jme3-effects:' + jmonkeyengineVersion
     *     implementation 'org.jmonkeyengine:jme3-networking:' + jmonkeyengineVersion
     */
    JME_DEPENDENCIES,

    /**
     * Dependencies that are only used on the VR platform and are only placed in the VR module (in multimodule)
     *
     * (Libraries that are only included in projects that include VR but are nonetheless put in the main game
     * dependencies in multiproject applications WILL NOT be in here)
     */
    VR_SPECIALISED_DEPENDENCIES,

    /**
     * Dependencies that are only used on the android platform and are only placed in the android module (in multimodule)
     *
     * (Libraries that are only included in projects that include android but are nonetheless put in the main game
     * dependencies in multiproject applications WILL NOT be in here)
     */
    ANDROID_SPECIALISED_DEPENDENCIES,

    /**
     * Dependencies that are only used on the desktop platform and are only placed in the desktop module (in multimodule)
     *
     * (Libraries that are only included in projects that include desktop but are nonetheless put in the main game
     * dependencies in multiproject applications WILL NOT be in here)
     */
    DESKTOP_SPECIALISED_DEPENDENCIES,

    /**
     * This is all non jmonkey libraries that are not specialised. I.e. in multimodule projects they go in the
     * game module (not for example the android module).
     */
    ALL_NON_JME_NON_SPECIALISED_DEPENDENCIES,

    /**
     * this is a series of variables for use in the toml file.
     * <p>
     * e.g.tamarin = "3.0.2"
     * </p>
     */
    ALL_NON_JME_VERSION_REFERENCES,

    /**
     * this is a series of variables for use in the toml file.
     *
     */
    ALL_NON_JME_TOML_LIBRARY_REFERENCES,

    /**
     * Maven repos required by the libraries. E.g. jcenter()
     *
     * This is a deduped list of all the libraries required by all the libraries
     */
    MAVEN_REPOS,

    /**
     * Because tamarin is used in the templates itself it is handled seperately from the other non JME libraries
     */
    TAMARIN_VERSION

    ;

    /**
     * What should be searched for in strings to be replaced by this merge field's data
     * @return
     */
    public String getMergeFieldInText(){
        return "[" + name() + "]";
    }

}
