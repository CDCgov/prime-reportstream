# Console and telemetry

## The problem

Error reporting, and console emissions are currently unorganized and application insight telemetry is mostly accomplished by replacing the original console methods
with custom proxy functions that emit telemetry in addition to console emission. This creates an unfavorable scenario where all console emissions, including from our
web app, are reportable to application insights. This creates junk data and also depending on console severity (ex: error) will trigger false positives for notifications.
Also, making changes to native global objects in general is bad practice and can cause issues where those native global objects are expected to be untouched
(ex: tests). This requires extra work to work around these issues.

In addition, there are a lot of low-level functions that were built to be smart by catching errors, emitting to console, and returning a safe value when they should be simpler
and throw an error. Higher-level calling logic should dictate how errors are handled (ex: a component that can decide if there's a safe value that can be given instead
and emit information if desirable).

## Proposal

Replace all current direct usages of global console in our app with a custom rsconsole object (available through a React context) that can be used for the purpose of emitting
in global console and application insights. We continue to have a "middleman" to do custom logic while keeping the global console untouched. Usages of global
console in utility functions are removed. Utility functions should be kept simple where they throw if anything unexpected happens (using the most
appropriate error type and descriptive messaging) so that higher-level logic can catch and handle them if desired.

This will ensure for the most part that only our app is causing telemetry to be transmitted. Though, remember, the reality is that any bad actor can find the application insights object
or any of our objects that call to it and forcibly cause junk transmissions. There are no protections for abuse as a client-side aka Single-Page Application.

We can provide a custom object with similar methods to console for different scenarios and severity of emissions.

| rsconsole | AI telemetry type | AI trace severity | notes                                                                                                                                                                             |
| --------- | ----------------- | ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| info      | trace             | information       | string message as first argument, anything following                                                                                                                              |
| warn      | trace             | warning           | string message as first argument, anything following                                                                                                                              |
| error     | exception         | n/a               | error object as first argument, anything following                                                                                                                                |
| debug     | trace             | verbose           | string message as first argument, anything following                                                                                                                              |
| assert    | exception or none | n/a               | the value to evaluate for truthyness as first argument, anything following. if falsey will create a non-thrown error (just like console) and emit ai exception, otherwise nothing |
| trace     | trace             | information       | string message as first argument, anything following                                                                                                                              |
| dev       | n/a               | n/a               | anything for args, will only console.log if in dev environment                                                                                                                    |

The most appropriate method should be used depending on scenario for correct AI emission.
