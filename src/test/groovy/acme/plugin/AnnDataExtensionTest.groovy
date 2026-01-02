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

    def 'should throw exception for non-existent String path'() {
        given:
        def extension = new AnnDataExtension()
        extension.init(Mock(Session))
        def nonExistentFile = tempDir.resolve('nonexistent.h5ad')

        when:
        extension.anndata(nonExistentFile.toString())

        then:
        thrown(Exception) // FileNotFoundException or similar
    }

    def 'should throw exception for non-existent Path'() {
        given:
        def extension = new AnnDataExtension()
        extension.init(Mock(Session))
        def nonExistentFile = tempDir.resolve('nonexistent.h5ad')

        when:
        extension.anndata(nonExistentFile)

        then:
        thrown(Exception) // FileNotFoundException or similar
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

