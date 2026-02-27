package com.mdeo.optimizerexecution.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object OptimizerExecutionsTable : Table("optimizer_executions") {
    val id = uuid("id")
    val projectId = uuid("project_id")
    val configPath = varchar("config_path", 1024)
    val state = varchar("state", 32)
    val progress = text("progress").nullable()
    val createdAt = timestamp("created_at")
    val startedAt = timestamp("started_at").nullable()
    val completedAt = timestamp("completed_at").nullable()
    val error = text("error").nullable()
    val configData = text("config_data").nullable()
    override val primaryKey = PrimaryKey(id)
    init { index(false, projectId) }
}

object OptimizerResultFilesTable : Table("optimizer_result_files") {
    val id = uuid("id")
    val executionId = uuid("execution_id") references OptimizerExecutionsTable.id
    val filePath = varchar("file_path", 1024)
    val content = text("content")
    val mimeType = varchar("mime_type", 128)
    override val primaryKey = PrimaryKey(id)
    init {
        index(false, executionId)
        uniqueIndex(executionId, filePath)
    }
}

object OptimizerTables {
    fun createTables() {
        transaction {
            SchemaUtils.create(OptimizerExecutionsTable, OptimizerResultFilesTable)
        }
    }
}
