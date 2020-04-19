# named-to-positional-sql-params

Simple library that converts sql queries using named parameters into sql queries using positional
parameters, thus allowing you to use named parameters even if your database tools only support
positional parameters.

For example, you can write code that looks like this:

```kotlin
val namedSql = """
    SELECT *
    FROM users
    WHERE id = :id
    AND name = :name
""".trimIndent()
val namedParams = mapOf(
    "id" to 1,
    "name" to "John"
)
val (positionalSql, positionalParams) = convertNamedToPositional(namedSql, namedParams)
```

And `positionalSql` will be equal to:

```kotlin
"""
    SELECT *
    FROM users
    WHERE id = $1
    AND name = $2
""".trimIndent()
```

And `positionalParams` will be equal to:

```kotlin
listOf(1, "John")
```

You can add it to your project by adding the following line to your `build.gradle.kts`:

```kotlin
implementation("dev.fuelyour:named-to-positional-sql-params:0.0.2")
```

Couple of notes to keep in mind. First, param names can only consist of alphanumeric characters
and the underscore. Trying to use any other character will cause an exception to be thrown. For
example, this won't work:

```kotlin
val sql = "SELECT :pb&j"
val params = mapOf("pb&j" to "peanut butter and jelly")
val result = convertNamedToPositional(sql, params) //This will throw an exception
```

Additionally, param names are case sensitive, so `name`, `Name`, and `NAME` will map to different
parameters.

Lastly, it will not replace places in the sql where it find the param name, but it has two colons
in front of it. This is in order to protect casting operations. So if you have the following code:

```kotlin
val namedSql = "SELECT :text::text"
val namedParams = mapOf("text" to "Some text")
val (positionalSql, positionalParams) = mapOf(namedSql, namedParams)
```

Then `positionalSql` will come out as:

```kotlin
"SELECT $1::text"
```