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

import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Options;
import org.openqa.selenium.WebDriver.Timeouts;
import org.openqa.selenium.support.ui.FluentWait;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.easymock.EasyMock.*;

/**
 * @author venkatesh
 */
public class ManagedFluentWaitTest {

    private IMocksControl control;
    private WebDriver mockedDriver;
    private FluentWait<WebDriver> fluentWait;

    @Before
    public void setUp() {
        control = EasyMock.createControl();
        mockedDriver = control.createMock(WebDriver.class);
        fluentWait = new FluentWait<WebDriver>(mockedDriver);
    }

    @Test
    public void untilFunctionDoesNotEnableAndDisableDriverWaitIfAlreadyDisabled() {
        AtomicInteger reentrant = new AtomicInteger(1);
        Optional<Duration> implicitTimeout = Optional.of(Duration.millis(1));
        control.replay();

        new ManagedFluentWait(fluentWait, implicitTimeout, reentrant)
                .until(Functions.<WebDriver>identity());

        control.verify();
    }

    @Test
    public void untilFunctionEnableAndDisableDriverWaitIfNotAlreadyDisabled() {
        AtomicInteger reentrant = new AtomicInteger(0);
        Optional<Duration> implicitTimeout = Optional.of(Duration.millis(1));

        setUpMock();
        control.replay();

        new ManagedFluentWait(fluentWait, implicitTimeout, reentrant)
                .until(Functions.<WebDriver>identity());

        control.verify();
    }

    @Test
    public void untilPredicateDoesNotEnableAndDisableDriverWaitIfAlreadyDisabled() {
        AtomicInteger reentrant = new AtomicInteger(1);
        Optional<Duration> implicitTimeout = Optional.of(Duration.millis(1));
        control.replay();

        new ManagedFluentWait(fluentWait, implicitTimeout, reentrant)
                .until(Predicates.<WebDriver>alwaysTrue());

        control.verify();
    }

    @Test
    public void untilPredicateEnableAndDisableDriverWaitIfNotAlreadyDisabled() {
        AtomicInteger reentrant = new AtomicInteger(0);
        Optional<Duration> implicitTimeout = Optional.of(Duration.millis(1));

        setUpMock();
        control.replay();

        new ManagedFluentWait(fluentWait, implicitTimeout, reentrant)
                .until(Predicates.<WebDriver>alwaysTrue());

        control.verify();
    }

    private void setUpMock() {
        Options mockOptions = control.createMock(Options.class);
        Timeouts timeouts = control.createMock(Timeouts.class);

        // for disable
        expect(mockedDriver.manage()).andReturn(mockOptions);
        expect(mockOptions.timeouts()).andReturn(timeouts);
        expect(timeouts.implicitlyWait(anyLong(), anyObject(TimeUnit.class))).andReturn(timeouts);

        // for enable
        expect(mockedDriver.manage()).andReturn(mockOptions);
        expect(mockOptions.timeouts()).andReturn(timeouts);
        expect(timeouts.implicitlyWait(anyLong(), anyObject(TimeUnit.class))).andReturn(timeouts);
    }
}
