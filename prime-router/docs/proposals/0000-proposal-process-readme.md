## Background

This document is a description of the engineering proposal process as a proposal.

## Goals

The proposal process aims to promote detailed discussion on significant engineering changes. Putting these designs in writing allows for a better review. Reviewers can comment on the proposal as a whole as well as specific small details. With this up-front review, we can improve a design early and avoid spending coding time on the wrong plan.

At the same time, there is a balance between the spent on paper design and writing code. There is a limit to the types of problems a paper design can flush out. We do not expect a design proposal to have all aspects of a design detailed. Every engineer should judge how much detail to put into their documents. After their pull-request is approved, the engineer should feel free to improve a design as they code.

Many projects have an explicit multi-step review process that works well for them.
Given that we are a young project, we are aiming for a minimal process, but will make changes as we need them. 


## Proposal

The proposal process involves a Markdown document for the proposal, and a GitHub pull-request that will contain the discussion about the proposal. Using the project's public GitHub infrastructure allows for outside reviewers.

**Steps**

1. Put a proposal document in a single markdown document in the project's proposal directory or if multiple files are needed in a subdirectory of this directory. Prefix each proposal (file or directory) with a 4-digit number to keep the proposals sorted in order. Please include sections for context and motivation behind the proposal, the goals of the proposal, and the details of the proposal itself.

2. Create a pull-request that contains instructions to the reviewers. Describe the aspects of the design that the reviewers should consider.

3. Conduct the review. Add all active contributors in the area of the proposal as reviewers. 

4. Approve the PR. The proposer should determine when enough approvals are obtained. 

The proposer should lead the review process, either following the normal PR process, or calling specific meetings if needed.    
