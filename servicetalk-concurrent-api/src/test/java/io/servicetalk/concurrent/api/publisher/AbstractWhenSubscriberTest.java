/*
 * Copyright © 2018-2019 Apple Inc. and the ServiceTalk project authors
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
package io.servicetalk.concurrent.api.publisher;

import io.servicetalk.concurrent.PublisherSource.Subscriber;
import io.servicetalk.concurrent.api.Publisher;
import io.servicetalk.concurrent.api.TestPublisherSubscriber;
import io.servicetalk.concurrent.internal.DeliberateException;

import org.junit.Test;

import java.util.function.Supplier;

import static io.servicetalk.concurrent.api.Publisher.from;
import static io.servicetalk.concurrent.api.SourceAdapters.toSource;
import static io.servicetalk.concurrent.internal.DeliberateException.DELIBERATE_EXCEPTION;
import static io.servicetalk.concurrent.internal.TerminalNotification.complete;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public abstract class AbstractWhenSubscriberTest {

    private final TestPublisherSubscriber<String> subscriber = new TestPublisherSubscriber.Builder<String>()
            .disableDemandCheck().build();
    private final TestPublisherSubscriber<String> finalSubscriber = new TestPublisherSubscriber<>();

    @Test
    public void testOnWithOnComplete() {
        toSource(doSubscriber(from("Hello"), () -> subscriber)).subscribe(finalSubscriber);
        finalSubscriber.request(1);
        assertThat(finalSubscriber.takeItems(), contains("Hello"));
        assertThat(finalSubscriber.takeTerminal(), is(complete()));
        assertTrue(subscriber.subscriptionReceived());
        assertThat(subscriber.takeItems(), contains("Hello"));
        assertThat(subscriber.takeTerminal(), is(complete()));
    }

    @Test
    public void testOnWithOnError() {
        toSource(doSubscriber(Publisher.failed(DeliberateException.DELIBERATE_EXCEPTION), () -> subscriber))
                .subscribe(finalSubscriber);
        assertThat(finalSubscriber.takeError(), sameInstance(DELIBERATE_EXCEPTION));
        assertTrue(subscriber.subscriptionReceived());
        assertThat(subscriber.takeError(), sameInstance(DELIBERATE_EXCEPTION));
    }

    protected abstract <T> Publisher<T> doSubscriber(Publisher<T> publisher,
                                                     Supplier<Subscriber<? super T>> subscriberSupplier);
}
