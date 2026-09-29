#!/usr/bin/env python3
"""Generate documentation inventories directly from this project's actual source files."""
from pathlib import Path
import hashlib, html
import xml.etree.ElementTree as ET
R=Path(__file__).resolve().parents[1]
D=R/'docs'; RES=R/'app/src/main/res'
paths=sorted(p for p in (R/'app/src').rglob('*') if p.is_file() and p.suffix in ('.java','.xml','.json','.sql'))
paths += [R/'app/build.gradle',R/'build.gradle',R/'settings.gradle',R/'gradle.properties',R/'gradle/wrapper/gradle-wrapper.properties']
style='''body{font:16px/1.55 Arial,sans-serif;color:#112746;max-width:1140px;margin:auto;padding:32px}h1,h2{color:#0b1932}h2{margin-top:38px}a{color:#194e91}pre{font:12px/1.5 Consolas,monospace;background:#f3f6fb;padding:16px;white-space:pre-wrap;overflow-wrap:anywhere}table{border-collapse:collapse;width:100%;font-size:13px}td,th{padding:10px;border:1px solid #dce5ef;text-align:left;vertical-align:top;overflow-wrap:anywhere}th{background:#0b1932;color:white}small{color:#53677d}@media print{body{padding:0}h2{break-after:avoid}thead{display:table-header-group}tr{break-inside:avoid}pre{font-size:8pt}}'''
head=lambda title:f'<!doctype html><html lang="es"><meta charset="utf-8"><title>{title}</title><style>{style}</style><body><h1>{title}</h1>'
manifest=[];nav=[];blocks=[]
for i,p in enumerate(paths):
    rel=p.relative_to(R).as_posix(); digest=hashlib.sha256(p.read_bytes()).hexdigest()
    manifest.append(f'{digest}  {rel}')
    nav.append(f'<li><a href="#source-{i}">{html.escape(rel)}</a></li>')
    blocks.append(f'<section><h2 id="source-{i}">{html.escape(rel)}</h2><p><small>SHA-256: {digest}</small></p><pre>{html.escape(p.read_text())}</pre></section>')
(D/'MANIFIESTO_FUENTES.sha256').write_text('\n'.join(manifest)+'\n')
(D/'CODIGO_PARA_ENTREGA.html').write_text(head('NutriMaster 2.1.0 - C\u00f3digo de la entrega')+'<p>Fuentes reales del proyecto. No son resultados de ejecuci\u00f3n. El men\u00fa tiene cinco opciones; las recetas se consultan en Fotos. Las pruebas instrumentadas incluidas a\u00fan requieren Android.</p><ol>'+''.join(nav)+'</ol>'+''.join(blocks)+'</body></html>')
ns='{http://schemas.android.com/apk/res/android}'
styles={}
for p in (RES/'values').glob('*.xml'):
    for el in ET.parse(p).getroot().findall('style'):
        styles[el.attrib['name']]=(el.attrib.get('parent',''),{c.attrib['name']:c.text or '' for c in el.findall('item')})
def inherited(name,seen=None):
    seen=set() if seen is None else seen
    name=name.removeprefix('@style/')
    if name in seen or name not in styles:return {}
    seen.add(name);parent,attrs=styles[name];v=inherited(parent,seen);v.update(attrs);return v
rows=[]; records=[]
for p in sorted((RES/'layout').glob('*.xml')):
    for seq,el in enumerate(ET.parse(p).getroot().iter(),1):
        id=el.attrib.get(ns+'id','').split('/')[-1]
        attrs=inherited(el.attrib.get('style',''))
        for k,v in el.attrib.items():
            if k.startswith(ns): k='android:'+k[len(ns):]
            elif k.startswith('{'): k='app:'+k.split('}',1)[1]
            attrs[k]=v
        attrs.pop('android:id',None)
        record={'file':p.name,'id':id or '(sin ID: vista estructural/decorativa)','type':el.tag,'properties':attrs}
        records.append(record)
        txt='<br>'.join(html.escape(k+' = '+v) for k,v in attrs.items())
        rows.append(f'<tr><td>{p.name}</td><td>{html.escape(record["id"])}</td><td>{html.escape(el.tag)}</td><td>{txt}</td></tr>')
(D/'INVENTARIO_VISTAS.html').write_text(head('NutriMaster 2.1.0 - Inventario de vistas')+'<p>Extra\u00eddo de todos los layouts, con propiedades locales y estilos NM heredados. Los valores predeterminados de estilos externos Android/Material no se expanden. Las vistas sin ID se identifican expresamente; no se inventan identificadores.</p><table><thead><tr><th>XML</th><th>ID</th><th>Tipo</th><th>Propiedades</th></tr></thead><tbody>'+''.join(rows)+'</tbody></table></body></html>')
print(f'Generated evidence for {len(paths)} source files and {len(records)} XML views.')
