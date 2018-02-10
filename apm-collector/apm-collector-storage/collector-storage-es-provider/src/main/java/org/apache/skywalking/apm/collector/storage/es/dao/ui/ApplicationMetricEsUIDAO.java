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

package org.apache.skywalking.apm.collector.storage.es.dao.ui;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.skywalking.apm.collector.client.elasticsearch.ElasticSearchClient;
import org.apache.skywalking.apm.collector.storage.dao.ui.IApplicationMetricUIDAO;
import org.apache.skywalking.apm.collector.storage.es.base.dao.EsDAO;
import org.apache.skywalking.apm.collector.storage.table.MetricSource;
import org.apache.skywalking.apm.collector.storage.table.application.ApplicationMetricTable;
import org.apache.skywalking.apm.collector.storage.ui.common.Step;
import org.apache.skywalking.apm.collector.storage.ui.overview.ApplicationTPS;
import org.apache.skywalking.apm.collector.storage.utils.TimePyramidTableNameBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.InternalSimpleValue;
import org.elasticsearch.search.aggregations.pipeline.PipelineAggregatorBuilders;

/**
 * @author peng-yongsheng
 */
public class ApplicationMetricEsUIDAO extends EsDAO implements IApplicationMetricUIDAO {

    public ApplicationMetricEsUIDAO(ElasticSearchClient client) {
        super(client);
    }

    private static final String AVG_TPS = "avg_tps";

    @Override
    public List<ApplicationTPS> getTopNApplicationThroughput(Step step, long start, long end, long betweenSecond,
        int topN,
        MetricSource metricSource) {
        String tableName = TimePyramidTableNameBuilder.build(step, ApplicationMetricTable.TABLE);

        SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(tableName);
        searchRequestBuilder.setTypes(ApplicationMetricTable.TABLE_TYPE);
        searchRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must().add(QueryBuilders.rangeQuery(ApplicationMetricTable.COLUMN_TIME_BUCKET).gte(start).lte(end));
        boolQuery.must().add(QueryBuilders.termQuery(ApplicationMetricTable.COLUMN_SOURCE_VALUE, metricSource.getValue()));

        searchRequestBuilder.setQuery(boolQuery);
        searchRequestBuilder.setSize(0);

        TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms(ApplicationMetricTable.COLUMN_APPLICATION_ID).field(ApplicationMetricTable.COLUMN_APPLICATION_ID).size(topN);
        aggregationBuilder.subAggregation(AggregationBuilders.sum(ApplicationMetricTable.COLUMN_TRANSACTION_CALLS).field(ApplicationMetricTable.COLUMN_TRANSACTION_CALLS));
        aggregationBuilder.subAggregation(AggregationBuilders.sum(ApplicationMetricTable.COLUMN_TRANSACTION_ERROR_CALLS).field(ApplicationMetricTable.COLUMN_TRANSACTION_ERROR_CALLS));

        Map<String, String> bucketsPathsMap = new HashMap<>();
        bucketsPathsMap.put(ApplicationMetricTable.COLUMN_TRANSACTION_CALLS, ApplicationMetricTable.COLUMN_TRANSACTION_CALLS);
        bucketsPathsMap.put(ApplicationMetricTable.COLUMN_TRANSACTION_ERROR_CALLS, ApplicationMetricTable.COLUMN_TRANSACTION_ERROR_CALLS);

        String idOrCode = "(params." + ApplicationMetricTable.COLUMN_TRANSACTION_CALLS + " - params." + ApplicationMetricTable.COLUMN_TRANSACTION_ERROR_CALLS + ")"
            + " / "
            + "(" + betweenSecond + ")";
        Script script = new Script(idOrCode);
        aggregationBuilder.subAggregation(PipelineAggregatorBuilders.bucketScript(AVG_TPS, bucketsPathsMap, script));

        searchRequestBuilder.addAggregation(aggregationBuilder);
        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();

        List<ApplicationTPS> applicationTPSs = new LinkedList<>();
        Terms serviceIdTerms = searchResponse.getAggregations().get(ApplicationMetricTable.COLUMN_APPLICATION_ID);
        serviceIdTerms.getBuckets().forEach(serviceIdTerm -> {
            int applicationId = serviceIdTerm.getKeyAsNumber().intValue();

            ApplicationTPS serviceMetric = new ApplicationTPS();
            InternalSimpleValue simpleValue = serviceIdTerm.getAggregations().get(AVG_TPS);

            serviceMetric.setApplicationId(applicationId);
            serviceMetric.setTps((int)simpleValue.getValue());
            applicationTPSs.add(serviceMetric);
        });
        return applicationTPSs;
    }
}
