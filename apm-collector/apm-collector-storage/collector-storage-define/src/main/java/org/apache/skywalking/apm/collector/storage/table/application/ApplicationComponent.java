/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.collector.storage.table.application;

import org.apache.skywalking.apm.collector.core.data.Column;
import org.apache.skywalking.apm.collector.core.data.StreamData;
import org.apache.skywalking.apm.collector.core.data.operator.CoverOperation;
import org.apache.skywalking.apm.collector.core.data.operator.NonOperation;

/**
 * @author peng-yongsheng
 */
public class ApplicationComponent extends StreamData {

    private static final Column[] STRING_COLUMNS = {
        new Column(ApplicationComponentTable.COLUMN_ID, new NonOperation()),
        new Column(ApplicationComponentTable.COLUMN_METRIC_ID, new NonOperation()),
    };

    private static final Column[] LONG_COLUMNS = {
        new Column(ApplicationComponentTable.COLUMN_TIME_BUCKET, new CoverOperation()),
    };

    private static final Column[] DOUBLE_COLUMNS = {};

    private static final Column[] INTEGER_COLUMNS = {
        new Column(ApplicationComponentTable.COLUMN_COMPONENT_ID, new CoverOperation()),
        new Column(ApplicationComponentTable.COLUMN_APPLICATION_ID, new CoverOperation()),
    };

    private static final Column[] BYTE_COLUMNS = {};

    public ApplicationComponent() {
        super(STRING_COLUMNS, LONG_COLUMNS, DOUBLE_COLUMNS, INTEGER_COLUMNS, BYTE_COLUMNS);
    }

    @Override public String getId() {
        return getDataString(0);
    }

    @Override public void setId(String id) {
        setDataString(0, id);
    }

    @Override public String getMetricId() {
        return getDataString(1);
    }

    @Override public void setMetricId(String metricId) {
        setDataString(1, metricId);
    }

    public Long getTimeBucket() {
        return getDataLong(0);
    }

    public void setTimeBucket(Long timeBucket) {
        setDataLong(0, timeBucket);
    }

    public Integer getComponentId() {
        return getDataInteger(0);
    }

    public void setComponentId(Integer componentId) {
        setDataInteger(0, componentId);
    }

    public Integer getApplicationId() {
        return getDataInteger(1);
    }

    public void setApplicationId(Integer applicationId) {
        setDataInteger(1, applicationId);
    }
}
