param (
    [Parameter(Mandatory = $true)]
    [string]
    $NgrokTargetUri,
    [Parameter(Mandatory = $true)]
    [string]
    $WorkspaceId,
    [Parameter(Mandatory = $true)]
    [string]
    $ProjectId,
    [Parameter(Mandatory = $true)]
    [string]
    $AccessToken,
    [Parameter(Mandatory = $true)]
    [string]
    $ApiBaseUrl,
    [Parameter()]
    [string]
    $RedirectUri
)

$AuthHeaders = @{Authorization = "Bearer $AccessToken" }
$BaseUri = "$($ApiBaseUrl)/console/v0.2/workspaces/$($WorkspaceId)/projects/$($ProjectId)"

$Project = Invoke-RestMethod -Method Get -Headers $AuthHeaders -Uri $BaseUri

$Configuration = $Project.configuration | ConvertTo-Json -Depth 5 | ConvertFrom-Json -AsHashtable

Write-Debug "Updating the following config:"
Write-Debug (ConvertTo-Json -Depth 5 $Configuration)

$Configuration.oidc = @{
    flow_post_url   = "$NgrokTargetUri/bridge/oidc-login"
    flow_result_url = "$NgrokTargetUri/bridge/result"
    login_hint_label = "Okta Username"
}

if ($RedirectUri) {
    $Configuration.oidc.flow_result_url = $RedirectUri
}

Write-Debug "New config:"
Write-Debug (ConvertTo-Json -Depth 5 $Configuration)

$PatchOperation = @(@{
        op    = "replace"
        path  = "/configuration"
        value = $Configuration
    })

$UpdateProjectParams = @{
    Method      = "Patch"
    Headers     = $AuthHeaders
    ContentType = "application/json-patch+json"
    Body        = (ConvertTo-Json -Depth 5 $PatchOperation)
    Uri         = $BaseUri
}

Invoke-RestMethod @UpdateProjectParams
