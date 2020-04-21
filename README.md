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
implementation("dev.fuelyour:named-to-positional-sql-params:0.0.3")
```

Couple of notes to keep in mind. First, param names can only consist of alphanumeric characters
and the underscore. Trying to use any other character will cause an exception to be thrown. For
example, this won't work:

```kotlin
val sql = "SELECT :pb&j"
val params = mapOf("pb&j" to "peanut butter and jelly")
val result = convertNamedToPositional(sql, params) //This will throw an exception
```

Also, positional ordering is determined by the map or set that is passed in. If you use a data
structure that maintains insertion order, such as a `LinkedHashMap` or a `LinkedHashSet` (which
are the data structures used by the `mapOf` and `setOf` functions in Kotlin), then the positional
parameter ordering will match your insertion order. If you use a data structure that does not
maintain insertion order, such as `HashMap` and `HashSet` then the parameter ordering is not
guaranteed to match your insertion order. Note that the query and parameter list position will
still match up, though, and so everything will still work without issue.

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

There is also a `prepareNamedAsPositional` function that works similarly to the
`convertNamedToPositional` function, but it does not immediately take a map of parameters. It
can optionally take a set of parameter names, but if this is not supplied it will auto-generate
this set by examining the query provided. This will convert the sql from named to positional,
and will provide a `convertParams` function for converting named parameter maps to positional
parameter lists. This prepared query and converter function can then be used and reused with
different sets of data. Here are some examples:

Omitting Parameter Names:

```kotlin
val sql = """
        SELECT *
        FROM users
        WHERE id = :id
        AND name = :name
    """.trimIndent()
val paramNames = setOf("id", "name")

val preparedPositional = prepareNamedAsPositional(sql, paramNames)

val namedParams1 = mapOf(
    "id" to 2,
    "name" to "Jill"
)
val positionalParams1 = preparedPositional.convertParams(namedParams1)

val namedParams2 = mapOf(
    "id" to 6,
    "name" to "Jack"
)
val positionalParams2 = preparedPositional.convertParams(namedParams2)
```

Explicitly Stating Parameter Names:

```kotlin
val sql = """
        SELECT *
        FROM users
        WHERE id = :id
        AND name = :name
    """.trimIndent()

val preparedPositional = prepareNamedAsPositional(sql)

val namedParams1 = mapOf(
    "id" to 2,
    "name" to "Jill"
)
val positionalParams1 = preparedPositional.convertParams(namedParams1)

val namedParams2 = mapOf(
    "id" to 6,
    "name" to "Jack"
)
val positionalParams2 = preparedPositional.convertParams(namedParams2)
```