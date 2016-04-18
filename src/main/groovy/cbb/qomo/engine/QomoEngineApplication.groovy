package cbb.qomo.engine

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class QomoEngineApplication {

    @Autowired
    RabbitTemplate rabbitTemplate

	static void main(String[] args) {
		SpringApplication.run QomoEngineApplication, args
	}


}
