/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package demo;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;
import org.springframework.util.StringUtils;

/**
 * A metric reader that aggregates values from a source reader, normally one that has been
 * collecting data from many sources in the same form (like a scaled-out application). The
 * source has metrics with names in the form '*.*.counter.**' and '*.*.gauge.**' (the
 * length of the prefix is controlled by the {@link #setTruncateKeyLength(int) key length}
 * property, and defaults to 2, meaning 2 period separated fields), and the result has
 * metric names in the form 'aggregate.count.**' and 'aggregate.gauge.**'. Counters are
 * summed and gauges are aggregated by choosing the most recent value.
 * 
 * @author Dave Syer
 *
 */
public class AggregateMetricReader implements MetricReader {

	private MetricReader source;

	private int truncate = 2;

	private String prefix = "aggregate.";

	public AggregateMetricReader(MetricReader source) {
		this.source = source;
	}

	/**
	 * The number of period-separated keys to remove from the start of the input metric
	 * names before aggregating.
	 * 
	 * @param truncate length of source metric prefixes
	 */
	public void setTruncateKeyLength(int truncate) {
		this.truncate = truncate;
	}

	/**
	 * Prefix to apply to all output metrics. A period will be appended if no present in
	 * the provided value.
	 * 
	 * @param prefix the prefix to use default "aggregator.")
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix.endsWith(".") ? prefix : prefix + ".";
	}

	@Override
	public Metric<?> findOne(String metricName) {
		if (!metricName.startsWith(prefix)) {
			return null;
		}
		InMemoryMetricRepository result = new InMemoryMetricRepository();
		String baseName = metricName.substring(prefix.length());
		for (Metric<?> metric : source.findAll()) {
			String name = getSourceKey(metric);
			if (baseName.equals(name)) {
				update(result, name, metric);
			}
		}
		return result.findOne(metricName);
	}

	@Override
	public Iterable<Metric<?>> findAll() {
		InMemoryMetricRepository result = new InMemoryMetricRepository();
		for (Metric<?> metric : source.findAll()) {
			String key = getSourceKey(metric);
			if (key != null) {
				update(result, key, metric);
			}
		}
		return result.findAll();
	}

	@Override
	public long count() {
		long count = 0;
		for (Metric<?> metric : source.findAll()) {
			if (getSourceKey(metric) != null) {
				count++;
			}
		}
		return count;
	}

	private void update(InMemoryMetricRepository result, String key, Metric<?> metric) {
		String name = prefix + key;
		Metric<?> aggregate = result.findOne(name);
		if (aggregate == null) {
			aggregate = new Metric<Number>(name, metric.getValue(), metric.getTimestamp());
		}
		else if (key.startsWith("counter")) {
			// accumulate all values
			aggregate = new Metric<Number>(name, metric.increment(
					aggregate.getValue().intValue()).getValue(), metric.getTimestamp());
		}
		else if (aggregate.getTimestamp().before(metric.getTimestamp())) {
			// sort by timestamp and only take the latest
			aggregate = new Metric<Number>(name, metric.getValue(), metric.getTimestamp());
		}
		result.set(aggregate);
	}

	private String getSourceKey(Metric<?> metric) {
		String[] keys = StringUtils.delimitedListToStringArray(metric.getName(), ".");
		if (keys.length <= truncate) {
			return null;
		}
		StringBuilder builder = new StringBuilder(keys[truncate]);
		for (int i = truncate + 1; i < keys.length; i++) {
			builder.append(".").append(keys[i]);
		}
		String name = builder.toString();
		return name;
	}

}
