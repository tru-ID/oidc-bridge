# PingOne OIDC integration

tru.ID leverages PingOne External Identity Providers to provide a passwordless login experience.

The passwordless phone verification flow will be controlled by our OIDC bridge ([more info](./bridge/README.md)).

## Pre-Requisites

* tru.ID developer account ([sign up](https://tru.id/signup))
* OIDC Bridge application to control the verification flow ([more info](https://github.com/tru-ID/oidc-bridge))
* PingOne tenant

## Configuration

The [.example.env](./.example.env) file contains all the environment variables necessary to run a
full (`bridge`+`PingOne`+`sample-ui`) flow and how to populate them.

### Create a tru.ID project that supports OIDC

- Sign in to [https://developer.tru.id/](https://developer.tru.id/)
- Go to **Console > Projects > Add Project**
- Give the project a name and click **Create Project**
- Fill in the **OIDC** section with ([see an example](../bridge/README.md)):
    - OIDC bridge application public URL
    - `Callback URL` property of your tru.ID External IDP (Identity Providers > External IDPs > tru.ID > Connection > Connection Details > Callback URL)
- Create **Authorization Code** credentials by clicking the **Generate New** button
- Copy the `client_id` and the `client_secret` so you can use them in the next step.

### Configure External Identity Provider in your PingOne Environment

- Sign in to your PingOne instance
- Select your environment
- Go to **Connections > Identity Providers > External IDPs > Add Provider**
- Select **Custom > OpenID Connect**
- Fill name with **tru.ID** and click **Continue**
- In the **Connection Details** section fill in the **Client ID** and **Client Secret** with the values obtained from the previous section
- In the **Discovery Details** section, fill in the following values:
    - **Authorization endpoint:** `https://{data-residency}.api.tru.id/oauth2/v1/auth`
    - **Token endpoint:** `https://{data-residency}.api.tru.id/oauth2/v1/token`
    - **JWKS endpoint:** `https://{data-residency}.api.tru.id/oidc/.well-known/jwks.json`
    - **Issuer:** `https://{data-residency}.api.tru.id/`
- In the **Token Endpoint Authentication Method** section, pick **Client Secret Post**
- Click Save and Continue
- Skip the mapping section

### Create a worker application client to enable querying the User Directory

- Sign in to your PingOne instance
- Select your environment
- Go to **Connections > Applications** and click the plus sign to create a new application
- Fill in the application name e.g. **tru.ID OIDC Bridge**
- In the **Application Type** section, choose **Worker**
- In the Application list, click on the newly created application
- Go to the **Configuration** tab an click **General** to expose the **Client ID** and **Client Secret**
- Copy these to the environment file as `SAMPLE_PINGONE_CLIENT_ID` and `SAMPLE_PINGONE_CLIENT_SECRET`

### Create a new Authentication Policy that leverages the configured External Identity Provider

- Sign in to your PingOne instance
- Select your environment
- Go to **Experiences > Policies > Authentication** and click **Add Policy**
- Give it a name
- To setup a passwordless experience choose **Identifier First**
- Add a discovery rule where you can pin this to a certain user e.g. `john.doe@yourdomain.com` 
  or all users in that domain e.g. `@yourdomain.com`
- For the **Identity Provider** choose **tru.ID**

This configuration will have PingOne prompt for the username first, before redirecting the user to 
tru.ID to verify the phone number.

There are other ways to configure this policy, by adding extra steps e.g. step 1 Login and step 2 
external identity provider. 

### (Optional) Create a web application client to use the sample UI

- Similar to the previous section but in the **Application Type** section choose **OIDC Web App**
- In the Application list, click on the newly created application
- Go to the **Configuration** tab an click **General** to expose the **Client ID** and **Client Secret**
- Copy these to the environment file as `SAMPLE_UI_IAM_CLIENT_ID` and `SAMPLE_UI_IAM_CLIENT_SECRET`
- In the **Redirect URIs** section, add `<ngrok tunnel base URL>/sample-ui/login/callback/iam`

## Notes

- Any questions, visit [https://support.tru.id/](https://support.tru.id/)
