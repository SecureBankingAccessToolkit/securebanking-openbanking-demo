import java.text.SimpleDateFormat
import java.time.Instant
import java.time.temporal.ChronoUnit

/*
 * Script to prepare payment consent
 * Input: OB payment intent JSON
 * Output: IDM create object
 */

def fapiInteractionId = request.getHeaders().getFirst("x-fapi-interaction-id");
if(fapiInteractionId == null) fapiInteractionId = "No x-fapi-interaction-id";
SCRIPT_NAME = "[ProcessPaymentConsent] (" + fapiInteractionId + ") - ";

logger.debug(SCRIPT_NAME + "Running...")

String idempotencyKeyHeaderValue = request.getHeaders().getFirst("x-idempotency-key")

def apiClientId = contexts.oauth2.accessToken.info.client_id
if (apiClientId == null || apiClientId == "") {
    // in case of client credentials grant
    apiClientId = contexts.oauth2.accessToken.info.sub
}

def method = request.method

def status
// Check if the argument is defined. Null check is not enough.
// routeArgSetConsentStatus can be used for any response that needs a status override
if (binding.hasVariable('routeArgSetConsentStatus')) {
    status = routeArgSetConsentStatus
} else {
    status = null
}

switch (method.toUpperCase()) {

    case "POST":
        def consentId = routeArgConsentIdPrefix + UUID.randomUUID().toString()
        paymentIntentData = request.entity.getJson()
        processProcessPaymentConsentRequestData(consentId, paymentIntentData, status)

        // calculate the expired time for idempotency key (current date time + 24 hours)
        Instant idempotencyKeyExpiredDateTime = Instant.now().plus(24, ChronoUnit.HOURS)

        def version = getObApiVersion(request)
        def idmIntent = [
                _id               : consentId,
                OBVersion         : version,
                OBIntentObjectType: routeArgObIntentObjectType,
                OBIntentObject    : paymentIntentData,
                apiClient         : ["_ref": "managed/" + routeArgObjApiClient + "/" + apiClientId],
                Oauth2ClientId    : apiClientId,
                IdempotencyKey    : idempotencyKeyHeaderValue,
                IdempotencyKeyExpirationTime: idempotencyKeyExpiredDateTime.getEpochSecond()
        ]

        logger.debug(SCRIPT_NAME + "IDM object json [" + idmIntent + "]")
        request.setEntity(idmIntent)
        request.uri.path = "/openidm/managed/" + routeArgObjIntent
        request.uri.query = "action=create";
        return next.handle(context, request).then(this.&extractOBIntentObjectFromIdmResponse)

    case "GET":
        def consentId = request.uri.path.substring(request.uri.path.lastIndexOf("/") + 1);
        return getIdmConsent(request, consentId, apiClientId)

    default:
        logger.debug(SCRIPT_NAME + "Method not supported")
        return new Response(Status.METHOD_NOT_ALLOWED);
}

private Promise<Response, NeverThrowsException> getIdmConsent(originalRequest, consentId, oauth2ClientId) {
    // Query IDM for a consent with the matching id
    var getRequest = new Request(originalRequest)
    getRequest.method = "GET"
    getRequest.uri.path = "/openidm/managed/" + routeArgObjIntent + "/" + consentId
    getRequest.uri.query = "_fields=OBIntentObject,apiClient/oauth2ClientId"
    return next.handle(context, getRequest)
            .then(response -> performAccessAuthorisationCheck(response, oauth2ClientId))
            .then(this.&extractOBIntentObjectFromIdmResponse)
}

/**
 * Verify that the TPP is allowed to access the consent by checking the TPP's oauth2ClientId (extracted from the access_token)
 * is the same as the oauth2ClientId used to create the consent
 */
private Response performAccessAuthorisationCheck(response, oauth2ClientId) {
    if (response.status.isSuccessful()) {
        if (response.entity.getJson().get("apiClient").get("oauth2ClientId") != oauth2ClientId) {
            logger.debug(SCRIPT_NAME + "TPP not authorised to access consent")
            return new Response(Status.FORBIDDEN)
        }
    }
    return response
}

/**
 * Responses from IDM will always contain the "OBIntentObject" as a top level field (even if we filter).
 * We want to send only the contents of the OBIntentObject as the response to the client i.e. a valid Open Banking API response
 */
private static Response extractOBIntentObjectFromIdmResponse(response) {
    if (response.status.isSuccessful()) {
        response.entity = response.entity.getJson().get("OBIntentObject")
    }
    return response
}

private static void processProcessPaymentConsentRequestData(consentId, paymentIntentData, statusToSet) {
    def tz = TimeZone.getTimeZone("UTC");
    def df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    df.setTimeZone(tz);
    def nowAsISO = df.format(new Date())
    paymentIntentData.Data.ConsentId = consentId
    paymentIntentData.Data.CreationDateTime = nowAsISO
    paymentIntentData.Data.StatusUpdateDateTime = nowAsISO
    if (statusToSet != null) {
        paymentIntentData.Data.Status = statusToSet
    }
}

/**
 * Extract the Open Banking Api version from the request uri
 * Example uri: /rs/open-banking/v3.1.10/aisp/account-access-consents
 */
private static String getObApiVersion(request) {
    def uri = request.uri.toString()
    def pathPrefix = "/rs/open-banking/"
    int prefixEndIndex = uri.indexOf(pathPrefix)
    if (prefixEndIndex < 0) {
        throw new IllegalStateException("Failed to determine OB API version from uri: " + uri)
    }
    int versionStartIndex = prefixEndIndex + pathPrefix.length()
    int versionEndIndex = uri.indexOf('/', versionStartIndex)
    if (versionEndIndex < 0) {
        throw new IllegalStateException("Failed to determine OB API version from uri: " + uri)
    }
    return uri.substring(versionStartIndex, versionEndIndex)
}
