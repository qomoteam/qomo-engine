package cbb.qomo.engine

import groovy.util.logging.Slf4j
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener


@Slf4j
@SpringBootApplication
class QomoEngineApplication {

    @Autowired
    RabbitTemplate rabbitTemplate

	static void main(String[] args) {
        def app = new SpringApplication(QomoEngineApplication)
		app.addListeners(new ApplicationListener<ApplicationEvent>() {
			@Override
			void onApplicationEvent(ApplicationEvent event) {
				if (event instanceof ApplicationReadyEvent) {
                    log.info("ENV: [db=${event.applicationContext.environment.getProperty('spring.datasource.url')}]")
                    log.info("ENV: [rabbitmq=${event.applicationContext.environment.getProperty('spring.rabbitmq.addresses')}]")
                    log.info("ENV: [rabbitmq.virtual-host=${event.applicationContext.environment.getProperty('spring.rabbitmq.virtual-host')}]")
                    log.info("ENV: [rabbitmq.queue=${event.applicationContext.environment.getProperty('app.queue.name')}]")
				}
			}
		})

		app.run(args)
	}


}
