package com.poligran.nutrimaster.bootstrap;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.lang.reflect.InvocationTargetException;
import java.util.jar.JarFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Small, auditable bootstrap; NOT a binary distributed by Gradle.
 * Fetches the unmodified official wrapper from the pinned Gradle tag over HTTPS.
 * The official wrapper then installs the distribution in gradle-wrapper.properties.
 * Source is included because the authoring environment could not download binaries.
 */
public final class GradleBootstrap {
    private static final String VERSION="9.6.0";
    private static final String SHA256="497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7";
    private static final String[] URLS={
        "https://raw.githubusercontent.com/gradle/gradle/v"+VERSION+"/gradle/wrapper/gradle-wrapper.jar",
        "https://github.com/gradle/gradle/raw/refs/tags/v"+VERSION+"/gradle/wrapper/gradle-wrapper.jar"
    };
    public static void main(String[] args) {
        if(args.length==1&&"--bootstrap-info".equals(args[0])) {
            System.out.println("NutriMaster bootstrap / official Gradle wrapper "+VERSION+" / Java 8+ / HTTPS only");return;
        }
        try {
            Path self=Paths.get(GradleBootstrap.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path folder=self.getParent();
            Path wrapper=folder.resolve("gradle-wrapper-official.jar");
            if(!valid(wrapper)) {
                System.out.println("NutriMaster: descargando el wrapper oficial de Gradle "+VERSION+" (primera ejecucion)...");
                download(wrapper);
            }
            // WrapperMain derives its properties filename from its own JAR filename.
            Files.copy(folder.resolve("gradle-wrapper.properties"),folder.resolve("gradle-wrapper-official.properties"),StandardCopyOption.REPLACE_EXISTING);
            try(URLClassLoader loader=new URLClassLoader(new URL[]{wrapper.toUri().toURL()},null)) {
                Thread.currentThread().setContextClassLoader(loader);
                Class<?> main=Class.forName("org.gradle.wrapper.GradleWrapperMain",true,loader);
                try {main.getMethod("main",String[].class).invoke(null,(Object)args);}
                catch(InvocationTargetException e){throw e.getCause();}
            }
        } catch(Throwable e) {
            System.err.println("No se pudo iniciar Gradle: "+e);
            System.err.println("Comprueba Internet, proxy, Gradle JDK y SDK. Lee LEEME_PRIMERO.md.");
            System.err.println("Para reemplazar el bootstrap: tools/install-official-wrapper.ps1 o .sh");
            System.exit(1);
        }
    }
    private static boolean valid(Path p) {
        if(!Files.isRegularFile(p))return false;
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try(InputStream in=Files.newInputStream(p)) {byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}
            StringBuilder hash=new StringBuilder();for(byte b:digest.digest())hash.append(String.format("%02x",b&255));
            if(!SHA256.equals(hash.toString()))return false;
            try(JarFile jar=new JarFile(p.toFile())){return jar.getEntry("org/gradle/wrapper/GradleWrapperMain.class")!=null;}
        } catch(IOException|NoSuchAlgorithmException e){return false;}
    }
    private static void download(Path target)throws IOException {
        IOException last=null;
        for(String address:URLS) {
            Path temp=Files.createTempFile(target.getParent(),"wrapper-",".tmp");
            try {
                URL url=new URL(address);HttpURLConnection c=null;
                for(int redirects=0;redirects<6;redirects++) {
                    if(!"https".equals(url.getProtocol()))throw new IOException("Redireccion no HTTPS bloqueada");
                    c=(HttpURLConnection)url.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(60000);
                    c.setRequestProperty("User-Agent","NutriMaster-GradleBootstrap/1.0");
                    int status=c.getResponseCode();
                    if(status>=300&&status<400){String location=c.getHeaderField("Location");c.disconnect();if(location==null)throw new IOException("Redireccion sin destino");url=new URL(url,location);continue;}
                    if(status!=200){c.disconnect();throw new IOException("HTTP "+status+" al descargar el wrapper");}
                    break;
                }
                if(c==null||c.getResponseCode()!=200)throw new IOException("Demasiadas redirecciones");
                try(InputStream in=c.getInputStream();OutputStream out=Files.newOutputStream(temp)) {
                    byte[] buffer=new byte[16384];int n;long size=0;
                    while((n=in.read(buffer))!=-1){size+=n;if(size>4*1024*1024)throw new IOException("Respuesta demasiado grande");out.write(buffer,0,n);}
                } finally {c.disconnect();}
                if(!valid(temp))throw new IOException("La respuesta no contiene un wrapper Gradle valido");
                Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);return;
            } catch(IOException e){last=e;} finally{Files.deleteIfExists(temp);}
        }
        throw new IOException("Descarga HTTPS no disponible. Instala el wrapper oficial con los scripts incluidos.",last);
    }
}
