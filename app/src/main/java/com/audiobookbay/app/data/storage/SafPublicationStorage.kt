package com.audiobookbay.app.data.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SafPublicationStorage @Inject constructor(@ApplicationContext private val context: Context) : PublicationStorage {
    private val resolver get() = context.contentResolver
    override fun root(treeUri: String): String {
        val tree = Uri.parse(treeUri)
        require(tree.scheme == "content" && DocumentsContract.isTreeUri(tree)) { "Select a shared audiobook folder" }
        check(resolver.persistedUriPermissions.any { it.uri == tree && it.isReadPermission && it.isWritePermission }) { "Folder access was revoked; select the folder again" }
        val document = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree)).toString()
        check(exists(document)) { "Shared folder is unavailable" }
        return document
    }
    override fun exists(uri: String): Boolean = resolver.query(Uri.parse(uri), arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)?.use { it.moveToFirst() } ?: false
    override fun children(parent: String): List<PublicationStorage.Document> {
        val uri = Uri.parse(parent)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getDocumentId(uri))
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE)
        return checkNotNull(resolver.query(children, projection, null, null, null)) { "Cannot read shared folder" }.use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(PublicationStorage.Document(
                    DocumentsContract.buildDocumentUriUsingTree(uri, cursor.getString(0)).toString(), cursor.getString(1),
                    cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR))
            }
        }
    }
    override fun directory(parent: String, name: String) = create(parent, name, DocumentsContract.Document.MIME_TYPE_DIR)
    override fun file(parent: String, name: String): String {
        val mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase()) ?: "application/octet-stream"
        return create(parent, name, mime)
    }
    private fun create(parent: String, name: String, mime: String) = checkNotNull(DocumentsContract.createDocument(resolver, Uri.parse(parent), mime, name)) { "Cannot create shared file or folder" }.toString()
    override fun read(uri: String) = checkNotNull(resolver.openInputStream(Uri.parse(uri))) { "Cannot read shared file" }
    override fun write(uri: String) = checkNotNull(resolver.openOutputStream(Uri.parse(uri), "wt")) { "Cannot write shared file" }
    override fun rename(uri: String, name: String) = checkNotNull(DocumentsContract.renameDocument(resolver, Uri.parse(uri), name)) { "Cannot finalize shared book folder" }.toString()
    override fun delete(uri: String) { check(DocumentsContract.deleteDocument(resolver, Uri.parse(uri))) { "Cannot replace incomplete copy" } }
}
