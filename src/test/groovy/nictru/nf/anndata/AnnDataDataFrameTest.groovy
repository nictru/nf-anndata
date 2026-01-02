package nictru.nf.anndata

/**
 * DataFrame integration tests for AnnData class
 * 
 * These tests require the test h5ad file from src/test/data/pbmc3k_processed.h5ad
 */
class AnnDataDataFrameTest extends AnnDataTestBase {

    def 'should have obs and var dataframes with columns'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)

        expect:
        ad.obs.colnames.length > 0
        ad.var.colnames.length > 0
        ad.obs.rownames.length > 0
        ad.var.rownames.length > 0

        cleanup:
        ad?.close()
    }

    def 'should access column data'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)
        def firstCol = ad.obs.colnames[0]

        when:
        def column = ad.obs.get(firstCol)

        then:
        column != null
        column.data != null
        column.data.length == ad.n_obs
        column.unique != null
        column.n_unique() >= 0

        cleanup:
        ad?.close()
    }

    def 'should get unique values from column'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)
        def louvainCol = ad.obs.get('louvain')

        expect:
        louvainCol.unique != null
        louvainCol.unique.size() > 0
        'B cells' in louvainCol.unique
        !('B cell' in louvainCol.unique) // Exact match required

        cleanup:
        ad?.close()
    }

    def 'should calculate n_unique correctly'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)
        def louvainCol = ad.obs.get('louvain')

        expect:
        louvainCol.n_unique() == louvainCol.unique.size()

        cleanup:
        ad?.close()
    }
}

