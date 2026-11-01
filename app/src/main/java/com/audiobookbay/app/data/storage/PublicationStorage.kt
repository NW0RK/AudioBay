package com.audiobookbay.app.data.storage

import java.io.InputStream
import java.io.OutputStream

/** Document operations use opaque URIs, never converted into guessed filesystem paths. */
interface PublicationStorage {
    fun root(treeUri: String): String
    fun exists(uri: String): Boolean
    fun children(parent: String): List<Document>
    fun directory(parent: String, name: String): String
    fun file(parent: String, name: String): String
    fun read(uri: String): InputStream
    fun write(uri: String): OutputStream
    fun rename(uri: String, name: String): String
    fun delete(uri: String)
    data class Document(val uri: String, val name: String, val isDirectory: Boolean)
}
