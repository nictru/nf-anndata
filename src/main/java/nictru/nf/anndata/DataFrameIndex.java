package nictru.nf.anndata;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents the index (row names) of a DataFrame.
 * Provides methods similar to pandas Index for accessing unique values.
 */
public class DataFrameIndex {
    final String[] values;
    final String name;

    public DataFrameIndex(String[] values, String name) {
        this.values = values;
        this.name = name;
    }

    /**
     * Get all index values.
     * 
     * @return Array of index values
     */
    public String[] getValues() {
        return values;
    }

    /**
     * Get the name of the index.
     * 
     * @return Index name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the number of elements in the index.
     * 
     * @return Length of the index
     */
    public int size() {
        return values != null ? values.length : 0;
    }

    /**
     * Get unique values in the index.
     * Similar to pandas Index.unique()
     * 
     * @return Set of unique index values
     */
    public Set<String> unique() {
        if (values == null || values.length == 0) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(values));
    }

    /**
     * Get the number of unique values in the index.
     * Similar to pandas Index.nunique()
     * 
     * @return Number of unique index values
     */
    public int n_unique() {
        return unique().size();
    }

    /**
     * Check if a value exists in the index.
     * 
     * @param value The value to check
     * @return true if the value exists in the index
     */
    public boolean contains(String value) {
        if (values == null) {
            return false;
        }
        for (String v : values) {
            if (v != null && v.equals(value)) {
                return true;
            }
            if (v == null && value == null) {
                return true;
            }
        }
        return false;
    }
}
