import java.text.SimpleDateFormat

/*
 * Script to prepare payment consent
 * Input: OB payment intent JSON
 * Output: IDM create object
 */

SCRIPT_NAME = "[ProcessPaymentConsent] - "
logger.debug(SCRIPT_NAME + "Running...")

def apiClientId = contexts.oauth2.accessToken.info.client_id
if (apiClientId == null || apiClientId == "") {
    // in case of client credentials grant
    apiClientId = contexts.oauth2.accessToken.info.sub
}

def method = request.method

switch(method.toUpperCase()) {

    case "POST":
        def consentId = routeArgConsentIdPrefix + UUID.randomUUID().toString()
        paymentIntentData = request.entity.getJson()
        processProcessPaymentConsentRequestData(consentId, paymentIntentData)

        def version = getObApiVersion(request)
        def idmIntent = [
                _id: consentId,
                OBVersion: version,
                OBIntentObjectType: routeArgObIntentObjectType,
                OBIntentObject: paymentIntentData,
                apiClient: [ "_ref" : "managed/" + routeArgObjApiClient + "/" +  apiClientId ],
        ]

        logger.debug(SCRIPT_NAME + "IDM object json [" + idmIntent + "]")
        request.setEntity(idmIntent)
        request.uri.path = "/openidm/managed/" + routeArgObjDomesticPaymentConsent
        request.uri.query = "action=create";
        break

    case "GET":
        def consentId = request.uri.path.substring(request.uri.path.lastIndexOf("/") + 1);
        request.uri.path = "/openidm/managed/" + routeArgObjDomesticPaymentConsent + "/" + consentId
        request.uri.query = "_fields=OBIntentObject"
        break

    default:
        logger.debug(SCRIPT_NAME + "Method not supported")
        return new Response(Status.METHOD_NOT_ALLOWED);
}
return next.handle(context, request).then(this.&extractOBIntentObjectFromIdmResponse)

/**
 * Responses from IDM will always contain the "OBIntentObject" as a top level field (even if we filter).
 * We want to send only the contents of the OBIntentObject as the response to the client i.e. a valid Open Banking API response
 */
private Response extractOBIntentObjectFromIdmResponse(response) {
    if (response.status.isSuccessful()) {
        response.entity = response.entity.getJson().get("OBIntentObject")
    }
    return response
}

private void processProcessPaymentConsentRequestData(consentId, paymentIntentData) {
    def tz = TimeZone.getTimeZone("UTC");
    def df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    df.setTimeZone(tz);
    def nowAsISO = df.format(new Date())
    paymentIntentData.Data.ConsentId = consentId
    paymentIntentData.Data.CreationDateTime = nowAsISO
    paymentIntentData.Data.StatusUpdateDateTime = nowAsISO
}

/**
 * Extract the Open Banking Api version from the request uri
 * Example uri: /rs/open-banking/v3.1.10/aisp/account-access-consents
 */
private String getObApiVersion(request) {
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
