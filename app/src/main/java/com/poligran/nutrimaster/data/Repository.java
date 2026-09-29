package com.poligran.nutrimaster.data;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.poligran.nutrimaster.model.Snapshot;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Serializes disk I/O; no SQLite access is performed on the main thread. */
public final class Repository {
    public interface Work<T> { T run(NutriDatabase database) throws Exception; }
    public interface Success<T> { void accept(T value,Snapshot snapshot); }
    public interface Failure { void accept(Exception error); }
    private final NutriDatabase database;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    public Repository(Context context) { database=new NutriDatabase(context); }
    public <T> void submit(Work<T> work,Success<T> success,Failure failure) {
        worker.execute(()->{
            try {
                T result=work.run(database); Snapshot snapshot=database.snapshot();
                main.post(()->success.accept(result,snapshot));
            } catch(Exception e) {
                Log.e("NutriMaster","Repository operation failed",e);
                main.post(()->failure.accept(e));
            }
        });
    }
    public void close() { worker.execute(database::close);worker.shutdown(); }
}
