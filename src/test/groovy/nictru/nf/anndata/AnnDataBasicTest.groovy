package nictru.nf.anndata

/**
 * Basic integration tests for AnnData class
 * 
 * These tests require the test h5ad file from src/test/data/pbmc3k_processed.h5ad
 */
class AnnDataBasicTest extends AnnDataTestBase {

    def 'should load valid h5ad file'() {
        given:
        def testFile = findTestFile()

        when:
        def ad = new AnnData(testFile)

        then:
        ad != null
        ad.n_obs > 0
        ad.n_vars > 0
        ad.obs != null
        ad.var != null

        cleanup:
        ad?.close()
    }

    def 'should have correct dimensions'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)

        expect:
        ad.n_obs == ad.obs.size
        ad.n_vars == ad.var.size
        ad.obs_names.length == ad.n_obs
        ad.var_names.length == ad.n_vars

        cleanup:
        ad?.close()
    }

    def 'should have correct toString representation'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)

        when:
        def str = ad.toString()

        then:
        str != null
        str.contains('n_obs × n_vars')
        str.contains(ad.n_obs.toString())
        str.contains(ad.n_vars.toString())

        cleanup:
        ad?.close()
    }
}

