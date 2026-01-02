package nictru.nf.anndata

/**
 * Field access integration tests for AnnData class (layers, obsm, obsp, varm, uns)
 * 
 * These tests require the test h5ad file from src/test/data/pbmc3k_processed.h5ad
 */
class AnnDataFieldsTest extends AnnDataTestBase {

    def 'should access layers'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)

        expect:
        ad.layers != null
        'counts' in ad.layers
        !('raw' in ad.layers)

        cleanup:
        ad?.close()
    }

    def 'should access obsm fields'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)

        expect:
        ad.obsm != null
        'X_umap' in ad.obsm
        !('X_scvi' in ad.obsm)

        cleanup:
        ad?.close()
    }

    def 'should access obsp fields'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)

        expect:
        ad.obsp != null
        'distances' in ad.obsp

        cleanup:
        ad?.close()
    }

    def 'should access varm fields'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)

        expect:
        ad.varm != null
        'PCs' in ad.varm

        cleanup:
        ad?.close()
    }

    def 'should access uns fields'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)

        expect:
        ad.uns != null
        'neighbors' in ad.uns

        cleanup:
        ad?.close()
    }
}

