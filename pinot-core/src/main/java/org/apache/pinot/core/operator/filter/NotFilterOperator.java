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
package org.apache.pinot.core.operator.filter;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.pinot.core.common.Operator;
import org.apache.pinot.core.operator.blocks.FilterBlock;
import org.apache.pinot.core.operator.docidsets.NotDocIdSet;


public class NotFilterOperator extends BaseFilterOperator {
  private static final String OPERATOR_NAME = "NotFilterOperator";
  private static final String EXPLAIN_NAME = "FILTER_NOT";
  private final BaseFilterOperator _filterOperator;
  private final int _numDocs;

  public NotFilterOperator(BaseFilterOperator filterOperator, int numDocs) {
    _filterOperator = filterOperator;
    _numDocs = numDocs;
  }

  @Override
  public String getOperatorName() {
    return OPERATOR_NAME;
  }

  @Override
  public List<Operator> getChildOperators() {
    return Collections.singletonList(_filterOperator);
  }

  @Nullable
  @Override
  public String toExplainString() {
    return EXPLAIN_NAME;
  }

  @Override
  protected FilterBlock getNextBlock() {
    return new FilterBlock(new NotDocIdSet(_filterOperator.nextBlock().getBlockDocIdSet(), _numDocs));
  }

  public BaseFilterOperator getChildFilterOperator() {
    return _filterOperator;
  }
}
