name: Deploy Terraform

on:
  push:
    branches:
      - main
      - production
    paths:
      - '**.tf'

env:
  AZURE_CREDENTIALS: '{"clientId":"${{ secrets.AZURE_CLIENT_ID }}","clientSecret":"${{ secrets.AZURE_CLIENT_SECRET }}","subscriptionId":"${{ secrets.AZURE_SUBSCRIPTION_ID }}","tenantId":"${{ secrets.AZURE_TENANT_ID }}"}'

jobs:
  pre_job:
    name: Set Build Environment
    concurrency: 
      group: ${{ github.workflow }}-${{ needs.pre_job.outputs.env_name }}
      cancel-in-progress: true
    runs-on: ubuntu-latest
    outputs:
      env_name: ${{ steps.build_vars.outputs.env_name }}
      tf_change: ${{ steps.build_vars.outputs.has_terraform_change }}
    steps:
      - name: Check out changes
        uses: actions/checkout@v4
      - name: Build vars
        id: build_vars
        uses: ./.github/actions/build-vars

  check_for_deprecations:
    name: Check for Deprecated Resources
    runs-on: ubuntu-latest
    needs:
      - pre_job
    steps:
      - name: Check Out Changes
        uses: actions/checkout@v4
      - name: Run Deprecation Check
        run: ./scripts/check_deprecations.sh

  confirm_changes:
    name: Check Terraform Stats - ${{ needs.pre_job.outputs.env_name }}
    if: ${{ needs.pre_job.outputs.tf_change == 'true' }}
    concurrency: 
      group: ${{ github.workflow }}-${{ needs.pre_job.outputs.env_name }}
      cancel-in-progress: true
    needs:
      - pre_job
    environment: ${{ needs.pre_job.outputs.env_name }}
    runs-on: ubuntu-latest
    outputs:
      change_count: ${{ steps.stats1.outputs.change-count }}
    steps:
      - name: Check Out Changes
        uses: actions/checkout@v4
      - name: Connect to VPN and login to Azure
        uses: ./.github/actions/vpn-azure
        with:
          env-name: ${{ needs.pre_job.outputs.env_name }}
          tls-key: ${{ secrets.TLS_KEY }}
          ca-cert: ${{ secrets.CA_CRT }}
          user-crt: ${{ secrets.USER_CRT }}
          user-key: ${{ secrets.USER_KEY }}
          sp-creds: ${{ env.AZURE_CREDENTIALS }}
          tf-auth: true
      - name: Collect Terraform stats
        uses: ./.github/actions/terraform-stats
        id: stats1
        with:
          terraform-directory: operations/app/terraform/vars/${{ needs.pre_job.outputs.env_name }}
          terraform-version: 1.7.4
          add-args: "-refresh=false"
      - name: Terraform Format
        # fails on formatting issues, fix locally with `tf fmt -recursive` and push again if this step fails
        run: terraform fmt -check -recursive
      - name: Terraform Init
        run: terraform init -input=false
      - name: Terraform Validate
        run: terraform validate
      - name: Terraform Plan
        run: terraform plan -out=tf.plan -input=false -no-color -lock-timeout=600s
      - name: Comment Plan on PR
        uses: blinqas/tf-plan-pr-comment@v1
        with:
          output_file: ${{ github.workspace }}/plan_output.txt
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

  approve_deploy:
    name: Approve Deploy - ${{ needs.pre_job.outputs.env_name }}
    concurrency: 
      group: ${{ github.workflow }}-${{ needs.pre_job.outputs.env_name }}
      cancel-in-progress: true
    needs:
      - pre_job
      - check_for_deprecations
      - confirm_changes
    if: needs.confirm_changes.outputs.change_count > '0'
    runs-on: ubuntu-latest
    environment: ${{ needs.pre_job.outputs.env_name }}_terraform
    steps:
      - name: Echo change count
        run: echo ${{ needs.confirm_changes.outputs.change_count }}

  run_deploy:
    name: Run Deploy - ${{ needs.pre_job.outputs.env_name }}
    concurrency: 
      group: ${{ github.workflow }}-${{ needs.pre_job.outputs.env_name }}
      cancel-in-progress: true
    needs:
      - pre_job
      - check_for_deprecations
      - approve_deploy
    if: needs.confirm_changes.outputs.change_count > '0'
    runs-on: ubuntu-latest
    environment: ${{ needs.pre_job.outputs.env_name }}
    defaults:
      run:
        working-directory: operations/app/terraform/vars/${{ needs.pre_job.outputs.env_name }}
    steps:
      - name: Check Out Changes
        uses: actions/checkout@v4
      - name: Connect to VPN and login to Azure
        uses: ./.github/actions/vpn-azure
        with:
          env-name: ${{ needs.pre_job.outputs.env_name }}
          tls-key: ${{ secrets.TLS_KEY }}
          ca-cert: ${{ secrets.CA_CRT }}
          user-crt: ${{ secrets.USER_CRT }}
          user-key: ${{ secrets.USER_KEY }}
          sp-creds: ${{ env.AZURE_CREDENTIALS }}
          tf-auth: true
      - name: Use specific version of Terraform
        uses: hashicorp/setup-terraform@v1
        with:
          terraform_version: 1.7.4  # Specify the version here
      - name: Terraform Init
        run: terraform init -input=false
      - name: Run Terraform Sanity Checks
        run: |
          terraform fmt -check -recursive
          terraform validate
          terraform workspace select -or-create ${{ needs.pre_job.outputs.env_name }}
      - name: Run Terraform Plan
        run: |
          terraform plan -out ${{ needs.pre_job.outputs.env_name }}-tf.plan
      - name: Run Terraform Apply
        run: |
          terraform apply -input=false -no-color -lock-timeout=600s -auto-approve ${{ needs.pre_job.outputs.env_name }}-tf.plan
