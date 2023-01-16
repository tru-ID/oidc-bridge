# Bridge Context Lifecycle

## 1. Handle the POST from `oidc-service`

**Pieces of context added/updated:**

OIDC auth code flow params

- `login_hint`
- `state`

OIDC service params

- `flow_id`
- `flow_patch_url` (not needed)

# 2. Check if we are on a mobile device

Know if after resolving the login flow:

- `mobile_flow==true` -> redirect user agent to OIDC service (continue on the phone)
- `mobile_flow==false` -> redirect user agent to "please close this window" (continue on desktop)

**Pieces of context added/updated:**

- `mobile_flow`

# 3. Perform the phone verification

- resolve the context -> `login_hint` -> IdP user
- find factors for `IdpUser.user_id`
- execute factor (`PCK`, `PUSH`, and `TOTP`)

**Pieces of context added/updated:**

- `IdpUser`
  - `user_id` -- IdP internal user ID
  - `username` -- IdP user profile username e.g., email, special ID, etc. (typically the `sub` claim)
  - `phone_number` -- IdP user profile phone number

TOTP factor

- `challenge_id`
- `verification_type` --> `TOTP`

PUSH factor

- `check_id`
- `check_url` (don't need to store this)
- `challenge_id`
- `verification_type` --> `PUSH`

PCK factor

- `check_id`
- `check_url` (don't need to store this)
- `verification_type` --> `PHONECHECK`

# 4. Handle verification result

- resolve the login flow with the OIDC service

**Pieces of context added/updated:**

PUSH and PCK factors:

- `match` -- if the sim verification matched the number
- `check_status` -- sim verification method status

TOTP and PUSH factors:

- `challenge_status` -- authenticator challenge status

# Bridge Context Fields

- `context_id: str` -- internal storage ID

- `login_hint: str`
- `state: str`
- `flow_id: str`
- `mobile_flow: boolean`
- `user: IdpUser`
- `verification_type: PCK | TOTP | PUSH`
- `check_id: str`
- `challenge_id: str`
- `match: boolean`
- `check_status: ACCEPTED | PENDING | COMPLETED | EXPIRED | ERROR`
- `challenge_status: PENDING | VERIFIED | FAILED | EXPIRED`

Indexes needed:

- `check_id`
- `challenge_id`
- `flow_id`
- `login_hint` (not unique)
- `state` (not unique)