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
            obsCol.unique() != null
            obsCol.n_unique() >= 0
        }
        
        if (varCol != null) {
            varCol.data != null
            varCol.data.length == ad.n_vars
            varCol.unique() != null
            varCol.n_unique() >= 0
        }
        
        cleanup:
        closeAnnData(ad)
        
        where:
        filename << findAllTestFiles()
            .findAll { 
                // Only test files that are likely to have columns (skip edge cases with no columns)
                def name = it.fileName.toString()
                !name.startsWith('edge_')
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
        catCol.unique() != null
        catCol.unique().size() > 0
        
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
        def int8Col = ad.obs.get('int8')
        def int16Col = ad.obs.get('int16')
        def int32Col = ad.obs.get('int32')
        def int64Col = ad.obs.get('int64')
        def uint8Col = ad.obs.get('uint8')
        def float32Col = ad.obs.get('float32')
        def float64Col = ad.obs.get('float64')
        
        then:
        // All columns should be accessible and have correct length
        int8Col.data.length == ad.n_obs
        int16Col.data.length == ad.n_obs
        int32Col.data.length == ad.n_obs
        int64Col.data.length == ad.n_obs
        uint8Col.data.length == ad.n_obs
        float32Col.data.length == ad.n_obs
        float64Col.data.length == ad.n_obs
        
        // Values should be numeric (not null for non-nullable types)
        int32Col.data.every { it != null }
        float32Col.data.every { it != null }
        
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
        boolCol.unique() != null
        boolCol.unique().size() <= 2  // Boolean should have at most 2 unique values
        
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
        strCol.unique() != null
        
        cleanup:
        closeAnnData(ad)
    }

    def 'should get unique index values from DataFrame'() {
        given:
        def testFile = findTestFile('full_featured.h5ad')
        def ad = new AnnData(testFile)
        
        when:
        def obsUniqueIndex = ad.obs.index.unique()
        def varUniqueIndex = ad.var.index.unique()
        def obsNUnique = ad.obs.index.n_unique()
        def varNUnique = ad.var.index.n_unique()
        
        then:
        // Unique index should be a Set
        obsUniqueIndex instanceof Set
        varUniqueIndex instanceof Set
        
        // n_unique should match size of unique set
        obsNUnique == obsUniqueIndex.size()
        varNUnique == varUniqueIndex.size()
        
        // For a well-formed AnnData, indices should be unique (n_unique == n_obs/n_vars)
        obsNUnique == ad.n_obs
        varNUnique == ad.n_vars
        
        // The unique values should contain the same elements as obs_names/var_names
        obsUniqueIndex.containsAll(ad.obs_names as List)
        varUniqueIndex.containsAll(ad.var_names as List)
        
        cleanup:
        closeAnnData(ad)
    }

    def 'should handle empty DataFrame index unique'() {
        given:
        def testFile = findTestFile('edge_empty_obs.h5ad')
        def ad = new AnnData(testFile)
        
        when:
        def obsUniqueIndex = ad.obs.index.unique()
        def obsNUnique = ad.obs.index.n_unique()
        
        then:
        // Empty obs should have empty unique set
        obsUniqueIndex.isEmpty()
        obsNUnique == 0
        
        cleanup:
        closeAnnData(ad)
    }

    def 'should access index properties'() {
        given:
        def testFile = findTestFile('full_featured.h5ad')
        def ad = new AnnData(testFile)
        
        when:
        def obsIndex = ad.obs.index
        def varIndex = ad.var.index
        
        then:
        // Index should have values
        obsIndex.values != null
        obsIndex.values.length == ad.n_obs
        varIndex.values != null
        varIndex.values.length == ad.n_vars
        
        // Index should have size
        obsIndex.size() == ad.n_obs
        varIndex.size() == ad.n_vars
        
        // Index should have name
        obsIndex.name != null
        varIndex.name != null
        
        // Contains should work
        obsIndex.contains(ad.obs_names[0])
        !obsIndex.contains('nonexistent_cell_xyz')
        
        cleanup:
        closeAnnData(ad)
    }
}
