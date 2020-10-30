package ai.datadive.api;

import android.database.sqlite.SQLiteDatabase;

public interface DatabaseResetListener {
    public void onDatabaseReset(SQLiteDatabase db);
}
