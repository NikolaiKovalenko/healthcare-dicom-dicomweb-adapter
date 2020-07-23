package com.google.cloud.healthcare.imaging.dicomadapter;

import com.google.cloud.healthcare.imaging.dicomadapter.backupuploader.DelayCalculator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class DelayCalculatorTest extends Assert {
  HashMap<Integer, Long> values;

  @Before
  public void setup() {
    values =
        new HashMap<>() {
          {
            put(5, 100L);
            put(4, 2100L);
            put(3, 5000L);
            put(2, 5000L);
            put(1, 5000L);
            put(0, 5000L);
            put(20, 5000L);
          }
        };
  }

  @Test
  public void delay() {
    DelayCalculator calc = new DelayCalculator(5, 100, 5000);
    for (Map.Entry<Integer, Long> entry : values.entrySet()) {
      assertEquals(calc.getExponentialDelayMillis(entry.getKey()), entry.getValue().longValue());
    }
  }
}
