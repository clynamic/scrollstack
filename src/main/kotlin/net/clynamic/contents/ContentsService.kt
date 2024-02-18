package net.clynamic.contents

import net.clynamic.common.IntServiceTable
import net.clynamic.common.IntSqlService
import net.clynamic.common.instant
import net.clynamic.common.setAll
import net.clynamic.contents.ContentsService.Contents
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import java.time.Instant

class ContentsService(database: Database) :
    IntSqlService<ContentRequest, Content, ContentUpdate, Contents>(database) {
    object Contents : IntServiceTable() {
        val sourceUrl = text("source")
        val mime = text("mime")
        val createdAt = instant("created_at")
        val expiresAt = instant("expires_at").nullable()
    }

    override val table: Contents
        get() = Contents

    override fun toModel(row: ResultRow): Content {
        return Content(
            id = row[Contents.id],
            source = row[Contents.sourceUrl],
            mime = row[Contents.mime],
            createdAt = row[Contents.createdAt],
            expiresAt = row[Contents.expiresAt],
        )
    }

    override fun fromUpdate(statement: UpdateStatement, update: ContentUpdate) {
        statement.setAll {
            Contents.sourceUrl set update.source
            Contents.mime set update.mime
            Contents.expiresAt set update.expiresAt
        }
    }

    override fun fromRequest(statement: InsertStatement<*>, request: ContentRequest) {
        statement.setAll {
            Contents.sourceUrl set request.source
            Contents.mime set request.mime
            Contents.createdAt set Instant.now()
            Contents.expiresAt set request.expiresAt
        }
    }

    suspend fun findBySource(source: String?): Content? = dbQuery {
        if (source == null) return@dbQuery null
        table.select { Contents.sourceUrl eq source }
            .mapNotNull(::toModel)
            .singleOrNull()
    }
}
