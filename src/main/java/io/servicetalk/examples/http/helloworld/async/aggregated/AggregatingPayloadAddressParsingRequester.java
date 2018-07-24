/*
 * Copyright © 2018 Apple Inc. and the ServiceTalk project authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.servicetalk.examples.http.helloworld.async.aggregated;

import io.servicetalk.buffer.api.BufferAllocator;
import io.servicetalk.buffer.api.CompositeBuffer;
import io.servicetalk.concurrent.api.AsyncCloseables;
import io.servicetalk.concurrent.api.CompositeCloseable;
import io.servicetalk.http.netty.AddressParsingHttpRequesterBuilder;
import io.servicetalk.http.api.AggregatedHttpRequester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

import static io.servicetalk.http.api.AggregatedHttpRequests.newRequest;
import static io.servicetalk.http.api.HttpRequestMethods.GET;
import static java.nio.charset.StandardCharsets.US_ASCII;

public final class AggregatingPayloadAddressParsingRequester {
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregatingPayloadAddressParsingRequester.class);

    public static void main(String[] args) throws Exception {
        // Collection of all resources in this test that can be closed together at the end.
        try (CompositeCloseable resources = AsyncCloseables.newCompositeCloseable()) {

            // Build the requester.
            // This builder sets appropriate defaults for Service Discovery and load balancing.
            final AggregatedHttpRequester requester = resources.prepend(
                    new AddressParsingHttpRequesterBuilder().buildAggregated());

            // This example is demonstrating asynchronous execution, but needs to prevent the main thread from exiting
            // before the response has been processed. This isn't typical usage for a streaming API but is useful for
            // demonstration purposes.
            CountDownLatch responseProcessedLatch = new CountDownLatch(1);

            // Create a big buffer so that we can leverage aggregation on the response which is the request payload
            // echoed back.
            BufferAllocator alloc = requester.getExecutionContext().getBufferAllocator();
            final CompositeBuffer payload = alloc.newCompositeBuffer(10);
            for (int i = 0; i < 10; i++) {
                payload.addBuffer(alloc.fromAscii(i + " hello\n"));
            }

            requester.request(newRequest(GET,"http://localhost:8080/sayHello", payload))
                    .doAfterError(cause -> LOGGER.error("request failed!", cause))
                    .doAfterFinally(responseProcessedLatch::countDown)
                    .subscribe(response -> {
                        LOGGER.info("Got response \n{}", response.toString((name, value) -> value));
                        LOGGER.info("Response content: \n{}",
                                response.getPayloadBody().getContent().toString(US_ASCII));
                    });

            // Don't exit the main thread until after the response is completely processed.
            responseProcessedLatch.await();
        }
    }
}
