package cbb.qomo.engine.model

import cbb.qomo.engine.Status

class JobUnit {

    UUID id

    UUID toolId

    String command

    String wd

    String log

    Map<String, String> env

    Status status

    Date updatedAt
}
