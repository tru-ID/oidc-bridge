param (
    [Parameter(Mandatory = $true)]
    [string]
    $ApiKey,
    [Parameter(Mandatory = $true)]
    [string]
    $Domain,
    [Parameter(Mandatory = $true)]
    [string]
    $NgrokTargetUri,
    [Parameter(Mandatory = $true)]
    [string]
    $AppId
)

$AppsApiUri = "https://$Domain/api/v1/apps/$AppId"
$Headers = @{ Authorization = "SSWS $ApiKey" }

$OktaApp = Invoke-RestMethod -Uri $AppsApiUri -Headers $Headers

$RedirectUrisFiltered = $OktaApp.settings.oauthClient.redirect_uris -notlike "https://*.ngrok.io/okta/callback"
$NewUri = "$NgrokTargetUri/okta/callback"

# update redirect uris
$OktaApp.settings.oauthClient.redirect_uris = $RedirectUrisFiltered + $NewUri

Write-Debug "Updating the OktaApp with following redirect_uris:"
Write-Debug (ConvertTo-Json $OktaApp.settings.oauthClient.redirect_uris)

$PutParams = @{
    Method = 'Put'
    ContentType = 'application/json'
    Headers = $Headers
    Body = ConvertTo-Json -Depth 10 $OktaApp
    Uri = $AppsApiUri
}

Invoke-RestMethod @PutParams