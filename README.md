# <img src="src/main/resources/META-INF/pluginIcon.svg" style="width: 200px; height: 200px; "> A More Interactive Rebase


---
<!-- Plugin description -->
A More Interactive Rebase for JetBrains is a plugin for the integrated development
environments (IDEs) from JetBrains. The tool makes it easier for you to perform Git Interactive Rebase actions, without 
the need for a command line, by providing a clear 
graphical visualization of the commit history and the changes you want to perform on it.
<!-- Plugin description end -->

## 📋 Table of Contents

1. ✨ [Features](#features)
2. ⚙️ [Installation](#-installation)
3.  ⚡ [Quick Start](#-quick-start-)
4. 🔗 [Dependencies](#-dependencies)
5. ❓ [Support](#-support)
6. 🗺️ [Roadmap](#-roadmap)
7. ✒️ [Authors and Acknowledgement](#authors-and-acknowledgment)


## ✨ Features
A More Interactive Rebase offers the following functionality for a better user experience: 
* Opens in the editor tab for a better visualization
* Shows clearly the commit history in the form of a graph
* Gives details about the commits and their file changes
* Allows performing Interactive Rebase actions on selected commits
* Allows reordering of commits by drag-and-drop

[//]: # (Add short video here)

##  ⚙️ Installation
You can easily get started with setting up the plugin by following these guidelines:

### Guidelines for Setting up

1. Download the Plugin zip on your device
2. Extract the files in it in a folder
3. Open a JetBrains IDE and navigate to <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > 
<kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
4. Navigate to the folder of the Plugin and select `interactive-rebase-jetbrains-version.jar`
5. The Plugin should be in the list of Plugins and you can search for it by name *A More Interactive Rebase*
6. Click on the Plugin and enable it
7. Restart your IDE


## ⚡ Quick Start 
### Opening the Plugin
Navigate to <kbd>Tools</kbd> > <kbd> Interactive Rebase Current Branch</kbd>. The Plugin opens as a file in
the editor tab. The branch drawn shows the latest commits on the checked out branch. Closing the Plugin and opening it 
again keeps the changes that were made.

### Commit Information
Clicking on one or multiple commits selects them. The commit information and file changes of the selected commits 
are presented on the right side of the screen. Double-clicking on a file change opens its diff and shows the changes.

### Interactive Rebase Actions
After selecting the commits, the buttons that are *active* show the available Interactive Rebase actions. **It is 
important to note that any changes made on the graph are not actually executed until the `Start Rebasing` button
is pressed. The idea of the graph is to show a preview of how the commit history will look in the end.** If you are not very 
familiar with the Interactive Rebase actions that Git has to offer, you can read the 
[Rewriting History](https://git-scm.com/book/en/v2/Git-Tools-Rewriting-History) Chapter provided by Git
for more information.

* **<u>Reword</u>** - If a single commit is selected you can click the `reword` button that opens a text field 
in place of the commit message. Alternatively, if there is no selected commit, you can just double-click on a 
commit message, which will also enable the text field. You can edit the message and press `Enter`. 
If you want to cancel your rewording, pressing `Escape` closes the text field.
* **<u>Reorder</u>** - You can drag and drop the commits along the branch if you want to reorder them.
* **<u>Drop</u>** - After selecting commits, pressing the `drop` button will drop the selected commits.
* **<u>Squash</u>** - If there is only one selected commit pressing the `squash` button will squash that commit with the
previous one (older in the commit history). In the case where multiple commits are selected, they get squashed into the
oldest selected commit. After squashing, a text field opens for you to reword the commit message 
for the squashed commit.
* **<u>Fixup</u>** - If there is only one selected commit, pressing the `fixup` button will perform fixup of that 
commit with the previous one (older in the commit history). In the case where multiple commits are selected,
they are fixed up into the oldest selected commit. This action combines automatically the commit messages of the
selected commits.
* **<u>Stop to Edit</u>** - After selecting commits and pressing the `stop to edit` button the IDE opens each commit
in chronological order (from oldest to newest) and allows you to make changes to that commit (amend it). 
After having made your changes, you have to stage them by typing `git add .`. Pressing `Continue` on the pop-up
in the lower right corner of the screen will proceed with the rebasing of the next commit. You can also press
`Abort` to stop the 
stop-to-edit action.
* **<u>Pick</u>** - Selecting the commits and pressing `Pick` reverts all changes done on them except for reordering.
* **<u>Reset</u>** - clicking the `Reset` button reverts the graph to its initial state, 
before any changes were indicated.
* **<u>Start Rebasing</u>** - Pressing `Start Rebasing` starts the Interactive Rebase and executes all the changes 
that were made starting from the lowest changed commit on the graph. 

##  🔗 Dependencies

### IntelliJ OpenAPI
In order for our Plugin to interact with the current JetBrains IDEs we are making use of the
[Intellij Platform API](https://plugins.jetbrains.com/docs/intellij/explore-api.html). This streamlines the process of 
building a Plugin by offering extension points to already existing functionality.

### git4idea
To allow interaction with Git functionality, we use the [git4idea](https://plugins.jetbrains.com/plugin/13173-git) 
Plugin as a library. 

### IntelliJ Community
Our Plugin is compatible with [IntelliJ Community 2024.1](https://www.jetbrains.com/idea/download/other.html) and newer
versions. 
### Gradle
For managing the dependencies during runtime and testing we use
[Gradle 8.7](https://docs.gradle.org/8.7/release-notes.html). We have defined tasks for tools such as 
Jacoco and Spotless.




## ❓ Support
If you are experiencing any difficulties, need help or want to report a bug feel free to contact one 
of our team members through email:
* Marina Mădăraş - [M.Madaras@student.tudelft.nl](mailto:M.Madaras@student.tudelft.nl)
* Diego Becerra Merodio - [D.BecerraMerodio@student.tudelft.nl](mailto:D.BecerraMerodio@student.tudelft.nl)
* Aleksandra Savova - [A.Savova@student.tudelft.nl](mailto:A.Savova@student.tudelft.nl)
* Ada Turgut - [A.Turgut@student.tudelft.nl](mailto:A.Turgut@student.tudelft.nl)
* Galya Vergieva -  [G.Vergieva@student.tudelft.nl](mailto:G.Vergieva@student.tudelft.nl)


## 🗺️ Roadmap
Since the project is still under development, here you can see the plan for the foreseeable future. You can see
the whole Roadmap via this [link](https://gitlab.ewi.tudelft.nl/groups/cse2000-software-project/2023-2024/cluster-p/12c/-/roadmap?state=all&sort=start_date_asc&layout=WEEKS&timeframe_range_type=CURRENT_QUARTER&progress=WEIGHT&show_progress=true&show_milestones=true&milestones_type=ALL&show_labels=false).
Keep in mind that opening it requires access to the repository.


![roadmap.png](Images/roadmap.png)

## ✒️ Authors and acknowledgment
**Main contributors to the project are the members of our team:** 
* Marina Mădăraş
* Diego Becerra Merodio
* Aleksandra Savova
* Ada Turgut
* Galya Vergieva




