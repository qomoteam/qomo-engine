package cbb.qomo.engine

import cbb.qomo.engine.model.Job
import cbb.qomo.engine.service.JobService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MessageConsumer {

    @Autowired
    JobService jobService

    public void handleMessage(Job job) {
        jobService.run(job)
    }

}
