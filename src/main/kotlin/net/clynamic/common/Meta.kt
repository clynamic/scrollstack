package net.clynamic.common

class AppMeta {
    companion object {
        const val name = "scrollstack"
        const val developer = "clynamic"
        const val description = "API server for clynamic profile pages"
        const val version = "1.0.0"

        val userAgent: String
            get() = "$name/$version ($developer)"
    }
}