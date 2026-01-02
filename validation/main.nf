include { anndata } from 'plugin/nf-anndata'

workflow {
    // Example: Load an AnnData file and access its properties
    // This example demonstrates the plugin functionality using the test data file

    def testFile = file('src/test/data/pbmc3k_processed.h5ad', checkIfExists: true)

    ch_adata = Channel.of(testFile).map { file -> anndata(file) }

    ch_adata.view()

    // Show all the main properties of the AnnData object individually
    ch_adata.map { ad -> ad.n_obs }
        .map { n_obs ->
            println "n_obs: ${n_obs}"
        }

    ch_adata.map { ad -> ad.n_vars }
        .map { n_vars ->
            println "n_vars: ${n_vars}"
        }

    ch_adata.map { ad -> ad.layers }
        .map { layers ->
            println "layers: ${layers}"
        }

    ch_adata.map { ad -> ad.obsm }
        .map { obsm ->
            println "obsm: ${obsm}"
        }

    ch_adata.map { ad -> ad.varm }
        .map { varm ->
            println "varm: ${varm}"
        }

    ch_adata.map { ad -> ad.obsp }
        .map { obsp ->
            println "obsp: ${obsp}"
        }

    ch_adata.map { ad -> ad.varp }
        .map { varp ->
            println "varp: ${varp}"
        }

    ch_adata.map { ad -> ad.uns }
        .map { uns ->
            println "uns: ${uns}"
        }

    // Optionally show ad.obs and ad.var -- e.g., show column names
    ch_adata.map { ad -> ad.obs.colnames }
        .map { obs_colnames ->
            println "obs colnames: ${obs_colnames}"
        }

    ch_adata.map { ad -> ad.var.colnames }
        .map { var_colnames ->
            println "var colnames: ${var_colnames}"
        }

    // Example: Show unique information about the first obs column if available
    ch_adata.map { ad ->
        if (ad.obs.colnames.size() > 0) {
            def firstCol = ad.obs.colnames[0]
            def col = ad.obs.get(firstCol)
            def n_unique = col.n_unique()
            def unique_vals = col.unique.take(10)
            println "Column '${firstCol}' has ${n_unique} unique values"
            if (unique_vals.size() > 0) {
                println "Unique values (first 10): ${unique_vals.join(', ')}"
            }
        }
    }
}
