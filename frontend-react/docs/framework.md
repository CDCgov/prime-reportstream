# Framework

ReportStream's frontend is primarily based on the following:

## Vite

The project uses Vite for bundling/transpilation. There is custom coding and configuration
to allow Jest to continue to work (with caveats).

## React 18

The project is React 18 (classic mode) compatible. Due to dependencies we are
not strict-mode compatible.

## TypeScript 5.2

The project is TypeScript 5.2 compatible. Higher versions are likely compatible
in tandum with updates to related packages (eslint, babel, etc.).

## Jest 29.6

The project is Jest 29.6 compatible only. Later versions will likely break tests.
