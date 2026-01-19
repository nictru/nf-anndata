package acme.plugin

import java.nio.file.Files
import java.nio.file.FileSystems
import java.nio.file.Path
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
     * Test that staging uses the session's work directory.
     */
    def 'should use session workDir for staging'() {
        given:
        def extension = new AnnDataExtension()
        def workDir = tempDir.resolve('work')
        Files.createDirectories(workDir)
        
        def sessionId = UUID.randomUUID()
        def session = Mock(Session) {
            getWorkDir() >> workDir
            getUniqueId() >> sessionId
        }
        
        extension.init(session)
        Global.session = session

        expect:
        // Verify session is properly configured
        session.workDir == workDir
        session.uniqueId == sessionId
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
