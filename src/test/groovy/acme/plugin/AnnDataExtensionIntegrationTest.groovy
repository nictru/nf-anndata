package acme.plugin

import java.nio.file.Path
import nextflow.Session
import nictru.nf.anndata.AnnData
import nictru.nf.anndata.AnnDataTestBase

/**
 * Integration tests for AnnDataExtension using real h5ad files
 * 
 * These tests require the test h5ad file from src/test/data/pbmc3k_processed.h5ad
 * Tests will fail if the file is not available
 */
class AnnDataExtensionIntegrationTest extends AnnDataTestBase {

    def 'should load AnnData from String path'() {
        given:
        def extension = new AnnDataExtension()
        extension.init(Mock(Session))
        def testFile = findTestFile()

        when:
        def ad = extension.anndata(testFile.toString())

        then:
        ad != null
        ad instanceof AnnData
        ad.n_obs > 0
        ad.n_vars > 0

        cleanup:
        ad?.close()
    }

    def 'should load AnnData from Path object'() {
        given:
        def extension = new AnnDataExtension()
        extension.init(Mock(Session))
        def testFile = findTestFile()

        when:
        def ad = extension.anndata(testFile)

        then:
        ad != null
        ad instanceof AnnData
        ad.n_obs > 0
        ad.n_vars > 0

        cleanup:
        ad?.close()
    }

    def 'should return same object for String and Path'() {
        given:
        def extension = new AnnDataExtension()
        extension.init(Mock(Session))
        def testFile = findTestFile()

        when:
        def ad1 = extension.anndata(testFile.toString())
        def ad2 = extension.anndata(testFile)

        then:
        ad1.n_obs == ad2.n_obs
        ad1.n_vars == ad2.n_vars
        ad1.obs.colnames == ad2.obs.colnames

        cleanup:
        ad1?.close()
        ad2?.close()
    }
}

