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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.FileSystems
import java.util.concurrent.ConcurrentHashMap
import nextflow.Global
import nextflow.Session
import nextflow.extension.FilesEx
import nextflow.util.CacheHelper
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
    
    // Lock objects for synchronized file downloads (avoids String.intern() memory leak)
    private static final ConcurrentHashMap<String, Object> downloadLocks = new ConcurrentHashMap<>()

    private Session session
    
    // Track staged files for cleanup on shutdown
    private final Set<Path> stagedFiles = Collections.synchronizedSet(new HashSet<Path>())
    private volatile boolean cleanupRegistered = false

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
     * Register a shutdown hook to clean up staged files when the session ends.
     */
    private void registerCleanupHook() {
        if (cleanupRegistered) {
            return
        }
        
        synchronized (this) {
            if (cleanupRegistered) {
                return
            }
            
            def currentSession = session ?: (Session) Global.getSession()
            if (currentSession != null) {
                currentSession.onShutdown {
                    cleanupStagedFiles()
                }
                cleanupRegistered = true
                log.debug "Registered AnnData cache cleanup hook"
            }
        }
    }
    
    /**
     * Clean up all staged files and their parent directories.
     */
    private void cleanupStagedFiles() {
        if (stagedFiles.isEmpty()) {
            return
        }
        
        // Create a copy to avoid ConcurrentModificationException
        def filesToClean = new ArrayList<Path>(stagedFiles)
        stagedFiles.clear()
        
        log.debug "Cleaning up ${filesToClean.size()} staged AnnData file(s)"
        
        def cacheRoot = Paths.get(System.getProperty("java.io.tmpdir")).resolve("nf-anndata-cache")
        
        for (Path file : filesToClean) {
            try {
                if (Files.exists(file)) {
                    Files.delete(file)
                    log.trace "Deleted staged file: ${file}"
                    
                    // Try to delete empty parent directories up to the cache root
                    def parent = file.getParent()
                    while (parent != null && parent.startsWith(cacheRoot) && parent != cacheRoot) {
                        try {
                            Files.delete(parent)  // Only succeeds if directory is empty
                            log.trace "Deleted empty cache directory: ${parent}"
                            parent = parent.getParent()
                        } catch (Exception ignored) {
                            break  // Directory not empty or other error
                        }
                    }
                }
            } catch (Exception e) {
                log.debug "Failed to delete staged file ${file}: ${e.message}"
            }
        }
    }

    /**
     * Stage a remote file to local cache if needed.
     * Downloads the file to a local temporary directory to enable random access
     * required by the HDF5 library.
     * 
     * Staged files are automatically cleaned up when the Nextflow session ends.
     *
     * @param path The path to the file (local or remote)
     * @return A local path to the file (original if local, cached copy if remote)
     */
    private Path stageIfRemote(Path path) {
        if (!isRemotePath(path)) {
            return path
        }
        
        // Register cleanup hook on first remote file access
        registerCleanupHook()

        // Create a hash based only on the source path for cross-session caching
        def hash = CacheHelper.hasher(path).hash()
        def hashStr = hash.toString()
        
        // Use local temp directory with hash-based subdirectory structure
        // Structure: /tmp/nf-anndata-cache/<2-char-bucket>/<rest-of-hash>/<filename>
        def localTempDir = Paths.get(System.getProperty("java.io.tmpdir"))
        def cacheDir = localTempDir.resolve("nf-anndata-cache")
                .resolve(hashStr.substring(0, 2))
                .resolve(hashStr.substring(2))
        
        // Get filename safely (handle edge case of root paths)
        def fileName = path.getFileName()
        if (fileName == null) {
            throw new IllegalArgumentException("Cannot stage remote path without filename: ${path}")
        }
        def cachedFile = cacheDir.resolve(fileName.toString())

        // Check if already cached (from this or previous session)
        if (Files.exists(cachedFile)) {
            log.debug "Using cached AnnData file: ${cachedFile}"
            // Track for cleanup even if it existed before
            stagedFiles.add(cachedFile)
            return cachedFile
        }

        // Get or create a lock object for this cache directory (avoids String.intern() memory leak)
        def lockKey = cacheDir.toString()
        def lock = downloadLocks.computeIfAbsent(lockKey, { new Object() })
        
        // Download to cache with synchronization to handle concurrent access
        synchronized (lock) {
            // Double-check after acquiring lock
            if (Files.exists(cachedFile)) {
                log.debug "Using cached AnnData file: ${cachedFile}"
                stagedFiles.add(cachedFile)
                return cachedFile
            }

            log.debug "Staging remote AnnData file: ${path.toUri()} to local cache: ${cachedFile}"
            
            // Create parent directories
            Files.createDirectories(cacheDir)
            
            // Use FilesEx.copyTo which leverages efficient provider-specific download
            // For S3, this uses TransferManager which streams directly to disk
            FilesEx.copyTo(path, cachedFile)
            
            // Track for cleanup
            stagedFiles.add(cachedFile)
            
            log.debug "Remote file staged to: ${cachedFile}"
        }
        
        // Clean up lock object if no longer needed (optional, prevents unbounded growth)
        downloadLocks.remove(lockKey, lock)

        return cachedFile
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
     * to a local temporary directory before reading.
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

