package cbb.qomo.engine.service

import cbb.qomo.engine.model.JobUnit
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.AccessMode
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.WaitContainerResultCallback
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service

@Service
@Slf4j
class DockerService {

    int runCommand(JobService jobService, JobUnit jobUnit, String command, String wd, Map<String, String> env) {
        def script = new File('/mapr/qomo.cbb/qomo_production/tmp', "script-${jobUnit.id}.sh")
        try {
            script.deleteOnExit()
            script.withWriter { w ->
                env.each { e ->
                    w.write("${e.key}=${e.value}\n")
                }
                w.write("cd ${wd}\n")
                w.write(command)
                w.write("\n")
            }

            log.info("==> Script of job unit ${jobUnit.id}: ${script.absolutePath}")

            def docker = DockerClientBuilder.getInstance(dockerClient).build()

            def containerCmd = docker.createContainerCmd('qomo/runner')
                 .withName("qomo_runner_${jobUnit.id}")
                 .withTty(true)
                 .withDns('192.168.118.212').withDnsSearch('cbb')
            bindVolumes(containerCmd, script)
            def container = containerCmd.exec()
            log.info("Docker container ID: ${container.id}")

            jobService.saveJobUnitDockerContainerId(jobUnit, 'tcp://192.168.118.212:2375', container.id)

            docker.startContainerCmd(container.id).exec()

            def exitCode = docker.waitContainerCmd(container.id).exec(new WaitContainerResultCallback()).awaitStatusCode()

            log.info("Job unit ${jobUnit.id} end")

            return exitCode
        } finally {
            //script.delete()
        }
    }

    def getDockerClient() {
        DefaultDockerClientConfig.createDefaultConfigBuilder()
             .withDockerHost('tcp://192.168.118.212:2375')
             .build()
    }

    def bindVolumes(CreateContainerCmd containerCmd, File script) {
        def volMapr = new Volume('/opt/mapr')
        def volFs = new Volume('/mapr')
        def volScript = new Volume('/home/qomo/script.sh')
        containerCmd.withBinds(
             new Bind(script.absolutePath, volScript, AccessMode.ro),
             new Bind('/opt/mapr', volMapr, AccessMode.ro),
             new Bind('/mapr', volFs, AccessMode.rw)
        )
        return containerCmd
    }

}
