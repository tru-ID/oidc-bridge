param (
    [Parameter(Mandatory = $true)]
    [string]
    $ApiKey,
    [Parameter(Mandatory = $true)]
    [string]
    $Domain,
    [Parameter(Mandatory = $true)]
    [string]
    $IdpId,
    [Parameter(Mandatory = $true)]
    [string]
    $ApiBaseUrl,
    [Parameter(Mandatory = $true)]
    [string]
    $TruOidcClientId,
    [Parameter(Mandatory = $true)]
    [string]
    $TruOidcClientSecret
)

$IdpsApiUri = "https://$Domain/api/v1/idps/$IdpId"
$Headers = @{ Authorization = "SSWS $ApiKey" }

$OktaIdp = Invoke-RestMethod -Uri $IdpsApiUri -Headers $Headers

$IssuerUrl = "$ApiBaseUrl/" # note the slash here is important
$AuthorizationUrl = "$ApiBaseUrl/oauth2/v1/auth"
$TokenUrl = "$ApiBaseUrl/oauth2/v1/token"
$JwksUrl = "$ApiBaseUrl/oidc/.well-known/jwks.json"

Write-Debug "Updating OktaIdp name=$($OktaIdp.name) with the following config:"
Write-Debug "Issuer: $IssuerUrl"
Write-Debug "Authorization URL: $AuthorizationUrl"
Write-Debug "Token URL: $TokenUrl"
Write-Debug "JwksUrl URL: $JwksUrl"

$OktaIdp.protocol.issuer.url = $IssuerUrl
$OktaIdp.protocol.endpoints.authorization.url = $AuthorizationUrl
$OktaIdp.protocol.endpoints.token.url = $TokenUrl
$OktaIdp.protocol.endpoints.jwks.url = $JwksUrl
$OktaIdp.protocol.credentials.client.client_id=$TruOidcClientId
$OktaIdp.protocol.credentials.client.client_secret=$TruOidcClientSecret

$PutParams = @{
    Method = 'Put'
    ContentType = 'application/json'
    Headers = $Headers
    Body = ConvertTo-Json -Depth 10 $OktaIdp
    Uri = $IdpsApiUri
}

Invoke-RestMethod @PutParams