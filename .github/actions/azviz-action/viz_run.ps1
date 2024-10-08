Param(
    [Parameter(Mandatory)]
    [String]$RESOURCE_GROUP,
    [Parameter(Mandatory)]
    [String]$OUT_FILE = 'viz.svg',
    [Parameter(Mandatory)]
    [String]$SUB_NAME = 'Pay-As-You-Go',
    [Parameter(Mandatory)]
    [String]$THEME = 'neon',
    [Parameter(Mandatory)]
    [String]$DEPTH = '1',
    [Parameter(Mandatory)]
    [String]$VERBOSITY = '1',
    [Parameter(Mandatory)]
    [String]$FORMAT = 'svg',
    [Parameter(Mandatory)]
    [String]$DIRECTION = 'top-to-bottom',
    [String]$EXCLUDE_TYPES = '*excludethisthing1,excludethisthing2*',
    [Parameter(Mandatory)]
    [String]$SPLINES = 'spline'
)

# Create missing directory paths for output
New-Item -ItemType File -Force -Path ${OUT_FILE}

# Get current Azure context
$currentAzureContext = Get-AzContext;

# Check If Azure context exists
if ($currentAzureContext.Tenant.TenantId) {

    # Set Azure subscription to match SUB_NAME
    Set-AzContext -SubscriptionName ${SUB_NAME};
};

# Run AzViz and export Azure diagram to location OUT_FILE
Export-AzViz `
    -ResourceGroup ${RESOURCE_GROUP}.Split(",") `
    -Theme ${THEME} `
    -OutputFormat ${FORMAT} `
    -CategoryDepth ${DEPTH} `
    -LabelVerbosity ${VERBOSITY} `
    -ExcludeTypes ${EXCLUDE_TYPES}.Split(",") `
    -Splines ${SPLINES} `
    -Direction ${DIRECTION} `
    -OutputFilePath ${OUT_FILE};

if (${FORMAT} -eq 'svg') {

    # Move svg embedded png to output directory
    ((Get-Content -path ${OUT_FILE} -Raw) -replace '(?<=xlink:href\=").+?(?=icons)','') | Set-Content -Path ${OUT_FILE}
    $ICON_PATH=$(Split-Path -Path ${OUT_FILE})+'/icons/'
    Write-Host "Moving ${HOME}/*/AzViz/* icons to ${ICON_PATH}"
    New-Item -ItemType Directory -Force -Path ${ICON_PATH}
    Get-Childitem -Path ${HOME} -Force -recurse -include *.png -ErrorAction SilentlyContinue | Move-Item -dest ${ICON_PATH} -Force

};