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

In a very similar vein to [Nava's previous ADR](https://github.com/navapbc/template-application-nextjs/blob/0f58517add316235e7c187bd00a820e91d4221c9/docs/decisions/app/0004-uswds-in-react.md) on the subject, there are 3 options available to us.

For each of the following options, I will build a simple feature on top of the concept of the USWDS `<Table />` component: Adding a sticky header.

### Option 1: Wrap Trussworks USWDS React Components

#### Description

The Trussworks React component library is, in an ideal world, all we would need. As taken from the [Nava ADR](https://github.com/navapbc/template-application-nextjs/blob/0f58517add316235e7c187bd00a820e91d4221c9/docs/decisions/app/0004-uswds-in-react.md#use-the-existing-open-source-react-uswds-library), Truss maintains this repo, is a trusted vendor in the space, and they keep their library very up-to-date.

Wrapping their components provides all of their underlying work (latest USWDS version, adhere strictly to USWDS code design, etc) for free.

#### Pros

- Truss handles turning USWDS base library into exposed React components.
- Extremely actively maintained with an associated [Truss Storybook](https://trussworks.github.io/react-uswds/) which would make diffing our extended components very easy.
- Follows our currently implemented solutions of extending components, so least overhead in terms of code changes.
- Ideal for stylistic changes to Truss React components. Can simply use the scoped `.module` syntax to create new dynamic styles.

#### Cons
- Not ideal for complex functional extensions of React components.
- Can get convoluted quickly since we're essentially wrapping a wrapper.
- Potentially easy breaking changes as we'd be two levels from USWDS.

### Option 2: Utilize USWDS Base Library Only

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

### Option 3: Greenstart Customized Components

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