package net.sf.skey.view;

import net.sf.osql.model.Table;
import net.sf.osql.view.ITableView;

public interface SkTable extends ITableView {
    Table getTable();

    String getInsertionPerms();

    String getUpdatePerms();

    String getSelectView();

    String getUpdateView();

    String getGrants();
}
