/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package org.venkatesh.selenium.wait;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import org.joda.time.Duration;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author venkatesh
 */
public class ManagedFluentWait {

    public static final Duration DEFAULT_IMPLICIT_TIMEOUT = Duration.standardSeconds(30);

    public static final List<Class<? extends Throwable>> STALE_REFERENCTE_IGNORED_EXCEPTION =
            ImmutableList.<Class<? extends Throwable>>of(StaleElementReferenceException.class,
                    NoSuchElementException.class, NullPointerException.class);

    private final FluentWaitProvider provider;
    private final Optional<Duration> implicitTimeout;
    private final AtomicInteger reentrantCheck;

    @VisibleForTesting
    ManagedFluentWait(FluentWaitProvider provider, Optional<Duration> implicitTimeout,
                      AtomicInteger reentrantCheck) {
        this.provider = provider;
        this.implicitTimeout = implicitTimeout;
        this.reentrantCheck = reentrantCheck;
    }

    /**
     * Simple wrapper around the {@link FluentWait}. Calls {@link FluentWait#until(Function)}
     * disabling implicit timeout when before calling {@code function}. Re-enables the implicit
     * timeout after the {@code function} returns.
     *
     * @param function determines the output value from the input value
     *
     * @return desired value
     * @throws TimeoutException
     */
    public <T> T until(final Function<WebDriver, T> function) {
        return provider.get().until(get(function));
    }

    /**
     * Simple wrapper around the {@link FluentWait}. Calls {@link FluentWait#until(Function)}
     * disabling implicit timeout when before calling {@code function}. Re-enables the implicit
     * timeout after the {@code function} returns.
     *
     * @param function determines the output value from the input value
     *
     * @return desired value
     * @throws TimeoutException
     */
    public <F, T> T until(F input, final Function<F, T> function) {
        return provider.getForType(input).until(get(function));
    }

    /**
     * Simple wrapper around the {@link FluentWait}. Calls {@link FluentWait#until(Function)}
     * disabling implicit timeout when before calling {@code isTrue}. Re-enables the implicit
     * timeout after the {@code isTrue} returns.
     * @param isTrue a predicate that is applied repeatedly until it returns true or the timeout
     *          expires
     *
     * @throws TimeoutException
     */
    public void until(final Predicate<WebDriver> isTrue) {
        provider.get().until(new Predicate<WebDriver>() {
            @Override
            public boolean apply(WebDriver driver) {
                disableImplicitWait(driver);
                try {
                    return isTrue.apply(driver);
                } finally {
                    enableImplicitWait(driver);
                }
            }
        });
    }

    private <F, T> Function<F, T> get(final Function<F, T> function) {
        return new Function<F, T>() {
            @Override
            public T apply(@Nullable F input) {
                disableImplicitWait(provider.getDriver());
                try {
                    return function.apply(input);
                } finally {
                    enableImplicitWait(provider.getDriver());
                }
            }
        };
    }

    private void disableImplicitWait(WebDriver driver) {
        if (implicitTimeout.isPresent() &&  reentrantCheck.incrementAndGet() == 1) {
            driver.manage().timeouts().implicitlyWait(500, TimeUnit.MILLISECONDS);
        }
    }

    private void enableImplicitWait(WebDriver driver) {
        if (implicitTimeout.isPresent() && reentrantCheck.decrementAndGet() == 0) {
            driver.manage().timeouts().implicitlyWait(
                    implicitTimeout.get().getStandardSeconds(), TimeUnit.SECONDS);
        }
    }

    /**
     * Returns a {@link ManagedFluentWait} instance which waits for 15 sec and ignores stale
     * reference while waiting for the condition to pass.
     */
    public static ManagedFluentWait ignoreStaleReference(WebDriver driver, Duration timeout) {
        return onlyExplicitTimeout(driver, timeout, STALE_REFERENCTE_IGNORED_EXCEPTION);
    }

    public static ManagedFluentWait onlyExplicitTimeout(WebDriver driver, Duration timeout,
            List<Class<? extends Throwable>> ignoredException) {
        return create(driver, timeout, Optional.of(DEFAULT_IMPLICIT_TIMEOUT), ignoredException);
    }

    private static ManagedFluentWait create(WebDriver driver, Duration timeout,
            Optional<Duration> implicitTimeout, List<Class<? extends Throwable>> ignoredException) {
        FluentWaitProvider provider = new FluentWaitProvider(driver, timeout, ignoredException);
        return new ManagedFluentWait(provider, implicitTimeout, new AtomicInteger());
    }

    /**
     * Provides instances of fluent waits.
     */
    static class FluentWaitProvider {

        private final WebDriver driver;
        private final Duration timeout;
        private final List<Class<? extends Throwable>> ignoredException;

        FluentWaitProvider(WebDriver driver, Duration timeout,
                List<Class<? extends Throwable>> ignoredException) {
            this.driver = driver;
            this.timeout = timeout;
            this.ignoredException = ignoredException;
        }

        public FluentWait<WebDriver> get() {
            return get(driver);
        }

        public <T> FluentWait<T> getForType(T type) {
            return get(type);
        }

        WebDriver getDriver() {
            return driver;
        }

        private <T> FluentWait<T> get(T type) {
            return new FluentWait<T>(type)
                    .pollingEvery(500, TimeUnit.MILLISECONDS)
                    .withTimeout(timeout.getMillis(), TimeUnit.MILLISECONDS)
                    .ignoreAll(ignoredException);
        }
    }
}
