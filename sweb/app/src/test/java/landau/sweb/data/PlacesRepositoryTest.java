package landau.sweb.data;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PlacesRepositoryTest {

    @Mock
    private SQLiteDatabase mockDb;

    private PlacesRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        repository = new PlacesRepository(mockDb);
    }

    @Test
    public void addBookmark_callsInsert() {
        repository.addBookmark("Google", "https://www.google.com");
        verify(mockDb).insert(eq("bookmarks"), isNull(), any(ContentValues.class));
    }

    @Test
    public void addHistory_callsInsert() {
        repository.addHistory("Baidu", "https://www.baidu.com");
        verify(mockDb).insert(eq("history"), isNull(), any(ContentValues.class));
    }

    @Test
    public void clearHistory_callsDelete() {
        repository.clearHistory();
        verify(mockDb).delete(eq("history"), isNull(), isNull());
    }
}
