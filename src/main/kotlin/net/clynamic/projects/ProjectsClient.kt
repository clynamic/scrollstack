package net.clynamic.projects

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.clynamic.common.AppMeta
import net.clynamic.common.HttpErrorInterceptor
import net.clynamic.common.UserAgentInterceptor
import net.clynamic.contents.ContentRequest
import net.clynamic.contents.ContentUpdate
import net.clynamic.contents.ContentsService
import net.clynamic.contents.url
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.time.Instant
import java.util.logging.Logger

class ProjectsClient(private val contentsService: ContentsService) {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpErrorInterceptor())
        .addInterceptor(UserAgentInterceptor(AppMeta.userAgent))
        .build()

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule()).disable(
        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
    )

    private val logger = Logger.getLogger(ProjectsClient::class.java.name)

    suspend fun resolve(projects: List<ProjectSource>, origin: String? = null): List<Project> =
        projects.mapNotNull {
            try {
                resolve(it, origin)
            } catch (e: IOException) {
                // TODO: signal the user that the project could not be resolved
                // otherwise, this will just swallow invalid projects
                // or maybe introduce a new endpoint with broken projects
                logger.warning("Failed to resolve project: ${it.id}: ${e.message}")
                null
            }
        }

    suspend fun resolve(project: ProjectSource, origin: String? = null): Project =
        when (project.type) {
            ProjectType.GITHUB -> resolveGithubProject(project, origin)
        }

    private suspend fun resolveGithubProject(
        project: ProjectSource,
        origin: String? = null
    ): Project =
        withContext(Dispatchers.IO) {
            val (owner, repo) = project.getOwnerAndRepo()
            val request = Request.Builder()
                .url(
                    HttpUrl.Builder()
                        .scheme("https")
                        .host("api.github.com")
                        .addPathSegment("repos")
                        .addPathSegment(owner)
                        .addPathSegment(repo)
                        .build()
                )
                .build()
            val response = client.newCall(request).execute()
            return@withContext response.use {
                val body = response.body!!.string()
                val banner = resolveGithubBanner(project, origin)
                val map = mapper.readValue<Map<String, Any>>(body)
                return@use Project(
                    id = project.id,
                    name = map["name"] as String,
                    source = map["html_url"] as String,
                    description = map["description"] as? String?,
                    updated = map["pushed_at"]?.let { Instant.parse(it as String) },
                    website = map["homepage"] as? String?,
                    language = map["language"] as? String?,
                    banner = banner,
                    stars = map["stargazers_count"] as? Int ?: 0,
                )
            }
        }

    private suspend fun resolveGithubBanner(
        project: ProjectSource,
        origin: String? = null
    ): String? =
        withContext(Dispatchers.IO) {
            val (owner, repo) = project.getOwnerAndRepo()
            val request = Request.Builder()
                .url(
                    HttpUrl.Builder()
                        .scheme("https")
                        .host("github.com")
                        .addPathSegment(owner)
                        .addPathSegment(repo)
                        .build()
                )
                .build()

            val response = client.newCall(request).execute()
            return@withContext response.use {
                val document = Jsoup.parse(response.body!!.string())
                val metaTag = document.selectFirst("meta[property=og:image]")

                val githubUrl = metaTag?.attr("content") ?: return@use null

                val imageRequest = Request.Builder()
                    .url(githubUrl)
                    .head()
                    .build()

                val mimeType = client.newCall(imageRequest).execute().use { imageResponse ->
                    imageResponse.header("Content-Type", "image")!!
                }

                val current = contentsService.findBySource(githubUrl)
                val id = if (current != null) {
                    contentsService.update(
                        current.id,
                        ContentUpdate(
                            source = githubUrl,
                            mime = mimeType,
                            expiresAt = Instant.now().plusSeconds(60 * 60 * 24 * 7)
                        )
                    )
                    current.id
                } else {
                    contentsService.create(
                        ContentRequest(
                            source = githubUrl,
                            mime = mimeType,
                            expiresAt = Instant.now().plusSeconds(60 * 60 * 24 * 7)
                        )
                    )
                }

                val content = contentsService.read(id)

                val originUrl = origin?.toHttpUrlOrNull() ?: return@use content.url

                // This URL assembly will fail if the server is behind a sub-path
                return@use HttpUrl.Builder()
                    .scheme(originUrl.scheme)
                    .host(originUrl.host)
                    .port(originUrl.port)
                    .addPathSegments(content.url.removePrefix("/"))
                    .build()
                    .toString()
            }
        }
}