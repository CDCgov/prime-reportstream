## Background

This document is a description of the engineering proposal process as a proposal.

## Goals

The proposal process aims to promote detailed discussion on significant engineering changes. Putting these designs in writing allows for a better review. Reviewers can comment on the proposal as a whole as well as specific small details. With this up-front review, we can improve a design early and avoid spending coding time on the wrong plan.

At the same time, there is a balance between the spent on paper design and writing code. There is a limit to the types of problems a paper design can flush out. We do not expect a design proposal to have all aspects of a design detailed. Every engineer should judge how much detail to put into their documents. After their pull-request is approved, the engineer should feel free to improve a design as they code.

## Proposal

The proposal process involves a Markdown document for the proposal, and a GitHub pull-request that will contain the discussion about the proposal. Using the project's public GitHub infrastructure allows for outside reviewers.

**Steps**

1. Put a proposal document in a single markdown document in the project's proposal directory. Please include sections for context and motivation behind the proposal, the goals of the proposal, and the details of the proposal itself. Put assets in a subdirectory or the assets directory.

2. Create a pull-request that contains instructions to the reviewers. Describe the aspects of the design that the reviewers should consider.

3. Add all active contributors the area of the proposal as reviewers of the review. Aim for rough consensus in the discussion. At least two reviewers should approve. 

Follow the normal PR process for the review, including calling meetings if needed.    
