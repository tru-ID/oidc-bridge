from java.util import Arrays
from javax.faces.application import FacesMessage
from javax.faces.context import FacesContext

import java

from org.gluu.jsf2.message import FacesMessages
from org.gluu.jsf2.service import FacesService
from org.gluu.model.custom.script.type.auth import PersonAuthenticationType
from org.gluu.oxauth.security import Identity
from org.gluu.oxauth.service import AuthenticationService
from org.gluu.oxauth.service.net import HttpService
from org.gluu.oxauth.util import ServerUtil
from org.gluu.service.cdi.util import CdiUtil
from org.gluu.util import StringHelper

from uuid import uuid4

import json
import sys
import urllib


class PersonAuthentication(PersonAuthenticationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "tru.ID-MFA. init called"
        if not configurationAttributes.containsKey("truid_client_id"):
            print "tru.ID-MFA. init. Property 'truid_client_id' was not specified"
            return False
        self._client_id = configurationAttributes.get("truid_client_id").getValue2()
        
        if not configurationAttributes.containsKey("truid_client_secret"):
            print "tru.ID-MFA. init. Property 'truid_client_secret' was not specified"
            return False
        self._client_secret = configurationAttributes.get("truid_client_secret").getValue2()

        if not configurationAttributes.containsKey("truid_api_base_url"):
            print "tru.ID-MFA. init. Property 'truid_api_base_url' was not specified"
            return False
        self._api_base_url = configurationAttributes.get("truid_api_base_url").getValue2()

        print "tru.ID-MFA. init. Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "tru.ID-MFA. destroy called"
        print "tru.ID-MFA. destroy. Initialized successfully"
        return True

    def getApiVersion(self):
        return 1

    def isValidAuthenticationMethod(self, usageType, configurationAttributes):
        return True

    def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes):
        return None

    def authenticate(self, configurationAttributes, requestParameters, step):
        facesMessages = CdiUtil.bean(FacesMessages)
        facesMessages.setKeepMessages()

        if step == 1:
            print "tru.ID-MFA. authenticate for step 1 called"

            identity = CdiUtil.bean(Identity)

            credentials = identity.getCredentials()
            user_name = credentials.getUsername()
            user_password = credentials.getPassword()

            authenticationService = CdiUtil.bean(AuthenticationService)

            logged_in = False
            if (StringHelper.isNotEmptyString(user_name) and StringHelper.isNotEmptyString(user_password)):
                logged_in = authenticationService.authenticate(user_name, user_password)

            if not logged_in:
                return False

            user = authenticationService.getAuthenticatedUser()
            if user is None:
                print "tru.ID-MFA. authenticate for step 1. Could not find user for username '%s'" % user_name
                return False

            identity.setWorkingParameter("username", user_name)
            return True
        elif step == 2:
            print "tru.ID-MFA. authenticate for step 2 called"

            identity = CdiUtil.bean(Identity)

            state = identity.getWorkingParameter("state_truid")
            request_state = ServerUtil.getFirstValue(requestParameters, "state")

            if state != request_state:
                facesMessages.add(FacesMessage.SEVERITY_ERROR, "Failed to verify mobile phone number")
                print "tru.ID-MFA. authenticate for step 2. Failed to verify user for unknown oauth state query param '%s'" % request_state
                return False

            code = ServerUtil.getFirstValue(requestParameters, "code")
            if code is None:
                facesMessages.add(FacesMessage.SEVERITY_ERROR, "Failed to verify mobile phone number")
                error = ServerUtil.getFirstValue(requestParameters, "error")
                print "tru.ID-MFA. authenticate for step 2. tru.ID failed to verify user with error '%s'" % error
                return False

            token_response = self._code_exchange(code)
            if token_response is None:
                facesMessages.add(FacesMessage.SEVERITY_ERROR, "Failed to verify mobile phone number")
                print "tru.ID-MFA. authenticate for step 2. tru.ID failed to verify user because code exchange failed"
                return False

            return True
        else:
            return False

    def prepareForStep(self, configurationAttributes, requestParameters, step):
        if step == 1:
            print "tru.ID-MFA. prepareForStep 1 called"
            return True
        elif step == 2:
            print "tru.ID-MFA. prepareForStep 2 called"

            identity = CdiUtil.bean(Identity)
            username = identity.getWorkingParameter("username")

            redirect_uri = self._get_redirect_uri()

            # state used to reconcile oauth code callback
            state = str(uuid4())

            identity.setWorkingParameter("state_truid", state)

            auth_url = self._create_auth_url(username, redirect_uri, state)

            print "tru.ID-MFA. prepareForStep 2. Redirecting to '%s'" % auth_url
            # redirect to tru.ID to verify phone number
            facesService = CdiUtil.bean(FacesService)
            facesService.redirectToExternalURL(auth_url)
            return True
        else:
            return False 

    def getCountAuthenticationSteps(self, configurationAttributes):
        return 2

    def getExtraParametersForStep(self, configurationAttributes, step):
        return Arrays.asList("state_truid", "username")

    def getPageForStep(self, configurationAttributes, step):
        return ""

    def logout(self, configurationAttributes, requestParameters):
        return True

    def _get_redirect_uri(self):
        facesContext = CdiUtil.bean(FacesContext)
        request = facesContext.getExternalContext().getRequest()
        httpService = CdiUtil.bean(HttpService)
        url = httpService.constructServerUrl(request) + "/postlogin.htm"
        return url

    def _create_auth_url(self, login_hint, redirect_uri, state):
        auth_url = "%s/oauth2/v1/auth" % self._api_base_url

        query = {
            "response_type": "code",
            "scope": "openid profile",
            "client_id": self._client_id,
            "state": state,
            "redirect_uri": redirect_uri,
            "login_hint": login_hint
        }
        return "%s?%s" % (auth_url, urllib.urlencode(query))

    def _code_exchange(self, code):
        token_url = "%s/oauth2/v1/token" % self._api_base_url
        body = {
            "grant_type": "authorization_code",
            "code": code,
            "client_id": self._client_id,
            "client_secret": self._client_secret,
            "redirect_uri": self._get_redirect_uri(),
        }
        headers = {
            "Content-Type": "application/x-www-form-urlencoded"
        }

        httpService = CdiUtil.bean(HttpService)
        httpClient = httpService.getHttpsClient()

        try:
            http_service_response = httpService.executePost(httpClient, token_url, None, headers, urllib.urlencode(body))
            http_response = http_service_response.getHttpResponse()
        except:
            print "tru.ID-MFA. _code_exchange. Exception:", sys.exc_info()[1]
            return None

        token_response = None
        try:
            if not httpService.isResponseStastusCodeOk(http_response):
                print "tru.ID-MFA. _code_exchange. Failed to exchange code server replied with status code '%s'" % str(http_response.getStatusLine().getStatusCode())
                httpService.consume(http_response)
            else:
                response_bytes = httpService.getResponseContent(http_response)
                response_string = httpService.convertEntityToString(response_bytes)
                httpService.consume(http_response)
                token_response = json.loads(response_string)
        except:
            print "tru.ID-MFA. _code_exchange. Exception:", sys.exc_info()[1]
        finally:
            http_service_response.closeConnection()

        return token_response
