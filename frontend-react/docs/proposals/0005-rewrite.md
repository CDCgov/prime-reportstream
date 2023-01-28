# 0005: Rewrite of frontend-react

* Status: proposed
* Authors: @stephenkao 
* Deciders: 
* Proposal date: 
* Last updated:

## Problem Statement

With new eyes and hands on the ReportStream website code in the last few months, some concerns have been raised about its overall maintainability, specifically:

- Difficulty in building on or changing existing logic
- Inconsistent approach to the relationship with USWDS components and styling
- Inconsistent organization (e.g., file system patterns, component hierarchy, routing)
- Erroneous usage and antipatterns of core technologies (e.g., inheritance working against composition, React effects)
- Incomplete efforts to migrate to new technologies (e.g., rest-hooks and react-query)
- Lack of separation of concerns between many functions, components, hooks
- Opaque visibility into erring logic during bug triage
- Unnecessary maintenance of dead code

This has resulted in:

- More engineering time and energy spent on navigating suboptimal practices
- More onboarding time for new engineers
- Bugs frequently requiring either workarounds for temporary fixes or deeper structural changes for "true" fixes
- Less confidence in isolated changes not adversely affecting other parts of the site, potentially leading to regressions

## Decision Drivers

Now would be the optimal time to undertake a rewrite -- if at all -- for the following reasons:

- The website will be undergoing a content refresh in the near future, which would likely benefit from a better, more cohesive and intentional foundation of code
- The website's usage is mostly internal at the moment, so there's less risk for external users in a rewrite
- The new maintainers of the website have frequently experienced many of the aforementioned pain points during implementation

## Considered Options

NOTE: Regardless of which option we choose to go forward with, this proposal and any potential work taken on will **not** involve the following items as they exist outside the intended scope and ultimately may not be in line with our ATO:

- Bundler changes: CRA ejection, Webpack alternatives (Rollup), et cetera
- Renderer changes: moving off of React to Vue, Svelte, Angular, et cetera
- Build/application changes: Moving off of SPA, moving to Next.js/Vite
- API changes
- Environment/deployment changes: basically anything involving Azure updates

This is strictly proposing a rewrite of the existing code in `frontend-react` to help consolidate patterns for future scalability mostly within the boundaries of our current technologies.  The above points can be discussed as separate endeavors in the future.

Accordingly, **the goals of a rewrite should be**:

- Consistency and team scalability: logic is predictable and agnostic to individual authors
- Extensibility: new features and patterns are generally easy to introduce without requiring fundamental restructuring

Additionally, it should be noted that **no option will require writing all code from scratch**; any approach to a rewrite will likely involve copying -- and, in some cases, refactoring -- many pieces of the existing code to align with the agreed-upon patterns.

### Option 1: Refactor-as-you-go

#### Description

This option would consist of building new patterns within the current organization and only refactoring as parts are touched during implementation. 

**Estimated time for full migration: N/A**

#### Pros

- No immediate changes needed

#### Cons

- Doesn't address the maintenance issues at hand
- May indirectly prohibit a "full" refactor of underlying logic (i.e., will need to maintain backwards-compatible logic for core functionality)
- Indeterminate timeline of staying between old and new patterns
- Ongoing refactors may introduce bugs over a long period of time and would require more scrutiny during QA

### Option 2A: Piecemeal

#### Description

This option would involve engineers maintaining two separate versions of the site, v1 (existing) and v2 (new).  This could be facilitated in a number of different ways, including subrouting and subdirectories.  For example, all new code could exist in a `v2/` subdirectory:

```jsx
frontend-react/
  src/
    # all existing files
    App.tsx
    AppRouter.tsx
    components/
    config/
    ...

    # new files with domain-driven directory structure
    v2/
      auth/
        components/
        hooks/
        utils/
      common/
        components/
        hooks/
        utils/
      senders/
        components/
        hooks/
        utils/
      ...
```

And all pages built by new code could be nested behind an admin-locked `v2` route:

```
https://reportstream.cdc.gov/v2 # root
https://reportstream.cdc.gov/v2/product/overview
https://reportstream.cdc.gov/v2/submissions
https://reportstream.cdc.gov/v2/daily-data
...and so on
```

Under this approach, once a critical mass of functionality has been ported to `v2` code, an alert can be shown to users for a period of time to provide an adequate buffer for transitioning user workflows to the new routes.  This could also allow admin users to preview functionality and escalate bugs or missing features long before the full release.

**Estimated time for full migration: TBD**

#### Pros

- Allows coexistence with the old code 
- Can "preview" and slowly transition workflows to portions of site with new code
- Can be done over a longer period of time with less risk to the existing site
- Able to target specific pieces for QA

#### Cons

- Need to maintain similar logic in two places (old code and new code)
- Reduced engineering bandwidth for feature work
- Potential file reference and routing issues 

### Option 2B: All-at-once

This option would involve most engineering time in the near future to be spent on the rewrite, only working on existing code to address bug fixes.  The general approach could involve rewriting groups of code at a time, possibly on a per-page level.

#### Description

**Estimated time for full migration: TBD**

#### Pros

- Get it all done at once

#### Cons

- High risk of regressions upon launch
- Significantly slowed or halted feature work on existing code 
- Significantly reduced engineering bandwidth

## Decision Outcome

TK

### Rollout Plan

TK

### Follow-Ups

TK

## Additional Information

N/A