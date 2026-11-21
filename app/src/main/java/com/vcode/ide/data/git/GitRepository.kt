package com.vcode.ide.data.git

import com.vcode.ide.domain.model.GitCommitInfo
import com.vcode.ide.domain.model.GitFileStatus
import com.vcode.ide.domain.model.GitState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.treewalk.filter.PathFilter
import java.io.ByteArrayOutputStream
import java.io.File

class GitRepository {
    suspend fun inspect(projectRoot: String): GitState = withContext(Dispatchers.IO) {
        val dir = File(projectRoot)
        if (!File(dir, ".git").exists()) return@withContext GitState(isInitialized = false)
        runCatching {
            Git.open(dir).use { git ->
                val repo = git.repository
                val branch = repo.branch ?: "main"
                val branches = git.branchList().call().map { it.name.removePrefix("refs/heads/") }
                val status = git.status().call()
                val changes = mutableListOf<GitFileStatus>()
                status.added.forEach { changes += GitFileStatus(it, "Added", true) }
                status.changed.forEach { changes += GitFileStatus(it, "Staged", true) }
                status.removed.forEach { changes += GitFileStatus(it, "Deleted", true) }
                status.modified.forEach { changes += GitFileStatus(it, "Modified", false) }
                status.untracked.forEach { changes += GitFileStatus(it, "Untracked", false) }
                status.missing.forEach { changes += GitFileStatus(it, "Deleted", false) }

                val commits = if (repo.resolve(Constants.HEAD) != null) {
                    git.log().setMaxCount(25).call().map { rev ->
                        GitCommitInfo(rev.name.take(7), rev.shortMessage, rev.authorIdent?.name ?: "VCode", rev.commitTime * 1000L)
                    }
                } else emptyList()

                GitState(true, branch, branches.ifEmpty { listOf(branch) }, changes.sortedWith(compareBy({ !it.isStaged }, { it.relativePath })), commits)
            }
        }.getOrElse { GitState(isInitialized = false) }
    }

    suspend fun initRepo(projectRoot: String): String = withContext(Dispatchers.IO) {
        Git.init().setDirectory(File(projectRoot)).call().use { "Initialized Git repository in ${it.repository.directory.absolutePath}" }
    }

    suspend fun stageFile(projectRoot: String, relativePath: String) = withContext(Dispatchers.IO) {
        Git.open(File(projectRoot)).use { git ->
            if (File(projectRoot, relativePath).exists()) git.add().addFilepattern(relativePath).call()
            else git.rm().addFilepattern(relativePath).call()
        }
    }

    suspend fun unstageFile(projectRoot: String, relativePath: String) = withContext(Dispatchers.IO) {
        Git.open(File(projectRoot)).use { git ->
            if (git.repository.resolve(Constants.HEAD) != null) git.reset().addPath(relativePath).call()
            else git.rm().setCached(true).addFilepattern(relativePath).call()
        }
    }

    suspend fun stageAll(projectRoot: String) = withContext(Dispatchers.IO) {
        Git.open(File(projectRoot)).use { git ->
            git.add().addFilepattern(".").call()
            git.add().setUpdate(true).addFilepattern(".").call()
        }
    }

    suspend fun commit(projectRoot: String, message: String, authorName: String = "VCode Developer", authorEmail: String = "dev@vcode.local"): String =
        withContext(Dispatchers.IO) {
            Git.open(File(projectRoot)).use { git ->
                val rev = git.commit().setMessage(message.ifBlank { "Update workspace" }).setAuthor(authorName, authorEmail).setCommitter(authorName, authorEmail).call()
                "Committed ${rev.name.take(7)}: ${rev.shortMessage}"
            }
        }

    suspend fun createAndCheckoutBranch(projectRoot: String, branchName: String) = withContext(Dispatchers.IO) {
        val clean = branchName.trim().replace(" ", "-")
        if (clean.isBlank()) return@withContext
        Git.open(File(projectRoot)).use { git -> git.checkout().setCreateBranch(true).setName(clean).call() }
    }

    suspend fun checkoutBranch(projectRoot: String, branchName: String) = withContext(Dispatchers.IO) {
        Git.open(File(projectRoot)).use { git -> git.checkout().setName(branchName).call() }
    }

    suspend fun diffForFile(projectRoot: String, relativePath: String): String = withContext(Dispatchers.IO) {
        val dir = File(projectRoot)
        if (!File(dir, ".git").exists()) return@withContext ""
        runCatching {
            Git.open(dir).use { git ->
                val repo = git.repository
                val headId = repo.resolve(Constants.HEAD) ?: return@withContext run {
                    val file = File(dir, relativePath)
                    if (file.exists()) "+++ $relativePath (Untracked)\n" + file.readText().lines().joinToString("\n") { "+ $it" } else ""
                }
                val out = ByteArrayOutputStream()
                DiffFormatter(out).use { formatter ->
                    formatter.setRepository(repo)
                    formatter.setPathFilter(PathFilter.create(relativePath))
                    val reader = repo.newObjectReader()
                    val headTree = RevWalk(repo).parseCommit(headId).tree
                    val oldTree = CanonicalTreeParser().apply { reset(reader, headTree) }
                    formatter.format(oldTree, FileTreeIterator(repo))
                }
                out.toString(Charsets.UTF_8.name()).ifBlank {
                    val file = File(dir, relativePath)
                    if (file.exists()) "+++ $relativePath (New)\n" + file.readText().lines().take(80).joinToString("\n") { "+ $it" } else "(No diff)"
                }
            }
        }.getOrElse { "(Diff unavailable: ${it.message})" }
    }
}
