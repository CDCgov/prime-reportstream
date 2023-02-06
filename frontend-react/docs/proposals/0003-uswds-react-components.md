# [0003: Creating New React Components]

* Status: accepted
* Authors: @etanb
* Deciders: TBD
* Proposal date: 2023-01-24
* Last updated: [YYYY-MM-DD when the proposal was last updated]

## Problem Statement

- We rely on [USWDS components](https://designsystem.digital.gov/components/overview/) for the basis of much of the code in ReportStream. This creates a rigidity in our codebase since our designs require extensions on their library and contributing to their codebase has proved a cumbersome and untimely process.
- On top of the base USWDS library, we also use [Trussworks React component library](https://github.com/trussworks/react-uswds). This adds another level of rigidity to the stack since we aren't interacting with the base USWDS library, we're only exposed to whatever `props` trussworks exposes for us.
- From-the-ground-up customized components while offering complete control, could move us too far off the standardized, accessible components offered by the USWDS + would be quite a lot of work to create and maintain.

## Decision Drivers

- Recent experience adding a simple addition to USWDS showed that we can't rely on them to update their library according to our needs.
- Trussworks React components provide extremely thin wrappers around basic USWDS markup, which makes them lightweight, but is also a roadblock for developers (like us) who want to make more complicated extensions to the USWDS library.
- Our `<Table />` component is becoming increasing complex and to be realized fully, requires us to deviate significantly from the USWDS base `<Table />` component.

## Considered Options

In a very similar vein to [Nava's previous ADR](https://github.com/navapbc/template-application-nextjs/blob/0f58517add316235e7c187bd00a820e91d4221c9/docs/decisions/app/0004-uswds-in-react.md) on the subject, there are 3 options available to us.

For each of the following options, I will build a simple feature on top of the concept of the USWDS `<Table />` component: Adding a sticky header.

---

<a name="option-one"></a>
### Option 1: Wrap Trussworks USWDS React Components

#### Description

The Trussworks React component library is, in an ideal world, all we would need. As taken from the [Nava ADR](https://github.com/navapbc/template-application-nextjs/blob/0f58517add316235e7c187bd00a820e91d4221c9/docs/decisions/app/0004-uswds-in-react.md#use-the-existing-open-source-react-uswds-library), Trussworks maintains this repo, is a trusted vendor in the space, and they keep their library very up-to-date.

Wrapping their components provides all of their underlying work (latest USWDS version, adhere strictly to USWDS code design, etc) for free.

#### Pros

- Trussworks handles turning the USWDS base library into exposed React components.
- Extremely actively maintained with an associated [Trussworks Storybook](https://trussworks.github.io/react-uswds/) which would make diffing our extended components very easy.
- Follows our currently implemented solutions of extending components, so least overhead in terms of code changes.
- Ideal for stylistic changes to Trussworks React components. Can simply use the scoped `.module` syntax to create new dynamic styles and more specific CSS rules.
- No need to add styles to our convoluted overrides file `_uswds_overrides.scss` anymore.

#### Cons
- Not ideal for complex functional extensions of React components.
- Can get convoluted quickly since we're essentially wrapping a wrapper.
- Potentially easy breaking changes as we'd be two levels from USWDS.
- May *potentially* need to use `!important` in our CSS rules to override some USWDS styling.

Sample `<RSTable/>` component with a `stickyHeader` feature:

`RSTable.jsx`:

```js
import { Table } from "@trussworks/react-uswds";

import styles from "./RSTable.module.css";

export const RSTable = ({ stickyHeader, children, ...props }) => {
    return (
        <div className={stickyHeader && styles.stickyHeader}>
            <Table {...props}>{children}</Table>
        </div>
    );
};
```

`RSTable.module.css`:

```css
.stickyHeader thead th {
    position: sticky;
    top: 0;
}
```

---

<a name="option-two"></a>
### Option 2: Utilize USWDS Base Library Only

#### Description

Utilize the raw [USWDS library](https://github.com/uswds/uswds) which would remove a level of abstraction to our components. We'd then wrap their components and create a React component that interacts directly with their code which will allow us full customization of props and functionality. CSS will still need to be overridden at times, but can be component-scoped.

#### Pros

- 1:1 relationship with USWDS which means we can upgrade our dependencies without having to rely on Trussworks upgrading theirs.
- Removes a layer of abstraction so we can create React components with naming conventions and styling that more closely match our designs and syntax.
- DOM structure will be exposed so we can more easily understand what's going on under the hood which will also help with writing better tests and writing more complex, yet clear, components.

#### Cons
- Potential breaking changes as we'd be building on top of USWDS.
- Still need to manually override USWDS styles on occasion using `!important`.
- Not ideal for very complicated features, like some of the more advanced ideas we had for a ReportStream table.

`RSTable.jsx`:

```js
import styles from "./RSTable.module.css";
import classnames from "classnames";

export const RSTable = ({ ...props, children }) => {
    // We could easily make a master hash file
    // with all USWDS CSS classes instead
    const USWDSTableClasses = {
      Borderless: 'usa-table--borderless',
      Compact: 'usa-table--compact',
      Striped: 'usa-table--striped',
      stickyHeader: 'rs-table--sticky-header',
    }
    
    // If the value of the key is falsy, 
    // it won't be included in the classnames output
    const classes = classnames("usa-table", {
      [USWDSTableClasses.Borderless]: borderless,
      [USWDSTableClasses.Compact]: compact,
    });

    return (
      <table className={classes}>
        {children}
      </table>
    );
};
```

`RSTable.module.css`:

```css
.rs-table--sticky-header thead th {
    position: sticky;
    top: 0;
}
```

---

<a name="option-three"></a>
### Option 3: Greenstart Customized Components

#### Description

Many of the USWDS components are not very complicated from a Design, animation and overall front-end point of view. Re-creating them from the ground up, especially potentially very complicated components like ReportStream's proposed new tables, would give us granular control and better testing. Engineers would have to make the case for how their markup is structured taking into considering responsiveness, web-browsers and accessibility.

#### Pros

- Complete control over presentation and functionality of our components.
- Can set the standard for other government applications and how they create accessible, usable and beautiful web components for their users.
- Zero dependencies on external teams to fix broken code or add certain features.

#### Cons
- Have to maintain, top-to-bottom, the codebase including: testing, accessibility, optimization, maintaining documentation and everything else that comes with creating a library.
- Removing ourselves from the USWDS ecosystem.
- Massive lift to recode and recreate entire components in our codebase.


`RSTable.jsx`:

```js
import styles from "./RSTable.module.css";
import classnames from "classnames";

export const RSTable = ({ styles, dataHeader, dataBody }) => {
    // The classnames library recursively flattens arrays
    const classes = classnames("rs-table", styles);
    // Engineers would have to make the case of what their markup would look like,
    // ie using <table> vs aria-role
    return (
      <div className={classes} aria-role="table">
        <div className="rs-table-header">
          {dataHeader.map((headerItem) => {
            return (
              <>
                {headerItem}
              </>
            )
          })}
        </div>
        <div className="rs-table-body">
          {dataBody.map((bodyItem) => {
            return (
              <>
                {bodyItem}
              </>
            )
          })}
        </div>
      </div>
    );
};
```

`RSTable.module.css`:

```css
/* We can port over any styles from USWDS we want to copy over */
.rs-table {
  font-family: Source Sans Pro Web,Helvetica Neue,Helvetica,Roboto,Arial,sans-serif;
  font-size: 1.06rem;
  line-height: 1.5;
  border-collapse: collapse;
  border-spacing: 0;
  color: #1b1b1b;
  margin: 1.25rem 0;
}

.fullWidth {
  width: 100%;
}

.scrollable {
  overflow-x: auto;
}
```

---


## Decision Outcome

There is no one-size-fits-all solution here. All of the above solutions are viable and have their place, it just depends on the context of what we're try to create. What we can have is a framework for deciding which of the above options is best suited for a developer:

**[Option 1 (Wrap Trussworks USWDS React Components)](#option-one):**

- [ ] My component's fundamentals are based on the USWDS design system.
- [ ] My component deviates from a USWDS-based component in only minor ways, preferably just stylistically.
- [ ] My component can be represented within a simple, single-level-deep wrapper of a single Trussworks USWDS React component.

**[Option 2 (Utilize USWDS Base Library Only)](#option-two):**

- [ ] My component's fundamentals are based on the USWDS design system.
- [ ] My component deviates from a USWDS-based component in deeper ways that include functionality and style.
- [ ] My component can be represented by extending a single or sewing together multiple Trussworks USWDS React component(s).

**[Option 3 (Greenstart Customized Components)](#option-three):**

- [ ] My component's fundamentals are *not* based on the USWDS design system.
- [ ] My component is very complicated and *cannot* be represented by extending multiple Trussworks USWDS React components.
- [ ] My Engineering, Product and Design teams understand that a fundamentally new component will be added to our ecosystem.



### Rollout Plan

As stated before, this is more a guide for developers on creating components within our system. However, part of this proposal _should_ be implementing Storybook within our repo because as we create more and more components, we need to be able to track and share them amongst our team. Especially as we create deviations / extensions of USWDS components.

### Follow-Ups

_List any actions that need to be made or any questions that need to be answered as a direct consequence of this decision outcome. If any of these are hard blockers to start on this proposal's implementation, they should optimally be factored into and discussed as part of this proposal._

- Will we convert our previously created components to our new structure or only new ones?
- If we're not cleaning up previous components, can we put that on the road map to tackle at some point?
- Will we want visual regression testing on our Storybook instance? Should it be a part of this implementation?
- What other testing do we need?
