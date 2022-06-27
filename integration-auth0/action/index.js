/* eslint-disable max-len */
/**
* Handler that will be called during the execution of a PostLogin flow.
*
* @param {Event} event - Details about the user and the context in which they are logging in.
* @param {PostLoginAPI} api - Interface whose methods can be used to change the behavior of the login.
*/
exports.onExecutePostLogin = async (event, api) => {
  const userId = event.user.user_id;
  const phoneNumber = event.user.app_metadata.phone_number;
  const userIp = event.request.ip;

  if (!phoneNumber) {
    api.access.deny(`user ${userId} doesn not have a phone number to verify`);
    return;
  }

  const token = api.redirect.encodeToken({
    secret: event.secrets.AUTH0_SIGNATURE_SECRET,
    expiresInSeconds: 60,
    payload: {
      phone_number: phoneNumber,
      user_ip: userIp,
    },
  });

  const appBaseUrl = event.secrets.APP_BASE_URL;
  api.redirect.sendUserTo(`${appBaseUrl}/auth0/action/redirect`, {
    query: { session_token: token },
  });
};

/**
* Handler that will be invoked when this action is resuming after an external redirect. If your
* onExecutePostLogin function does not perform a redirect, this function can be safely ignored.
*
* @param {Event} event - Details about the user and the context in which they are logging in.
* @param {PostLoginAPI} api - Interface whose methods can be used to change the behavior of the login.
*/
exports.onContinuePostLogin = async (event, api) => {
  const payload = api.redirect.validateToken({
    secret: event.secrets.AUTH0_SIGNATURE_SECRET,
    tokenParameterName: 'truid_token',
  });

  const { match } = payload;

  if (!match) {
    api.access.deny('user in session did not match the phone number');
  }
};
