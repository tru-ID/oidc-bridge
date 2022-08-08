# Auth0 OIDC integration

tru.ID leverages [Auth0 Actions](https://auth0.com/docs/customize/actions) to provide an MFA login experience.

The first factor will be the Auth0 login experience (either using SSO or username/password).

The second factor will be controlled by our OIDC bridge ([more info](../bridge/README.md)).

## Pre-Requisites

* An Auth0 tenant
* tru.ID Login Action

## Configuration

The [.example.env](./.example.env) file contains all the environment variables necessary to run a
full (`sample-ui`+`bridge`+`auth0`) flow and how to populate them.

Some important variables:

* `SAMPLE_AUTH0_API_KEY` - used by the bridge to query the Auth0 Users endpoint
* `SAMPLE_AUTH0_SIGNATURE_SECRET` - shared secret to encode JWT exchanged between the `bridge` and the `action`

## Create an OIDC Application to try the flow through the `sample-ui`

The `sample-ui` project in this repository showcases an OIDC login using the IAM provider as the identity provider.

To use it, create an [regular web application in your Auth0 tenant](https://auth0.com/docs/get-started/applications) 
with the following configuration:

* Application Login Uri - `<ngrok-endpoint>/sample-ui/login`
* Allowed Callback URLs - `<ngrok-endpoint>/sample-ui/login/callback/iam`
* Allowed Logout URLs - `<ngrok-endpoint>/sample-ui`
* Allowed Web Origins - `<ngrok-endpoint>`

Afterwards, you should fill in the `sample-ui` configuration section in the [.example.env](./.example.env) file.
