# Contributing Guidelines

### Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Questions](#questions)
- [Reporting a Bug](#reporting-a-bug)
- [Requesting a Feature](#requesting-a-feature)
- [Opening an Issue](#opening-an-issue)
- [Code Review](#code-review)
- [Coding Style](#coding-style)
- [Making an MR](#making-an-mr)
- [Our Codebase](#our-codebase)


> **This guide serves to explain clearly how to contribute and work in a team with others.
> Everyone is welcome to help improve
> and develop the project together.**

## Code of Conduct

Please review our [Code of Conduct](CODE_OF_CONDUCT.md). We expect those who contributes to follow it
in order to ensure a nice working environment for everyone. 

## Questions
For any questions or difficulties, please refer to our [Support guide](SUPPORT.md).

## Reporting a Bug
If you stumbled upon a bug and want to report it you can open an issue in our repository by following the set rules:
1. Use the existing issue template
2. Label it as a `bug`
3. Use any other label that seems appropriate

## Requesting a Feature

If you thought of a cool feature that would improve our plugin you can create an issue for it following the set rules:
1. Use the existing issue template
2. Label it as a `discussion` as we first want to review it
3. Use any other label that seems appropriate

## Opening an Issue

Before you open an issue look at the appropriate documentation and check for duplicate issues.
Our general rules for opening an issue:
- Use appropriate language
- Use the provided issue template
- Give clear explanation and, if needed, photos.
- Put time tracking estimate of the time it should take to complete the issue
- Give it a difficulty and a priority label

## Code Review

- **Review the code.** Look for and comment on improvements without being disrespectful or demeaning towards the author.
- **Explain your reasoning.**
- **Accept constructive criticism.** When your code is critiqued, do not take it personally but as an 
opportunity to improve your skills and understanding.


## Coding Style

Please follow the already established style rules. We are using `Spotless`, so before making a merge request,
 please run `./gradlew spotlessApply` and `./gradlew spotlessCheck` to ensure good quality and structured code.

## Making an MR
If you have completed an issue, please follow the set rules for creating a merge request:
- Use appropriate language
- Use the provided template for merge requests
- Give clear explanation of the implementation
- Support your description with photos
- Always write tests and do not lower the test coverage
- Close the related issue
- Put the time it took you to finish the issue on the time tracking

## Our Codebase

In the following section, we explain more in detail the functionality of each of the packages to get a proper
understanding of the underlying implementation of the plugin.

### Actions `com.jetbrains.interactiveRebase.actions`

This package is responsible for managing all the actions that form part of the plugin. Inside there are three more
packages, each one corresponds to a specific group of actions namely `buttonActions` (actions that affect all 
commits), `changePanel` (actions that modify the main panel) and `gitPanel` (actions that affect individual commits, 
all of them correlate to a command).

All actions extend `AnAction` which is part of the IntelliJ Platform API. This allows for easy integration 
with the platform as adding these actions to the `plugin.xml` file provides plenty out of the box functionality. 
Included in this functionality is displaying tooltips when hovering over the actions, calling `actionPerformed` when
interacted with, integration into context menus and action toolbars.

### Commands `com.jetbrains.interactiveRebase.commands`

In here, one can find all the commands used for the invoker. All classes extend `IRCommand` and require an `execute` 
method. The invoker will later depend on these commands to execute the actions and perform the desired interactive 
rebase. Each command also contains specific attributes to be able to properly display and propagate the correct data 
to the frontend. 

### Data Classes `com.jetbrains.interactiveRebase.dataClasses`

It contains the main data used in the plugin. I consists of three classes namely `BranchInfo`, `CommitInfo` 
and `GraphInfo`. 

`CommitInfo` contains a GitCommit which encapsulates all of the commit's data, a list of commands to 
know the state of the commit and the actions that have been taken on the commit and a list of flags to know the status 
of the commit in order to properly display it in the GUI. 

`BranchInfo` contains various lists of `CommitInfo` as well as the name of branch and other flags that provide 
information regarding the state of the branch. As for the lists,`initialCommits` is the list that reflects the state of 
the actual branch in the repository. The `selectedCommits` keep track of which commits are currently selected in the GUI
such that if an action gets executed, these commits get affected. Finally `currentCommits` keeps track of the commits 
that are being displayed by the UI at any given moment in time, this is specially important for collapsed, squashed and
fixup commits.

`GraphInfo` contains two branches. The main branch which is the branch in which the user is currently checked out and
added branch which is null when only one branch is being displayed or contains the information of the second branch if 
two branches are being displayed.

All data classes have an internal listener class. When the events of this listeners get called, the corresponding panels
get modified triggering a refresh in the frontend, this way we ensure that the GUI is always up to date with the data.

### Editors `com.jetbrains.interactiveRebase.editors`

Solely contains the `IRFileEditorBase` class. It is responsible for creating the main panel and opening the 
plugin as an editor tab.

### Exceptions `com.jetbrains.interactiveRebase.excpetions`

This package contains all the custom exceptions created for the plugin, at the moment only `IRInaccesibleException` 
exists. It is mostly used when errors are encountered while fetching the data to populate the data classes when the 
plugin is opened.

### Listeners `com.jetbrains.interactiveRebase.listeners`

All listeners used throughout the plugin can be found here. Inside of these package, three subpackages can be found. The 
package `keyListeners` contains the listener used for keyboard navigation inside of the plugin as well for text input. 
Inside of `mouseListeners`, the logic for clicking, pressing and hovering as well as that for dragging and dropping can 
be found. Finally `systemListeners` handles those listeners who are subscribed to changes in the system such as an 
update in the repository or the invokation of popups.

### Providers `com.jetbrains.interactiveRebase.providers`

Contains the classes that open the `IRVirtualFile` created in order to open the plugin, it is strongly linked with the 
`IRFileEditorBase`. 

### Services `com.jetbrains.interactiveRebase.providers`

In this package all the services can be found. They contain the main business logic for the plugin. All of the
services are handled at project level allowing us to have different instances of the plugin in different opened projects
within the same application. 

#### Action Service 

Responsible for all the logic regarding the actions. It handles both checking if an action is available 
and performing said action when the user interacts with the mentioned action.

#### Branch Service

Handles the logic for fetching branch names as well as for checking various conditions for the state of the branch.

#### Commit Service

Is responsible for the logic of fetching the commits of a current branch as well as verifying and updating the state of
the commits within a certain branch.

#### Dialog Service

Manages the opening and handling of popups and dialogs that occur while making use of the plugin.

#### Graph Service

Manages all the logic regarding the fetching of a branch, regardless of it is the main branch or the added branch as 
well as looking for interesting objects of both branches such as the branching commit from one branch to the other.

#### IR Virtual File Service

Is tasked with creating, retrieving and closing the virtual file created to be able to open and close the plugin.

#### Model Service

Responsible for all interactions with the data classes, most importantly the branchInfo. Often used when selecting or
deselecting commits of a branch.

#### Rebase Invoker

Handles the execution of all queued commands by the actions taken. Makes use of git4idea to create a model and execute
the desired interactive rebase actions.]

### Utils `com.jetbrains.interactiveRebase.utils`

This package contains the classes responsible for interacting with external classes and/or processes that take a longer 
time to execute. A good example is `IRGitUtils` that fetches a repository within one of the methods.

### Virtual File `com.jetbrains.interactiveRebase.virtualFile`

To open the plugin, a virtual file must be opened. This package contains the classes related to the `IRVirtualFile` 
itself as well as the interactions with it through the `IRVirtualFileSystem`

### Visuals `com.jetbrains.interactiveRebase.visuals`

This package contains all the components that we make use of in our GUI. `MainPanel` is our base component where all the
other components get placed on, it is composed mainly by a `HeaderPanel` which contains an action toolbar, a 
`CommitInfoPanel` which displays the files contained within the selected files, and a `JBScrollPane` named 
`contentPanel`. 

Inside of `contentPanel` we have the principal components of our application. On the topmost layer we add a `HelpPanel` 
and more importantly a `GraphPanel`. Within `GraphPanel` there can be one or two instances of a `LabeledBranchPanel` for 
the main branch and the added branch respectively. The `LabeledBranchPanel` contains a `BranchPanel` which is composed 
of various `CirclePanel` which is implemented by `CollapseCirclePanel`, `DropCirclePanel`, `SquashedCirclePanel` and
`StopToEditCirclePanel`. These panels respectively show which interactive rebase actions have been taken on each commit,
making it so that the user is always informed of the changes he has taken on his branch,