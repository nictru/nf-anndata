package nictru.nf.anndata

/**
 * Validation integration tests for AnnData class
 * 
 * These tests require the test h5ad file from src/test/data/pbmc3k_processed.h5ad
 */
class AnnDataValidationTest extends AnnDataTestBase {

    def 'should have expected obs columns'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)
        def expectedCols = ['louvain', 'n_counts', 'n_genes', 'percent_mito']

        expect:
        expectedCols.every { it in ad.obs.colnames }

        cleanup:
        ad?.close()
    }

    def 'should have expected var columns'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)

        expect:
        'n_cells' in ad.var.colnames

        cleanup:
        ad?.close()
    }

    def 'should have expected row names'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)

        expect:
        'TTTCGAACTCTCAT-1' in ad.obs.rownames
        'PCNA' in ad.var.rownames

        cleanup:
        ad?.close()
    }

    def 'should validate column data constraints'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)
        def nGenesCol = ad.obs.get('n_genes')
        def percentMitoCol = ad.obs.get('percent_mito')

        expect:
        nGenesCol.data.every { it >= 100 }
        percentMitoCol.data.every { it < 0.5 }

        cleanup:
        ad?.close()
    }
}

