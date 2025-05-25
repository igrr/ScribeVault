package me.igrr.scribevault.data.vault

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.first
import me.igrr.scribevault.data.preferences.UserPreferencesRepository
import java.io.IOException

class VaultManager(
    private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) {
    
    companion object {
        private const val TAG = "VaultManager"
    }

    /**
     * Files a note to the vault with the given content
     */
    suspend fun fileNote(
        folderPath: String,
        noteFilename: String,
        content: String
    ): Boolean {
        return try {
            val vaultUri = preferencesRepository.vaultUri.first()
            if (vaultUri == null) {
                Log.e(TAG, "No vault URI configured")
                return false
            }

            val vaultRoot = DocumentFile.fromTreeUri(context, vaultUri)
            if (vaultRoot == null || !vaultRoot.exists()) {
                Log.e(TAG, "Vault root does not exist")
                return false
            }

            // Create or get the target folder
            val targetFolder = createOrGetFolder(vaultRoot, folderPath)
            if (targetFolder == null) {
                Log.e(TAG, "Failed to create or access target folder: $folderPath")
                return false
            }

            // Create the markdown file
            val noteSuccess = createMarkdownFile(targetFolder, noteFilename, content)
            if (!noteSuccess) {
                Log.e(TAG, "Failed to create markdown file")
                return false
            }

            Log.d(TAG, "Successfully filed note: $noteFilename in $folderPath")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error filing note", e)
            false
        }
    }

    /**
     * Creates or gets a folder at the specified path relative to the vault root
     */
    private fun createOrGetFolder(vaultRoot: DocumentFile, folderPath: String): DocumentFile? {
        val pathSegments = folderPath.split("/").filter { it.isNotBlank() }
        var currentFolder = vaultRoot

        for (segment in pathSegments) {
            val existingFolder = currentFolder.findFile(segment)
            currentFolder = if (existingFolder != null && existingFolder.isDirectory) {
                existingFolder
            } else {
                currentFolder.createDirectory(segment) ?: return null
            }
        }

        return currentFolder
    }

    /**
     * Creates a markdown file with the given content
     */
    private fun createMarkdownFile(targetFolder: DocumentFile, filename: String, content: String): Boolean {
        return try {
            // Check if file already exists and delete it (overwrite)
            val existingFile = targetFolder.findFile(filename)
            if (existingFile != null) {
                existingFile.delete()
            }

            // Create new markdown file
            val markdownFile = targetFolder.createFile("text/markdown", filename)
            if (markdownFile == null) {
                Log.e(TAG, "Failed to create markdown file: $filename")
                return false
            }

            // Write content to the file
            val outputStream = context.contentResolver.openOutputStream(markdownFile.uri)
            if (outputStream == null) {
                Log.e(TAG, "Failed to open output stream for markdown file")
                return false
            }

            outputStream.use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
            }

            Log.d(TAG, "Successfully created markdown file: $filename")
            true

        } catch (e: IOException) {
            Log.e(TAG, "Error creating markdown file", e)
            false
        }
    }

    /**
     * Reads the folder structure of the vault (for future implementation)
     */
    suspend fun getFolderStructure(): List<String> {
        // For now, return empty list - this would be implemented later
        // to actually read the vault folder structure
        return emptyList()
    }

    /**
     * Validates that the given URI is a valid Obsidian vault
     */
    suspend fun validateVault(vaultUri: Uri): Boolean {
        return try {
            val vaultRoot = DocumentFile.fromTreeUri(context, vaultUri)
            val obsidianFolder = vaultRoot?.findFile(".obsidian")
            obsidianFolder?.exists() == true
        } catch (e: Exception) {
            Log.e(TAG, "Error validating vault", e)
            false
        }
    }
} 