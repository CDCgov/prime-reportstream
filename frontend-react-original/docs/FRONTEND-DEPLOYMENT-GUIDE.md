# Frontend-React Production Deployment Guide

Complete guide for deploying the frontend-react application to production.

**Note:** Trial frontend environments (trialfrontend01-03) and demo environments (demo1-3) have been deprecated and are no longer supported.

---

## How to Deploy Frontend-React to Production

### Step-by-Step Process

1. **Merge to Production Branch**
   - Create a pull request targeting the `production` branch
   - Ensure all CI checks pass (unit tests, E2E tests, linting, accessibility)
   - Get PR approval and merge

2. **Automated Pipeline Triggers**
   - Two workflows automatically start:
     - `release_to_azure.yml` - Deploys to Azure blob storage
     - `release_to_github.yml` - Creates GitHub release

3. **Build Phase**
   - Node.js 20.15.1 environment setup
   - Dependencies installed via `yarn install`
   - Linting runs (`yarn lint`)
   - Unit tests run (`yarn test:ci`)
   - E2E tests run across 6 parallel shards (`yarn test:e2e`)
   - Production build executes: `yarn build:production`
   - Build artifact created: `static-website-react-v-YYYY.MM.DD-HHMMSS.tar.gz`

4. **Approval Gate**
   - Workflow pauses for manual approval
   - GitHub environment: `prod-frontend`
   - Requires authorized approver to proceed

5. **Deployment Phase**
   - Establishes VPN connection to Azure
   - Authenticates via Azure Service Principal
   - Downloads build artifact
   - Deletes existing content from Azure Storage `$web` container
   - Uploads new files to `pdhprodpublic` storage account
   - Uses Azure CLI: `az storage blob upload-batch`

6. **GitHub Release**
   - Creates versioned release (format: `v-YYYY.MM.DD-HHMMSS`)
   - Attaches build artifacts
   - Generates changelog from commits

---

## Key Configuration Details

### Production Environment Variables

- **Okta Client ID:** `0oa376pah9o4G2HVJ4h7`
- **Okta URL:** `https://reportstream.okta.com`
- **Application Insights:** Production key injected during build
- **Azure Storage:** `pdhprodpublic` blob storage account
- **Container:** `$web` (static website hosting)

### Build Command

```bash
yarn build:production
```

### Critical Files

- **Workflow:** `.github/workflows/release_to_azure.yml`
- **Build Action:** `.github/actions/build-frontend/action.yml`
- **Deploy Action:** `.github/actions/deploy-frontend/action.yml`
- **Env Config:** `frontend-react/.env.production`

---

## Testing Requirements Before Production

All deployments must pass:

1. **Linting:** ESLint + Prettier checks
2. **Unit Tests:** 561+ tests via Vitest
3. **E2E Tests:** Playwright tests across 6 shards
4. **Accessibility:** Pa11y WCAG2AA compliance (on PRs)
5. **Visual Regression:** Chromatic Storybook checks (optional)

---

## Security & Compliance

- **Container Security:** Uses Chainguard hardened images
- **VPN:** Azure access via OpenVPN with TLS 1.2+
- **Authentication:** Azure Service Principal with least privilege
- **508 Compliance:** WCAG2AA accessibility testing
- **Approval Required:** Manual gate prevents accidental deployments

---

## Alternative Deployment Branches

- **Staging:** Merge to `main` → deploys to `pdhstagingpublic`
- **Test:** Merge to `test` → deploys to `pdhtestpublic`

---

## Monitoring Deployment

1. **GitHub Actions Tab:** Monitor workflow progress
2. **Approval Queue:** Check pending approvals in environment protection rules
3. **Azure Portal:** Verify blob storage updates in `pdhprodpublic/$web`
4. **GitHub Releases:** Confirm release creation with artifacts
5. **Application Insights:** Monitor production telemetry

---

## Rollback Procedure

If deployment issues occur:

1. Identify last known good version from GitHub Releases
2. Create hotfix branch from that tag
3. Merge to `production` to trigger redeployment
4. Or manually upload previous build artifact to Azure Storage

---

**Document Version:** 1.1
**Last Updated:** 2025-12-09
**Maintained By:** Prime ReportStream Team
