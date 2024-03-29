param (
    [Parameter(Mandatory = $true)]
    [hashtable]
    $Config
)

Set-StrictMode -Version latest
$ErrorActionPreference = "Stop"

$NgrokTargetUri = $Config.SAMPLE_PUBLIC_URL

$Bytes = [System.Text.Encoding]::UTF8.GetBytes("$($Config.TRU_WORKSPACE_CLIENT_ID):$($Config.TRU_WORKSPACE_CLIENT_SECRET)")
$BasicAuth = [Convert]::ToBase64String($Bytes)

$TruIDBaseUrl = $Config.TRU_API_BASE_URL
$TokenBody = @{
    "grant_type" = "client_credentials"
    "scope"      = "console"
}
$Headers = @{ "Authorization" = "Basic $BasicAuth" }
$AccessToken = (Invoke-RestMethod -Method Post -Body $TokenBody -Headers $Headers -Uri "$TruIDBaseUrl/oauth2/v1/token").access_token

Write-Debug "Updating OIDC..."
$OIDCParams = @{
    NgrokTargetUri = $NgrokTargetUri
    WorkspaceId    = $Config.TRU_WORKSPACE_ID
    ProjectId      = $Config.TRU_PROJECT_ID
    AccessToken    = $AccessToken
    ApiBaseUrl     = $TruIDBaseUrl
    RedirectUri    = $Config.SAMPLE_PINGONE_REDIRECT_URL
}
$null = ./Update-OIDCClient @OIDCParams

Write-Debug "Updating Authenticator Issuer..."
$AuthenticatorParams = @{
    NgrokTargetUri = $NgrokTargetUri
    WorkspaceId    = $Config.TRU_WORKSPACE_ID
    ProjectId      = $Config.TRU_PROJECT_ID
    AccessToken    = $AccessToken
    ApiBaseUrl     = $TruIDBaseUrl
}
$null = ./Update-Issuer @AuthenticatorParams

Write-Debug "All OK"