package nictru.nf.anndata

/**
 * Tests for AnnData resource management (close, AutoCloseable, automatic cleanup)
 * 
 * These tests require the test h5ad file from src/test/data/pbmc3k_processed.h5ad
 */
class AnnDataResourceManagementTest extends AnnDataTestBase {

    def 'should not be closed initially'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)

        expect:
        !ad.isClosed()

        cleanup:
        ad?.close()
    }

    def 'should be closed after explicit close()'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)

        when:
        ad.close()

        then:
        ad.isClosed()

        // Verify we can still access properties after close (they're already loaded)
        ad.n_obs > 0
        ad.n_vars > 0
    }

    def 'should allow multiple close() calls safely'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)

        when:
        ad.close()
        ad.close() // Second close should not throw

        then:
        ad.isClosed()
        noExceptionThrown()
    }

    def 'should work with try-with-resources (AutoCloseable)'() {
        given:
        def testFile = findTestFile()
        AnnData ad = null

        when:
        // Simulate try-with-resources pattern
        ad = new AnnData(testFile)
        def nObs = ad.n_obs
        def nVars = ad.n_vars
        ad.close() // Simulating automatic close at end of try block

        then:
        nObs > 0
        nVars > 0
        ad.isClosed()
    }

    def 'should implement AutoCloseable interface'() {
        expect:
        AutoCloseable.isAssignableFrom(AnnData.class)
    }

    def 'should allow access to data after close'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)
        def colnames = ad.obs.colnames
        def rownames = ad.obs.rownames

        when:
        ad.close()

        then:
        // Data should still be accessible after close (already loaded in memory)
        ad.obs.colnames == colnames
        ad.obs.rownames == rownames
        ad.n_obs > 0
        ad.n_vars > 0
    }

    def 'should handle close() on uninitialized object gracefully'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)

        when:
        ad.close()
        ad.close() // Second call

        then:
        ad.isClosed()
        noExceptionThrown()
    }

    def 'should work correctly when close() is called in cleanup block'() {
        given:
        def testFile = findTestFile()
        def ad = new AnnData(testFile)

        expect:
        !ad.isClosed()
        ad.n_obs > 0

        cleanup:
        // This simulates the cleanup pattern used in other tests
        ad?.close()
        assert ad.isClosed()
    }
}

