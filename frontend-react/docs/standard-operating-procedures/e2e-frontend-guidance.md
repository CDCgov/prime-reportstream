# E2E Guidance

## Run an E2E test on a public page vs authenticated page

Before a test(s) is run the admin, receiver, and sender are authenticated and credentials are saved in session storage.

This process allows us to seamlessly go from public to authenticated tests without having to authenticate before each test.

To run a test as an authenticated user, you will need to specify which storageState to use before the test(s).

```
test.describe("admin user - a group of tests", () => {
    test.use({ storageState: "e2e/.auth/admin.json" })

    test("a test"", async () => {
        // test something
    });
})
```

Currently, we test all happy path scenarios using the admin account.

---

## Troubleshooting E2E

If your e2e tests fail during the GitHub build. The following steps will help in troubleshooting.

1. Click on 'Details' for the Frontend / Build Frontend React(push) task
2. Either in the left panel or in the Annotations section, select the 'Build Frontend React' link (you should see a red x icon).
3. You can now view the errors that occurred on the server and if you scroll to the bottom, you can download the artifact.
4. Once the artifact is downloaded, navigate to your Downloads -> e2e-data -> report folder and opn the index.html file.
5. This will contain the snapshot of each test that failed and on which step the failure occurred.

---

## Updating the TEST\_\*\_USERNAME OKTA accounts

1. Change the password in OKTA for the `TEST_*_USERNAME` user account(s).
2. Have DevOps update the [GitHub Action](https://github.com/CDCgov/prime-reportstream/settings/secrets/actions) `TEST_*_PASSWORD` secret(s) for the `TEST_*_USERNAME` user account(s).
3. Have DevOps update the [Dependabot](https://github.com/CDCgov/prime-reportstream/settings/secrets/dependabot) `TEST_*_PASSWORD` secret(s) for the `TEST_*_USERNAME` user account(s).

---

## Updating the TEST\_\*\_USERNAME OKTA accounts

1. Change the password in OKTA for the `TEST_*_USERNAME` user account(s).
2. Have DevOps update the [GitHub Action](https://github.com/CDCgov/prime-reportstream/settings/secrets/actions) `TEST_*_PASSWORD` secret(s) for the `TEST_*_USERNAME` user account(s).
3. Have DevOps update the [Dependabot](https://github.com/CDCgov/prime-reportstream/settings/secrets/dependabot) `TEST_*_PASSWORD` secret(s) for the `TEST_*_USERNAME` user account(s).

---

## Processes

Always run the e2e tests before committing to GitHub

```bash
CI=true yarn run test:e2e-ui # Runs a local instance of Playwright UI that mimics GitHub integration
```
