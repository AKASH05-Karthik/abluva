/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.graalvm.polyglot.Value
 *  org.graalvm.polyglot.proxy.ProxyObject
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;
import com.galliumdata.server.handler.mssql.tokens.TokenColMetadata;
import com.galliumdata.server.handler.mssql.tokens.TokenError;
import com.galliumdata.server.handler.mssql.tokens.TokenRow;
import com.galliumdata.server.util.StringUtil;
import java.util.Arrays;
import java.util.List;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public class MSSQLResultSet
implements ProxyObject {
    private TokenColMetadata meta;
    private List<TokenRow> rows;
    private TokenError error;
    private static final int MAX_COL_WIDTH = 40;

    public MSSQLResultSet(TokenColMetadata meta, List<TokenRow> rows) {
        this.meta = meta;
        this.rows = rows;
    }

    public MSSQLResultSet(TokenError error) {
        this.error = error;
    }

    public TokenColMetadata getMetaData() {
        return this.meta;
    }

    public List<TokenRow> getRows() {
        return this.rows;
    }

    public TokenError getError() {
        return this.error;
    }

    public String toString() {
        int i;
        if (this.error != null) {
            return "Error: " + this.error.getMessage();
        }
        StringBuilder sb = new StringBuilder();
        List<ColumnMetadata> cols = this.meta.getColumns();
        int[] colWidths = new int[cols.size()];
        for (int i2 = 0; i2 < cols.size(); ++i2) {
            String colName = cols.get(i2).getColumnName();
            if (colName == null || colName.isBlank()) {
                colName = "<empty>";
            }
            colWidths[i2] = colName.length() > 40 ? 40 : colName.length();
        }
        for (TokenRow row : this.rows) {
            for (i = 0; i < cols.size(); ++i) {
                int valueWidth;
                ColumnMetadata colMeta = this.meta.getColumns().get(i);
                Object value = row.getValue(colMeta.getColumnName());
                if (value == null) {
                    valueWidth = "<null>".length();
                } else {
                    valueWidth = value.toString().length();
                    if (valueWidth > 40) {
                        valueWidth = 40;
                    }
                }
                if (colWidths[i] >= valueWidth) continue;
                colWidths[i] = valueWidth;
            }
        }
        int tableWidth = 1 + Arrays.stream(colWidths).sum() + cols.size() * 3;
        String dashRow = "-".repeat(tableWidth);
        sb.append(dashRow);
        sb.append("\n");
        sb.append("|");
        for (i = 0; i < cols.size(); ++i) {
            String colName = cols.get(i).getColumnName();
            if (colName == null || colName.isBlank()) {
                colName = "<empty>";
            }
            if (colName.length() > 40) {
                colName = StringUtil.getShortenedString(colName, 40);
            }
            sb.append(" ");
            sb.append(colName);
            int padLen = colWidths[i] - colName.length();
            if (padLen > 0) {
                sb.append(" ".repeat(padLen));
            }
            sb.append(" |");
        }
        sb.append("\n");
        sb.append(dashRow);
        sb.append("\n");
        for (TokenRow row : this.rows) {
            sb.append("|");
            for (int i3 = 0; i3 < cols.size(); ++i3) {
                String valueStr;
                String colName = cols.get(i3).getColumnName();
                Object value = row.getValue(colName);
                if (value == null) {
                    valueStr = "<null>";
                } else {
                    valueStr = value.toString();
                    if (valueStr.length() > 40) {
                        valueStr = StringUtil.getShortenedString(valueStr, 40);
                    }
                }
                sb.append(" ");
                sb.append(valueStr);
                int padLen = colWidths[i3] - valueStr.length();
                if (padLen > 0) {
                    sb.append(" ".repeat(padLen));
                }
                sb.append(" |");
            }
            sb.append("\n");
        }
        sb.append(dashRow);
        sb.append("\n");
        return sb.toString();
    }

    public Object getMember(String key) {
        switch (key) {
            case "metadata": {
                return this.meta;
            }
            case "rows": {
                return this.rows;
            }
            case "error": {
                return this.error;
            }
            case "toString": {
                return arguments -> this.toString();
            }
        }
        throw new ServerException("db.mssql.logic.NoSuchMember", key);
    }

    public Object getMemberKeys() {
        return new String[]{"metadata", "rows", "error", "toString"};
    }

    public boolean hasMember(String key) {
        switch (key) {
            case "metadata": 
            case "rows": 
            case "error": 
            case "toString": {
                return true;
            }
        }
        return false;
    }

    public void putMember(String key, Value value) {
        throw new ServerException("db.mssql.logic.NoSuchMember", key);
    }

    public boolean removeMember(String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", key, "Token token");
    }
}
