package net.clynamic.contents

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.clynamic.common.AppMeta
import net.clynamic.common.HttpErrorInterceptor
import net.clynamic.common.UserAgentInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL

class ContentsClient {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpErrorInterceptor())
        .addInterceptor(UserAgentInterceptor(AppMeta.userAgent))
        .build()


    suspend fun resolve(url: String): InputStream = withContext(Dispatchers.IO) {
        try {
            val parsedUrl = URL(url)
            when (parsedUrl.protocol) {
                "http", "https" -> {
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) throw IOException("Failed to download file: $response")
                    response.body?.byteStream() ?: throw IOException("Response body is null")
                }

                "file" -> FileInputStream(parsedUrl.path)
                else -> throw IllegalArgumentException("Unsupported URL protocol")
            }
        } catch (e: MalformedURLException) {
            throw IOException("Malformed URL: $url", e)
        } catch (e: FileNotFoundException) {
            throw IOException("File not found: $url", e)
        } catch (e: IOException) {
            throw IOException("Error resolving URL: $url", e)
        } catch (e: IllegalArgumentException) {
            throw IOException("Error resolving URL due to illegal argument: $url", e)
        }
    }
}