package com.poligran.nutrimaster.util;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;
import com.poligran.nutrimaster.R;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bounds-checked sampling and small memory cache; file decoding never blocks the UI. */
public final class ImageLoader {
    private static final ExecutorService IO=Executors.newFixedThreadPool(2);
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final LruCache<String,Bitmap> CACHE=new LruCache<String,Bitmap>(8*1024*1024) {
        @Override protected int sizeOf(String key,Bitmap value) {return value.getByteCount();}
    };
    private ImageLoader() { }
    public static void load(ImageView image,String source) {
        String key=source==null?"":source;
        if(key.equals(image.getTag()))return;
        image.setTag(key);
        image.setImageResource(key.isEmpty()||key.equals("avatar")?R.drawable.avatar:R.drawable.ic_leaf);
        if(key.isEmpty()||key.equals("avatar"))return;
        Bitmap cached=CACHE.get(key);if(cached!=null){image.setImageBitmap(cached);return;}
        Context context=image.getContext().getApplicationContext();WeakReference<ImageView> ref=new WeakReference<>(image);
        IO.execute(()->{
            Bitmap decoded=null;
            try {
                BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
                try(InputStream in=open(context,key)){BitmapFactory.decodeStream(in,null,bounds);}
                if(bounds.outWidth<=0||bounds.outHeight<=0)throw new IllegalArgumentException("Imagen no v\u00e1lida");
                BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;
                while(bounds.outWidth/options.inSampleSize>900||bounds.outHeight/options.inSampleSize>900)options.inSampleSize*=2;
                try(InputStream in=open(context,key)){decoded=BitmapFactory.decodeStream(in,null,options);}
                if(decoded!=null)CACHE.put(key,decoded);
            } catch(Exception ignored) { }
            final Bitmap result=decoded;
            MAIN.post(()->{ImageView v=ref.get();if(v!=null&&key.equals(v.getTag())) {
                if(result!=null)v.setImageBitmap(result);
                else {v.setImageResource(R.drawable.avatar);v.setContentDescription("Imagen no disponible. Puedes seleccionar otro archivo.");}
            }});
        });
    }
    private static InputStream open(Context c,String source) throws Exception {
        InputStream in;
        if(source.startsWith("content://"))in=c.getContentResolver().openInputStream(Uri.parse(source));
        else {int id=c.getResources().getIdentifier(source,"drawable",c.getPackageName());if(id==0)throw new IllegalArgumentException("Imagen no encontrada");in=c.getResources().openRawResource(id);}
        if(in==null)throw new IllegalArgumentException("No se pudo abrir la imagen");return in;
    }
}
