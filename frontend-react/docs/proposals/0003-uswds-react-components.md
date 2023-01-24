# [0003: Title of Proposal]

* Status: [proposed | rejected | accepted | deprecated | superseded by [0005](0005-example.md)]
* Authors: @etanb
* Deciders: TBD
* Proposal date: 2023-01-24
* Last updated: [YYYY-MM-DD when the proposal was last updated]

## Problem Statement

- We rely on USWDS components for the basis of much of the code here in ReportStream. This creates a rigidity in our codebase since our designs require improvements on their initial code and contributing to their codebase has proved a cumbersome and untimely process.
- On top of the base USWDS, we also use their trussworks react component library. This adds another level of rigidity to the stack since we aren't interacting with the base USWDS library, we're only exposed to whatever `props` trussworks exposes for us.
- From-the-ground-up customized components while offering complete control, could move us too far off the standardized, accessible components offered by the USWDS + would be quite a lot of work to create and maintain.

## Decision Drivers

- Recent experience adding a simple addition to USWDS showed that we can't rely on them to update their library according to our needs.
- Our `<Table />` component is becoming increasing complex and to be utilized fully, requires us to deviate from the USWDS base `<Table />` component.

## Considered Options

In a very similar vein to [Nava's previous ADR](https://github.com/navapbc/template-application-nextjs/blob/0f58517add316235e7c187bd00a820e91d4221c9/docs/decisions/app/0004-uswds-in-react.md) on the subject, there are 3 options available to us:

### Option 1: [option 1 title]

#### Description

_Write a few sentences of what this approach would entail.  At a high level, what would change between the current state and the ideal state with Option 1?_

#### Pros

- [option 1 pro]
- [option 1 pro]
- ...

#### Cons
- [option 1 con]
- [option 1 con]
- ...

### Option 2: [option 2 title]

#### Description

_Write a few sentences of what this approach would entail.  At a high level, what would change between the current state and the ideal state with Option 2?_

#### Pros

- [option 2 pro]
- [option 2 pro]
- ...

#### Cons
- [option 2 con]
- [option 2 con]
- ...

## Decision Outcome

_Note which option was chosen and list the main contributing factors that led to this decision.  Until a decision has been reached by the team, this section can be left blank._

### Rollout Plan

_Explain how the proposed change will be implemented.  Break down the implementation into small steps to mitigate as much risk as possible and note what sort of user impact (internal or external) this may have. Note any potential obstacles or blockers that may impede progress along the way._

- [step 1]
- [step 2]
- ...

### Follow-Ups

_List any actions that need to be made or any questions that need to be answered as a direct consequence of this decision outcome. If any of these are hard blockers to start on this proposal's implementation, they should optimally be factored into and discussed as part of this proposal._

- [follow-up 1]
- [follow-up 2]
- ...

## Additional Information

_List follow-up points or links (e.g., example or real GitHub repositories) that helped to influence this proposal._  

- [link 1]
- [link 2]
- ...