/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.segment.local.realtime.impl.dictionary;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.io.IOException;
import java.util.Arrays;
import org.apache.pinot.common.request.context.predicate.RangePredicate;
import org.apache.pinot.segment.local.realtime.impl.forward.FixedByteSVMutableForwardIndex;
import org.apache.pinot.segment.spi.memory.PinotDataBufferMemoryManager;
import org.apache.pinot.spi.data.FieldSpec.DataType;


@SuppressWarnings("Duplicates")
public class IntOffHeapMutableDictionary extends BaseOffHeapMutableDictionary {
  private final FixedByteSVMutableForwardIndex _dictIdToValue;

  private volatile int _min = Integer.MAX_VALUE;
  private volatile int _max = Integer.MIN_VALUE;

  public IntOffHeapMutableDictionary(int estimatedCardinality, int maxOverflowSize,
      PinotDataBufferMemoryManager memoryManager, String allocationContext) {
    super(estimatedCardinality, maxOverflowSize, memoryManager, allocationContext);
    int initialEntryCount = nearestPowerOf2(estimatedCardinality);
    _dictIdToValue =
        new FixedByteSVMutableForwardIndex(false, DataType.INT, initialEntryCount, memoryManager, allocationContext);
  }

  @Override
  public int index(Object value) {
    Integer integerValue = (Integer) value;
    updateMinMax(integerValue);
    return indexValue(integerValue, null);
  }

  @Override
  public int[] index(Object[] values) {
    int numValues = values.length;
    int[] dictIds = new int[numValues];
    for (int i = 0; i < numValues; i++) {
      Integer integerValue = (Integer) values[i];
      updateMinMax(integerValue);
      dictIds[i] = indexValue(integerValue, null);
    }
    return dictIds;
  }

  @Override
  public int compare(int dictId1, int dictId2) {
    return Integer.compare(getIntValue(dictId1), getIntValue(dictId2));
  }

  @Override
  public IntSet getDictIdsInRange(String lower, String upper, boolean includeLower, boolean includeUpper) {
    int numValues = length();
    if (numValues == 0) {
      return IntSets.EMPTY_SET;
    }
    IntSet dictIds = new IntOpenHashSet();

    if (lower.equals(RangePredicate.UNBOUNDED)) {
      int upperValue = Integer.parseInt(upper);
      if (includeUpper) {
        for (int dictId = 0; dictId < numValues; dictId++) {
          int value = getIntValue(dictId);
          if (value <= upperValue) {
            dictIds.add(dictId);
          }
        }
      } else {
        for (int dictId = 0; dictId < numValues; dictId++) {
          int value = getIntValue(dictId);
          if (value < upperValue) {
            dictIds.add(dictId);
          }
        }
      }
    } else if (upper.equals(RangePredicate.UNBOUNDED)) {
      int lowerValue = Integer.parseInt(lower);
      if (includeLower) {
        for (int dictId = 0; dictId < numValues; dictId++) {
          int value = getIntValue(dictId);
          if (value >= lowerValue) {
            dictIds.add(dictId);
          }
        }
      } else {
        for (int dictId = 0; dictId < numValues; dictId++) {
          int value = getIntValue(dictId);
          if (value > lowerValue) {
            dictIds.add(dictId);
          }
        }
      }
    } else {
      int lowerValue = Integer.parseInt(lower);
      int upperValue = Integer.parseInt(upper);
      if (includeLower && includeUpper) {
        for (int dictId = 0; dictId < numValues; dictId++) {
          int value = getIntValue(dictId);
          if (value >= lowerValue && value <= upperValue) {
            dictIds.add(dictId);
          }
        }
      } else if (includeLower) {
        for (int dictId = 0; dictId < numValues; dictId++) {
          int value = getIntValue(dictId);
          if (value >= lowerValue && value < upperValue) {
            dictIds.add(dictId);
          }
        }
      } else if (includeUpper) {
        for (int dictId = 0; dictId < numValues; dictId++) {
          int value = getIntValue(dictId);
          if (value > lowerValue && value <= upperValue) {
            dictIds.add(dictId);
          }
        }
      } else {
        for (int dictId = 0; dictId < numValues; dictId++) {
          int value = getIntValue(dictId);
          if (value > lowerValue && value < upperValue) {
            dictIds.add(dictId);
          }
        }
      }
    }
    return dictIds;
  }

  @Override
  public Integer getMinVal() {
    return _min;
  }

  @Override
  public Integer getMaxVal() {
    return _max;
  }

  @Override
  public int[] getSortedValues() {
    int numValues = length();
    int[] sortedValues = new int[numValues];

    for (int dictId = 0; dictId < numValues; dictId++) {
      sortedValues[dictId] = getIntValue(dictId);
    }

    Arrays.sort(sortedValues);
    return sortedValues;
  }

  @Override
  public DataType getValueType() {
    return DataType.INT;
  }

  @Override
  public int indexOf(String stringValue) {
    return getDictId(Integer.valueOf(stringValue), null);
  }

  public Integer get(int dictId) {
    return getIntValue(dictId);
  }

  @Override
  public int getIntValue(int dictId) {
    return _dictIdToValue.getInt(dictId);
  }

  @Override
  public long getLongValue(int dictId) {
    return (long) getIntValue(dictId);
  }

  @Override
  public float getFloatValue(int dictId) {
    return getIntValue(dictId);
  }

  @Override
  public double getDoubleValue(int dictId) {
    return getIntValue(dictId);
  }

  @Override
  public String getStringValue(int dictId) {
    return Integer.toString(getIntValue(dictId));
  }

  @Override
  protected void setValue(int dictId, Object value, byte[] serializedValue) {
    _dictIdToValue.setInt(dictId, (Integer) value);
  }

  @Override
  protected boolean equalsValueAt(int dictId, Object value, byte[] serializedValue) {
    return getIntValue(dictId) == (Integer) value;
  }

  @Override
  public int getAvgValueSize() {
    return Integer.BYTES;
  }

  @Override
  public long getTotalOffHeapMemUsed() {
    return getOffHeapMemUsed() + Integer.BYTES * (long) length();
  }

  @Override
  public void doClose()
      throws IOException {
    _dictIdToValue.close();
  }

  private void updateMinMax(int value) {
    if (value < _min) {
      _min = value;
    }
    if (value > _max) {
      _max = value;
    }
  }
}
