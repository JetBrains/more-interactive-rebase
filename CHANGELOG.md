# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [1.1.5] - 2024-06-26

### Added
- Squashes through drag and drop
- Stop to edit a squashed commit puts a pause icon on the squashed commit.
- Progress Indicator when fetching data from Git.
- Notification to collapse when there are too many expanded commits.

### Changed
- Changes the icon for squashing to display three circles overlaying each other.
- If more than 30 commits are collapsed, only 20 are shown when expanding and the rest are still collapsed.
- Perform Batch Updating when performing actions.

### Fixed
- Fixes reordering through drag and drop with the dragged commit no longer getting offset from the mouse position
- Fixes collapsing if main branch is small but added branch is very long.
- Fixes collapsing of second branch on the diff.
- Resets cherry-picking after finishing the process.
- Diffs works with cherry-picking.
- Cherry-picking is not reverting if a cherry-pick fails.
- Adding and removing a second branch does not flicker.
- Collapsing of commits is working properly when resetting.
- Removing a branch from the view while still fetching.
- Reordering of commits clears history for redo.

## [1.1.4] - 2024-06-21

### Added
 - Explanation of the backend structure in the Contributing guide.
 - Cherry-picking through an action.
 - Cherry-picking through drag-and-drop.
 - warning icon in the commit the process is stopped on, if it is due to conflicts.
 - Small text under the main branch to indicate the name is draggable.

### Changed
 - Integration tests are bigger and more complex.
 - The commit on which it is stopped-to-edit is yellow.
### Fixed
- Selection of commits is cleared after rebasing.
- Side panel bug of not properly selecting a second branch.


## [1.1.3] - 2024-06-20
### Added
 - Added `See Difference` action. When clicking the button, a diff pops-up, showing the difference between the current
state of the graph and its initial state.
 - Added Rebase as an action. Now, it is possible to rebase onto a specific commit.
 - Added a "progress bar" coloring in green the commits that have already beed rebased and showing that the process
has stopped due to conflicts or stop-to-edit.
 - Added Continue and abort buttons for the rebasing process.
### Changed
 - Changing the add branch icon to change whenever it is open.
 - Fix up icon is more meaningful.
 - Changed the icon of the virual file
### Fixed
 - Undoing problem due to kotlin using equals methods.
 - Rewording a commit saves the new message when clicking outside the text box.
 - Double-clicking on the commit message opens the text box for rewording.
 - Right-clicking on a commit also selects it.
 - The context menu can be opened from right-clicking anywhere on the screen.
 - Fixed concurrent modification bug, by making listener methods synchronised.


## [1.1.2] - 2024-06-17
### Added

### Changed

### Fixed
- Fixing collapsing null pointer exception.

## [1.1.1] - 2024-06-17
### Added
- Added branch navigation with the second branch and also with the side panel.
- Rebasing through dragging-and-dropping branch names.
- Animation for rebasing.
- Undo and redo for rebasing.
- Displays message if there are no commits on the branch.
- Showing a warning that there was a Git error.
- Keyboard shortcuts in README.
- Help button that opens the README.
- When adding a branch and having made changes to the graph, a warning pops-up, informing that 
the changes will be scraped.
### Changed
- Update README rebase instructions.
- Entry point is in the Git window.
- Rebase button is blue.
- Separating integration tests as separate independent tasks in the pipeline.
- Disallow `Rebase` and `Reset` if there are no changes.
- Made `Rebase` and `Reset` actions with tooltips.
### Fixed
 - Labels resizing.
 - Picking of squash and fixup.
 - Catching the IRInaccessibleException.
 - Empty invoker after rebase process.
 - Fixed problem with initial fetching of commits.
 - Fixes the problem with the unnecessary warning when opening the side panel for adding branches.
 - Refreshing the UI after clicking `Rebase`.
 - Loading of the branches in the side panel.


## [1.1.0] - 2024-06-12
### Added
- Backend functionality for rebasing onto a commit.
- Add deploy stage to pipeline.
- Gradle build uses token for environmental variable.
- Shortcut for opening the `Add branch` tab.
- Context menu opening on right-click, showing the available rebase actions.
- Connecting backend and frontend for second branch visualization.
### Changed
- Remove recolouring of reordered commits.
- Using Pick Command to enable undo and redo.
- Make a separation in the backend for the two branches - created GraphInfo.
### Fixed
 - Update main branch when we check out.
 

## [1.0.2] - 2024-06-06
### Added
- Adding collapse action and expand of branch.
- Adding a second branch to the view.
- Adding shortcuts for selecting commits on the branch in a range through `Shift` or selecting individual ones through.
  `Ctrl`/`Command`.
- Reorder commits with shortcuts.
- Side branch panel displays the real fetched branches from the backend.

### Changed

### Fixed
- Difference of commits is working.

## [1.0.1] - 2024-05-31
### Added
 - Integration tests.
 - Tooltips for all the actions.
 - Fetching names of local branches.
 - Sidebar for adding branch.
 - Keyboard shortcuts for actions.
 - Disallowing rebase actions.
 - Drag and drop of commits to reorder.
 - Scroll panel for long branches.
 - Handles long commit messages.
 - Add fading line that shows direction of graph.
 - Badges to the README.
 - Squash/Fixup functionality visually.
 - Update roadmap on README.
 - Squash/Fixup backend.
 - Added Pick and Reset functionality.
 - Connect stop-to-edit to the backend.
 - Add stop-to-edit visually.
 - README
 - Connect backend to frontend for rebasing.
 - Add listeners to frontend classes.
 - Add visual rewording.
 - Add visually drop commits.
 - Add command design pattern.
 - Add all the backend classes for performing interactive rebase. Instantiates the connection to Git4Idea.
 - Commit information is displayed to the side, together with the changed files.
 - Add CommitInfo class - wrapper for GitCommit.
 - Present the real commits on the UI.
 - Present commit messages.
 - Creates the visual representation of a branch.
 - Configure Jacoco task for showing test percentage.
 - Opening in the Editor tab.
 - Add tasks to pipeline.
 - Add template for issues and merge requests.
 - Add spotless ot the project.
 - Set up of pipeline.
 - Added basic plugin project template.

### Changed
- Remove shadows.
- Instead of buttons, have actions for the rebase actions.
- Mark component as dirty for thread update.
- Setting the reference branch is configurable.
### Fixed
- Clear selected commits after fixup.
- Text field requests focus.
- Squash circles are not cut off.
- Modified squash circle icon.
- Combinations of squashing and reordering.
- Order of commits after rebasing in the backend.


