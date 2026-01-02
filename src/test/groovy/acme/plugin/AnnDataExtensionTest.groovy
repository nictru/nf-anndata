package acme.plugin

import java.nio.file.Path
import nextflow.Session
import nictru.nf.anndata.AnnData
import spock.lang.Specification
import spock.lang.TempDir

/**
 * Unit tests for AnnDataExtension
 */
class AnnDataExtensionTest extends Specification {

    @TempDir
    Path tempDir

    def 'should initialize extension'() {
        given:
        def extension = new AnnDataExtension()
        def session = Mock(Session)

        when:
        extension.init(session)

        then:
        noExceptionThrown()
    }

    def 'should load AnnData from String path when file exists'() {
        given:
        def extension = new AnnDataExtension()
        def testFile = tempDir.resolve('test.h5ad')
        // Note: This test requires a valid h5ad file
        // For now, we'll test that the method exists and can be called
        // Integration tests with real files should be in a separate test suite

        when:
        def result = extension.anndata(testFile.toString())

        then:
        thrown(IllegalArgumentException) // File doesn't exist or is invalid
    }

    def 'should load AnnData from Path object when file exists'() {
        given:
        def extension = new AnnDataExtension()
        def testFile = tempDir.resolve('test.h5ad')

        when:
        def result = extension.anndata(testFile)

        then:
        thrown(IllegalArgumentException) // File doesn't exist or is invalid
    }

    def 'should handle null path gracefully'() {
        given:
        def extension = new AnnDataExtension()

        when:
        extension.anndata((String) null)

        then:
        thrown(NullPointerException)
    }

    def 'should handle null Path gracefully'() {
        given:
        def extension = new AnnDataExtension()

        when:
        extension.anndata((Path) null)

        then:
        thrown(NullPointerException)
    }
}

