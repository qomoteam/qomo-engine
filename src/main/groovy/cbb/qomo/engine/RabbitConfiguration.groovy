package cbb.qomo.engine

import cbb.qomo.engine.model.Job
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.DefaultClassMapper
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitConfiguration {

    @Bean
    MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter()
        DefaultClassMapper classMapper = new DefaultClassMapper()
        classMapper.setDefaultType(Job)
        converter.setClassMapper(classMapper)
        ObjectMapper mapper = new ObjectMapper()
        return converter
    }

    @Bean
    Queue queue() {
        return new Queue("qomo_development.jobs", true, false, false)
    }

    @Bean
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
                                             MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)
        container.setQueueNames('qomo_development.jobs')
        container.setMessageListener(listenerAdapter)
        return container
    }

    @Bean
    MessageListenerAdapter listenerAdapter(MessageConsumer consumer,
                                           MessageConverter converter) {
        return new MessageListenerAdapter(consumer, converter)
    }

}
