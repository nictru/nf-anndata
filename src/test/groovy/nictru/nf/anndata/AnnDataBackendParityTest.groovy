package nictru.nf.anndata

import spock.lang.Unroll

/**
 * Cross-backend parity tests for h5ad and zarr fixtures.
 */
class AnnDataBackendParityTest extends AnnDataTestBase {

    private static final List<String> PARITY_CASES = [
        'pbmc3k_processed',
        'dtypes_categorical',
        'dtypes_numeric',
        'dtypes_nullable',
        'dtypes_nullable_string',
        'index_categorical',
        'layers_mixed',
        'obsm_dense',
        'obsp_dense',
        'uns_nested',
        'full_featured',
        'edge_minimal',
        'edge_empty_obs',
        'edge_unicode',
        'index_named',
        'index_integer',
    ]

    @Unroll
    def 'h5ad and zarr should produce equivalent structure for #caseName'() {
        given:
        def h5ad = new AnnData(findFixture(caseName, 'h5ad'))
        def zarr = new AnnData(findFixture(caseName, 'zarr'))

        expect:
        h5ad.n_obs == zarr.n_obs
        h5ad.n_vars == zarr.n_vars
        h5ad.obs.colnames as List == zarr.obs.colnames as List
        h5ad.var.colnames as List == zarr.var.colnames as List
        h5ad.obs_names as List == zarr.obs_names as List
        h5ad.var_names as List == zarr.var_names as List
        h5ad.layers == zarr.layers
        h5ad.obsm == zarr.obsm
        h5ad.varm == zarr.varm
        h5ad.obsp == zarr.obsp
        h5ad.varp == zarr.varp
        h5ad.uns == zarr.uns
        h5ad.yaml == zarr.yaml

        cleanup:
        closeAnnData(h5ad)
        closeAnnData(zarr)

        where:
        caseName << PARITY_CASES
    }
}
