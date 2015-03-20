/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

import com.opengamma.platform.finance.rate.swap.PaymentEvent;
import com.opengamma.platform.finance.rate.swap.PaymentPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.impl.MockPricingEnvironment;
import com.opengamma.platform.pricer.swap.PaymentEventPricerFn;
import com.opengamma.platform.pricer.swap.PaymentPeriodPricerFn;

/**
 * Test.
 */
@Test
public class DefaultExpandedSwapLegPricerFnTest {

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment();

  public void test_presentValue() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    when(mockPeriod.presentValue(MOCK_ENV, SwapDummyData.IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    when(mockEvent.presentValue(MOCK_ENV, SwapDummyData.NOTIONAL_EXCHANGE))
        .thenReturn(1000d);
    DefaultExpandedSwapLegPricerFn test = new DefaultExpandedSwapLegPricerFn(mockPeriod, mockEvent);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.IBOR_EXPANDED_SWAP_LEG), 2000d, 0d);
  }

  public void test_futureValue() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    when(mockPeriod.futureValue(MOCK_ENV, SwapDummyData.IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    when(mockEvent.futureValue(MOCK_ENV, SwapDummyData.NOTIONAL_EXCHANGE))
        .thenReturn(1000d);
    DefaultExpandedSwapLegPricerFn test = new DefaultExpandedSwapLegPricerFn(mockPeriod, mockEvent);
    assertEquals(test.futureValue(MOCK_ENV, SwapDummyData.IBOR_EXPANDED_SWAP_LEG), 2000d, 0d);
  }

}
