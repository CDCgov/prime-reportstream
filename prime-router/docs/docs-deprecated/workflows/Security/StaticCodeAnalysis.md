**Static Code Analysis**
&nbsp;  

The purpose of the Static Code Analysis wiki is to describe Static Code Analysis, how it is done, Benefits using Static Code analysis and Integrating with SDLC.
&nbsp;  
**What**

Static code analysis is a method of computer program debugging that is done by examining the code without executing the program. The process provides an understanding of the code structure and can help ensure that the code adheres to industry standards. Static analysis is used in software engineering by software development and quality assurance teams. Automated tools like Veracode can assist programmers and developers in carrying out static analysis. The software will scan all code in a project to check for vulnerabilities while validating the code.
&nbsp;  
Static analysis is generally good at finding coding issues such as:
•	Programming errors
•	Coding standard violations
•	Undefined values
•	Syntax violations
•	Security vulnerabilities
The static analysis process is also useful for addressing weaknesses in source code that could lead to buffer overflows -- a common software vulnerability.
**How**
The static analysis process is relatively simple, as long as it's automated. Generally, static analysis occurs before software testing in early development. In the DevOps development practice, it will occur in the create phases.
Once the code is written, a static code analyzer should be run to look over the code. It will check against defined coding rules from standards or custom predefined rules. Once the code is run through the static code analyzer, the analyzer will have identified whether or not the code complies with the set rules. It is sometimes possible for the software to flag false positives, so it is important for someone to go through and dismiss any. Once false positives are waived, developers can begin to fix any apparent mistakes, generally starting from the most critical ones. Once the code issues are resolved, the code can move on to testing through execution.
Manual process of Static analysis will take a lot of work and effort to review the code and figure out how it will behave in runtime environments. Therefore, it's a good idea to find a tool that automates the process. Getting rid of any lengthy processes will make for a more efficient work environment.

&nbsp;  
**Benefits**
<ul><li>
**Speed**&nbsp;  
It Can find the bugs early in development cycle, which means less cost to fix them and increase in software delivery speed. All the advantages of static code analyzer can be best utilized only if it is part of build process.
</li>
<li>
**Depth**&nbsp;  
It helps in thorough analysis of your code, without executing them at a faster pace. Static analysis scans ALL code. If there are vulnerabilities in the distant corners of your application, which are not even used, then also static analysis has a higher probability of finding those vulnerabilities.
</li>
<li>
**Accuracy**&nbsp;  
Static code analysis can help define project specific rules to assist developers, and they will be ensured to follow without any manual intervention. If any Developer forget to follow those rules, they will be highlighted by static code analyzer.
</li>
</ul>
&nbsp;  
**Integrating with SDLC**
The Software Development Lifecycle (SDLC) outlines the stages that a development team passes through when creating, deploying, and maintaining software. This includes everything from the initial planning stages to long-term maintenance and eventual end-of-life.
&nbsp;  
Applying security earlier in the SDLC is cheaper and more efficient for an organization. The later the issues are discovered in the SDLC, the more difficult they are to correct and the more work that may need to be redone as a result.
&nbsp;  
A major advantage of SAST is that it can be applied to source code, including incomplete applications. This makes it possible to apply it earlier in the SDLC rather than relying on a functional and executable version of the application. This makes it possible for SAST to identify certain types of errors and vulnerabilities when they can be corrected more easily and cheaply.
&nbsp;  
**CodeQL – SAST code analysis**
CodeQL is the code analysis engine developed by GitHub to automate security checks. You can analyze your code using CodeQL and display the results as code scanning alerts.
&nbsp;  
Prime report stream has been configured to run CodeQL code scans on pushes to master, production branches and on pull request from any branch to master branch.


