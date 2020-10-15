# CDC GitHub Practices for Open Source Projects

The CDCGov organization on GitHub is designed for use by CDC programs to collaborate with open communities further CDC mission to protect America from health, safety and security threats, both foreign and in the U.S.

This is a collection of practices to help programs design projects and collaborate with diverse communities looking to find, use, and contribute to open science.

We designed these practices to be intuitive, helpful, and evolving based on program and community input. Some practices are required and some are recommended. For required practices, projects that don't adhere to them will be contacted by administrators to help them meet the practices. Projects that habitually fail to  meet these practices will be archived or made private. That has never happened, but we want to make sure CDC's projects are usable.

## Requesting Access

If you would like to use GitHub for your CDC project, [please fill out this Office 365 Form](https://forms.office.com/Pages/ResponsePage.aspx?id=aQjnnNtg_USr6NJ2cHf8j44WSiOI6uNOvdWse4I-C2NUNk43NzMwODJTRzA4NFpCUk1RRU83RTFNVi4u). This will require your CDC login, so if you don't have a login, please find someone who does and ask them to request on your behalf.

GitHub is a third party web application used by CDC to collaborate with the public. Official CDC health messages will always be distributed through www.cdc.gov and through appropriate channels. 

## Creating Projects

If you would like to create a new project, please complete this Form requesting access and confirming you have completed training on our [rules of behavior](rules_of_behavior.md).

Note that any source code used within CDC systems must comply with all cybersecurity processes prior to production use, including static and dynamic scanning. The state of source code stored on GitHub is independent from, and usually varies, from the built code used in production systems.

If you need support with your project, please submit an issue to the template repo, or send an email to [mailto:data@cdc.gov](data@cdc.gov).

If you are interested in using GitHub for non-open source projects, please check out our enterprise organization [CDCent](https://github.com/cdcent) or search for "GitHub Enterprise" on the CDC intranet.

## Required Practices

* [ ] Obtain clearance from your organization prior to setting up and publishing a repository. Until you have completed clearance, include clear language in your repo indicating the current status, something like "**As a first step, this document is under governance review. When the review completes as appropriate per local and agency processes, the project team will be allowed to remove this notice. This material is draft.**" 
* [ ] Set a meaningful project name, description, and [topics](https://help.github.com/en/github/administering-a-repository/classifying-your-repository-with-topics) to improve discovery and use of your project. For AI-related projects, the [Code.gov Implementation Guidance to Federal Agencies Regarding Enterprise Data and Source Code Inventories](https://code.gov/assets/data/ai_inventory-guidance.pdf) must be followed when setting topics.
* [ ] Add a readme.md file at the root with a description of your project, the team responsible for the project. This should help users understand how to setup and use your project.
* [ ] Assign an open source license based on program need. For guidance on licenses, please review the article,  ["Open Source Development for Public Health Informatics"](https://www.philab.cdc.gov/index.php/2012/03/27/open-source-development-for-public-health-informatics/), refer to existing CDCgov projects, or ask for consultation support in choosing a license.
* [ ] Include the required notice sections in your readme.md to comply with relevant CDC policies and procedures, adapt as necessary based on your program need.
  * [ ] [Public Domain Standard Notice](https://github.com/CDCgov/template#public-domain-standard-notice)
  * [ ] [License Standard Notice](https://github.com/CDCgov/template#license-standard-notice)
  * [ ] [Privacy Standard Notice](https://github.com/CDCgov/template#privacy-standard-notice)
  * [ ] [Contributing Standard Notice](https://github.com/CDCgov/template#contributing-standard-notice)
  * [ ] [Records Management Standard Notice](https://github.com/CDCgov/template#records-management-standard-notice)
  * [ ] [Additional Standard Notices](https://github.com/CDCgov/template#additional-standard-notices)
* [ ] Include a description of your development process in the readme.md file, if your project is not active, mark it as archived to help users understand that it is not an active project.
* [ ] If active, enable [GitHub automated security alerts](https://help.github.com/en/github/managing-security-vulnerabilities/about-security-alerts-for-vulnerable-dependencies) and configure notification for the repo admin to see and respond to these alerts in a timely manner. Projects that do not respond to security alerts will have issues raised in their project by admins and may be archived to protect users.
* [ ] Never commit sensitive information, including usernames, passwords, tokens, PII, PHI. Use pre-commit tools like [Clouseau](https://github.com/cfpb/clouseau) to systematically review material before committing.
* [ ] Enable issues to allow for administrative and automated issues related to their project.
* [ ] Respond to issues and PRs created by admins in a timely manner. Ignored issues on your project will result in archiving or deletion.

## Recommended Practices

* [ ] Agree on project conventions and include them in your readme.md. Depending on what type of project, this includes folder structure for data, linters, editor configuration (eg, [MicrobeTrace's .editorconfig](https://github.com/CDCgov/MicrobeTrace/blob/master/.editorconfig)). This will help improve the quality of your project and make it easier for others to contribute to your project.
* [ ] Describe support and community procedures. CDC does not provide warranty or official support for open source projects, but describing how you would like questions and issues will assist users of your project. If you use a wiki, or project board, or package manager, describe and link to that.
* [ ] Include references to publications, presentations, and sites featuring your project.
* [ ] Add an entry to open.cdc.gov to the data, code, api, or event page to help people find your project on cdc.gov
* [ ] Add versions and tags describing major releases and milestones. For example, [open.cdc.gov's releases each time a new version is published to the web site](https://github.com/CDCgov/opencdc/releases/tag/v1.0.9) or [geneflow's changelog](https://github.com/CDCgov/geneflow/blob/master/CHANGELOG.md).
* [ ] Follow [Semantic Versioning 2.0.0](https://semver.org/) when creating versions for your project.
* [ ] Describe and test reproducible practices to install and build your project. For example, [injury_autocoding's code section on running the project's scripts](https://github.com/cdcai/injury_autocoding#code)).
* [ ] Recognize contributors and existing resources that have helped the project. For example, [fdns-ms-hl7-utils' AUTHORS file](https://github.com/CDCgov/fdns-ms-hl7-utils/blob/master/AUTHORS).
* [ ] Automate build and test procedures to reduce the effort of outside contributors to send pull requests (eg, [Travis CI](https://travis-ci.org/), [Circle CI](https://circleci.com/), [GitHub Actions](https://help.github.com/en/actions))
* [ ] Establish pull request templates to make it easier for contributors to send pull requests. For example [SDP-V has a checklist for each PR to match their development practices.](https://github.com/CDCgov/SDP-Vocabulary-Service/blob/master/.github/PULL_REQUEST_TEMPLATE)
* [ ] [Appropriately gather metrics](https://opensource.guide/metrics/) on how your project is used and incorporate this into your feature planning process.
* [ ] [Incorporate documentation into your development cycle](https://github.com/GSA/code-gov-open-source-toolkit/blob/master/toolkit_docs/documentation.md), and where possible, automate the generation of documentation so it is more likely to be up to date and useful to people interested in your project.

## Open Source Checklist

This checklist was adapted from the CDC IT Guard Rail and put here to help people who don't have access to the intranet.

* [ ] Create a new project using the [template repo](https://github.com/CDCgov/template).
* [ ] Update your readme.md following the [CDC GitHub Practices for Open Source Projects](https://github.com/CDCgov/template/blob/master/open_practices.md)
* [ ] Choose a license. Most projects are ASL2, but license should meet public health program need. See <https://www.philab.cdc.gov/index.php/2012/03/27/open-source-development-for-public-health-informatics/> for more info on choosing a license.
* [ ] Remove all sensitive info.
* [ ] Talk with your ADI, ADS, and ISSO for review and clearance.
* [ ] After approval, create a GitHub user.
* [ ] Fill out the [Request a Repo form](https://forms.office.com/Pages/ResponsePage.aspx?id=aQjnnNtg_USr6NJ2cHf8j44WSiOI6uNOvdWse4I-C2NUNk43NzMwODJTRzA4NFpCUk1RRU83RTFNVi4u) for a new repo on [CDCGov](https://github.com/cdcgov) or [CDCai](https://github.com/cdcai).
* [ ] When you get an email or push alert that your repo is ready, push to GitHub
* [ ] Keep your project up to date, when you're finished flag it as [archived](https://help.github.com/en/github/creating-cloning-and-archiving-repositories/archiving-repositories).

## Questions or Recommendations

We welcome any feedback and ideas for how to make these practices more useful. [Please submit ideas using the built-in issues function of this project](https://github.com/CDCgov/template/issues).

## References

Many existing projects and resources helped us create this set of practices.

* [CFPB Open Tech](https://cfpb.github.io/)
* [TTS Engineering Practices Guide](https://engineering.18f.gov/)
* [18F Open Source Policy](https://github.com/18F/open-source-policy) and [Practicing our open source policy](https://github.com/18F/open-source-policy/blob/master/practice.md)
* [GitHun and Government: How agencies build software](https://government.github.com/)
* [code.gov](https://code.gov)
* [Federal Source Code and Open Source Toolkit](https://github.com/GSA/code-gov-open-source-toolkit)
* [Federal Source Code Policy (M-16-21)](https://sourcecode.cio.gov/)
* [openCDC](https://open.cdc.gov)
* [CDC/ATSDR Policy on Releasing and Sharing Data](https://www.cdc.gov/maso/Policy/ReleasingData.pdf)
* [Digital Services Playbook](https://playbook.cio.gov/)
