package net.sf.skey.view;

import net.sf.osql.model.Table;
import net.sf.osql.view.Database;
import net.sf.osql.view.IDbView;

public class SkDbView implements IDbView<SkTable>{
    private final Database database;
    private final SkDialect dialect;


    public SkDbView(Database database, SkDialect dialect) {
        this.database = database;
        this.dialect = dialect;
    }

    @Override
    public SkTable render(Table table) {
        return dialect.render(table, database.getTableDocument(table));
    }
}
