# PingID DaVinci connector integration

tru.ID leverages the DaVinci Connector platform in order to provide an MFA login experience.

The login experience is controlled through a DaVinci Flow, where you can plug the tru.ID connector at
any stage of that flow in order to trigger the phone verification.

The phone verification factor will be controlled by our OIDC bridge ([more info](./bridge/README.md)).

## Pre-Requisites

* tru.ID developer account ([sign up](https://tru.id/signup))
* OIDC Bridge application to control the verification flow ([more info](https://github.com/tru-ID/oidc-bridge))
* PingID DaVinci tenant

## Configuration

The [.example.env](./.example.env) file contains all the environment variables necessary to run a
full (`bridge`+`davinci`) flow and how to populate them.

### Create a tru.ID project that supports OIDC

- Sign in to [https://developer.tru.id/](https://developer.tru.id/)
- Go to **Console > Projects > Add Project**
- Give the project a name and click **Create Project**
- Fill in the **OIDC** section with ([see an example](../bridge/README.md)):
    - OIDC bridge application public URL
    - `Redirect URL` property of your tru.ID DaVinci connector instance (see next section)
- Create **Authorization Code** credentials by clicking the **Generate New** button
- Copy the `client_id` and the `client_secret` so you can use them in the next step.

### Add the Connector to your DaVinci instance

- Sign in to your DaVinci instance
- Go to **Connections > New Connection**
- Search for **tru.ID** and click the plus sign
- In the connector details fill in the following fields ({dr} stands for you account's Data Residency e.g. eu, us, etc.):
    - **Issuer:** https://{dr}.api.tru.id/
    - **Authorization endpoint:** https://{dr}.api.tru.id/oauth2/v1/auth
    - **Token endpoint:** https://{dr}.api.tru.id/oauth2/v1/token
    - **JWKS URI:** https://{dr}.api.tru.id/oidc/.well-known/jwks.json
    - **Client ID** obtained from the previous step
    - **Client Secret** obtained from the previous step
    - **Scope** openid

### Add your connector to a flow

You can add the connector to any flow you might already have or you can create a new flow.

The connector takes the PingID username as input, which eventually gets resolved into a phone number.

## Notes

- Any questions, visit [https://support.tru.id/](https://support.tru.id/)
