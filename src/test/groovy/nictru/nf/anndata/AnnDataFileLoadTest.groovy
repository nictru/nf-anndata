package nictru.nf.anndata

import spock.lang.Unroll

/**
 * Tests that all generated fixtures can be successfully opened
 */
class AnnDataFileLoadTest extends AnnDataTestBase {

    @Unroll
    def 'should successfully load #caseName (#backend)'() {
        given:
        def testFile = path as java.nio.file.Path

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
        fixture << findAllFixtures()
        caseName = fixture.caseName
        backend = fixture.backend
        path = fixture.path
    }
}
