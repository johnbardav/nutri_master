package com.poligran.nutrimaster.domain;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Applied to typed addresses AND redirects. Never allow file/content/javascript URLs. */
public final class UrlPolicy {
    private UrlPolicy() { }
    public static String normalize(String input) {
        String value=input==null?"":input.trim();
        if(value.isEmpty() || value.length()>2048 || value.matches(".*\\s+.*"))
            throw new IllegalArgumentException("Escribe una direcci\u00f3n HTTPS sin espacios.");
        if(!value.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) value="https://"+value;
        try {
            URI uri=new URI(value);
            String host=uri.getHost();
            if(!"https".equalsIgnoreCase(uri.getScheme()) || uri.getRawUserInfo()!=null ||
               host==null || host.indexOf('.')<0 || host.startsWith(".") ||
               uri.getPort()==0 || uri.getPort()>65535)
                throw new IllegalArgumentException("Solo se admiten direcciones HTTPS de sitios web, sin credenciales.");
            if(host.equalsIgnoreCase("nutrimaster.invalid"))
                throw new IllegalArgumentException("Usa el bot\u00f3n Gu\u00eda local.");
            // Preserve raw escapes: decoding a%26b would change the meaning of a query.
            String path=uri.getRawPath();
            String normalized="https://"+host.toLowerCase(Locale.ROOT)+(uri.getPort()<0?"":":"+uri.getPort())+
                    (path==null||path.isEmpty()?"/":path)+
                    (uri.getRawQuery()==null?"":"?"+uri.getRawQuery())+
                    (uri.getRawFragment()==null?"":"#"+uri.getRawFragment());
            return new URI(normalized).toASCIIString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("La direcci\u00f3n no tiene un formato v\u00e1lido.");
        }
    }
    public static boolean allowed(String value) {
        try { normalize(value); return true; } catch(IllegalArgumentException e) { return false; }
    }
}
