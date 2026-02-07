'use strict';

const e = React.createElement;

class ReactGameForm extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            gameName: "",
            package : "",
            //all the libraries that aren't radios are in here.Its just a big list of selected library keys
            freeSelectLibraries: [],
            //these are groups which are determined by the server (e.g. physics). This is a map of group key -> selected library
            groupSelectedLibraries: {},
            //library key of the libary that supports desktop, VR etc
            platformLibraries: [],
            //WINDOWS, LINUX etc
            deploymentOptions:[],
            //this is the big bundle of data that comes down from the server to say what libraries are available, what the defaults are etc
            availableLibraryData : null,
            //if the user has clicked download (but not updated the data) a message is displayed. This controls that
            hasDownloaded: false,
            gradlePreview: null,
            validationMessage: null,
            explicitlyUnticked: [] //this allows for "default selections" to work, as it allows us to differentiate between things that have been unticked deliberately and just were not available till now
        };
    }

    componentDidMount() {
        fetch('/jme-initializer/libraries')
            .then(res => res.json())
            .then((data) => {
                let stateUpdate = {
                    availableLibraryData: data,
                    freeSelectLibraries:data.defaultSelectedFreeChoiceLibraries,
                    platformLibraries:data.defaultPlatform
                };
                //add defaults (if available) for the groupSelectedLibraries
                let groupSelectedLibraries = {};
                data.specialCategories.forEach( specialCategoryData => {
                    groupSelectedLibraries[specialCategoryData.category.key] = specialCategoryData.defaultLibrary;
                })
                stateUpdate.deploymentOptions = []; //by default select all applicable deployment options
                data.deploymentOptions.forEach( deploymentOption => {
                    stateUpdate.deploymentOptions.push(deploymentOption.key);
                })
                stateUpdate.groupSelectedLibraries = groupSelectedLibraries;

                this.setState(stateUpdate, () => this.handleAutoSelectAndDeselect())
            })
            .catch(console.log)
    }

    handleSetGameName = (event) => {
        this.setState({gameName: event.target.value, hasDownloaded:false, validationMessage:null});
    }

    handleSetPackage = (event) => {
        this.setState({package: event.target.value, hasDownloaded:false, validationMessage:null});
    }

    handleTogglePlatformLibrary = (libraryKey) => {
        let currentlySelected = this.state.platformLibraries.includes(libraryKey)
        if (currentlySelected){
            let newFreeSelectLibraries = this.state.platformLibraries.filter( v => v !== libraryKey )
            this.setState({platformLibraries: newFreeSelectLibraries, hasDownloaded:false, validationMessage:null }, () => this.handleAutoSelectAndDeselect() );
        }else{
            this.setState({platformLibraries: [...this.state.platformLibraries, libraryKey], hasDownloaded:false, validationMessage:null}, () => this.handleAutoSelectAndDeselect());
        }

    }

    handleToggleDeploymentOption = (deploymentOption) => {
        let currentlySelected = this.state.deploymentOptions.includes(deploymentOption)
        if (currentlySelected){
            let newDeploymentOptions = this.state.deploymentOptions.filter( v => v !== deploymentOption )
            this.setState({deploymentOptions: newDeploymentOptions, hasDownloaded:false, validationMessage:null }, () => this.handleAutoSelectAndDeselect());
        }else{
            this.setState({deploymentOptions: [...this.state.deploymentOptions, deploymentOption], hasDownloaded:false, validationMessage:null}, () => this.handleAutoSelectAndDeselect());
        }
    }

    handleAutoSelectAndDeselect = () => {
        //select default libraries
        let newState = { freeSelectLibraries : this.state.freeSelectLibraries,  groupSelectedLibraries: {} };

        let newlySelectedLibraries = this.state.availableLibraryData.defaultSelectedFreeChoiceLibraries
            .filter(libKey => {
                let lib = this.state.availableLibraryData.allLibraries[libKey];
                return this.libraryCurrentlySupported(lib) && !this.state.explicitlyUnticked.includes(libKey) && !this.state.freeSelectLibraries.includes(libKey)
            });

        if (newlySelectedLibraries.length !== 0){
            newState.freeSelectLibraries = [...this.state.freeSelectLibraries, ...newlySelectedLibraries ]
        }

        //deselect illegal libraries
        newState.freeSelectLibraries = this.filterNonAvailableLibrariesFromList(newState.freeSelectLibraries);
        for(let groupKey in this.state.groupSelectedLibraries){
            let currentLibrary = this.state.groupSelectedLibraries[groupKey];
            if (currentLibrary !== null && this.libraryCurrentlySupported(this.state.availableLibraryData.allLibraries[currentLibrary])){
                newState.groupSelectedLibraries[groupKey] = currentLibrary;
            }else{
                newState.groupSelectedLibraries[groupKey] = null;
            }
        }
        this.setState(newState);
    }

    filterNonAvailableLibrariesFromList = (libraryKeys) => {
        if (libraryKeys == null){
            return null;
        }
        return libraryKeys.filter(libraryKey => {
            let library = this.state.availableLibraryData.allLibraries[libraryKey];
            return this.libraryCurrentlySupported(library);
        } )
    }

    handleToggleFreeFormLibrary = (library) => {
        let libraryKey = library.key;
        let currentlySelected = this.state.freeSelectLibraries.includes(libraryKey)
        if (currentlySelected){
            let newFreeSelectLibraries = this.state.freeSelectLibraries.filter( v => v !== libraryKey )
            this.setState({freeSelectLibraries: newFreeSelectLibraries, hasDownloaded:false, validationMessage:null });

            if (library.selectedByDefault && !this.state.explicitlyUnticked.includes(libraryKey)){
                this.setState({ explicitlyUnticked: [...this.state.explicitlyUnticked, libraryKey] });
            }

        }else{
            this.setState({freeSelectLibraries: [...this.state.freeSelectLibraries, libraryKey], hasDownloaded:false, validationMessage:null});
        }

    }

    handleSubmit = (event) => {
        event.preventDefault(); //don't refresh the page
        if (this.validate()) {
            this.setState({ hasDownloaded: true });

            setTimeout(() => {
                window.scrollTo(0, document.body.scrollHeight);
            }, 1000);

            document.querySelectorAll("button").forEach(el => {
                el.style.pointerEvents = "none";
                el.innerHTML = `<i class="fas fa-spinner fa-spin"></i> `;
            });

            fetch("/jme-initializer/zip?" + this.formOptionsQueryString())
                .then(resp => resp.blob())
                .then(blob => {
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.style.display = 'none';
                    a.href = url;
                    a.download = this.state.gameName + ".zip";
                    document.body.appendChild(a);
                    a.click();
                    window.URL.revokeObjectURL(url);
                    setTimeout(() => {
                        window.location.href = "#!funding=" + this.getRequiredLibrariesAsCommaSeperatedList()
                    }, 2000);
                });

        }

    }

    formOptionsQueryString = () => {
        return "gameName=" + encodeURIComponent(this.state.gameName) + "&packageName=" +encodeURIComponent(this.state.package)+"&libraryList=" + encodeURIComponent(this.getRequiredLibrariesAsCommaSeperatedList())+ "&deploymentOptionsList=" + encodeURIComponent(this.getRequiredDeploymentOptionsAsCommaSeperatedList());
    }

    handleSetLibrarySelectedInGroup =  (group, library)=>{
        let newSelectedLibraries = Object.assign({}, this.state.groupSelectedLibraries);
        newSelectedLibraries[group] = library;
        this.setState({groupSelectedLibraries:newSelectedLibraries});
    }

    getRequiredLibrariesAsCommaSeperatedList = () => {
        let fullRequiredLibrarys = []
        fullRequiredLibrarys = fullRequiredLibrarys.concat(this.state.platformLibraries);
        fullRequiredLibrarys = fullRequiredLibrarys.concat(this.state.freeSelectLibraries);
        for(const categoryKey in this.state.groupSelectedLibraries){
            const library = this.state.groupSelectedLibraries[categoryKey];
            if (library != null){
                fullRequiredLibrarys.push(library);
            }
        }

        return fullRequiredLibrarys.join(",");
    }

    getRequiredDeploymentOptionsAsCommaSeperatedList = () => {
        let fullRequiredDeploymentOptions= []
        for(const deploymentOption of this.state.availableLibraryData.deploymentOptions ){
            if (this.state.deploymentOptions.includes(deploymentOption.key) && this.deploymentOptionShouldBeAvailable(deploymentOption)){
                fullRequiredDeploymentOptions.push(deploymentOption.key);
            }
        }
        return fullRequiredDeploymentOptions.join(",");
    }

    isLibrarySelectedInGroup = (group, library) => {
        if ( group in this.state.groupSelectedLibraries ){
            return this.state.groupSelectedLibraries[group] === library;
        }else{
            return false;
        }
    }

    hasNoLibraryForGroup = (group) => {
        return !( group in this.state.groupSelectedLibraries ) || this.state.groupSelectedLibraries[group] == null;
    }

    fetchGradlePreview = (event) => {

        if (this.validate()) {
            event.preventDefault(); //don't submit the form

            fetch('/jme-initializer/gradle-preview?' + this.formOptionsQueryString())
                .then(response => response.json())
                .then((data) => {
                    this.setState({gradlePreview: data});
                })
                .catch(console.log)
        }
    }

    /**
     * Validates the form, returns if its valid (and also updates the state to show a validation message if its invalid)
     */
    validate = () => {
        if (this.state.platformLibraries.length === 0){
            this.setState({ validationMessage:"You must select at least 1 platform" });
            return false
        }

        if (this.state.gameName === ""){
            this.setState({ validationMessage:"The game must have a name" });
            return false
        }

        return true;
    }

    /**
     * Given a list of required platform keys (empty means no requirements) returns if at least one of those is
     * selected
     * @param library
     */
    libraryCurrentlySupported = (library) => {

        let requiredPlatformList = library.requiredPlatforms;
        let incompatiblePlatformList = library.incompatiblePlatformsAndDeployments;
        if (requiredPlatformList.length === 0 && incompatiblePlatformList.length === 0){
            return true;
        }

        let platformsAndDeployments = this.allSelectedPlatformsAndDeployments();
        const availableRequiredPlatforms = requiredPlatformList.filter(value => platformsAndDeployments.includes(value));
        const forbiddenPlatforms = incompatiblePlatformList.filter(value => platformsAndDeployments.includes(value));
        return (requiredPlatformList.length === 0 || availableRequiredPlatforms.length > 0) && forbiddenPlatforms.length === 0;
    }

    allSelectedPlatformsAndDeployments(){
        return this.state.platformLibraries.concat(this.state.deploymentOptions);
    }

    renderRequiredPlatformStatement(library){

        if (this.libraryCurrentlySupported(library)){
            return;
        }

        let requiredPlatformList = library.requiredPlatforms;
        let incompatiblePlatformList = library.incompatiblePlatformsAndDeployments;

        const requiredPlatformStrings = [];
        this.state.availableLibraryData.jmePlatforms.forEach(platform => {
            if (requiredPlatformList.includes(platform.key)){
                requiredPlatformStrings.push(platform.platformName);
            }
        });
        const incompatiblePlatformStrings = [];
        this.state.availableLibraryData.jmePlatforms.forEach(platform => {
            if (incompatiblePlatformList.includes(platform.key)){
                incompatiblePlatformStrings.push(platform.platformName);
            }
        });
        this.state.availableLibraryData.deploymentOptions.forEach(deploymentOption => {
            if (incompatiblePlatformList.includes(deploymentOption.key)){
                incompatiblePlatformStrings.push(deploymentOption.name);
            }
        });

        let statement = [];
        if (requiredPlatformStrings.length>0){
            statement.push(<p key = {"requires_" + library.key}><small>{"This library is only applicable for platform(s): " + requiredPlatformStrings.join(", ") }</small></p>)
        }
        if (incompatiblePlatformStrings.length>0) {
            statement.push(<p key={"incompatible_" + library.key}> <small>{"This library can't be used with: " + incompatiblePlatformStrings.join(", ")}</small></p>)
        }

        return statement;
    }

    renderPlatformCheckboxes(){
        if (this.state.availableLibraryData === null){
            return <div/>
        }else{
            const platformCheckboxes = [];

            this.state.availableLibraryData.jmePlatforms.forEach(platform => {
                platformCheckboxes.push(<div className="form-check" key = {"platformRadioDiv" + platform.key}>
                    <input className="form-check-input" type="checkbox" name="platformRadios" id={"platformRadio" + platform.key} value={platform.key} checked = {this.state.platformLibraries.includes(platform.key)} onChange={event => this.handleTogglePlatformLibrary(platform.key)} />
                    <label className="form-check-label" htmlFor={"platformRadio" + platform.key}>
                        <b>{platform.platformName}</b>
                        <p>{platform.platformDescription}</p>
                    </label>
                </div>);

            });
            return <div onChange={this.handleSetPlatform}>
                {platformCheckboxes}
                <small>Select at least 1 platform</small>
            </div>;
        }
    }

    deploymentOptionShouldBeAvailable(deploymentOption){
        return deploymentOption.applicablePlatforms.filter(value => this.state.platformLibraries.includes(value)).length>0
    }

    renderDeploymentOptionsCheckboxes(){
        if (this.state.availableLibraryData === null){
            return <div/>
        }else {
            const deploymentOptionsCheckboxes = [];
            //go through the selected platform. If a deployment option is relevant to that platform offer it
            this.state.availableLibraryData.deploymentOptions.forEach(deploymentOption => {

                let includeOption = this.deploymentOptionShouldBeAvailable(deploymentOption);
                if (includeOption){
                    deploymentOptionsCheckboxes.push(<div className="form-check" key={"deploymentCheckboxDiv" + deploymentOption.key}>
                        <input className="form-check-input" type="checkbox" name="platformRadios"
                               id={"platformRadio" + deploymentOption.key} value={deploymentOption.key}
                               checked={this.state.deploymentOptions.includes(deploymentOption.key)}
                               onChange={event => this.handleToggleDeploymentOption(deploymentOption.key)}/>
                        <label className="form-check-label" htmlFor={"platformRadio" + deploymentOption.key}>
                            <b>{deploymentOption.name}</b>
                        </label>
                    </div>);
                }
            });
            if (deploymentOptionsCheckboxes.length > 0) {
                return <div><h2>Deployment</h2>
                    <p>Deployment options will provide platform specific deployments (with a bundled JRE), and may also restrict available libraries</p>
                    <div onChange={this.handleSetPlatform}>
                        {deploymentOptionsCheckboxes}
                    </div>
                </div>;
            }else{
                return <div/>;
            }
        }
    }

    renderCategories(){
        if (this.state.availableLibraryData === null){
            return <div/>
        }else{
            const categories = [];

            this.state.availableLibraryData.specialCategories.forEach(categoryAndLibraries => {
                if (categoryAndLibraries.category.onlyOneAllowed){
                    categories.push(this.renderAsRadios(categoryAndLibraries))
                }else{
                    categories.push(this.renderAsCheckboxes(categoryAndLibraries))
                }
            });
            return categories;
        }
    }

    renderAsRadios(categoryAndLibraries){
        const thisGroupRadios = [];

        categoryAndLibraries.libraries.forEach(library => {
            thisGroupRadios.push(<div className="form-check" key = {"platformRadioDiv" + library.key}>
                <input disabled = {!this.libraryCurrentlySupported(library)} className="form-check-input" type="radio" name={categoryAndLibraries.category.key+"Radios"} id={categoryAndLibraries.category.key+"Radio" + library.key} value={library.key} checked = { this.isLibrarySelectedInGroup(categoryAndLibraries.category.key, library.key)} onChange={event => this.handleSetLibrarySelectedInGroup(categoryAndLibraries.category.key, library.key)} />
                <label className="form-check-label" htmlFor={categoryAndLibraries.category.key+"Radio" + library.key}>
                    <b dangerouslySetInnerHTML={{ __html: library.libraryName}}></b>
                    <p>{library.libraryDescription}</p>
                    {this.renderRequiredPlatformStatement(library)}
                </label>

            </div>)
        })

        thisGroupRadios.push(<div className="form-check" key = {"platformRadioDivNone"}>
            <input className="form-check-input" type="radio" name={categoryAndLibraries.category.key+"Radios"} id={categoryAndLibraries.category.key+"RadioNone"} value="" checked = {this.hasNoLibraryForGroup(categoryAndLibraries.category.key)} onChange={event => this.handleSetLibrarySelectedInGroup(categoryAndLibraries.category.key, null)} />
            <label className="form-check-label" htmlFor={categoryAndLibraries.category.key+"RadioNone"}>
                <b>None</b>
                <p>Do not include a library for this category</p>
            </label>
        </div>)

        return <div key = {categoryAndLibraries.category.key}>
            <h2>{categoryAndLibraries.category.categoryDisplayName}</h2>
            <p>{categoryAndLibraries.category.categoryDescription}</p>
            {thisGroupRadios}
        </div>;
    }

    renderAsCheckboxes(categoryAndLibraries){
        const thisGroupCheckboxes = [];

        categoryAndLibraries.libraries.forEach(library => {
            thisGroupCheckboxes.push(<div className="form-check" key = {"platformRadioDiv" + library.key}>
                <input disabled = {!this.libraryCurrentlySupported(library)} className="form-check-input" type="checkbox" value={library.key} id={"libraryCheck" + library.key} checked = {this.state.freeSelectLibraries.includes(library.key)} onChange = {event => this.handleToggleFreeFormLibrary(library)} />
                <label className="form-check-label" htmlFor={"libraryCheck" + library.key}>
                    <b dangerouslySetInnerHTML={{ __html: library.libraryName}}></b>
                    <p>{library.libraryDescription}</p>
                    {this.renderRequiredPlatformStatement(library)}
                </label>
            </div>)
        })


        return <div key = {categoryAndLibraries.category.key}>
            <h2>{categoryAndLibraries.category.categoryDisplayName}</h2>
            <p>{categoryAndLibraries.category.categoryDescription}</p>
            {thisGroupCheckboxes}
        </div>;
    }

    renderGradlePreview(){
        let numberOfFiles = Object.keys(this.state.gradlePreview).length;

        var introductoryText = "";
        if (numberOfFiles === 1){
            introductoryText = "This is the build file for your requested libraries. The full download will contain this and other important files";
        }else{
            introductoryText = "This is the build files for your requested libraries (As a multiplatform project there are multiple build.gradle files). The full download will contain this and other important files";
        }

        const gradleFiles = [];
        for( var path in this.state.gradlePreview){

            if (this.state.gradlePreview.hasOwnProperty(path)) {
                var content = this.state.gradlePreview[path];

                gradleFiles.push(<div key = {path}>
                    {numberOfFiles >1 && <h3>{path}</h3>}
                    <div style={{backgroundColor: "black"}}>
                        <pre className="pre-scrollable" style={{marginLeft: "2px"}}>
                            <code style={{color: "white"}}>{content}</code>
                        </pre>
                    </div>
                </div>)
            }
        }


        return <div>
            <h2>build.gradle preview</h2>
            <p>{introductoryText}</p>
            {gradleFiles}
        </div>

    }

    render() {
        return <form onSubmit={this.handleSubmit}>
            <div className="form-group">
                <label htmlFor="gameName"><b>Application Name</b></label>
                <input className="form-control" value={this.state.gameName} onChange={this.handleSetGameName} id="gameName" aria-describedby="gameNameHelp" placeholder="e.g. Asteroids" required/>
                <small id="gameNameHelp" className="form-text">This will be the name of your project. Try to keep to english letters as it will also be used in your java source</small>
            </div>
            <div className="form-group">
                <label htmlFor="gamePackage"><b>Package Name (Optional)</b></label>
                <input className="form-control" value={this.state.package} onChange={this.handleSetPackage} id="gamePackage" aria-describedby="gamePackageHelp" placeholder="e.g. com.mycompany"/>
                <small id="gameNameHelp" className="form-text">A package name keeps your classes unique. If you have a website it's traditionally the website backwards (all lower case). So myamazinggame.co.uk would become uk.co.myamazinggame. If you don't have a website choose something like that, or just leave it blank</small>
            </div>

            <div className="form-group">
                <h2>
                    Platform
                </h2>
                <p>JMonkeyEngine can target many platforms, select the platform(s) your application will target</p>
                {this.renderPlatformCheckboxes()}
            </div>
            {this.renderDeploymentOptionsCheckboxes()}
            <br/> <br/>
            <div className="alert alert-secondary" role="alert">
                Don't worry if you're not sure about libraries, you can always add more later
            </div>
            <p>Essential JME libraries are included by default but select any more that may be useful here</p>

            {this.renderCategories()}

            <br/>

            { this.state.gradlePreview !== null && this.renderGradlePreview() }

            {this.state.hasDownloaded && <div className="alert alert-success downloadAlert" role="alert">
                <p>A zip will now download. Unzip it and use it as a starter project in the IDE of your choice.</p>
                <p>IntelliJ, Android Studio and Eclipse will support this project by default, Netbeans will support it with the Gradle plugin installed</p>
            </div>}
            {this.state.validationMessage && <div className="alert alert-danger" role="alert">
                <p>{this.state.validationMessage}</p>
            </div> }
            <div className="btn-group" role="group" aria-label="Basic example">
                <button type="submit" className="btn btn-primary mr-2">Download a starter project</button>
                <button className="btn btn-secondary mr-2" onClick={this.fetchGradlePreview}>Preview build.gradle file</button>
            </div>
            <br/>

        </form>
    }
}

$(document).ready(function () {
    const domContainer = document.querySelector('#react_game_form');
    ReactDOM.render(e(ReactGameForm), domContainer);
})
