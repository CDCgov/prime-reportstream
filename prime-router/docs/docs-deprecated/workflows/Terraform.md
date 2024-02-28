GitHub repositories are source control repositories using Git version control along with additional features provided by GitHub, like pull requests and forking. GitHub offers a rich set of code collaboration tools that can be used to streamline the software development process as well as the infrastructure as code development process. The same GitHub version control workflow of using GitHub projects, git repositories, pull requests, and other features applies to managing infrastructure as code (IaC) deployments with HashiCorp Terraform as it does to application code builds and deployments.

**GitHub Action to run Terraform Plan**
The Terraform project code is located within the \prime-reportstream\operations\app\terraform\modules folder of the GitHub repository inside the main branch. The GitHub Actions are lo
cated inside  \prime-reportstream\.github\workflows\ example is used at various workflows in the folder and can be easily modified to handle any other configuration.

The following shows a simple GitHub Action to run the Terraform init, and plan commands, then store the Terraform Plan using a commit to the GitHub repository:
name: Terraform Plan
<div style="background-color:rgba(0, 0, 0, 0.0470588); text-align:center; vertical-align: middle; padding:40px 0; margin-top:30px">

# configure to run on merges to 'main' branchbranchon:
  push:
    branches: [ master ]

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout code
      uses: actions/checkout@v2

    # be sure to authenticate with cloud provider
    - name: Login to Microsoft Azure
      uses: azure/login@v1
      with:
        creds: ${{ secrets.AZURE_CREDENTIALS }}

    - name: Setup Terraform
      uses: hashicorp/setup-terraform@v2

    - name: Terraform Init
      run: terraform init

    # generate Terraform Plan and store in .tfplan file
    - name: Terraform Plan
      run: terraform plan -out:terraform.tfplan

    # commit Terraform plan if running on 'main' branch
    # commit is made to the 'terraform-plan' branch
    - name: Commit Terraform Plan for Approval
      if: github.ref == 'refs/heads/main'
      uses: peter-evans/create-pull-request@v4
      with:
        commit-message: 'Terraform Plan'
        branch: 'terraform-plan'
        base: 'main'
        token: ${{ secrets.GITHUB_TOKEN }}

</div>
The previous GitHub Action example runs the following steps:
<body>
<ol type = "1">
<li>GitHub Action runs on pushes to the master branch</li>
<li>Repository code is checked out</li>
<li>Cloud provider is authenticated. In this example, Microsoft Azure is authenticated against.</li>
<li>Terraform will be setup using the hashicorp/setup-terraform action.</li>
<li>The Terraform init command is to initialize the Terraform project.</li>
<li>The Terraform plan command is to generate the Terraform plan for later approval.</li>
<li>The generated Terraform Plan file (.tfplan) is committed to the GitHub repository under the terraform-plan branch.</li>
</ol>
The next step left out of this GitHub Action is the Terraform Apply step. Generally, the Terraform Apply is going to be run manually so that the Terraform plan can be verified before execution.
 &nbsp;  
**GitHub Action to Run Terraform Apply**
Once the previous GitHub Action is run when Terraform code is pushed to the main branch of the GitHub repository, then another GitHub Action is necessary for the manual step of approving the execution of the plan to apply the infrastructure changes to the cloud environment. Sure, the Terraform apply can be automatically run after the plan is generated, but it’s best practice to use a manual step to verify the plan before applying it. This is done to ensure the cloud infrastructure environment is modified only in a safe way, just in case there are any errors in the Terraform code or unexpected infrastructure changes triggered by the latest Terraform code changes.
The following is an example of a GitHub Action that will run on the terraform-plan branch in the GitHub repository when triggered manually, then execute the previously generated plan:
 &nbsp; 
name: Terraform Apply

# configure manual trigger for GitHub Action
on:
  workflow_dispatch:
    branches: [ terraform-plan ]

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout code
      uses: actions/checkout@v2

    # be sure to authenticate with cloud provider
    - name: Login to Microsoft Azure
      uses: azure/login@v1
      with:
        creds: ${{ secrets.AZURE_CREDENTIALS }}

    - name: Setup Terraform
      uses: hashicorp/setup-terraform@v2

    - name: Terraform Init
      run: terraform init

    # apply previously generated Terraform Plan
    # from the terraform.tfplan file in the branch
    - name: Terraform Apply
      run: terraform apply "terraform.tfplan"
       &nbsp; 

The previous GitHub Action runs the following steps:
<ol type = "1">
<li>GitHub Action runs on pushes to the master branch.</li>
<li>Repository code is checked out.</li>
<li>Cloud provider is authenticated. In this example, Microsoft Azure is authenticated against.</li>
<li>Terraform is setup using the hashicorp/setup-terraform action.</li>
<li>The Terraform init command is run to initialize the Terraform project.</li>
<li>The Terraform apply command is run, passing in the terraform.tfplan file to tell it what Terraform Plan to apply.</li>
</ol>
&nbsp; 
The GitHub Actions to automatically generate a Terraform Plan and then manually trigger the Terraform Apply of the plan in this article is a very simplistic example. This code can be easily modified to extend it to handle any custom deployment scenarios. When using this example and modifying it, be sure to use any GitHub Actions and Terraform best practices to ensure the security of the infrastructure environments being deployed.

</body>