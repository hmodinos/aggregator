package demo;

import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.metrics.export.Exporter;
import org.springframework.boot.actuate.metrics.export.MetricCopyExporter;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.boot.actuate.metrics.repository.redis.RedisMetricRepository;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ComponentScan
@EnableAutoConfiguration
@RestController
@EnableScheduling
public class GeneratorApplication {

	@Autowired
	private RedisConnectionFactory connectionFactory;

	@Autowired
	private ApplicationContext context;

	@RequestMapping("/")
	public String hello() {
		return "Hello World";
	}
	
	@Bean
	protected MetricWriter writer() {
		RedisMetricRepository repository = new RedisMetricRepository(connectionFactory);
		repository.setPrefix("spring.metrics.collector." + context.getId() + "." + ObjectUtils.getIdentityHexString(context) + ".");
		repository.setKey("keys.spring.metrics.collector");
		return repository;
	}
	
	@Bean
	@Primary
	protected MetricRepository reader() {
		return new InMemoryMetricRepository();
	}
	
	@Bean
	public Exporter exporter(InMemoryMetricRepository reader) {
		return new MetricCopyExporter(reader, writer()) {
			@Override
			@Scheduled(fixedRate=5000)
			public void export() {
				super.export();
			}
		};
	}
	
    public static void main(String[] args) {
        SpringApplication.run(GeneratorApplication.class, args);
    }

	@Bean(name="org.springframework.autoconfigure.redis.RedisAutoConfiguration$RedisProperties")
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
