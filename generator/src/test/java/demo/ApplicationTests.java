package demo;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;
import org.springframework.boot.test.SpringApplicationContextLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = GeneratorApplication.class, loader=SpringApplicationContextLoader.class)
public class ApplicationTests {
	
	@Autowired
	private GaugeService gauges;

	@Test
	public void contextLoads() {
		assertTrue(ReflectionTestUtils.getField(gauges, "writer") instanceof InMemoryMetricRepository);
	}

}
