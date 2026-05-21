package models.services;

/**
 * Simple CSV builder utility.
 * Handles quoting, escaping, and newline formatting.
 */
public class CsvBuilder {

    private final StringBuilder sb = new StringBuilder();

    /**
     * Add header row.
     */
    public CsvBuilder header(String... columns) {
        appendRow(columns);
        return this;
    }

    /**
     * Add a data row.
     */
    public CsvBuilder row(String... values) {
        appendRow(values);
        return this;
    }

    /**
     * Build the final CSV string.
     */
    public String build() {
        return sb.toString();
    }

    private void appendRow(String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(values[i]));
        }
        sb.append("\r\n");
    }

    private String escape(String value) {
        if (value == null) return "";
        // Quote if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
