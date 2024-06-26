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
- [Testing](#testing-)


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

### Software Architecture
When building our product, we have incorporated principles from the Event-Driven Architecture (EDA),
since we needed an accurate timely reflection of external events, such as user actions or changes
in the repository, to make the plugin as responsive as possible. We deemed this to be suitable for our project,
as it ensures an asynchronous connection between loosely coupled classes and focuses on 
triggering events that offer real-time responses. We used this to our advantage 
when fetching the repository history, as well as for propagating the changes we have done back to it.

In the plugin, we differentiate between three types of events: foreground, background and application-triggered.

* **Foreground Events** - A foreground event is initiated when a user interacts with the visual 
elements displayed in the GUI. Since many of the graphical components extend Swing visual components, we effectively
extend the functionality using Swing's built-in listeners, which adhere to the Delegation Event Model. 
This model operates with a source that triggers an event and a corresponding listener that processes it.
In our implementation, each Swing component, such as a label or a drawn graphic panel, can subscribe to listeners 
which handle various interactions e.g. clicks, drag-and-drop, and keyboard shortcuts. 
Depending on the specific cases, these listeners invoke methods that carry out updates, affecting either the visual 
representation or back-end functionality.
* **Background Events** - We consider a background change to be any modification in the state of the Git repository.
These changes include events such as:

  - <u>New Commits</u> - Triggered by pushing new changes, merging push requests, or cherry-picking commits.
  - <u>Branch Changes</u> - Occur when the active branch is switched, or when branches are added or removed.
  - <u>Remote Repository Updates</u> - Happen when commits are fetched or pulled from a remote repository.
  - <u>Merges or Rebases</u> - Completed operations that alter the commit history, etc.


To recognize these events, we create a listener that extends IntelliJ's `GitRepositoryChangeListener`. 
This listener is responsible for delegating the logic required to respond to the various 
changes in the repository's state. The IntelliJ Platform provides a messaging infrastructure that we leverage 
to subscribe our project-level Model Service to a message bus. This bus operates on a dedicated channel, referred to 
as a topic, specifically designed for handling repository changes ("GitRepository.GIT\_REPO\_CHANGE"). Through this 
channel, notifications and information related to repository updates are transmitted to our listener. 
After our listener is notified, it delegates the business logic to services.


* **Data Update Events** - Any change made to the state of our model - such as updating values, 
toggling flags, or modifying data structures - is categorized as a data update event. 
Instead of using an event queue to manage these changes, we opted for an in-memory 
of listeners within each data class. When the business logic updates the model, this triggers the corresponding
listeners. As a result, our GUI is refreshed, as needed, to accurately reflect the current state of the data,
ensuring the visual representation remains consistent.


### Design Patterns

Throughout the development process, we made use of two design patterns in our project.

Firstly, we implemented a **Command Design Pattern** to facilitate staging multiple rebase actions allowing us 
to execute them all together at a later point. Namely, each time the user indicates they want 
to perform a rebase action, we create the corresponding command containing all relevant information about 
the particular action. We then collect all commands in an invoker class that stores them for later use. 
Lastly, upon clicking the rebase button, the actual back-end rebase process starts and each command gets
executed one after the other. This way, the Command Design Pattern decouples the sender of the command from
its execution, promoting a clear separation of responsibilities. Furthermore, it supports undo and redo
actions by adding and removing the commands from the queue. Therefore, we easily control the connection
between changes requested by the user, and the actual execution of the rebase, thus ensuring flexibility 
and extensibility of the code.



Secondly, we made use of the **Strategy Design Pattern**. The problem was that our implemented text field had the set
behavior of rewording a commit once Enter was pressed after renaming, as it was initially created for this purpose only.
However, we realized that the text field can be re-used for other functionality such as squashing or any editing action
to be implemented in the future. To solve this problem, an option was to use inheritance to create a custom text field
for every feature we want to use the field in. However, we chose to apply the Strategy pattern instead as it provided
a way to change the behavior within an object during runtime. This meant that the same implementation of our text 
field could be set to have different strategies, making it more scalable than the alternative of using 
inheritance to create multiple objects.


### Actions `com.jetbrains.interactiveRebase.actions`

This package is responsible for managing all the actions that form part of the plugin. Inside there are three more
packages, each one corresponds to a specific group of actions namely `buttonActions` (actions that affect all 
commits), `changePanel` (actions that modify the main panel) and `gitPanel` (actions that affect individual commits, 
all of them correlate to a command).

All actions extend `AnAction` which is part of the IntelliJ Platform API. This allows for easy integration 
with the platform as adding these actions to the `plugin.xml` file provides plenty out-of-the-box functionality. 
Included in this functionality is displaying tooltips when hovering over the actions, calling `actionPerformed` when
interacted with, integration into context menus and action toolbars.

### Commands `com.jetbrains.interactiveRebase.commands`

In here, one can find all the commands used for the invoker. All classes extend `IRCommand` and require an `execute` 
method. The invoker will later depend on these commands to execute the actions and perform the desired interactive 
rebase. Each command also contains specific attributes to be able to properly display and propagate the correct data 
to the frontend. Each command is part of the command design pattern we have implemented and the execute methods are 
triggered by the execute method of the RebaseInvoker, which is the invoker for the patter. 

### Data Classes `com.jetbrains.interactiveRebase.dataClasses`

It contains the main data used in the plugin. It consists of three classes namely `BranchInfo`, `CommitInfo` 
and `GraphInfo`. 

`CommitInfo` contains a GitCommit which encapsulates all the commits' data, a list of commands to 
know the state of the commit, the actions that have been taken on the commit and a list of flags to know the status 
of the commit in order to properly display it in the GUI. 

`BranchInfo` contains various lists of `CommitInfo` as well as the name of branch and other flags that provide 
information regarding the state of the branch. As for the lists,`initialCommits` is the list that reflects the state of 
the actual branch in the repository. The `selectedCommits` keep track of which commits are currently selected in the
GUI, such that, if an action gets executed, these commits get affected. Finally `currentCommits` keeps track
of the commits that are being displayed by the UI at any given moment in time, this is specially important for
collapsed, squashed and fixup commits.

`GraphInfo` contains two branches. The main branch, which is the branch the user is currently checked out on, and
the added branch, which might be null.

All data classes have an internal listener class. When the events of this listeners get called, the corresponding panels
get modified triggering a refresh in the frontend, this way we ensure that the GUI is always up-to-date with the data.

### Editors `com.jetbrains.interactiveRebase.editors`

Solely contains the `IRFileEditorBase` class. It is responsible for creating the main panel and opening the 
plugin as an editor tab.

### Exceptions `com.jetbrains.interactiveRebase.exceptions`

This package contains all the custom exceptions created for the plugin, at the moment only `IRInaccesibleException` 
exists. It is mostly used when errors are encountered while fetching the data to populate the data classes, when the 
plugin is opened.

### Listeners `com.jetbrains.interactiveRebase.listeners`

All listeners used throughout the plugin can be found here. Inside of these package, three subpackages can be found. The 
package `keyListeners` contains the listener used for keyboard navigation as well as for text input. 
Inside of `mouseListeners`, the logic for clicking, pressing and hovering as well as that for dragging and dropping can 
be found. Finally `systemListeners` handles those listeners which are subscribed to changes in the system such as an 
update in the repository or the invokation of popups.

### Providers `com.jetbrains.interactiveRebase.providers`

Contains the classes that open the `IRVirtualFile` created in order to open the plugin, it is strongly linked with the 
`IRFileEditorBase`. 

### Services `com.jetbrains.interactiveRebase.providers`

In this package all the services can be found. They contain the main business logic for the plugin. All the
services are handled at project level allowing us to have different instances of the plugin in 
different opened projects, within the same application. 

#### Action Service 

Responsible for all the logic regarding the actions. It handles both checking if an action is available, 
and performing said action when the user interacts with the mentioned action.

#### Branch Service

Handles the logic for fetching branch names as well as for checking various conditions for the state of the branch.

#### Commit Service

Is responsible for the logic of fetching the commits of a current branch as well as verifying and updating the state of
the commits within a certain branch.

#### Dialog Service

Manages the opening and handling of popups and dialogs that occur while making use of the plugin.

#### Graph Service

Manages all the logic regarding the fetching of a branch, regardless of it is the main branch or the added branch, as 
well as looking for interesting objects of both branches such as the branching commit from one branch to the other.

#### IR Virtual File Service

Is tasked with creating, retrieving and closing the virtual file.

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
making it so that the user is always informed of the changes he has taken on his branch.

## Testing 
### Unit Testing
In order to test the system in smaller parts in isolation we implemented unit tests.
All the unit test classes inherit from the `BasePlatformTestCase` of the IntelliJ Platform.
This provided access to test fixtures like a test project, which was essential for instantiating all units responsible 
for executing the business logic. The parent test class uses the JUnit 4 framework for executing its test methods.
Most unit tests isolated the component under test from external dependencies, such as Git commit information.


### Integration Testing

Integration tests have been implemented to offer the guarantee that it works as a whole, with all the various 
software components interacting seamlessly which Unit tests do not ensure. These tests verify the behavior from
the entry point to the completion of the intended general use case, which ends by pressing the ``Rebase'' button.
The setup of integration tests relied on the necessity of using real resources, such that it imitated real-life 
scenarios as close as possible. While this realistic approach ensures effective validation of the end-to-end 
functionality and integration of all the components in the system, it also makes tests 
very heavy and resource-intensive.

* **Git Setup for Tests** - We used the `Intellij Platform VCS Test Framework` to create Git repositories, branches and 
commits. This framework granted us access to the testing setup of the Git4Idea library, providing
convenient methods to streamline interaction with Git. More explicitly, we inherited the `VcsPlatformTest` class,
which provided an appropriate environment for configuring a repository. This environment included a test 
project structure, for which the repository corresponds to, virtual directories, and various environment 
variables tailored to establish the conditions under which the tests were executed.

To effectively generate Git resources, we made use of two utility classes: `GitExecutor` and `GitTestUtil`. 
These classes integrated predefined methods for essential tasks like creating repositories,
making commits, and managing branches. Moreover, they provide the flexibility to execute any Git command. 

* **Resource-Intensive Tests** - The integration tests exhibited slower performance due to the necessity of 
creating entire repositories, conducting numerous Git operations, and interacting with various components within 
the IDE. As a result, it became necessary to wait for specific asynchronous operations
to complete before proceeding with the executions, mirroring the scenario 
where certain actions would not be visually available to the user during manual testing.
However, Kotlin lacks native support for futures. Therefore, 
we relied on the `Awaitility` library, which provided a comparable alternative. 
We used it to pause test execution until specific slow operations fulfill a certain assertion.

* **Test Workflow** - Each integration test was designed to follow a stand-alone use case. The tests started by
opening of our plugin, which in turn starts the retrieval of repository data, including details such as the checked-out
branch, its commits, and the names of local branches. Next, we selected commits and assign
them various rebase actions. We verified that actions, front-end components, 
listeners, and general logic propagated changes and data correctly throughout the code. 
Finally, we concluded the test use case by simulating a press of the ``Rebase'' button,
triggering the execution of all Git-related logic. The tests then verified if the Git 
history accurately reflected the changes specified in our application.

