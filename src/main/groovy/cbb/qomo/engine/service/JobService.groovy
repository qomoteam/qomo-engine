package cbb.qomo.engine.service

import cbb.qomo.engine.Status
import cbb.qomo.engine.model.Job
import cbb.qomo.engine.model.JobUnit
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import org.postgresql.util.PGobject
import org.springframework.amqp.core.MessageBuilder
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

@Slf4j
@Service
class JobService {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

    @Autowired
    RabbitTemplate rabbitTemplate

    @Autowired
    DockerService dockerService

    String statusQueueName

    JobService(@Value('${app.queue.name}') String queueName) {
        statusQueueName = queueName + '.status'
    }

    void run(Job job) {
        for (JobUnit unit : job.units) {
            try {
                updateStatus(unit, Status.RUNNING)
                int exitValue = runJobUnit(unit)
                updateStatus(unit, exitValue == 0 ? Status.SUCCESS : Status.FAIL)
            } catch (Exception e) {
                StringWriter sw = new StringWriter()
                e.printStackTrace(new PrintWriter(sw))
                unit.log += sw.toString()
                saveJobUnitLog(unit)
                updateStatus(unit, Status.FAIL)
                break
            }
        }

    }

    void notifyStatusChange(JobUnit unit) {
        rabbitTemplate.send(
             statusQueueName,
             MessageBuilder.withBody(unit.id.toString().getBytes())
                  .setContentType('text/plain')
                  .build()
        )
    }

    private int runJobUnit(JobUnit unit) {
        def exitCode = dockerService.runCommand(this, unit, unit.command, unit.wd, unit.env)
        return exitCode
    }

    public void updateStatus(JobUnit unit, Status status) {
        unit.status = status
        switch (status) {
            case Status.RUNNING:
                jdbcTemplate.update(
                     'UPDATE job_units SET status=:status, started_at=:started_at, updated_at=:updated_at WHERE id=:id',
                     [
                          status    : unit.status.ordinal(),
                          id        : unit.id,
                          started_at: new Date(),
                          updated_at: new Date()
                     ]
                )
                break
            case Status.SUCCESS:
            case Status.FAIL:
                jdbcTemplate.update(
                     'UPDATE job_units SET status=:status, ended_at=:ended_at, updated_at=:updated_at WHERE id=:id',
                     [
                          status    : unit.status.ordinal(),
                          id        : unit.id,
                          ended_at  : new Date(),
                          updated_at: new Date()
                     ]
                )
                break
            default:
                jdbcTemplate.update(
                     'UPDATE job_units SET status=:status, updated_at=:updated_at WHERE id=:id',
                     [
                          status    : unit.status.ordinal(),
                          id        : unit.id,
                          updated_at: new Date()
                     ]
                )
        }
        notifyStatusChange(unit)
    }

    void saveJobUnitLog(JobUnit jobUnit) {
        jdbcTemplate.update(
             'UPDATE job_units SET log=:log, updated_at=:updated_at WHERE id=:id',
             [
                  log       : jobUnit.log,
                  id        : jobUnit.id,
                  updated_at: new Date()
             ]
        )
    }

    def saveJobUnitDockerContainerId(JobUnit jobUnit, String dockerHost, String dcid) {
        def docker = new PGobject();
        docker.setValue(new JsonBuilder([
             cid : dcid,
             host: dockerHost
        ]).toString())
        docker.setType('json')
        jdbcTemplate.update(
             'UPDATE job_units SET docker=:docker, updated_at=:updated_at WHERE id=:id',
             [
                  docker    : docker,
                  id        : jobUnit.id,
                  updated_at: new Date()
             ]
        )
    }
}
