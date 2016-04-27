package cbb.qomo.engine.service

import cbb.qomo.engine.Status
import cbb.qomo.engine.model.Job
import cbb.qomo.engine.model.JobUnit
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service


@Slf4j
@Service
class JobService {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate

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

    private int runJobUnit(JobUnit unit) {
        log.info("Run command [${unit.command}], with working dir [${unit.wd}]")
        def envp = unit.env.entrySet().collect {
            it.key + '=' + it.value
        }
        def process = ['sh','-c', parseCmd(unit)].execute(envp, new File(unit.wd))
        LogAppender logAppender = new LogAppender(this, unit)
        process.waitForProcessOutput(logAppender, logAppender)
        return process.exitValue()
    }


    String parseCmd(JobUnit unit) {
        def cmd = unit.command
        // Replace $ to \$
        cmd.replaceAll('\\$', '\\\\\\$')
        return "cd ${unit.wd};${unit.command}"
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
                          ended_at: new Date(),
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


}
