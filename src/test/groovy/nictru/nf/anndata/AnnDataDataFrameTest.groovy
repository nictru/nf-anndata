package nictru.nf.anndata

import spock.lang.Unroll

/**
 * Tests for DataFrame functionality across different test files
 */
class AnnDataDataFrameTest extends AnnDataTestBase {

    @Unroll
    def 'should access DataFrameColumn data for #filename'() {
        given:
        def testFile = findTestFile(filename)
        def ad = new AnnData(testFile)
        
        when:
        // Get first column from obs if available
        def obsCol = ad.obs.colnames.length > 0 ? ad.obs.get(ad.obs.colnames[0]) : null
        def varCol = ad.var.colnames.length > 0 ? ad.var.get(ad.var.colnames[0]) : null
        
        then:
        if (obsCol != null) {
            obsCol.data != null
            obsCol.data.length == ad.n_obs
            obsCol.unique != null
            obsCol.n_unique() >= 0
        }
        
        if (varCol != null) {
            varCol.data != null
            varCol.data.length == ad.n_vars
            varCol.unique != null
            varCol.n_unique() >= 0
        }
        
        cleanup:
        closeAnnData(ad)
        
        where:
        filename << findAllTestFiles()
            .findAll { 
                // Only test files that are likely to have columns (skip edge cases)
                def name = it.fileName.toString()
                !name.startsWith('edge_') && name != 'x_none.h5ad'
            }
            .collect { it.fileName.toString() }
    }
    
    def 'should handle categorical columns'() {
        given:
        def testFile = findTestFile('dtypes_categorical.h5ad')
        def ad = new AnnData(testFile)
        
        when:
        def catCol = ad.obs.get('cat_unordered')
        def catOrderedCol = ad.obs.get('cat_ordered')
        
        then:
        catCol != null
        catCol.data != null
        catCol.data.length == ad.n_obs
        catCol.unique != null
        catCol.unique.size() > 0
        
        catOrderedCol != null
        catOrderedCol.data != null
        catOrderedCol.data.length == ad.n_obs
        
        cleanup:
        closeAnnData(ad)
    }
    
    def 'should handle numeric columns'() {
        given:
        def testFile = findTestFile('dtypes_numeric.h5ad')
        def ad = new AnnData(testFile)
        
        when:
        def intCol = ad.obs.get('int32')
        def floatCol = ad.obs.get('float32')
        
        then:
        intCol != null
        intCol.data != null
        intCol.data.length == ad.n_obs
        
        floatCol != null
        floatCol.data != null
        floatCol.data.length == ad.n_obs
        
        cleanup:
        closeAnnData(ad)
    }
    
    def 'should handle boolean columns'() {
        given:
        def testFile = findTestFile('dtypes_boolean.h5ad')
        def ad = new AnnData(testFile)
        
        when:
        def boolCol = ad.obs.get('is_selected')
        
        then:
        boolCol != null
        boolCol.data != null
        boolCol.data.length == ad.n_obs
        boolCol.unique != null
        boolCol.unique.size() <= 2  // Boolean should have at most 2 unique values
        
        cleanup:
        closeAnnData(ad)
    }
    
    def 'should handle string columns'() {
        given:
        def testFile = findTestFile('dtypes_string.h5ad')
        def ad = new AnnData(testFile)
        
        when:
        def strCol = ad.obs.get('sample_id')
        
        then:
        strCol != null
        strCol.data != null
        strCol.data.length == ad.n_obs
        strCol.unique != null
        
        cleanup:
        closeAnnData(ad)
    }
}
