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
}
