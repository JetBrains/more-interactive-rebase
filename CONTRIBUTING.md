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
all of them correlate )