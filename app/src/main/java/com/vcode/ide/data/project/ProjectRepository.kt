package com.vcode.ide.data.project

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.vcode.ide.data.local.ProjectDao
import com.vcode.ide.data.local.ProjectEntity
import com.vcode.ide.data.local.RecentTabEntity
import com.vcode.ide.domain.model.FileNode
import com.vcode.ide.domain.model.ProjectTemplate
import com.vcode.ide.domain.model.SearchMatch
import com.vcode.ide.domain.model.VCodeProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ProjectRepository(
    private val context: Context,
    private val projectDao: ProjectDao
) {
    private val workspaceRoot: File
        get() = File(context.filesDir, "projects").apply { mkdirs() }

    fun observeProjects(): Flow<List<VCodeProject>> =
        projectDao.observeProjects().map { list -> list.map { it.toDomain() } }

    suspend fun ensureDefaultProject(): VCodeProject = withContext(Dispatchers.IO) {
        val existing = projectDao.getMostRecentProject()
        if (existing != null && File(existing.rootPath).exists()) {
            return@withContext existing.toDomain()
        }
        createProjectFromTemplate("VCode Web Studio", TemplateProvider.templates.first())
    }

    suspend fun createProjectFromTemplate(name: String, template: ProjectTemplate): VCodeProject = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString().take(8)
        val safeFolderName = name.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "project_$id" }
        val rootDir = File(workspaceRoot, "${safeFolderName}_$id").apply { mkdirs() }

        template.files.forEach { (relPath, content) ->
            val target = File(rootDir, relPath)
            target.parentFile?.mkdirs()
            target.writeText(content)
        }

        val entity = ProjectEntity(
            id = id,
            name = name.trim().ifBlank { "Untitled Project" },
            rootPath = rootDir.absolutePath,
            templateId = template.id,
            lastOpenedAt = System.currentTimeMillis()
        )
        projectDao.upsertProject(entity)
        entity.toDomain()
    }

    suspend fun importFolderFromSaf(treeUri: Uri): VCodeProject = withContext(Dispatchers.IO) {
        val docTree = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalArgumentException("Unable to access selected folder")
        val folderName = docTree.name?.ifBlank { "Imported Project" } ?: "Imported Project"
        val id = UUID.randomUUID().toString().take(8)
        val safeName = folderName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val rootDir = File(workspaceRoot, "${safeName}_$id").apply { mkdirs() }

        copyDocumentTreeToLocal(docTree, rootDir, 0)

        val entity = ProjectEntity(
            id = id,
            name = folderName,
            rootPath = rootDir.absolutePath,
            templateId = "saf-import",
            lastOpenedAt = System.currentTimeMillis(),
            linkedTreeUri = treeUri.toString()
        )
        projectDao.upsertProject(entity)
        entity.toDomain()
    }

    private fun copyDocumentTreeToLocal(srcDir: DocumentFile, destDir: File, depth: Int) {
        if (depth > 8) return
        srcDir.listFiles().forEach { child ->
            val childName = child.name ?: return@forEach
            if (childName == ".git" || childName == "node_modules" || childName == "build") return@forEach
            if (child.isDirectory) {
                val sub = File(destDir, childName).apply { mkdirs() }
                copyDocumentTreeToLocal(child, sub, depth + 1)
            } else if (child.isFile && child.length() <= 2 * 1024 * 1024L) {
                val destFile = File(destDir, childName)
                context.contentResolver.openInputStream(child.uri)?.use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }
            }
        }
    }

    suspend fun touchProject(projectId: String) = withContext(Dispatchers.IO) {
        projectDao.getProjectById(projectId)?.let { entity ->
            projectDao.upsertProject(entity.copy(lastOpenedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteProject(projectId: String): Unit = withContext(Dispatchers.IO) {
        val entity = projectDao.getProjectById(projectId)
        if (entity != null) {
            File(entity.rootPath).deleteRecursively()
            projectDao.clearTabsForProject(projectId)
            projectDao.deleteProject(projectId)
        }
        Unit
    }

    suspend fun buildFileTree(rootPath: String, expandedPaths: Set<String>): List<FileNode> = withContext(Dispatchers.IO) {
        val root = File(rootPath)
        if (!root.exists()) return@withContext emptyList()
        val result = mutableListOf<FileNode>()
        walkTree(root, root, 0, expandedPaths, result)
        result
    }

    private fun walkTree(root: File, currentDir: File, depth: Int, expandedPaths: Set<String>, out: MutableList<FileNode>) {
        val children = currentDir.listFiles()
            ?.filter { it.name != ".git" }
            ?.sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
            ?: return

        for (child in children) {
            val rel = child.relativeTo(root).path.replace(File.separatorChar, '/')
            val isExpanded = child.isDirectory && expandedPaths.contains(child.absolutePath)
            out += FileNode(
                path = child.absolutePath,
                relativePath = rel,
                name = child.name,
                isDirectory = child.isDirectory,
                sizeBytes = if (child.isFile) child.length() else 0L,
                lastModified = child.lastModified(),
                depth = depth,
                isExpanded = isExpanded
            )
            if (isExpanded) walkTree(root, child, depth + 1, expandedPaths, out)
        }
    }

    suspend fun listAllProjectFiles(rootPath: String): List<FileNode> = withContext(Dispatchers.IO) {
        val root = File(rootPath)
        if (!root.exists()) return@withContext emptyList()
        root.walkTopDown()
            .onEnter { it.name != ".git" && it.name != "node_modules" && it.name != "build" }
            .filter { it.isFile }
            .sortedBy { it.relativeTo(root).path }
            .map { file ->
                FileNode(
                    path = file.absolutePath,
                    relativePath = file.relativeTo(root).path.replace(File.separatorChar, '/'),
                    name = file.name,
                    isDirectory = false,
                    sizeBytes = file.length(),
                    lastModified = file.lastModified(),
                    depth = 0
                )
            }.toList()
    }

    suspend fun readFile(path: String): String = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists() || !file.isFile) return@withContext ""
        if (file.length() > 2 * 1024 * 1024L) return@withContext "// File exceeds 2 MB limit"
        file.readText()
    }

    suspend fun writeFile(path: String, content: String) = withContext(Dispatchers.IO) {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    suspend fun createFile(parentDirPath: String, fileName: String): File = withContext(Dispatchers.IO) {
        val cleanName = fileName.trim().trimStart('/')
        val target = File(parentDirPath, cleanName)
        target.parentFile?.mkdirs()
        if (!target.exists()) target.createNewFile()
        target
    }

    suspend fun createFolder(parentDirPath: String, folderName: String): File = withContext(Dispatchers.IO) {
        val cleanName = folderName.trim().trimStart('/')
        val target = File(parentDirPath, cleanName)
        target.mkdirs()
        target
    }

    suspend fun renameNode(path: String, newName: String): File? = withContext(Dispatchers.IO) {
        val source = File(path)
        if (!source.exists()) return@withContext null
        val dest = File(source.parentFile, newName.trim())
        if (source.renameTo(dest)) dest else null
    }

    suspend fun deleteNode(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).deleteRecursively()
    }

    suspend fun copyOrMoveNode(sourcePath: String, targetDirPath: String, move: Boolean): File? = withContext(Dispatchers.IO) {
        val source = File(sourcePath)
        val targetDir = File(targetDirPath)
        if (!source.exists() || !targetDir.isDirectory) return@withContext null
        var dest = File(targetDir, source.name)
        if (dest.absolutePath == source.absolutePath && !move) {
            val base = source.nameWithoutExtension
            val ext = if (source.extension.isNotEmpty()) ".${source.extension}" else ""
            dest = File(targetDir, "${base}_copy$ext")
        }
        if (move) {
            if (source.renameTo(dest)) dest else null
        } else {
            source.copyRecursively(dest, overwrite = true)
            dest
        }
    }

    suspend fun importFileFromUri(uri: Uri, targetDirPath: String): File = withContext(Dispatchers.IO) {
        val displayName = resolveDisplayName(uri) ?: "imported_${System.currentTimeMillis()}.txt"
        val dest = File(targetDirPath, displayName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Cannot read selected file")
        dest
    }

    suspend fun exportFileToUri(sourcePath: String, destUri: Uri) = withContext(Dispatchers.IO) {
        val source = File(sourcePath)
        context.contentResolver.openOutputStream(destUri)?.use { output ->
            FileInputStream(source).use { input -> input.copyTo(output) }
        } ?: throw IllegalStateException("Cannot write to destination URI")
    }

    suspend fun exportProjectAsZip(rootPath: String, destUri: Uri) = withContext(Dispatchers.IO) {
        val root = File(rootPath)
        context.contentResolver.openOutputStream(destUri)?.use { rawOut ->
            ZipOutputStream(rawOut).use { zip ->
                root.walkTopDown().filter { it.isFile }.forEach { file ->
                    val entryName = file.relativeTo(root).path.replace(File.separatorChar, '/')
                    zip.putNextEntry(ZipEntry(entryName))
                    FileInputStream(file).use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } ?: throw IllegalStateException("Cannot write ZIP archive")
    }

    suspend fun searchInProject(rootPath: String, query: String, caseSensitive: Boolean, useRegex: Boolean): List<SearchMatch> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            val root = File(rootPath)
            if (!root.exists()) return@withContext emptyList()
            val regex = if (useRegex) {
                runCatching { Regex(query, if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)) }.getOrNull()
                    ?: return@withContext emptyList()
            } else null

            val matches = mutableListOf<SearchMatch>()
            root.walkTopDown()
                .onEnter { it.name != ".git" && it.name != "node_modules" && it.name != "build" }
                .filter { it.isFile && it.length() <= 512 * 1024L }
                .forEach { file ->
                    val rel = file.relativeTo(root).path.replace(File.separatorChar, '/')
                    val lines = runCatching { file.readLines() }.getOrNull() ?: return@forEach
                    lines.forEachIndexed { idx, lineText ->
                        val col = if (regex != null) regex.find(lineText)?.range?.first
                        else lineText.indexOf(query, ignoreCase = !caseSensitive).takeIf { it >= 0 }
                        if (col != null) {
                            matches += SearchMatch(file.absolutePath, rel, file.name, idx + 1, col + 1, lineText.trim().take(120))
                        }
                        if (matches.size >= 300) return@withContext matches
                    }
                }
            matches
        }

    suspend fun replaceInProject(rootPath: String, query: String, replacement: String, caseSensitive: Boolean, useRegex: Boolean): Int =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext 0
            val root = File(rootPath)
            var modifiedCount = 0
            val regex = if (useRegex) {
                runCatching { Regex(query, if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)) }.getOrNull()
                    ?: return@withContext 0
            } else null

            root.walkTopDown()
                .onEnter { it.name != ".git" && it.name != "node_modules" }
                .filter { it.isFile && it.length() <= 512 * 1024L }
                .forEach { file ->
                    val original = runCatching { file.readText() }.getOrNull() ?: return@forEach
                    val updated = if (regex != null) regex.replace(original, replacement)
                    else original.replace(query, replacement, ignoreCase = !caseSensitive)
                    if (updated != original) {
                        file.writeText(updated)
                        modifiedCount++
                    }
                }
            modifiedCount
        }

    suspend fun getRecentTabs(projectId: String): List<RecentTabEntity> =
        withContext(Dispatchers.IO) { projectDao.getRecentTabsForProject(projectId) }

    suspend fun saveRecentTab(projectId: String, filePath: String, line: Int, col: Int) = withContext(Dispatchers.IO) {
        projectDao.upsertRecentTab(RecentTabEntity(filePath, projectId, System.currentTimeMillis(), line, col))
    }

    suspend fun removeRecentTab(filePath: String): Unit = withContext(Dispatchers.IO) {
        projectDao.deleteTab(filePath)
        Unit
    }

    private fun resolveDisplayName(uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return cursor.getString(idx)
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun ProjectEntity.toDomain() = VCodeProject(id, name, rootPath, templateId, lastOpenedAt, linkedTreeUri)
}
