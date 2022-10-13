# Okta OIDC integration

tru.ID leverages Okta 3rd party identity providers to provide a passwordless login experience.

The second factor will be controlled by our OIDC bridge ([more info](./bridge/README.md)).

## Pre-Requisites

* tru.ID developer account ([sign up](https://tru.id/signup))
* OIDC Bridge application to control the verification flow ([more info](https://github.com/tru-ID/oidc-bridge))
* Okta tenant

## Configuration

The [.example.env](./.example.env) file contains all the environment variables necessary to run a
full (`sample-ui`+`bridge`+`okta`) flow and how to populate them.

### Create a tru.ID project that supports OIDC

- Sign in to [https://developer.tru.id/](https://developer.tru.id/)
- Go to **Console > Projects > Add Project**
- Give the project a name and click **Create Project**
- Fill in the **OIDC** section with ([see an example](../bridge/README.md)):
    - OIDC bridge application public URL
    - Okta tenant callback URL e.g. https://{tenantID}.okta.com/oauth2/v1/authorize/callback
- Create **Authorization Code** credentials by clicking the **Generate New** button
- Copy the `client_id` and the `client_secret` so you can use them in the next step.

### Configure new Identity Provider in your Okta tenant

- Sign in to your Okta tenant
- Go to **Security > Identity Providers > Add Identity Provider**
- Select **OpenID Connect IdP**
- In the **General Setting** section, fill in the **Name** with [**tru.ID](http://tru.ID) OIDC**
- In the **Client Details** section, fill in the **Client ID** and **Client Secret** with the values obtained from the previous step
- In the **Endpoints** section fill in the following values:
    - **Issuer:** https://eu.api.tru.id/
    - **Authorization endpoint:** [https://eu.api.tru.id/oauth2/v1/auth](https://eu.api.tru.id/oauth2/v1/auth)
    - **Token endpoint:** [https://eu.api.tru.id/oauth2/v1/token](https://eu.api.tru.id/oauth2/v1/token)
    - **JWKS endpoint:** [https://eu.api.tru.id/oidc/.well-known/jwks.json](https://eu.api.tru.id/oidc/.well-known/jwks.json)
- In the **Authentication Settings** section:
    - Change the **IdP Username** value to `idpuser.externalId`
    - Change the **If no match is found** value to **Redirect to Okta sign-in page**

Once you've created the identity provider, capture its IdP ID since it is necessary for the `sample-ui` configuration.

## Create an Okta API token to resolve user profiles in the bridge

- Sign in to your Okta tennant
- Go to **Security > API > Tokens > Create Token**
- Give it a name e.g., `bridge-resolver`
- Copy the token value and use it as the value for the `SAMPLE_OKTA_API_KEY` environment variable

### Configure new Application to run a full test flow (with the sample-ui as the Relying Party)

- Go to **Applications > Applications > Create App Integration**
- For **Sign-in method** choose the **OIDC - OpenID Connect**
- For **Application type** choose **Web Application**
- On the **General Settings > Sign-in redirect URIs** section add our sample UI public redirect URL
    - e.g. `<ngrok-endpoint>/sample-ui/login/callback/iam`
- On the **General Settings> Sign-out redirect URIs** section add our sample UI public sign out redirect URL
    - e.g. `<ngrok-endpoint>/sample-ui`
- On **Assignments > Controlled access** choose **Skip group assignment for now**
- Copy the **Client ID** and the **Secret** so you can provide them to our `sample-ui` configuration

### Create a new user to test the integration

- Go to **Directory > People > Add Person**
- Fill in the required fields
- Add a phone number to the newly created user by:
    - Click on the user
    - Go to **Profile > Edit**
    - Fill in the **Primary phone** field with a valid phone number

## Notes

- Any questions, visit [https://support.tru.id/](https://support.tru.id/)
