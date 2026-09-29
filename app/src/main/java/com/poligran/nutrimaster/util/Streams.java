package com.poligran.nutrimaster.util;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
public final class Streams {
    private Streams() { }
    public static String text(InputStream stream) throws IOException {
        try(InputStream in=stream; ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] b=new byte[8192]; int n;
            while((n=in.read(b))!=-1) { out.write(b,0,n); if(out.size()>8*1024*1024)throw new IOException("Archivo demasiado grande"); }
            return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
}
