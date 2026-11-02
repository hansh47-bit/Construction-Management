import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Export helper: BACKUP is the only SQL mutation allowed on the source database. */
class LinuxDataExport {
    static String required(String key) {
        return Objects.requireNonNull(System.getenv(key), key);
    }
    public static void main(String[] args) throws Exception {
        String user = System.getenv().getOrDefault("EXPORT_DB_USER", "sa");
        String password = System.getenv().getOrDefault("EXPORT_DB_PASSWORD", "");
        if (args[0].equals("backup")) {
            String source = required("EXPORT_SOURCE_DB");
            String zip = required("EXPORT_BACKUP_ZIP").replace("'", "''");
            try (Connection db = DriverManager.getConnection("jdbc:h2:file:" + source
                    + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE;IFEXISTS=TRUE", user, password);
                 Statement sql = db.createStatement()) {
                sql.execute("BACKUP TO '" + zip + "'");
            }
            System.out.println("Consistent H2 backup created; source accounts/data preserved.");
            return;
        }
        String oldRoot = required("EXPORT_SOURCE_UPLOADS").replace('\\', '/') + "/";
        String newRoot = required("EXPORT_LINUX_DATA_DIR") + "/uploads/";
        Path copiedUploads = Path.of(required("EXPORT_COPIED_UPLOADS")).toRealPath();
        try (Connection db = DriverManager.getConnection("jdbc:h2:file:" + required("EXPORT_COPY_DB")
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;IFEXISTS=TRUE", user, password)) {
            db.setAutoCommit(false);
            List<String[]> fields = new ArrayList<>();
            try (ResultSet columns = db.getMetaData().getColumns(null, "public", "%", "%")) {
                while (columns.next()) {
                    String column = columns.getString("COLUMN_NAME");
                    if (column.equals("file_path") || column.equals("storage_key"))
                        fields.add(new String[]{columns.getString("TABLE_NAME"), column});
                }
            }
            if (fields.isEmpty()) throw new IllegalStateException("No attachment columns found; check database schema.");
            int migrated = 0;
            for (String[] field : fields) {
                String table = field[0], column = field[1];
                if (!table.matches("[a-z_]+")) throw new IllegalStateException("Unexpected table name");
                List<String> paths = new ArrayList<>();
                try (Statement sql = db.createStatement(); ResultSet rows = sql.executeQuery(
                        "SELECT DISTINCT " + column + " FROM \"" + table + "\" WHERE " + column + " IS NOT NULL")) {
                    while (rows.next()) paths.add(rows.getString(1));
                }
                for (String original : paths) {
                    String normalized = original.replace('\\', '/');
                    if (!normalized.startsWith(oldRoot)) throw new IllegalStateException("Attachment outside source uploads: " + table);
                    String relative = normalized.substring(oldRoot.length());
                    Path file = copiedUploads.resolve(relative).normalize();
                    if (!file.startsWith(copiedUploads) || !Files.isRegularFile(file))
                        throw new IllegalStateException("Missing exported attachment in " + table);
                    try (PreparedStatement sql = db.prepareStatement("UPDATE \"" + table
                            + "\" SET " + column + "=? WHERE " + column + "=?")) {
                        sql.setString(1, newRoot + relative);
                        sql.setString(2, original);
                        migrated += sql.executeUpdate();
                    }
                }
            }
            db.commit();
            int accounts = count(db, "app_users");
            int projects = count(db, "projects");
            System.out.printf("Verified copy: accounts=%d, projects=%d, attachment columns=%d, relocated references=%d%n", accounts, projects, fields.size(), migrated);
        }
    }
    static int count(Connection db, String table) throws SQLException {
        try (Statement sql = db.createStatement(); ResultSet result = sql.executeQuery("SELECT COUNT(*) FROM " + table)) {
            result.next();
            return result.getInt(1);
        }
    }
}
