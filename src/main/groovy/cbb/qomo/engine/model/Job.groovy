package cbb.qomo.engine.model

import cbb.qomo.engine.Status


class Job {

    UUID id

    List<JobUnit> units

    Status status

    Date updatedAt
}
