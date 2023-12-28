package com.leadrdrk.umapatcher.unity

class OutOfRangeException(message: String? = "attempt to read file at out of range position", cause: Throwable? = null)
    : Exception(message, cause)

class InvalidValueException(message: String?, cause: Throwable? = null)
    : Exception(message, cause)

class TypeTreeException(message: String?, cause: Throwable? = null)
    : Exception(message, cause)