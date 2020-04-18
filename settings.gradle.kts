rootProject.name = "named-to-positional-sql-params"

val ossrhUsername: String? by settings
val ossrhPassword: String? by settings
extra["ossrhUsername"] = ossrhUsername ?: ""
extra["ossrhPassword"] = ossrhPassword ?: ""