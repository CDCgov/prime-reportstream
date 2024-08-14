% Allow-listing Gitleaks False Positives

# Introduction

We use [Gitleaks](https://github.com/zricethezav/gitleaks) to scan our repository and commits for sensitive values. The repository is scanned on a schedule-basis through a [GitHub Action](../../.github/workflows/run_gitleaks.yaml) (aka 'nightly run') and your commits are scanned by a [pre-commit hook](getting_started.md#git-hooks) which must be enabled in each of your local repository clones.

Gitleaks uses pattern matching to scan for values that it flags as 'potentially sensitive' if they match the provided pattern(s). This does mean however that it will sometimes report 'hits' that are False Positives. This document describes how to inform gitleaks of a False Positive and suppress warning(s) both for work in progress as well as already committed work.

In all of this, there is one thing to keep in mind:

"**The purpose is not to have non-failing Gitleaks Nightly Runs; the purpose is to not commit sensitive values into our repository and to be able to deal with them ASAP when this does happen, the _side-effect_ thereof is that the Gitleaks Nightly Run succeeds!**"

# Where to configure Gitleaks

Gitleaks is configured through a [TOML](https://en.wikipedia.org/wiki/Toml) file which is located at the following path in your repository: "[.environment/gitleaks/gitleaks-config.toml](../../.environment/gitleaks/gitleaks-config.toml)". This is the configuration file that is used for both the scheduled run and the pre-commit hook.

It is important to understand how the different Gitleaks runs pick up the configuration that applies to each type of run:

* **pre-commit hook**: uses the _configuration file from your branch_, only scans those files that are listed as staged
* **scheduled/nightly run**: uses the _configuration file as present in the "`master`" branch_ and the rules contained in that version of the file will apply on _all_ commits that it scans in its run. This run spans _all_ commits, in _all_ branches, matching the provided (date-based) filter. In other words: your branch "`foo`" that you pushed up last night, will be scanned by the nightly run using the rules defined in the "`master`" branch! **You may have suppressed some false positives in the version of the configuration file in _your_ branch, but unless these are merged into master, they will be ignored by the scheduled run and you will still see failures.**

Now that we understand how the Nightly Run picks up its configuration information, we can appreciate why there is a distinct process associated with adding suppressions to prevent unnecessary flagging of genuine False Positives by the nightly run.

# Suppression Process

The process for adding a suppression is the same, regardless of whether or not this is for work in progress, or work that's already been pushed up (regardless of its branch).

* Code containing Gitleaks violations is committed in branch "`culprit`", this may or may not already have been pushed up.
* Check out a new branch off of the "`master`" branch (let's call this one "`gitleaks-culprit`").
* In "`gitleaks-culprit`", modify the `gitleaks-config.toml` file to have the correct suppressions.
* Push up the "`gitleaks-culprit`" branch and open a Pull Request, include at least one member of the DevOps team (e.g. @CDCgov/prime-reportstream-devops). The Pull Request _must_ contain an explanation of:
    * What you are suppressing and why it's ok: the specific patterns/values/commits; example:
        * "Gitleaks correctly flagged the Private Key pattern, but this is a test key so it's not sensitive."
    * How you ensured the smallest possible scope for the suppression, you may be asked to narrow the scope; example:
        * "This commit does not contain any other code except for the addition of test keys, there is nothing else in this commit to be flagged. The suppression disables this particular rule for just this commit. Other rules still apply."
    * Why the suppression will not cause False Negatives; example:
        * "Suppressing the pattern "`it\.key\.contains\(`" in the "Generic Credential" rule set is fine because this value is not a credential, it is a key lookup in an iterator."
* Get approval for the PR and merge the PR into "`master`" on approval by at least one member of the DevOps team, when the nightly run kicks off, it will now have your suppression(s) applied and not raise a false positive on the changes in your branch.
* To bring the suppression into your own branch, either `git merge master` into your branch or `git cherrypick` the commit that contains the suppression on to your branch.

# How to Suppress

It is crucial to scope the suppression as small as possible. Suppressions that are too broad may inadvertently cause False Negatives (cases where a sensitive value should have been flagged, but isn't because it matches an exclusion). _While false positives can be annoying, false negatives can be really bad_!

Gitleaks allows for the definition of rules by RegEx and allows for suppressions of that specific rule based on one of the following mechanisms:

* By pattern: if a line matches your rule, but _also_ matches this other pattern, then do not flag this as a violation of the rule
* By commit: do not apply this rule on the commit with hash "`H`"
* By file or path: do not apply this rule on these files or files under this path

_As a courtesy to your fellow developers_, when putting suppressions in place, if the suppression involves adding to a list, always terminate each added line with a comma (allowed by TOML) so that each addition (or removal) now or in the future shows up as a single line modification in the source code diffs.

While suppressions are certainly possible and sometimes needed, if you can rewrite your code (pre-commit) to not need even a suppression, then that is always preferred! That being said, this previous sentence is of course not an invitation to [Underhanded C Contest](https://en.wikipedia.org/wiki/Underhanded_C_Contest)-like behavior.

## By Pattern

**CAUTION**: Using pattern-based suppressions has a ***moderate risk for false negatives**; it is crucially important that suppression patterns are as narrow as you can make them.

Sometimes you'll want to exclude a category of false positives, this can be achieved by pattern-based exclusions. This is probably your first port of call to put a suppression in place.

```toml
[[rules]]
    description = "IPv4 addresses"
    regex = '\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\.|$)){4}\b'
    tags = [
        "network",
        "IPv4",
    ]
    [rules.allowlist]
        # If a line trips this rule (see the RegEx above)
        # and that same line matches any of the following RegExes
        # Then ignore the rule trip
        regexes = [
            '([0-1])\.0\.0\.0',                         # 0.0.0.0 and 1.0.0.0 are not sensitive values
            '\d+\.\d+\.\d+\.\d+\.',                     # Fix for bug in regex (allows trailing dots)
            '1\.1\.1\.1',                               # Public DNS
            '10\.(\d+\.){2}\d+',                        # Cheapo 10.0.0.0/8 range
            '127\.(\d+\.){2}\d+',                       # Cheapo 127.0.0.0/8 range
            '165.225.48.87',                            # zScaler
            '172\.(1[6-9]|2[0-9]|3[0-1])\.',            # 172.16.0.0/12 (172.16.0.0.0 -> 172.31.255.255) range is private
            'receivingApplicationOID',                  # Category-exclusion
            'receivingFacilityOID',                     # Category-exclusion
            'reportingFacilityId',                      # Category-exclusion
        ]
        # ...
```

## By Commit

This suppression mechanism require that your code is already commited (which may not always be the case) and that you have a hash for the commit. Merge commits may throw a wrench in the works and mess this up!

An example of a rule that contains exclusions for commits, the the `[rules.allowlist]`-section which lists a `commits` key containing a list of commits to exclude from the rule:

```toml
[[rules]]
    description = "Private Keys"
    regex = '-----BEGIN ((EC|PGP|DSA|RSA|OPENSSH) )?PRIVATE KEY( BLOCK)?-----'
    tags = [
        "key",
        "AsymmetricPrivateKey",
    ]
    [rules.allowlist]
        # This rule is not enforced on the following commit
        commits = [
            '00bc6c1bc1f51d2375e22917e95deac6f6370694',                 # Invalidated
            'c07433b133225d9fa04ba763df7047545a5da217',                 # Test Keys
        ]
```

**CAUTION**: Using commit-based suppressions have a ***moderate risk for false negatives** through merge-commit flagging.

## By Path or by File RegEx

**CAUTION**: Using file/path-based suppressions have a **HIGH risk for false negatives** if you exclude paths that contain more than _just_ the thing that trips the rule.

Sometimes a file contains _only_ values that are not sensitive but match rules for good reasons. A good example of this are private keys that are test artifacts (i.e. they are only there for the tests to work, and aren't used anywhere else). In that case, it is highly advised to store these sensitive values in a dedicated file (e.g. `./prime-router/src/test/inputs/secrets/some.private.key.txt`) which has its content read at run-time by the test. This avoid polution of test code with artifacts and externalizes the (not really) 'sensitive' value in a single artifact on which different rules can apply as a whole.

```toml
[[rules]]
    description = "Private Keys"
    regex = '-----BEGIN ((EC|PGP|DSA|RSA|OPENSSH) )?PRIVATE KEY( BLOCK)?-----'
    tags = [
        "key",
        "AsymmetricPrivateKey",
    ]
    [rules.allowlist]
        paths = [
            './prime-router/src/test/inputs/secrets/some.private.key.txt',  # This specific file; this will typically be what you want
        ]
        files = [
            '\.testkey$',                                                   # Any file name that matches this pattern (i.e. ends with '.testkey')
        ]
```

# Suppressing

Not everything can nor should be suppressed. Remember that this tool is sensitive, and that _this is by design_. We want to ensure that real, sensitive values are not committed nor shared through our repository.

## False Positives

False positives must always be double-checked and confirmed as true _false_ positives. If they are confirmed as such (and justified in the Pull Request), then it's fine to suppress these. Test-only private keys are a good example of this.

## What not to Suppress

True positives.

# Do's and Don'ts

* *DO* externalize sensitive values (e.g. test keys) in dedicated files. This makes it easy to exclude just that file from a particular rule and avoids the need for an allow-list rule that may cause a False Negative:
    ```kotlin
    // DON'T
    val SomePrivKey = """
    -----BEGIN RSA PRIVATE KEY-----
    ...KEY HERE...
    -----END RSA PRIVATE KEY-----
    """.trimIndent()

    // DO
    // Add exclusion for the file "path/to/secret/key.privkey"
    val SomePrivKey = java.io.File("path/to/secret/key.privkey").readText()
    ```