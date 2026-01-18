include { anndata } from 'plugin/nf-anndata'

workflow {
    def testFile = file('src/test/data/test_cases/pbmc3k_processed.h5ad', checkIfExists: true)
    ch_adata = channel.of(testFile).map { file -> anndata(file) }

    // Basic properties
    ch_adata.map { ad -> println "n_obs: ${ad.n_obs}" }

    ch_adata.map { ad -> println "n_vars: ${ad.n_vars}" }

    ch_adata.map { ad -> println "layers: ${ad.layers}" }

    ch_adata.map { ad -> println "obsm: ${ad.obsm}" }

    ch_adata.map { ad -> println "varm: ${ad.varm}" }

    ch_adata.map { ad -> println "obsp: ${ad.obsp}" }

    ch_adata.map { ad -> println "varp: ${ad.varp}" }

    ch_adata.map { ad -> println "uns: ${ad.uns}" }

    ch_adata.map { ad -> println "has_counts: ${ad.layers.contains('counts')}" }

    ch_adata.map { ad -> println "has_not_a_layer: ${ad.layers.contains('not_a_layer')}" }

    // DataFrame column access
    ch_adata.map { ad -> println "obs colnames: ${ad.obs.colnames}" }

    ch_adata.map { ad -> println "obs has louvain column: ${ad.obs.colnames.contains('louvain')}" }

    ch_adata.map { ad -> println "var colnames: ${ad.var.colnames}" }

    ch_adata.map { ad ->
        if (!ad.obs.colnames.contains('louvain')) {
            error 'Column \'louvain\' is missing'
        } else {
            println "Column 'louvain' is present"
        }
    }

    ch_adata.map { ad ->
        if (ad.obs.get('louvain').n_unique() < 2) {
            error 'Column \'louvain\' has less than 2 unique values'
        } else {
            println "Column 'louvain' has ${ad.obs.get('louvain').n_unique()} unique values"
        }
    }

    ch_adata.map { ad ->
        if (!ad.obs.get('louvain').unique().contains('Dendritic cells')) {
            error 'Column \'louvain\' is missing Dendritic cells'
        } else {
            println "Column 'louvain' has Dendritic cells"
        }
    }

    ch_adata.map { ad ->
        if (ad.obs.colnames.size() > 0) {
            def firstCol = ad.obs.colnames[0]
            def col = ad.obs.get(firstCol)
            def n_unique = col.n_unique()
            def unique_vals = col.unique().take(10)
            println "Column '${firstCol}' has ${n_unique} unique values"
            if (unique_vals.size() > 0) {
                println "Unique values (first 10): ${unique_vals.join(', ')}"
            }
        }
    }

    // DataFrame index access
    ch_adata.map { ad ->
        println "obs index name: ${ad.obs.index.name}"
        println "obs index size: ${ad.obs.index.size()}"
        println "obs index n_unique: ${ad.obs.index.n_unique()}"
    }

    ch_adata.map { ad ->
        println "var index name: ${ad.var.index.name}"
        println "var index size: ${ad.var.index.size()}"
        println "var index n_unique: ${ad.var.index.n_unique()}"
    }

    // Check if indices are unique (they should be for well-formed AnnData)
    ch_adata.map { ad ->
        if (ad.obs.index.n_unique() != ad.n_obs) {
            println "WARNING: Observation names are not unique!"
        } else {
            println "All observation names are unique"
        }
    }

    ch_adata.map { ad ->
        if (ad.var.index.n_unique() != ad.n_vars) {
            println "WARNING: Variable names are not unique!"
        } else {
            println "All variable names are unique"
        }
    }

    // Check if specific cell exists
    ch_adata.map { ad ->
        def firstCell = ad.obs_names[0]
        if (ad.obs.index.contains(firstCell)) {
            println "Index contains first cell: ${firstCell}"
        }
    }

    // Get some unique index values
    ch_adata.map { ad ->
        def uniqueObs = ad.obs.index.unique().take(5)
        println "First 5 unique obs names: ${uniqueObs.join(', ')}"
    }
}
