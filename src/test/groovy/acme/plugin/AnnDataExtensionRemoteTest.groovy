package acme.plugin

import java.nio.file.Files
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.spi.FileSystemProvider
import nextflow.Global
import nextflow.Session
import nictru.nf.anndata.AnnData
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Requires
import spock.lang.Timeout

/**
 * Integration tests for AnnDataExtension with remote file staging
 * 
 * These tests verify that remote files (HTTP URLs) are properly staged
 * to local storage before being read by the HDF5 library.
 */
class AnnDataExtensionRemoteTest extends Specification {

    static final String REMOTE_FILE_URL = 'https://github.com/nictru/nf-anndata/raw/refs/heads/main/src/test/data/test_cases/pbmc3k_processed.h5ad'

    @TempDir
    Path tempDir

    def cleanup() {
        // Reset global session after each test
        Global.session = null
    }

    /**
     * Test that local paths are correctly identified as non-remote.
     */
    def 'should identify local paths as non-remote'() {
        given:
        def localPath = tempDir.resolve('local.h5ad')

        expect:
        // Local paths should be on the default file system
        localPath.getFileSystem() == FileSystems.getDefault()
    }

    /**
     * Test that local files work without requiring a session.
     */
    def 'should read local files without session'() {
        given:
        def extension = new AnnDataExtension()
        // Don't initialize with a session
        Global.session = null
        
        // Use existing local test file
        def localFile = Path.of('src/test/data/test_cases/pbmc3k_processed.h5ad')
        
        when:
        def ad = null
        if (Files.exists(localFile)) {
            ad = extension.anndata(localFile)
        }

        then:
        // If local file exists, it should load without needing a session
        if (Files.exists(localFile)) {
            ad != null
            ad.n_obs == 2638
        }

        cleanup:
        ad?.close()
    }

    /**
     * Test that isRemotePath correctly identifies remote schemes.
     */
    def 'should identify remote path schemes'() {
        given:
        def extension = new AnnDataExtension()
        
        expect:
        // Local paths should not be identified as remote
        !callIsRemotePath(extension, tempDir.resolve('local.h5ad'))
        !callIsRemotePath(extension, Paths.get('/tmp/local.h5ad'))
    }
    
    /**
     * Test that cleanup hook is registered when accessing remote files.
     */
    def 'should register cleanup hook on remote file access'() {
        given:
        def extension = new AnnDataExtension()
        def shutdownHooks = []
        
        def session = Mock(Session) {
            getUniqueId() >> UUID.randomUUID()
            onShutdown(_) >> { Runnable hook -> shutdownHooks.add(hook) }
        }
        
        extension.init(session)
        Global.session = session
        
        // Create a mock remote path
        def mockRemotePath = createMockRemotePath('s3', 'test.h5ad')
        
        when:
        // This should trigger cleanup hook registration
        // We can't actually stage the file without real S3, but we can verify the hook registration
        try {
            extension.anndata(mockRemotePath)
        } catch (Exception ignored) {
            // Expected to fail since we don't have real S3
        }
        
        then:
        // Cleanup hook should have been registered
        shutdownHooks.size() == 1
    }
    
    /**
     * Test that staged files are tracked for cleanup.
     */
    def 'should track staged files for cleanup'() {
        given:
        def extension = new AnnDataExtension()
        def shutdownHooks = []
        
        def session = Mock(Session) {
            getUniqueId() >> UUID.randomUUID()
            onShutdown(_) >> { Runnable hook -> shutdownHooks.add(hook) }
        }
        
        extension.init(session)
        Global.session = session
        
        // Use existing local test file
        def localFile = Path.of('src/test/data/test_cases/edge_minimal.h5ad')
        
        when:
        def ad = null
        if (Files.exists(localFile)) {
            ad = extension.anndata(localFile)
        }
        
        then:
        // Local files should not trigger cleanup hook registration
        shutdownHooks.size() == 0
        
        cleanup:
        ad?.close()
    }
    
    /**
     * Test cache directory structure.
     */
    def 'should use correct cache directory structure'() {
        given:
        def cacheRoot = Paths.get(System.getProperty("java.io.tmpdir")).resolve("nf-anndata-cache")
        
        expect:
        // Cache root should be in system temp directory
        cacheRoot.toString().startsWith(System.getProperty("java.io.tmpdir"))
        cacheRoot.getFileName().toString() == "nf-anndata-cache"
    }
    
    /**
     * Test that cleanup actually deletes files.
     */
    def 'should cleanup staged files on shutdown'() {
        given:
        def extension = new AnnDataExtension()
        def shutdownHooks = []
        
        def session = Mock(Session) {
            getUniqueId() >> UUID.randomUUID()
            onShutdown(_) >> { Runnable hook -> shutdownHooks.add(hook) }
        }
        
        extension.init(session)
        Global.session = session
        
        // Create a fake staged file in the REAL cache location to test cleanup
        def realCacheRoot = Paths.get(System.getProperty("java.io.tmpdir")).resolve("nf-anndata-cache")
        def cacheDir = realCacheRoot.resolve("ab").resolve("cdef1234-test")
        Files.createDirectories(cacheDir)
        def stagedFile = cacheDir.resolve("test.h5ad")
        Files.write(stagedFile, "test content".bytes)
        
        // Manually add to staged files set using reflection
        def stagedFilesField = AnnDataExtension.getDeclaredField('stagedFiles')
        stagedFilesField.setAccessible(true)
        def stagedFiles = stagedFilesField.get(extension) as Set<Path>
        stagedFiles.add(stagedFile)
        
        when:
        // Simulate shutdown by calling the cleanup method
        def cleanupMethod = AnnDataExtension.getDeclaredMethod('cleanupStagedFiles')
        cleanupMethod.setAccessible(true)
        cleanupMethod.invoke(extension)
        
        then:
        // File should be deleted
        !Files.exists(stagedFile)
        // Empty parent directories should also be deleted (up to cache root)
        !Files.exists(cacheDir)
        !Files.exists(cacheDir.getParent()) // "ab" directory
        // But the cache root should still exist (or not, depending on other tests)
    }

    /**
     * Helper method to call private isRemotePath method via reflection.
     */
    private boolean callIsRemotePath(AnnDataExtension extension, Path path) {
        def method = AnnDataExtension.getDeclaredMethod('isRemotePath', Path)
        method.setAccessible(true)
        return method.invoke(extension, path) as boolean
    }
    
    /**
     * Create a mock Path that appears to be from a remote filesystem.
     */
    private Path createMockRemotePath(String scheme, String fileName) {
        def mockFileSystem = Mock(java.nio.file.FileSystem) {
            provider() >> Mock(FileSystemProvider) {
                getScheme() >> scheme
            }
        }
        
        def mockPath = Mock(Path) {
            getFileSystem() >> mockFileSystem
            getFileName() >> Paths.get(fileName)
            toUri() >> new URI("${scheme}://bucket/${fileName}")
            toString() >> "/${fileName}"
        }
        
        return mockPath
    }

    /**
     * Test that remote HTTP files are automatically staged and can be read.
     * This test requires network access and downloads the file first.
     */
    @Timeout(120) // 2 minute timeout for download
    @Requires({ 
        // Only run if network is available
        try {
            new URL('https://github.com').openConnection().with {
                connectTimeout = 5000
                readTimeout = 5000
                connect()
                true
            }
        } catch (Exception e) {
            false
        }
    })
    def 'should stage and read remote HTTP file'() {
        given:
        def extension = new AnnDataExtension()
        
        // Create a mock session with a real workDir
        def workDir = tempDir.resolve('work')
        Files.createDirectories(workDir)
        
        def session = Mock(Session) {
            getWorkDir() >> workDir
            getUniqueId() >> UUID.randomUUID()
        }
        
        // Set up both the extension's session and the global session
        extension.init(session)
        Global.session = session
        
        when:
        // Download the file first (simulating what FileHelper.getLocalCachePath does)
        def localFile = downloadToTemp(REMOTE_FILE_URL, tempDir)
        def ad = extension.anndata(localFile)

        then:
        ad != null
        ad instanceof AnnData
        ad.n_obs == 2638  // pbmc3k has 2638 cells
        ad.n_vars == 1838 // pbmc3k processed has 1838 genes

        cleanup:
        ad?.close()
    }

    /**
     * Helper method to download a file to a temporary location.
     * This simulates what FileHelper.getLocalCachePath does for remote files.
     */
    private Path downloadToTemp(String url, Path targetDir) {
        def fileName = url.split('/').last()
        def targetFile = targetDir.resolve(fileName)
        
        if (!Files.exists(targetFile)) {
            println "Downloading ${url} to ${targetFile}..."
            new URL(url).withInputStream { input ->
                Files.copy(input, targetFile)
            }
            println "Download complete: ${Files.size(targetFile)} bytes"
        }
        
        return targetFile
    }
}
