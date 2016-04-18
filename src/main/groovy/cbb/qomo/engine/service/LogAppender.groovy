package cbb.qomo.engine.service

import cbb.qomo.engine.model.JobUnit
import groovy.transform.TupleConstructor


@TupleConstructor
class LogAppender implements Appendable {

    JobService jobService

    JobUnit jobUnit

    StringBuffer buffer = new StringBuffer()

    @Override
    Appendable append(CharSequence csq) throws IOException {
        buffer.append(csq)

        jobUnit.log = buffer.toString()
        jobService.saveJobUnitLog(jobUnit)
        return this
    }

    @Override
    Appendable append(CharSequence csq, int start, int end) throws IOException {
        return append(csq.subSequence(start, end))
    }

    @Override
    Appendable append(char c) throws IOException {
        return append(new String(c))
    }
}
