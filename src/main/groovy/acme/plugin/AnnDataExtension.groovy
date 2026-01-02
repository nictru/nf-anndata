/*
 * Copyright 2025, Seqera Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package acme.plugin

import groovy.transform.CompileStatic
import java.nio.file.Path
import nextflow.Session
import nextflow.plugin.extension.Function
import nextflow.plugin.extension.PluginExtensionPoint
import nictru.nf.anndata.AnnData

/**
 * Implements custom functions for reading AnnData (.h5ad) files
 * which can be imported by Nextflow scripts.
 */
@CompileStatic
class AnnDataExtension extends PluginExtensionPoint {

    @Override
    protected void init(Session session) {
    }

    /**
     * Load an AnnData object from a file path string.
     *
     * @param path The path to the .h5ad file as a String
     * @return AnnData object with access to obs, var, layers, obsm, varm, etc.
     */
    @Function
    AnnData anndata(String path) {
        return new AnnData(Path.of(path))
    }

    /**
     * Load an AnnData object from a Path object.
     *
     * @param path The path to the .h5ad file as a Path
     * @return AnnData object with access to obs, var, layers, obsm, varm, etc.
     */
    @Function
    AnnData anndata(Path path) {
        return new AnnData(path)
    }

}

