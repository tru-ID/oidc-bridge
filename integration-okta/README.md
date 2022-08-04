# Okta OIDC integration

tru.ID leverages Okta 3rd party identity providers to provide a passwordless login experience.



The second factor will be controlled by our OIDC bridge ([more info](./bridge/README.md)).

## Pre-Requisites

* An Okta tenant
* An OIDC identity provider pointing to tru.ID
* An Application to initiate Okta logins through the `sample-ui`

## Configuration

The [.example.env](./.example.env) file contains all the environment variables necessary to run a
full (`sample-ui`+`bridge`+`okta`) flow and how to populate them.

## Create the tru.ID Identity Provider

Create a [enterprise OIDC identity provider](https://developer.okta.com/docs/guides/add-an-external-idp/openidconnect/main/)
with the following configuration:

* Client ID and Client Secret - same as tru.ID OIDC client credentials, created through your tru.ID project
* Endpoints:
    * Issuer - https://eu.api.tru.id/
    * Authorization Endpoint - https://eu.api.tru.id/oauth2/v1/auth
    * Token Endpoint - https://eu.api.tru.id/oauth2/v1/token
    * JWKS Endpoint - https://eu.api.tru.id/oidc/.well-known/jwks.json
* Authentication Settings:
    * IdP Username - `idpuser.externalId`
    * Match Against - `Okta Username`

Once you've created the identity provider, capture it's IdP ID since it's necessary in for the `sample-ui` configuration.

## Create an OIDC Application to try the flow through the `sample-ui`

The `sample-ui` project in this repository showcases an OIDC login using the IAM provider as the identity provider.

Create an [Application in your Okta tenant](https://help.okta.com/en-us/Content/Topics/Apps/Apps_App_Integration_Wizard_OIDC.htm) 
with the following configuration:

* Sign-in redirect URIs - `<ngrok-endpoint>/sample-ui/login/callback/iam`
* Sign-out redirect URIs - `<ngrok-endpoint>/sample-ui`

Afterwards, you should fill in the `sample-ui` configuration section in the [.example.env](./.example.env) file.
