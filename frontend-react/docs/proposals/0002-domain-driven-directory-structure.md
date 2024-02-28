# 0002: Domain-Driven Directory Structure

* Status: accepted
* Authors: @stephenkao
* Deciders: @jpandersen87, @etanb, @kevinhaube, @penny-lischer
* Proposal date: 2023-01-12
* Last updated: 2023-01-19

## Problem Statement

Currently, the `frontend-react/` directory has a flat structure, meaning that we have one `components/` directory for components, one `utils/` directory for utilities, one `hooks/` directory for hooks, et cetera.  This works for a simple application, but as we continue to build new features and add new files, it's becoming more difficult to immediately answer questions about where specific files should live and how we should separate logic into said files.

## Decision Drivers

- Lack of teamwide alignment on when we should create new files and where files should live
- Consolidation of a component system for deviations from/customizations on top of USWDS
- Desire for predictability, colocation, and ease of searching within directories

## Considered Options

### Option 1: Continue with current directory structure

#### Description

This option proposes no change to our current organization so the structure will remain as-is.

```
src/
  components/
    AuthElement.tsx
    AuthElement.test.tsx
    Admin/
      AdminFormEdit.test.tsx
      AdminFormEdit.tsx
    alerts/
      NoServicesAlert.test.tsx
      NoServicesAlert.tsx
  utils/
    Analytics.test.ts
    Analytics.ts
  contexts/
  hooks/
  styles/
```

#### Pros

- No time or effort needed
- No risk of file migration

#### Cons
- Harder to maintain
- No consolidation on where files should live
- Opaque colocation of related logic
- Lack of consistency in naming, subdirectories
- One broad top-level directory for each of components, hooks, styles, utilities, all of which are only similar in type, not purpose

### Option 2: Introduce domain-driven directory structure

#### Description

This option proposes we group our files by domain, and we define "domain" as an evergreen concept within the scope of ReportStream.  Some example domains would include `auth`, `organization`, `service` or `sender`/`receiver` since these concepts will not change drastically in ReportStream for the foreseeable future.  Additionally, we can have one `common` domain that would house atomic and generalized functionality intended to be used across the site.

Each domain would have the following directories, and each should be kept as flat as possible in order to maintain a deterministic paths for each of the contained files.

- `components`: all domain-specific React component files, including component definitions, tests, colocated styles, and Storybook stories
- `hooks`: all domain-specific React hooks and tests
- `decorators`: all domain-specific React decorators
- `utils`: all domain-specific utilities, tests, and endpoint resources

Each distinct "entity" should have its own directory, but that directory should not contain other directories:

```
# Don't do this:
components/
  ComponentA/
    index.tsx
    ComponentA.test.tsx
    ComponentB/
      index.tsx
      ComponentB.test.tsx
      
# Do this instead:
components/
  ComponentA/
    index.tsx
    ComponentA.test.tsx
  ComponentB/
    index.tsx
    ComponentB.test.tsx
```

The reason for this is to 1) make the structure deterministic so there's no question about when to create a new directory or how "deep" a facet of a domain goes and 2) to try to better surface generalizable logic across the domain.

Ultimately, a typical structure for a given domain will look like the following:

```
src/
  <domain>/
    components/
      ComponentA/
        index.tsx
        ComponentA.test.tsx
        ComponentA.stories.tsx
        ComponentA.scss
      ComponentB/
        index.tsx
        ComponentB.test.tsx
        ComponentB.stories.tsx
        ComponentB.scss
    hooks/
      UseHookA/
        index.ts
        UseHookA.test.ts
      UseHookB/
        index.ts
        UseHookB.test.ts
    utils/
      UtilsA/
        index.ts
        UtilsA.test.ts
      UtilsB/
        index.ts
        UtilsB.test.ts
```

If there's a need to nest domains, they can recurse indefinitely as subdomains that will sit at the same level as the parent domain's units:

```
domainA/
  components/
  hooks/
  utils/
  subdomainB/
    components/
    hooks/
    utils/
    subdomainC/
      components/
      hooks/
      utils/
```

As part of this first wave of reorganization, we can include the following domains:

- `auth`: functionality specific to user login and session
- `admin`: functionality specific to admin usage
- `organization`: functionality specific to non-admin Organizations
  - Can also split into `sender` and `receiver` domains 
- `common`: functionality that is atomic or shared between other domains

Tentatively proposed domains:
- `styleguide`: functionality specific to Trussworks or USWDS overrides 
  - Q: Should this exist in `common` or is it useful to split it out?  
- `analytics`: functionality specific to telemetry/Azure App Insights/event-tracking
  - Q: Should this exist in `common` or is it useful to split it out?

#### Pros
- Scalable, predictable pattern for future feature work 
- Helps to naturally reinforce separation of concerns between modules
- Easier to find files
- More implicit boundaries for lazy-loaded modules

#### Cons
- Initial cost of reorganization
- Potential risk of reorganization/renaming issues
- More work to split up domains into smaller domains going forward

## Decision Outcome

We will be going forward with option 2, the domain-driven directory structure, since it'll serve as a more scalable and intentional pattern as more developers work in the codebase and need to maintain more files.

### Rollout Plan

The rollout plan is to add new files to domains and migrate existing files *gradually* as they're modified.  This means that the migration to our intended structure will be slow, but the reason for this is to mitigate risk with code that's already in production.  

### Follow-Ups

N/A

## Additional Information

N/A