package nictru.nf.anndata

import java.nio.file.Path
import spock.lang.Unroll

/**
 * Tests that all generated h5ad files can be successfully opened
 * 
 * This is a parameterized test that runs against all test files
 */
class AnnDataFileLoadTest extends AnnDataTestBase {

    @Unroll
    def 'should successfully load #filename'() {
        given:
        def testFile = findTestFile(filename)
        
        when:
        def ad = new AnnData(testFile)
        
        then:
        ad != null
        ad.n_obs >= 0
        ad.n_vars >= 0
        ad.obs != null
        ad.var != null
        
        cleanup:
        closeAnnData(ad)
        
        where:
        filename << findAllTestFiles().collect { it.fileName.toString() }
    }
}
