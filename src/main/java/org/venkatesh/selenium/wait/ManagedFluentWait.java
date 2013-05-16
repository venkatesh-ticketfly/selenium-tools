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
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.FluentWait;

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

    private final FluentWait<WebDriver> wait;
    private final Optional<Duration> implicitTimeout;
    private final AtomicInteger reentranCheck;

    @VisibleForTesting
    ManagedFluentWait(FluentWait<WebDriver> wait, Optional<Duration> implicitTimeout,
                      AtomicInteger reentranCheck) {
        this.wait = wait;
        this.implicitTimeout = implicitTimeout;
        this.reentranCheck = reentranCheck;
    }

    /**
     * Simple wrapper around the {@link FluentWait}. Calls {@link FluentWait#until(Function)}
     * disabling implicit timeout when before calling {@code function}. Re-enables the implicit
     * timeout after the {@code function} returns.
     * @param function determines the output value from the input value
     *
     * @return desired value
     * @throws TimeoutException
     */
    public <T> T until(final Function<WebDriver, T> function) {
        return wait.until(new ExpectedCondition<T>() {
            @Override
            public T apply(WebDriver driver) {
                disableImplicitWait(driver);
                try {
                    return function.apply(driver);
                } finally {
                    enableImplicitWait(driver);
                }
            }
        });
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
        wait.until(new Predicate<WebDriver>() {
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

    private void disableImplicitWait(WebDriver driver) {
        if (implicitTimeout.isPresent() &&  reentranCheck.incrementAndGet() == 1) {
            driver.manage().timeouts().implicitlyWait(500, TimeUnit.MILLISECONDS);
        }
    }

    private void enableImplicitWait(WebDriver driver) {
        if (implicitTimeout.isPresent() && reentranCheck.decrementAndGet() == 0) {
            driver.manage().timeouts().implicitlyWait(
                    implicitTimeout.get().getStandardSeconds(), TimeUnit.SECONDS);
        }
    }

    /**
     * Returns a {@link ManagedFluentWait} instance which waits for 15 sec and ignores stale
     * reference while waiting for the condition to pass.
     */
    public static ManagedFluentWait ignoreStaleReference(WebDriver driver) {
        return onlyExplicitTimeout(driver, Duration.standardSeconds(15),
                STALE_REFERENCTE_IGNORED_EXCEPTION);
    }

    public static ManagedFluentWait onlyExplicitTimeout(WebDriver driver, Duration timeout,
            List<Class<? extends Throwable>> ignoredException) {
        return create(driver, timeout, Optional.of(DEFAULT_IMPLICIT_TIMEOUT), ignoredException);
    }

    private static ManagedFluentWait create(WebDriver driver, Duration timeout,
            Optional<Duration> implicitTimeout, List<Class<? extends Throwable>> ignoredException) {
        return new ManagedFluentWait(new FluentWait<WebDriver>(driver)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .withTimeout(timeout.getStandardSeconds(), TimeUnit.SECONDS)
                .ignoreAll(ignoredException), implicitTimeout, new AtomicInteger());
    }
}
