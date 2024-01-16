/*
 * Copyright © 2020-2024 ForgeRock AS (obst@forgerock.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.forgerock.sapi.gateway.dcr.idm;

import static com.forgerock.sapi.gateway.dcr.idm.IdmApiClientDecoderTest.createIdmApiClientDataAllFields;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URISyntaxException;
import java.util.function.Function;

import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openig.heap.HeapException;
import org.forgerock.openig.heap.HeapImpl;
import org.forgerock.openig.heap.Name;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.forgerock.sapi.gateway.dcr.idm.IdmApiClientServiceTest.MockApiClientTestDataIdmHandler;

class AuthorizeResponseFetchApiClientFilterTest extends BaseAuthorizeResponseFetchApiClientFilterTest {

    @Override
    protected Function<Request, Promise<String, NeverThrowsException>> createClientIdRetriever() {
        return AuthorizeResponseFetchApiClientFilter.queryParamClientIdRetriever();
    }

    @Override
    protected Request createRequest() {
        final Request request = new Request();
        try {
            request.setUri("/authorize?client_id=" + clientId);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return request;
    }

    @Nested
    public class HeapletTests {
        @Test
        void failsToConstructIfClientHandlerIsMissing() {
            final HeapException heapException = assertThrows(HeapException.class, () -> new AuthorizeResponseFetchApiClientFilterHeaplet().create(Name.of("test"),
                    json(object()), new HeapImpl(Name.of("heap"))), "Invalid object declaration");
            assertEquals(heapException.getCause().getMessage(), "/clientHandler: Expecting a value");
        }

        @Test
        void failsToConstructIfIdmUrlIsMissing() {
            final Handler idmClientHandler = (ctx, req) -> Promises.newResultPromise(new Response(Status.OK));
            final HeapImpl heap = new HeapImpl(Name.of("heap"));
            heap.put("idmClientHandler", idmClientHandler);

            assertThrows(JsonValueException.class, () -> new AuthorizeResponseFetchApiClientFilterHeaplet().create(Name.of("test"),
                    json(object(field("clientHandler", "idmClientHandler"))), heap), "/idmGetApiClientBaseUri: Expecting a value");
        }

        @Test
        void successfullyCreatesFilterWithRequiredConfigOnly() throws Exception {
            final JsonValue idmApiClientData = createIdmApiClientDataAllFields(clientId);
            final Handler idmClientHandler = new MockApiClientTestDataIdmHandler(idmBaseUri, clientId, idmApiClientData);

            final HeapImpl heap = new HeapImpl(Name.of("heap"));
            heap.put("idmClientHandler", idmClientHandler);

            final JsonValue config = json(object(field("clientHandler", "idmClientHandler"),
                    field("idmGetApiClientBaseUri", idmBaseUri)));
            final AuthorizeResponseFetchApiClientFilter filter = (AuthorizeResponseFetchApiClientFilter) new AuthorizeResponseFetchApiClientFilterHeaplet().create(Name.of("test"), config, heap);

            // Test the filter created by the Heaplet
            callFilterValidateSuccessBehaviour(idmApiClientData, filter);
        }
    }
}