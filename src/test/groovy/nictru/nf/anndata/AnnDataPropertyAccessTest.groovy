package nictru.nf.anndata

import spock.lang.Unroll

/**
 * Tests that all currently supported properties can be accessed for all test files
 */
class AnnDataPropertyAccessTest extends AnnDataTestBase {

    @Unroll
    def 'should access basic properties for #caseName (#backend)'() {
        given:
        def testFile = path as java.nio.file.Path
        def ad = new AnnData(testFile)

        expect:
        ad.n_obs >= 0
        ad.n_vars >= 0
        ad.n_obs == ad.obs.size
        ad.n_vars == ad.var.size
        ad.obs_names != null
        ad.var_names != null
        ad.obs_names.length == ad.n_obs
        ad.var_names.length == ad.n_vars
        ad.obs != null
        ad.var != null
        ad.obs.colnames != null
        ad.var.colnames != null
        ad.obs.rownames != null
        ad.var.rownames != null

        cleanup:
        closeAnnData(ad)

        where:
        fixture << findAllFixtures()
        caseName = fixture.caseName
        backend = fixture.backend
        path = fixture.path
    }

    @Unroll
    def 'should access field collections for #caseName (#backend)'() {
        given:
        def testFile = path as java.nio.file.Path
        def ad = new AnnData(testFile)

        expect:
        ad.layers != null
        ad.obsm != null
        ad.varm != null
        ad.obsp != null
        ad.varp != null
        ad.uns != null
        ad.layers instanceof Set
        ad.obsm instanceof Set
        ad.varm instanceof Set
        ad.obsp instanceof Set
        ad.varp instanceof Set
        ad.uns instanceof Set

        cleanup:
        closeAnnData(ad)

        where:
        fixture << findAllFixtures()
        caseName = fixture.caseName
        backend = fixture.backend
        path = fixture.path
    }

    @Unroll
    def 'should access DataFrame columns for #caseName (#backend)'() {
        given:
        def testFile = path as java.nio.file.Path
        def ad = new AnnData(testFile)

        when:
        def obsColumns = ad.obs.colnames.collect { colName ->
            try {
                def col = ad.obs.get(colName)
                return [name: colName, accessible: true, hasData: col.data != null, dataLength: col.data?.length ?: 0]
            } catch (Exception e) {
                return [name: colName, accessible: false, error: e.message]
            }
        }

        def varColumns = ad.var.colnames.collect { colName ->
            try {
                def col = ad.var.get(colName)
                return [name: colName, accessible: true, hasData: col.data != null, dataLength: col.data?.length ?: 0]
            } catch (Exception e) {
                return [name: colName, accessible: false, error: e.message]
            }
        }

        then:
        obsColumns.every { it.accessible }
        varColumns.every { it.accessible }
        obsColumns.findAll { it.accessible }.every { it.hasData && it.dataLength == ad.n_obs }
        varColumns.findAll { it.accessible }.every { it.hasData && it.dataLength == ad.n_vars }

        cleanup:
        closeAnnData(ad)

        where:
        fixture << findAllFixtures()
            .findAll {
                def name = it.caseName as String
                !name.startsWith('edge_')
            }
        caseName = fixture.caseName
        backend = fixture.backend
        path = fixture.path
    }

    @Unroll
    def 'should have consistent toString representation for #caseName (#backend)'() {
        given:
        def testFile = path as java.nio.file.Path
        def ad = new AnnData(testFile)

        when:
        def str = ad.toString()

        then:
        str != null
        str.contains('n_obs × n_vars')
        str.contains(ad.n_obs.toString())
        str.contains(ad.n_vars.toString())

        cleanup:
        closeAnnData(ad)

        where:
        fixture << findAllFixtures()
        caseName = fixture.caseName
        backend = fixture.backend
        path = fixture.path
    }
}
