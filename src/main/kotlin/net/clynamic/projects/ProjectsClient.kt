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
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.time.Instant

class ProjectsClient {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpErrorInterceptor())
        .addInterceptor(UserAgentInterceptor(AppMeta.userAgent))
        .build()

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule()).disable(
        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
    )

    suspend fun resolve(projects: List<ProjectSource>): List<Project> =
        projects.map { resolve(it) }

    suspend fun resolve(project: ProjectSource): Project = when (project.type) {
        ProjectType.GITHUB -> resolveGithubProject(project)
    }

    private suspend fun resolveGithubProject(project: ProjectSource): Project =
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
                val banner = resolveGithubBanner(project)
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

    private suspend fun resolveGithubBanner(project: ProjectSource): String? {
        return withContext(Dispatchers.IO) {
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

                return@use metaTag?.attr("content")
            }
        }
    }
}