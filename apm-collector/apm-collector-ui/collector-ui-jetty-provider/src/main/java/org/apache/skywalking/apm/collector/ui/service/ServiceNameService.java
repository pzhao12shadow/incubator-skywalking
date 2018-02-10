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

package org.apache.skywalking.apm.collector.ui.service;

import java.text.ParseException;
import java.util.List;
import org.apache.skywalking.apm.collector.cache.CacheModule;
import org.apache.skywalking.apm.collector.cache.service.ServiceNameCacheService;
import org.apache.skywalking.apm.collector.core.module.ModuleManager;
import org.apache.skywalking.apm.collector.storage.StorageModule;
import org.apache.skywalking.apm.collector.storage.dao.ui.IServiceMetricUIDAO;
import org.apache.skywalking.apm.collector.storage.dao.ui.IServiceNameServiceUIDAO;
import org.apache.skywalking.apm.collector.storage.table.MetricSource;
import org.apache.skywalking.apm.collector.storage.ui.common.ResponseTimeTrend;
import org.apache.skywalking.apm.collector.storage.ui.common.SLATrend;
import org.apache.skywalking.apm.collector.storage.ui.common.Step;
import org.apache.skywalking.apm.collector.storage.ui.common.ThroughputTrend;
import org.apache.skywalking.apm.collector.storage.ui.service.ServiceInfo;
import org.apache.skywalking.apm.collector.storage.ui.service.ServiceMetric;
import org.apache.skywalking.apm.collector.storage.utils.DurationPoint;
import org.apache.skywalking.apm.collector.ui.utils.DurationUtils;

/**
 * @author peng-yongsheng
 */
public class ServiceNameService {

    private final IServiceNameServiceUIDAO serviceNameServiceUIDAO;
    private final IServiceMetricUIDAO serviceMetricUIDAO;
    private final ServiceNameCacheService serviceNameCacheService;

    public ServiceNameService(ModuleManager moduleManager) {
        this.serviceNameServiceUIDAO = moduleManager.find(StorageModule.NAME).getService(IServiceNameServiceUIDAO.class);
        this.serviceMetricUIDAO = moduleManager.find(StorageModule.NAME).getService(IServiceMetricUIDAO.class);
        this.serviceNameCacheService = moduleManager.find(CacheModule.NAME).getService(ServiceNameCacheService.class);
    }

    public int getCount() {
        return serviceNameServiceUIDAO.getCount();
    }

    public List<ServiceInfo> searchService(String keyword, int topN) {
        return serviceNameServiceUIDAO.searchService(keyword, topN);
    }

    public ThroughputTrend getServiceTPSTrend(int serviceId, Step step, long start, long end) throws ParseException {
        ThroughputTrend throughputTrend = new ThroughputTrend();
        List<DurationPoint> durationPoints = DurationUtils.INSTANCE.getDurationPoints(step, start, end);
        List<Integer> callsTrends = serviceMetricUIDAO.getServiceTPSTrend(serviceId, step, durationPoints);

        //TODO
        callsTrends.forEach(calls -> throughputTrend.getTrendList().add(calls / 1000));

        return throughputTrend;
    }

    public ResponseTimeTrend getServiceResponseTimeTrend(int serviceId, Step step, long start,
        long end) throws ParseException {
        ResponseTimeTrend responseTimeTrend = new ResponseTimeTrend();
        List<DurationPoint> durationPoints = DurationUtils.INSTANCE.getDurationPoints(step, start, end);
        responseTimeTrend.setTrendList(serviceMetricUIDAO.getServiceResponseTimeTrend(serviceId, step, durationPoints));
        return responseTimeTrend;
    }

    public SLATrend getServiceSLATrend(int serviceId, Step step, long start, long end) throws ParseException {
        SLATrend slaTrend = new SLATrend();
        List<DurationPoint> durationPoints = DurationUtils.INSTANCE.getDurationPoints(step, start, end);
        slaTrend.setTrendList(serviceMetricUIDAO.getServiceSLATrend(serviceId, step, durationPoints));
        return slaTrend;
    }

    public List<ServiceMetric> getSlowService(Step step, long start, long end,
        Integer top) throws ParseException {
        List<ServiceMetric> slowServices = serviceMetricUIDAO.getSlowService(0, step, start, end, top, MetricSource.Callee);
        slowServices.forEach(slowService -> {
            slowService.setName(serviceNameCacheService.get(slowService.getId()).getServiceName());
            //TODO
            slowService.setTps(1);
        });
        return slowServices;
    }
}
