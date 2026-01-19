/*
 * Copyright 2025, Seqera Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package acme.plugin

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import java.nio.file.Path
import java.nio.file.FileSystems
import nextflow.Global
import nextflow.Session
import nextflow.file.FileHelper
import nextflow.plugin.extension.Function
import nextflow.plugin.extension.PluginExtensionPoint
import nictru.nf.anndata.AnnData

/**
 * Implements custom functions for reading AnnData (.h5ad) files
 * which can be imported by Nextflow scripts.
 */
@Slf4j
@CompileStatic
class AnnDataExtension extends PluginExtensionPoint {

    private static final Set<String> REMOTE_SCHEMES = ['s3', 'gs', 'az', 'http', 'https', 'ftp'] as Set

    private Session session

    @Override
    protected void init(Session session) {
        this.session = session
    }

    /**
     * Check if a path is from a remote file system that doesn't support random access.
     */
    private static boolean isRemotePath(Path path) {
        def fileSystem = path.getFileSystem()
        if (fileSystem == FileSystems.getDefault()) {
            return false
        }
        def scheme = fileSystem.provider().getScheme()
        return REMOTE_SCHEMES.contains(scheme?.toLowerCase())
    }

    /**
     * Stage a remote file to local cache if needed.
     * Uses Nextflow's built-in FileHelper.getLocalCachePath which:
     * - Creates a hash-based cache path in the work directory
     * - Reuses cached files if they already exist
     * - Downloads the file if not cached
     *
     * @param path The path to the file (local or remote)
     * @return A local path to the file (original if local, cached copy if remote)
     */
    private Path stageIfRemote(Path path) {
        if (!isRemotePath(path)) {
            return path
        }

        // Get the session's work directory for caching
        def currentSession = session ?: (Session) Global.getSession()
        if (currentSession == null) {
            throw new IllegalStateException(
                "Cannot stage remote AnnData file: no active Nextflow session.\n" +
                "Remote files (${path.toUri().getScheme()}://) require an active session for staging."
            )
        }

        def workDir = currentSession.workDir
        def stageDir = workDir.resolve("stage-${currentSession.uniqueId}")

        log.debug "Staging remote AnnData file: ${path.toUri()} to cache dir: ${stageDir}"

        // Use Nextflow's built-in caching mechanism
        // This will reuse existing cached files and handle concurrent access
        def localPath = FileHelper.getLocalCachePath(path, stageDir, currentSession.uniqueId)

        log.debug "Remote file staged to: ${localPath}"
        return localPath
    }

    /**
     * Load an AnnData object from a file path string.
     *
     * @param path The path to the .h5ad file as a String
     * @return AnnData object with access to obs, var, layers, obsm, varm, etc.
     */
    @Function
    AnnData anndata(String path) {
        return new AnnData(Path.of(path))
    }

    /**
     * Load an AnnData object from a Path object.
     * Remote files (S3, GCS, Azure, HTTP, etc.) are automatically staged
     * to the local work directory before reading.
     *
     * @param path The path to the .h5ad file as a Path
     * @return AnnData object with access to obs, var, layers, obsm, varm, etc.
     */
    @Function
    AnnData anndata(Path path) {
        def localPath = stageIfRemote(path)
        return new AnnData(localPath)
    }

}

