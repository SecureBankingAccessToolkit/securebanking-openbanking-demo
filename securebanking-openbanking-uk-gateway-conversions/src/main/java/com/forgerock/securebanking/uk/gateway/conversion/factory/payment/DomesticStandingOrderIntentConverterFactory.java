package com.forgerock.securebanking.uk.gateway.conversion.factory.payment;

import com.forgerock.securebanking.openbanking.uk.common.api.meta.obie.OBVersion;
import com.forgerock.securebanking.openbanking.uk.common.api.meta.share.IntentType;
import com.forgerock.securebanking.uk.gateway.conversion.converters.GenericIntentConverter;
import com.forgerock.securebanking.uk.gateway.conversion.converters.payment.domestic.DomesticStandingOrderIntentConverter5;
import com.forgerock.securebanking.uk.gateway.conversion.converters.payment.domestic.DomesticStandingOrderIntentConverter6;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomesticStandingOrderIntentConverterFactory {

    private static final Logger logger = LoggerFactory.getLogger(DomesticStandingOrderIntentConverterFactory.class);

    public static GenericIntentConverter getConverter(OBVersion version) {
        switch (version) {
            case v3_1_4: // OBWriteDomesticStandingOrderConsentResponse5
                return new DomesticStandingOrderIntentConverter5();
            case v3_1_5: // OBWriteDomesticStandingOrderConsentResponse6
            case v3_1_6:
            case v3_1_7:
            case v3_1_8:
            case v3_1_9:
            case v3_1_10:
                return new DomesticStandingOrderIntentConverter6();
            default: {
                String message = String.format("Couldn't find the %s converter for version %s", IntentType.PAYMENT_DOMESTIC_STANDING_ORDERS_CONSENT.name(), version.name());
                logger.error(message);
                throw new RuntimeException(message);
            }
        }
    }
}
