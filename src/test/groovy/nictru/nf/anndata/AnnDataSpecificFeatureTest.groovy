package nictru.nf.anndata

import spock.lang.Unroll

/**
 * Tests for specific features in test files
 * Tests files that are known to have certain features
 */
class AnnDataSpecificFeatureTest extends AnnDataTestBase {

    def 'should handle files with layers'() {
        given:
        def testFile = findTestFile('layers_mixed.h5ad')
        def ad = new AnnData(testFile)
        
        expect:
        ad.layers != null
        ad.layers.size() > 0
        'counts' in ad.layers
        'normalized' in ad.layers
        'log' in ad.layers
        
        cleanup:
        closeAnnData(ad)
    }
    
    def 'should handle files with obsm/varm'() {
        given:
        def testFile = findTestFile('obsm_dense.h5ad')
        def ad = new AnnData(testFile)
        
        expect:
        ad.obsm != null
        ad.obsm.size() > 0
        'X_pca' in ad.obsm
        'X_umap' in ad.obsm
        ad.varm != null
        'PCs' in ad.varm
        
        cleanup:
        closeAnnData(ad)
    }
    
    def 'should handle files with obsp/varp'() {
        given:
        def testFile = findTestFile('obsp_dense.h5ad')
        def ad = new AnnData(testFile)
        
        expect:
        ad.obsp != null
        ad.obsp.size() > 0
        'connectivities' in ad.obsp
        'distances' in ad.obsp
        ad.varp != null
        'correlations' in ad.varp
        
        cleanup:
        closeAnnData(ad)
    }
    
    def 'should handle files with uns'() {
        given:
        def testFile = findTestFile('uns_nested.h5ad')
        def ad = new AnnData(testFile)
        
        expect:
        ad.uns != null
        ad.uns.size() > 0
        'scalar_int' in ad.uns
        'scalar_str' in ad.uns
        'nested' in ad.uns
        
        cleanup:
        closeAnnData(ad)
    }
    
    def 'should handle minimal file (only X)'() {
        given:
        def testFile = findTestFile('edge_minimal.h5ad')
        def ad = new AnnData(testFile)
        
        expect:
        ad.n_obs > 0
        ad.n_vars > 0
        ad.obs != null
        ad.var != null
        // Minimal file may have empty collections
        ad.layers != null
        ad.obsm != null
        ad.varm != null
        
        cleanup:
        closeAnnData(ad)
    }
    
    def 'should handle file with no X matrix'() {
        given:
        def testFile = findTestFile('x_none.h5ad')
        def ad = new AnnData(testFile)
        
        expect:
        ad.n_obs > 0
        ad.n_vars > 0
        ad.obs != null
        ad.var != null
        ad.obs.size == ad.n_obs
        ad.var.size == ad.n_vars
        
        cleanup:
        closeAnnData(ad)
    }
    
    def 'should handle full featured file'() {
        given:
        def testFile = findTestFile('full_featured.h5ad')
        def ad = new AnnData(testFile)
        
        expect:
        ad.n_obs > 0
        ad.n_vars > 0
        ad.obs != null
        ad.var != null
        ad.obs.colnames.length > 0
        ad.var.colnames.length > 0
        ad.layers.size() > 0
        ad.obsm.size() > 0
        ad.varm.size() > 0
        ad.obsp.size() > 0
        ad.varp.size() > 0
        ad.uns.size() > 0
        
        cleanup:
        closeAnnData(ad)
    }
    
    def 'should handle named index correctly'() {
        given:
        def testFile = findTestFile('index_named.h5ad')
        def ad = new AnnData(testFile)
        
        expect:
        // Named index should work - index is named 'cell_id'
        ad.n_obs == 20
        ad.obs_names != null
        ad.obs_names.length == 20
        // First obs name should be 'cell_0'
        ad.obs_names[0] == 'cell_0'
        ad.obs_names[19] == 'cell_19'
        // Index name should be stored in DataFrame
        ad.obs.indexName == 'cell_id'
        
        cleanup:
        closeAnnData(ad)
    }
    
    def 'should handle empty observations file'() {
        given:
        def testFile = findTestFile('edge_empty_obs.h5ad')
        def ad = new AnnData(testFile)
        
        expect:
        ad.n_obs == 0
        ad.n_vars == 10
        ad.obs_names != null
        ad.obs_names.length == 0
        ad.var_names.length == 10
        ad.obs.colnames.length == 0  // No columns in obs
        
        cleanup:
        closeAnnData(ad)
    }
    
    def 'should handle unicode in index and values'() {
        given:
        def testFile = findTestFile('edge_unicode.h5ad')
        def ad = new AnnData(testFile)
        
        expect:
        ad.n_obs == 20
        // Index should contain unicode characters (Greek letters)
        ad.obs_names[0].contains('αβγ')
        // Column values should contain unicode (emoji)
        def descCol = ad.obs.get('description')
        descCol.data[0].toString().contains('🧬')
        
        cleanup:
        closeAnnData(ad)
    }
    
    def 'should handle nullable columns with actual null values'() {
        given:
        def testFile = findTestFile('dtypes_nullable.h5ad')
        def ad = new AnnData(testFile)
        
        when:
        def nullableBool = ad.obs.get('nullable_bool')
        def nullableInt = ad.obs.get('nullable_int')
        
        then:
        nullableBool.data != null
        nullableBool.data.length == 20
        // Should have some null values (where mask is true)
        nullableBool.data.any { it == null }
        // Should have some non-null values
        nullableBool.data.any { it != null }
        
        nullableInt.data != null
        nullableInt.data.length == 20
        // Should have some null values
        nullableInt.data.any { it == null }
        
        cleanup:
        closeAnnData(ad)
    }
    
    def 'should handle integer index stored as strings'() {
        given:
        def testFile = findTestFile('index_integer.h5ad')
        def ad = new AnnData(testFile)
        
        expect:
        ad.n_obs == 20
        ad.obs_names != null
        ad.obs_names.length == 20
        // Integer indices are stored as strings "0", "1", etc.
        ad.obs_names[0] == '0'
        ad.obs_names[1] == '1'
        ad.obs_names[19] == '19'
        
        cleanup:
        closeAnnData(ad)
    }
    
    def 'should handle real-world pbmc3k file'() {
        given:
        def testFile = findTestFile('pbmc3k_processed.h5ad')
        def ad = new AnnData(testFile)
        
        expect:
        // Real-world file with known dimensions
        ad.n_obs == 2638
        ad.n_vars == 1838
        
        // Should have expected columns
        'louvain' in ad.obs.colnames
        'n_genes' in ad.obs.colnames
        
        // Should be able to access categorical column
        def louvain = ad.obs.get('louvain')
        louvain.data != null
        louvain.data.length == 2638
        louvain.n_unique() > 0
        
        // Should have obsm with embeddings
        'X_pca' in ad.obsm
        'X_umap' in ad.obsm
        
        // Should have layers
        'counts' in ad.layers
        
        cleanup:
        closeAnnData(ad)
    }
}
