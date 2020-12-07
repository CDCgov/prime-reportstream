# Welcome!
Thank you for your interest in contributing to the PRIME Data Hub! 
The PRIME Data Hub is an open-source project that is part of many PRIME projects which in turn are part of many open-source projects at the CDC. 
We love to receive contributions from our community — you! 
There are many ways to contribute, from writing tutorials or blog posts, improving the documentation, submitting bug reports and feature requests, or writing code for PRIME itself.

Before contributing, we encourage you to also read or [LICENSE](LICENSE),
[README](README.md), and
[code-of-conduct](code-of-conduct.md)
files, also found in this repository. If you have any inquiries or questions not
answered by the content of this repository, feel free to [contact us](mailto:surveillanceplatform@cdc.gov).

## Public Domain
This project is in the public domain within the United States, and copyright and
related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
All contributions to this project will be released under the CC0 dedication. By
submitting a pull request you are agreeing to comply with this waiver of
copyright interest.

## Bug reports

If you think you have found a bug in the Data Hub, search our [issues list](https://github.com/cdcgov/prime-data-hub/issues) on GitHub in case a similar issue has already been opened.

It is very helpful if you can prepare a reproduction of the bug. In other words, provide a small test case which we can run to confirm your bug. It makes it easier to find the problem and to fix it. 

## Feature requests

If you find yourself wishing for a feature that exists in the PRIME Data Hub, you are probably not alone. 

Open an issue on our [issues list](https://github.com/elastic/elasticsearch/issues) on GitHub, which describes the feature you would like to see, why you need it, and how it should work.

## Contributing code and documentation changes

If you would like to contribute a new feature or a bug fix to PRIME,
please discuss your idea first on the Github issue. 
If there is no Github issue for your idea, please open one. It may be that somebody is already working on it or that there are particular complexities that you should know about before
starting the implementation. 
There are often several ways to fix a problem, and it is essential to find the right approach before spending time on a PR that cannot be merged.

We use the `good first issue` label to mark the problems we think will be suitable for new contributors.

### Fork and clone the repository

You will need to fork the main code or documentation repository and clone it to your local machine. See
[github help page](https://help.github.com/articles/fork-a-repo) for help. 

Create a branch for your work. 
Since we are small team, we consider a branch to have one owner, so force-push on a branch is ok. 
If a you need to base your work on someone elses branch, talk to the branch owner and work something out.  

### Coding your changes

There are a couple engineering overview documents that to be written. Please read them when they are written.

We want to emphasize some tried and true coding principles. A piece of code is read much more than times than it is written. Hence, we want readable code. Keep code functions short and at a single level of abstractions. Choose meaningful names and labels. Comments are welcomed, but only if the information provided cannot be expressed in code. Consider if refactoring with understandable names would be a better approach. 

Our codebase is based on Kotlin because it is a multiplatform language with seamless JVM compatibility. We also like that Kotlin promotes type safety and nullability checks. Accordingly, we try to follow the [Kotlin coding conventions](https://kotlinlang.org/docs/reference/coding-conventions.html). We reformat files to follow the style guide formatting rules as implemented by the `ktlint` tool. We emphasize the preferred [Idioms of Kotlin](https://kotlinlang.org/docs/reference/idioms.html) which are based on the ideas of functional programming with immutable data. 


### Submitting your changes

Once your changes and tests are ready to submit for review:

1. **Test your changes**

    Run the test suite to make sure that nothing is broken. We do not have a CI pipeline just yet. 

2. **Rebase your changes**

    Update your local repository with the most recent code from the principal repository, and rebase your branch on top of the latest master branch. We prefer your initial changes to be squashed into a single commit. Later, if we ask you to make changes, add the changes as separate commits.  This makes the changes easier to review.  

3. **Submit a pull request**

    Push your local changes to your forked copy of the repository and [submit a pull request](https://help.github.com/articles/using-pull-requests). In the pull request, please follow the PR template provided.

    All PRs must have at least one approval. For complicated or large changes, consider asking for two approvals.  
    
    Note that squashing at the end of the review process should not be done. Rather, it should be done when the pull request is [integrated
    via GitHub](https://github.com/blog/2141-squash-your-commits). 

## Credit
This document is a mashup of CDC's contributors and ElastiSearch's contributors document. 