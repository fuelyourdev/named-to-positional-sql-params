package dev.fuelyour.namedtopositionalsqlparams.exceptions

import java.lang.RuntimeException

sealed class NamedToPositionalSqlParamsException(message: String): RuntimeException(message)

class InvalidParamKeyNameException: NamedToPositionalSqlParamsException(
    "Only alphanumeric characters and the underscore are allowed in parameter names"
)

class MissingParamException(missing: String): NamedToPositionalSqlParamsException(
    "Missing parameter for prepared positional sql: $missing"
)