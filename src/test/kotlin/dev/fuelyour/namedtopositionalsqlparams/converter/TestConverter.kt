package dev.fuelyour.namedtopositionalsqlparams.converter

import dev.fuelyour.namedtopositionalsqlparams.exceptions.NamedToPositionalSqlParamsException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

class TestConverter : StringSpec({
    listOf(
        row(
            "convertNamedToPositional should replace named parameters with positional " +
                "parameter according to map placement",
            """
                SELECT *
                FROM users
                WHERE id = :id
                AND name = :name
            """.trimIndent(),
            mapOf(
                "id" to 1,
                "name" to "Bob"
            ),
            """
                SELECT *
                FROM users
                WHERE id = $1
                AND name = $2
            """.trimIndent(),
            listOf(1, "Bob")
        ),
        row(
            "ordering of the parameters in the query itself won't affect positional " +
                "parameter numbers",
            """
                SELECT *
                FROM users
                WHERE name = :name
                AND id = :id
            """.trimIndent(),
            mapOf(
                "id" to 1,
                "name" to "Bob"
            ),
            """
                SELECT *
                FROM users
                WHERE name = $2
                AND id = $1
            """.trimIndent(),
            listOf(1, "Bob")
        ),
        row(
            "ordering of the parameters in the map will affect positional parameter numbers",
            """
                SELECT *
                FROM users
                WHERE id = :id
                AND name = :name
            """.trimIndent(),
            mapOf(
                "name" to "Bob",
                "id" to 1
            ),
            """
                SELECT *
                FROM users
                WHERE id = $2
                AND name = $1
            """.trimIndent(),
            listOf("Bob", 1)
        ),
        row(
            "parameters can be used multiple times in a query and will still be assigned " +
                "the correct number",
            """
                SELECT *
                FROM users
                WHERE id = :id
                AND name = :name
                AND :id != :name
            """.trimIndent(),
            mapOf(
                "id" to 1,
                "name" to "Bob"
            ),
            """
                SELECT *
                FROM users
                WHERE id = $1
                AND name = $2
                AND $1 != $2
            """.trimIndent(),
            listOf(1, "Bob")
        ),
        row(
            "convertNamedToPositional can properly handle parameter names that are " +
                "substrings of other parameter names",
            """
                SELECT *
                FROM users
                WHERE (name = :name OR name in :names)
                AND (username = :username OR username in :usernames)
            """.trimIndent(),
            mapOf(
                "name" to "Bob",
                "username" to "bob13",
                "names" to listOf("Robert", "Bob", "Bobby", "Rob"),
                "usernames" to listOf("robert95", "bob13")
            ),
            """
                SELECT *
                FROM users
                WHERE (name = $1 OR name in $3)
                AND (username = $2 OR username in $4)
            """.trimIndent(),
            listOf(
                "Bob",
                "bob13",
                listOf("Robert", "Bob", "Bobby", "Rob"),
                listOf("robert95", "bob13")
            )
        ),
        row(
            "null is allowed as a parameter",
            """
                SELECT *
                FROM users
                WHERE id = :id
            """.trimIndent(),
            mapOf(
                "id" to null
            ),
            """
                SELECT *
                FROM users
                WHERE id = $1
            """.trimIndent(),
            listOf(null)
        ),
        row(
            "parameter names are case sensitive",
            "SELECT :test, :Test, :TEST",
            mapOf(
                "test" to "lowercase",
                "Test" to "titlecase",
                "TEST" to "uppercase"
            ),
            "SELECT $1, $2, $3",
            listOf("lowercase", "titlecase", "uppercase")
        ),
        row(
            "begin and end of line scenarios are accounted for",
            ":test",
            mapOf(
                "test" to "value"
            ),
            "$1",
            listOf("value")
        ),
        row(
            "punctuation and casting is accounted for",
            """
                SELECT :id,:name::text
            """.trimIndent(),
            mapOf(
                "id" to 2,
                "name" to "Jane"
            ),
            """
                SELECT $1,$2::text
            """.trimIndent(),
            listOf(2, "Jane")
        ),
        row(
            "empty sql string is permitted",
            "",
            mapOf("id" to 1),
            "",
            listOf(1)
        ),
        row(
            "empty map is permitted",
            "SELECT :id",
            mapOf(),
            "SELECT :id",
            listOf()
        ),
        row(
            "underscores allowed in parameter names",
            """
                INSERT INTO users (name) VALUES (:new_name)
            """.trimIndent(),
            mapOf("new_name" to "Jordan"),
            """
                INSERT INTO users (name) VALUES ($1)
            """.trimIndent(),
            listOf("Jordan")
        ),
        row(
            "parameters right next to each other still get converted",
            ":test:test:test",
            mapOf("test" to "value"),
            "$1$1$1",
            listOf("value")
        ),
        row(
            "parameters following a :: are ignored, even if it matches a key",
            """
              SELECT :text::text
            """.trimIndent(),
            mapOf("text" to "Some text"),
            """
              SELECT $1::text
            """.trimIndent(),
            listOf("Some text")
        )
    ).map {
        (
            description: String,
            npQuery: String,
            npMap: Map<String, Any?>,
            ppQuery: String, ppList: List<Any?>
        ) ->
        description {
            convertNamedToPositional(npQuery, npMap) shouldBe Pair(ppQuery, ppList)
        }
    }

    listOf(
        row("parameter names with non alphanumeric characters are not allowed",
            "SELECT :id",
            mapOf(": ," to "value"),
            "Only alphanumeric characters and the underscore are allowed in parameter names"
        ),
        row("cannot use $<number> format as a key name",
            "SELECT :$1",
            mapOf("$1" to "value"),
            "Only alphanumeric characters and the underscore are allowed in parameter names"
        ),
        row("non alphanumeric character prevented at the beginning",
            "SELECT :*test",
            mapOf("*test" to "value"),
            "Only alphanumeric characters and the underscore are allowed in parameter names"
        ),
        row("non alphanumeric character prevented in the middle",
            "SELECT :te+st",
            mapOf("te+st" to "value"),
            "Only alphanumeric characters and the underscore are allowed in parameter names"
        ),
        row("non alphanumeric character prevented at the end",
            "SELECT :test=",
            mapOf("test=" to "value"),
            "Only alphanumeric characters and the underscore are allowed in parameter names"
        ),
        row("non alphanumeric character prevented at multiple places at simultaneously",
            "SELECT :%t^e-s@t ",
            mapOf("%t^e-s@t " to "value"),
            "Only alphanumeric characters and the underscore are allowed in parameter names"
        )
    ).map {
        (
            description: String,
            npQuery: String,
            npMap: Map<String, Any?>,
            errorMessage: String
        ) ->
        description {
            val exception = shouldThrow<NamedToPositionalSqlParamsException> {
                convertNamedToPositional(npQuery, npMap)
            }
            exception.message shouldBe errorMessage
        }
    }
})
