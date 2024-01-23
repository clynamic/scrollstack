package net.clynamic.projects

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.jsoup.Jsoup
import java.time.Instant

class ProjectClient {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpErrorInterceptor())
        // TODO: make this info clynamic ;)
        .addInterceptor(UserAgentInterceptor("scrollstack/1.0.0 (clynamic)"))
        .build()
    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule()).disable(
        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
    )

    class HttpErrorInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val response = chain.proceed(request)

            if (!response.isSuccessful) {
                throw IOException("Http Request Failed: $response")
            }

            return response
        }
    }

    class UserAgentInterceptor(private val userAgent: String) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val requestWithUserAgent = originalRequest.newBuilder()
                .header("User-Agent", userAgent)
                .build()
            return chain.proceed(requestWithUserAgent)
        }
    }

    suspend fun resolve(projects: List<PartialProject>): List<Project> {
        return projects.map { resolve(it) }
    }

    suspend fun resolve(project: PartialProject): Project {
        return when (project) {
            is Project -> project
            is RemoteGithubProject -> resolveGithubProject(project)
        }
    }

    private suspend fun resolveGithubProject(project: RemoteGithubProject): GithubProject {
        return withContext(Dispatchers.IO) {
            val uri = "https://api.github.com/repos/${project.owner}/${project.repo}"
            val request = Request.Builder()
                .url(uri)
                .build()
            val response = client.newCall(request).execute()
            return@withContext response.use {
                val body = response.body!!.string()
                val banner = resolveGithubBanner(project)

                val map = mapper.readValue<Map<String, Any>>(body)
                val (owner, repo) = map["full_name"].toString().split("/")

                return@use GithubProject(
                    id = project.id,
                    name = map["name"] as String,
                    owner = owner,
                    repo = repo,
                    description = map["description"] as? String?,
                    stars = map["stargazers_count"] as? Int ?: 0,
                    language = map["language"] as? String?,
                    lastCommit = map["pushed_at"]?.let { Instant.parse(it as String) },
                    homepage = map["homepage"] as? String?,
                    banner = banner,
                )
            }
        }
    }

    private suspend fun resolveGithubBanner(project: RemoteGithubProject): String? {
        return withContext(Dispatchers.IO) {
            val url = "https://github.com/${project.owner}/${project.repo}"
            val request = Request.Builder()
                .url(url)
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