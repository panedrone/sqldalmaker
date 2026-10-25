package com.sqldalmaker.cg;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

// Design-time, NOT a part of the plug-ins (like HarnessMain): dumps IDbMetadata of JdbcDbMetadata
// as text lines to compare with go-sdm/cli/metadump (the same format), e.g. on go-sdm/cli/metadump/my_tests_pg.txt.
// It is in package com.sqldalmaker.cg because JdbcDbMetadata is package-private.
// args: <jdbc-url> <user> <inputs-file>; password from env SDM_PWD
// inputs-file lines: "exec<TAB><sql>" (setup, prints nothing), "sql<TAB><jdbc sql>", "params<TAB><n><TAB><jdbc sql>", "table<TAB><name>", "pk<TAB><name>"
public class MetaDump {

    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection(args[0], args[1], System.getenv("SDM_PWD"));
        IDbMetadata md = new JdbcDbMetadata(conn);
        try (BufferedReader r = new BufferedReader(new FileReader(args[2]))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] p = line.split("\t");
                if ("exec".equals(p[0])) {
                    try (java.sql.Statement st = conn.createStatement()) {
                        st.execute(p[1]);
                    }
                    continue;
                }
                System.out.println("== " + line);
                try {
                    switch (p[0]) {
                        case "sql":
                            for (IDbMetadata.Column c : md.read_sql_columns(p[1])) {
                                System.out.println(col(c));
                            }
                            break;
                        case "params":
                            IDbMetadata.Params ps = md.read_sql_params(p[2], Integer.parseInt(p[1]));
                            System.out.println("count_unknown=" + ps.count_unknown + " count=" + ps.count + " types=" + ps.java_types);
                            break;
                        case "table":
                            IDbMetadata.Table t = md.read_table(p[1]);
                            for (IDbMetadata.Column c : t.columns) {
                                System.out.println(col(c));
                            }
                            for (IDbMetadata.TableColumn tc : t.table_columns) {
                                System.out.println("tc " + tc.name + " data_type=" + tc.data_type + " size=" + tc.column_size
                                        + " digits=" + tc.decimal_digits + " ai=" + tc.is_ai + " nullable=" + tc.is_nullable
                                        + " fk=" + tc.fk + " indexed=" + tc.is_indexed + " unique=" + tc.is_unique);
                            }
                            break;
                        case "pk":
                            List<String> pk = md.read_pk_col_names(p[1]);
                            System.out.println("pk=" + pk);
                            break;
                        default:
                            System.out.println("?? " + p[0]);
                    }
                } catch (Throwable e) {
                    System.out.println("ERROR: " + e.getMessage().replace('\n', ' ').trim());
                }
            }
        }
        conn.close();
    }

    private static String col(IDbMetadata.Column c) {
        return "col " + c.name + " java=" + c.java_type + " ai=" + c.is_ai + " scale=" + c.scale
                + " precision=" + c.precision + " display=" + c.display_size;
    }
}
