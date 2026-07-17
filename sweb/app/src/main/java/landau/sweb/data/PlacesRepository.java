package landau.sweb.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class PlacesRepository {
    private final SQLiteDatabase db;

    public PlacesRepository(SQLiteDatabase db) {
        this.db = db;
    }

    public void addBookmark(String title, String url) {
        if (db == null) return;
        ContentValues values = new ContentValues(2);
        values.put("title", title);
        values.put("url", url);
        db.insert("bookmarks", null, values);
    }

    public void deleteBookmark(int id) {
        if (db == null) return;
        db.delete("bookmarks", "id = ?", new String[]{String.valueOf(id)});
    }

    public void addHistory(String title, String url) {
        if (db == null || url == null || url.isEmpty() || url.equals("about:blank")) return;
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("url", url);
        values.put("time", System.currentTimeMillis());
        db.insert("history", null, values);
    }

    public Cursor getBookmarks() {
        if (db == null) return null;
        return db.rawQuery("SELECT title, url, id as _id FROM bookmarks", null);
    }

    public Cursor getHistory(int limit) {
        if (db == null) return null;
        return db.rawQuery("SELECT title, url, id as _id FROM history ORDER BY time DESC LIMIT ?", 
                new String[]{String.valueOf(limit)});
    }

    public void clearHistory() {
        if (db == null) return;
        db.delete("history", null, null);
    }
}
