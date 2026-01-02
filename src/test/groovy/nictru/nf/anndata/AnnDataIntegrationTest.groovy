package nictru.nf.anndata

import java.nio.file.Path
import java.nio.file.Paths
import spock.lang.Specification

/**
 * Integration tests for AnnData class using real h5ad files
 * 
 * These tests require the test h5ad file from src/test/data/pbmc3k_processed.h5ad
 * Tests will fail if the file is not available
 */
class AnnDataIntegrationTest extends Specification {

    def setup() {
        def testFile = Paths.get('src/test/data/pbmc3k_processed.h5ad')
        if (!testFile.toFile().exists()) {
            throw new FileNotFoundException("Test file not found: ${testFile.toAbsolutePath()}. Tests require src/test/data/pbmc3k_processed.h5ad")
        }
    }

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

    private Path findTestFile() {
        return Paths.get('src/test/data/pbmc3k_processed.h5ad')
    }
}

