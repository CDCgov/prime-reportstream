# runleaks

[![Scan Action Logs](https://github.com/JosiahSiegel/runleaks/actions/workflows/main.yml/badge.svg?branch=main)](https://github.com/JosiahSiegel/runleaks/actions/workflows/main.yml)

Leverages [git-secrets](https://github.com/awslabs/git-secrets) to identify potential leaks in GitHub action run logs.

 * Common Azure and Google Cloud patterns are available, thanks to fork [msalemcode/git-secrets](https://github.com/msalemcode/git-secrets).


## Inputs
```yml
  github-token:
    description: 'Token used to login to GitHub'
    required: true
  repo:
    description: 'Repo to scan run logs for exceptions'
    required: false
    default: ${{ github.repository }}
  run-limit:
    description: 'Limit on how many runs to scan'
    required: false
    default: '50'
  min-days-old:
    description: 'Min age of runs in days'
    required: false
    default: '0'
  max-days-old:
    description: 'Max age of runs in days'
    required: false
    default: '3'
  patterns-path:
    description: 'Patterns file path'
    required: false
    default: ".runleaks/patterns.txt"
  exclusions-path:
    description: 'Excluded patterns file path'
    required: false
    default: ".runleaks/exclusions.txt"
  fail-on-leak:
    description: 'Fail action if leak is found'
    required: false
    default: true
```

## Outputs
```yml
  exceptions:
    description: 'Json output of run logs with exceptions'
  count:
    description: 'Count of exceptions'
```

## Usage
 * Note: [GitHub rate limits](#rate-limits)
```yml
      - name: Checkout
        uses: actions/checkout@v3
      - name: Scan run logs
        uses: josiahsiegel/runleaks@v1
        id: scan
        with:
          github-token: ${{ secrets.GITHUB_TOKEN }}
          run-limit: 500
          fail-on-leak: false
      - name: Get scan exceptions
        if: steps.scan.outputs.count > 0
        run: echo "${{ steps.scan.outputs.exceptions }}"
```
or
```yml
      - name: Checkout
        uses: actions/checkout@v3
      - name: Scan run logs
        uses: josiahsiegel/runleaks@v1
        id: scan
        with:
          github-token: ${{ secrets.MY_TOKEN }}
          patterns-path: ".github/patterns.txt"
          exclusions-path: ".github/exclusions.txt"
          fail-on-leak: false
      - name: Get scan exceptions
        if: steps.scan.outputs.count > 0
        run: echo "${{ steps.scan.outputs.exceptions }}"
```
or
```yml
      - name: Checkout
        uses: actions/checkout@v3
        with:
          repository: 'me/my-repo'
      - name: Scan run logs
        uses: josiahsiegel/runleaks@v1
        id: scan
        with:
          github-token: ${{ secrets.MY_TOKEN }}
          repo: 'me/my-repo'
          run-limit: 200
          min-days-old: 0
          max-days-old: 4
          fail-on-leak: true
```

## Local testing
  * Registers default patterns
```sh
git clone https://github.com/JosiahSiegel/runleaks.git
cd runleaks/
docker build -t runleaks .
docker run scan "<PERSONAL_ACCESS_TOKEN>" "<REPO>" <RUN_LIMIT> <MIN_DAYS_OLD> <MAX_DAYS_OLD>
```

## Pattern file
 * Default location: `.runleaks/patterns.txt`

```
####################################################################

# Register a secret provider
#--register-azure
#--register-gcp
--register-aws

####################################################################

# Add a prohibited pattern
--add [A-Z0-9]{20}
--add Account[k|K]ey
--add Shared[a|A]ccessSignature

####################################################################

# Add a string that is scanned for literally (+ is escaped):
--add --literal foo+bar

####################################################################
```

## Exclusion file
 * Default location: `.runleaks/exclusions.txt`
```
####################################################################

# Add regular expressions patterns to filter false positives.

# Allow GUID
("|')[0-9A-Fa-f]{8}-([0-9A-Fa-f]{4}-){3}[0-9A-Fa-f]{12}("|')

####################################################################
```

## Performance

 * Scan 50 runs = 1 min

 * Scan 500 runs = 8 mins

* Scan 3000 runs = 50 mins

## Rate limits

Built-in secret `GITHUB_TOKEN` is [limited to 1,000 requests per hour per repository](https://docs.github.com/en/rest/overview/resources-in-the-rest-api#requests-from-github-actions).

To avoid repo-wide rate limiting, personal access tokens can be added to secrets, which are [limited to 5,000 requests per hour and per authenticated user](https://docs.github.com/en/rest/overview/resources-in-the-rest-api#requests-from-personal-accounts).
