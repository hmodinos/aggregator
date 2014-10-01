package demo;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.endpoint.MetricReaderPublicMetrics;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;
import org.springframework.boot.actuate.metrics.repository.redis.RedisMetricRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.redis.RedisProperties;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.service.common.RedisServiceInfo;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@ComponentScan
@EnableAutoConfiguration
@Controller
public class CollectorApplication {

	@Autowired
	private RedisConnectionFactory connectionFactory;

	@Autowired
	private ApplicationContext context;

	@RequestMapping("/")
	public String hello() {
		return "forward:/metrics";
	}

	@Bean
	public PublicMetrics metricsIndividual(final MetricReader reader) {
		return new MetricReaderPublicMetrics(repository()) {
			@Override
			public Collection<Metric<?>> metrics() {
				ArrayList<Metric<?>> list = new ArrayList<Metric<?>>(super.metrics());
				for (Metric<?> metric : reader.findAll()) {
					list.add(metric);
				}
				return list;
			}
		};
	}

	@Bean
	public PublicMetrics metricsAggregate() {
		return new MetricReaderPublicMetrics(aggregates());
	}

	@Bean
	protected MetricReader repository() {
		RedisMetricRepository repository = new RedisMetricRepository(connectionFactory);
		repository.setPrefix("spring.metrics.collector.");
		repository.setKey("keys.spring.metrics.collector");
		return repository;
	}

	@Bean
	protected MetricReader aggregates() {
		AggregateMetricReader repository = new AggregateMetricReader(repository());
		return repository;
	}

	@Bean
	@Primary
	protected MetricReader reader() {
		return new InMemoryMetricRepository();
	}

	public static void main(String[] args) {
		SpringApplication.run(CollectorApplication.class, args);
	}

	@Bean
	@Primary
	public RedisProperties redisProperties() throws UnknownHostException {
		RedisProperties properties = new RedisProperties();
		try {
			CloudFactory cloud = new CloudFactory();
			RedisServiceInfo serviceInfo = (RedisServiceInfo) cloud.getCloud()
					.getServiceInfo("redis");
			if (serviceInfo != null) {
				properties.setHost(serviceInfo.getHost());
				properties.setPassword(serviceInfo.getPassword());
				properties.setPort(serviceInfo.getPort());
			}
		} catch (CloudException e) {
			// ignore
		}
		return properties;
	}

}
