package dev.fuelyour.namedtopositionalsqlparams.converter

import dev.fuelyour.namedtopositionalsqlparams.exceptions.InvalidParamKeyNameException

data class PositionalSql(val sql: String, val params: List<Any?>)

/**
 * Converts a sql query using named parameters and a map of parameter names to parameter values
 * into a sql query using positional parameters and a list of parameter values.
 *
 * Note that a parameter key is only allowed to consist of alphanumeric characters and the
 * underscore.
 *
 * @param sql the sql query using named parameters
 * @param params the map of parameter keys to parameter values
 * @return a pair of the sql query using positional parameters and a list of parameter values
 * @throws InvalidParamKeyNameException if a key name contains characters other than alphanumeric
 *     characters and the underscore
 */
fun convertNamedToPositional(sql: String, params: Map<String, Any?>): PositionalSql {
    params.verifyParamNames()
    return params
        .toList()
        .fold(PositionalSql(sql, listOf())) { (sql, params), (paramName, paramValue) ->
            val newParams = listOf(*params.toTypedArray(), paramValue)
            val newSql = sql.replaceNamedParamWithPositional(paramName, newParams.lastIndex)
            PositionalSql(newSql, newParams)
        }
}

private fun Map<String, Any?>.verifyParamNames() {
    val charNotAlphaNumericUnderscore = """\W""".toRegex()
    if (any { (paramName, _) -> paramName.contains(charNotAlphaNumericUnderscore) }) {
        throw InvalidParamKeyNameException()
    }
}

private fun String.replaceNamedParamWithPositional(
    paramName: String,
    arrayPositionReference: Int
): String {
    /* The regex follows this logic:
        1. First, we want to all instances of the parameter key with a colon in front of it.
        2. Next, we want to ensure that the key we found isn't a substring of some other key
           (for instance, if you had the parameters 'name', 'namespace', and 'username').
           The colon at the front of the key prevents matching the key as a substring in the
           middle or at the end ('name' and 'username'), but doesn't protect against substrings
           at the start ('name' and 'namespace'). To protect against this, we look at the next
           character following the key. Since earlier we restricted key names to alphanumeric
           characters and the underscore only, we can simply check if the character is
           alphanumeric or the underscore. If it is, then it's a bad match. If it's not or
           there is no next character, then we move on to the next step.
        3. Lastly, we don't want to match on double colons. This is to protect casting in sql
           queries. For instance, if we had the query 'SELECT :text::text' and a parameter
           named 'text', we'd want to end up with the final result of 'SELECT $1::text'. To
           do this we look at the character immediately in front our :<param name> match. If
           there is no character, or that character is anything other than a colon, then it's
           a match.
        4. Now that we've got our match, we simply want to replace it with a $ and the reference
           to the position in the array (+1, since positional parameters are 1 based and not
           0 based).
     */
    val searchFor = """(?<=[^:]|^):$paramName(?=\W|$)""".toRegex()
    val replaceWith = """\$${arrayPositionReference + 1}"""
    return replace(searchFor, replaceWith)
}
