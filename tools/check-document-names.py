#!/usr/bin/env python3
"""Check actual Java AST declarations, XML IDs/texts and event wiring against the PDF.
Needs Python 3.9+ and JDK 17+. This is NOT an Android build or an emulator test.
"""
from pathlib import Path
import hashlib, json, re, subprocess, tempfile, xml.etree.ElementTree as ET
P=Path(__file__).resolve().parents[1]
J=P/'app/src/main/java/com/poligran/nutrimaster'
R=P/'app/src/main/res'
A='{http://schemas.android.com/apk/res/android}'
C=json.loads((P/'docs/contrato_documento.json').read_text())
results=[]
def check(ok, msg):
    results.append(('PASS' if ok else 'FAIL', msg))
    if not ok: raise AssertionError(msg)
with tempfile.TemporaryDirectory() as temp:
    subprocess.run(['javac','--release','17','-encoding','UTF-8','-d',temp,str(P/'tools/tests/DocumentSymbols.java')],check=True)
    raw=subprocess.check_output(['java','-cp',temp,'DocumentSymbols',str(J)],text=True,encoding='utf-8')
records=[]
for row in raw.splitlines():
    kind,cls,method,name,typ,line,path=row.split('\t',6)
    records.append(dict(kind=kind,cls=cls,method=method,name=name,type=typ.replace(' ',''),line=int(line),path=path))

def declared(cls,name,typ,kind=None,method=None):
    return [r for r in records if r['cls']==cls and r['name']==name and r['type']==typ.replace(' ','')
            and (kind is None or r['kind']==kind) and (method is None or r['method']==method)]

def method_exists(cls,name):
    return any(r['cls']==cls and r['name']==name and r['kind']=='METHOD' for r in records)

def call_exists(cls,method,select,argument=None):
    return any(r['cls']==cls and r['method']==method and r['kind']=='CALL' and r['name']==select
               and (argument is None or argument.replace(' ','') in r['type']) for r in records)

strings={el.attrib['name']:''.join(el.itertext()) for p in (R/'values').glob('*.xml') for el in ET.parse(p).getroot() if el.tag=='string'}
def resolve(value):
    return strings.get(value[8:],'MISSING') if value.startswith('@string/') else value

view_rows=[]
for cls,layout in C['classes'].items():
    source=(J/(cls+'.java')).read_text()
    views={el.get(A+'id','').split('/')[-1]:el for el in ET.parse(R/'layout'/(layout+'.xml')).getroot().iter() if A+'id' in el.attrib}
    for name,typ in C['fields'].get(cls,{}).items():
        ds=declared(cls,name,typ,kind='FIELD')
        check(bool(ds),f'{cls}: {typ} {name} is an actual field, not a comment')
        check(name in views,f'{cls}: XML has @+id/{name}')
        check(call_exists(cls,'onCreateView','vista.findViewById','R.id.'+name),f'{cls}: onCreateView binds vista.findViewById(R.id.{name})')
        if ds:
            view_rows.append([cls,name,typ,ds[0]['line'],layout+'.xml',resolve(views[name].get(A+'text',''))])
    for method in C['methods'][cls]:
        owner='OnOpcionSeleccionadaListener' if cls=='MenuFragment' and method=='onOpcionSeleccionada' else cls
        check(method_exists(owner,method),f'{owner}.{method}() exists in the Java AST')
    if cls!='MainActivity':
        check(bool(declared(cls,'vista','View',kind='LOCAL',method='onCreateView')),f'{cls}: local View vista in onCreateView')
        for name,typ in [('inflater','LayoutInflater'),('container','ViewGroup'),('savedInstanceState','Bundle')]:
            check(bool(declared(cls,name,typ,kind='PARAM',method='onCreateView')),f'{cls}.onCreateView: {typ} {name}')

extra=[('MenuFragment','listener','OnOpcionSeleccionadaListener','FIELD',None),
       ('MenuFragment','context','Context','PARAM','onAttach'),
       ('MainActivity','savedInstanceState','Bundle','PARAM','onCreate'),
       ('MainActivity','opcion','String','PARAM','onOpcionSeleccionada'),
       ('MainActivity','fragmento','Fragment','PARAM','cambiarContenido'),
       ('FotosFragment','recetas','String[]','FIELD',None),
       ('FotosFragment','descripciones','String[]','FIELD',None),
       ('FotosFragment','imagenes','int[]','FIELD',None),
       ('FotosFragment','adapter','ArrayAdapter<String>','FIELD',None),
       ('FotosFragment','parent','AdapterView<?>','PARAM','onItemClick'),
       ('FotosFragment','view','View','PARAM','onItemClick'),
       ('FotosFragment','position','int','PARAM','onItemClick'),
       ('FotosFragment','id','long','PARAM','onItemClick'),
       ('VideoFragment','controles','MediaController','FIELD',None),
       ('VideoFragment','uri','Uri','LOCAL','onCreateView'),
       ('WebFragment','url','String','LOCAL','cargarPagina'),
       ('BotonesFragment','receta','String','LOCAL','mostrarReceta')]
for cls,name,typ,kind,method in extra:
    check(bool(declared(cls,name,typ,kind,method)),f'{cls}: {kind} {typ} {name}')

buttons=[('MenuFragment','btnPerfil','Perfil','listener.onOpcionSeleccionada("perfil")','MainActivity.mostrarPerfil'),
('MenuFragment','btnFotos','Fotos','listener.onOpcionSeleccionada("fotos")','MainActivity.mostrarFotos'),
('MenuFragment','btnVideo','Video','listener.onOpcionSeleccionada("video")','MainActivity.mostrarVideo'),
('MenuFragment','btnWeb','Web','listener.onOpcionSeleccionada("web")','MainActivity.mostrarWeb'),
('MenuFragment','btnBotones','Botones','listener.onOpcionSeleccionada("botones")','MainActivity.mostrarBotones'),
('VideoFragment','btnReproducir','Reproducir','reproducirVideo()','VideoFragment.reproducirVideo'),
('VideoFragment','btnPausar','Pausar','pausarVideo()','VideoFragment.pausarVideo'),
('WebFragment','btnCargar','Cargar','cargarPagina()','WebFragment.cargarPagina'),
('BotonesFragment','btnMostrarReceta','Mostrar receta','mostrarReceta()','BotonesFragment.mostrarReceta'),
('BotonesFragment','btnMostrarMensaje','Mostrar mensaje','mostrarMensaje()','BotonesFragment.mostrarMensaje'),
('BotonesFragment','btnLimpiar','Limpiar','limpiar()','BotonesFragment.limpiar')]
for cls,name,label,invoked,target in buttons:
    layout=C['classes'][cls]
    views={el.get(A+'id','').split('/')[-1]:el for el in ET.parse(R/'layout'/(layout+'.xml')).getroot().iter() if A+'id' in el.attrib}
    check(resolve(views[name].get(A+'text',''))==label,f'{name}: exact visible text "{label}"')
    check(call_exists(cls,'onCreateView',name+'.setOnClickListener',invoked),f'{name}: setOnClickListener invokes {invoked}')
    owner,method=target.split('.')
    check(method_exists(owner,method),f'{name}: real target method {target}')
for module,fragment in [('Perfil','PerfilFragment'),('Fotos','FotosFragment'),('Video','VideoFragment'),('Web','WebFragment'),('Botones','BotonesFragment')]:
    check(call_exists('MainActivity','mostrar'+module,'cambiarContenido','new '+fragment+'()'),f'mostrar{module} creates {fragment}')
check(call_exists('MainActivity','onOpcionSeleccionada','vm.navigate'),'Callback routes through activity-owned state')
check(call_exists('FotosFragment','onCreateView','lstFotos.setOnItemClickListener','onItemClick(parent, view, position, id)'),'ListView registers the documented lambda and named onItemClick method')
check(call_exists('FotosFragment','onItemClick','imgFotoDetalle.setImageResource','imagenes[position]'),'List click uses imagenes[position]')
check(call_exists('FotosFragment','onItemClick','txtDescripcionFoto.setText','descripciones[position]'),'List click uses descripciones[position]')
for cls,method,select in [('VideoFragment','reproducirVideo','videoView.start'),('VideoFragment','pausarVideo','videoView.pause'),('WebFragment','cargarPagina','webView.loadUrl'),('BotonesFragment','mostrarMensaje','Toast.makeText'),('BotonesFragment','limpiar','edtReceta.setText')]:
    check(call_exists(cls,method,select),f'{cls}.{method} performs {select}')
expected_strings={'opcion_perfil':'Perfil','opcion_fotos':'Fotos','opcion_video':'Video','opcion_web':'Web','opcion_botones':'Botones',
'perfil_titulo':'Perfil profesional','perfil_estudios_titulo':'Estudios','perfil_experiencia_titulo':'Experiencia','perfil_info_titulo':'Informaci\u00f3n adicional',
'fotos_titulo':'Galer\u00eda de recetas','video_titulo':'Video: preparaci\u00f3n saludable','web_titulo':'Navegador NutriMaster','botones_titulo':'Ejemplo de botones',
'botones_mensaje':'Se mostr\u00f3 el mensaje de ejemplo.','botones_toast':'Ejemplo de mensaje mostrado con Toast','web_cargando':'Cargando p\u00e1gina...'}
for k,v in expected_strings.items():
    check(strings.get(k)==v,'Figure/XML visible text: '+k+' = '+v)
for id_,key in [('profile_title_heading','perfil_titulo'),('education_heading','perfil_estudios_titulo'),('experience_heading','perfil_experiencia_titulo'),('bio_heading','perfil_info_titulo')]:
    views={el.get(A+'id','').split('/')[-1]:el for el in ET.parse(R/'layout/fragment_perfil.xml').getroot().iter() if A+'id' in el.attrib}
    check(views[id_].get(A+'text')=='@string/'+key,'Profile title actually references '+key)
for cls in ['PerfilFragment','FotosFragment','VideoFragment','WebFragment','BotonesFragment']:
    check(not any(r['cls']==cls and r['kind'] in ['FIELD','LOCAL'] and r['name']=='root' for r in records),'Old root variable absent in '+cls)
# Field arrays and adapter are genuinely used for the dynamic library, not empty compatibility aliases.
foto=(J/'FotosFragment.java').read_text()
check('recetas[i] = recetasDisponibles.get(i).title;' in foto,'String[] recetas is populated from SQLite models')
check('recetas.length' in foto and 'ArrayAdapter<String> adapter = adaptadorConMiniaturas' in foto,'Document arrays and adapter participate in the real UI')
check('class FotosAdapter extends ArrayAdapter<String>' in (J/'ui/FotosAdapter.java').read_text(),'Rich rows retain the exact ArrayAdapter<String> base')
# Automatic capitalization would make the configured button labels differ in the running UI.
theme=(R/'values/themes.xml').read_text()
for sty in ['NM.Button','NM.Outline','NM.MenuButton']:
    styles={s.get('name'):s for s in ET.parse(R/'values/themes.xml').getroot().findall('style')}
    def style_values(name, visited=None):
        visited=set() if visited is None else visited
        if name not in styles or name in visited:return {}
        visited.add(name); target=styles[name]
        attrs=style_values(target.get('parent','').removeprefix('@style/'),visited)
        attrs.update({el.get('name'):el.text for el in target.findall('item')})
        return attrs
    check(style_values(sty).get('android:textAllCaps')=='false',sty+' preserves the case of document labels')
# Independently extracted declarations from ALL seven Java snippets on PDF pages 18-24.
# This checks local variables and lambda parameters as well as fields and methods.
snapshot=json.loads((P/'docs/simbolos_java_documento.json').read_text())
for expected in snapshot['declarations']:
    match=any(r['kind']==expected['kind'] and r['cls']==expected['class']
        and r['method']==expected['method'] and r['name']==expected['name']
        and r['type']==expected['type'].replace(' ','') for r in records)
    check(match, 'Full PDF declaration: '+expected['class']+'.'+expected['method']+
          ' / '+expected['kind']+' '+expected['type']+' '+expected['name'])
check(strings.get('app_descripcion')=='Aplicaci\u00f3n M\u00f3vil de Recetario Saludable',
      'Full application heading from PDF figures')
check('@string/app_descripcion' in (R/'layout/activity_main.xml').read_text(),
      'Document application heading actually used by activity_main.xml')
print('\n'.join(s+': '+m for s,m in results))
print(f'PASS: {len(results)} AST, binding, label and event checks. Android execution NOT covered.')
# Human-readable, searchable map, with actual source line numbers.
lines=['# NutriMaster 2.1.0 - Nombres, variables y eventos', '',
'Comparaci\u00f3n con Entrega_02_Semana5_NutriMaster.docx.pdf: numerales 2 a 11, tabla de IDs, Java/XML y textos de las figuras. Esta auditor\u00eda es est\u00e1tica; no equivale a una compilaci\u00f3n ni a una captura de la app ejecut\u00e1ndose.', '',
'## Variables de vista e identificadores', '', '| Clase | Variable y tipo Java | L\u00ednea Java | XML | Texto inicial |','|---|---|---:|---|---|']
for cls,name,typ,line,layout,label in view_rows:
    label_display = label.replace(chr(10), " / ").replace(chr(92)+"n", " / ") or "Contenido dinamico / no textual"
    lines.append(f'| `{cls}` | `{typ} {name}` / `R.id.{name}` | {line} | `{layout}` | {label_display} |')
lines+=['','## Botones: etiqueta visible, evento y m\u00e9todo','','| Etiqueta | Variable / ID | Evento registrado | M\u00e9todo de destino |','|---|---|---|---|']
for cls,name,label,invoked,target in buttons:lines.append(f'| {label} | `{name}` | `setOnClickListener` \u2192 `{invoked}` | `{target}()` |')
lines+=['| Selecci\u00f3n de foto | `lstFotos` | `setOnItemClickListener` | `FotosFragment.onItemClick()` |', '',
'## Otras variables del Java del documento', '', '| Clase | Nombre / tipo | Ubicaci\u00f3n |', '|---|---|---|']
for cls,name,typ,kind,method in extra: lines.append(f'| `{cls}` | `{typ} {name}` | `{method or "Campo de clase"}` |')
lines+=['', 'Cada `onCreateView` declara `View vista` y recibe `inflater`, `container` y `savedInstanceState`. Los nombres `recetas`, `descripciones` e `imagenes` son arreglos reales; se actualizan con el filtro de la biblioteca. `adapter` es `ArrayAdapter<String>`; la variable local se declara en `onCreateView` y se conserva tambi\u00e9n una referencia de clase para liberar la vista. Se usa una implementaci\u00f3n de filas que conserva miniaturas y metadatos.', '',
'## Alcance y diferencias intencionales', '',
'Los nombres t\u00e9cnicos y las etiquetas de los once botones del documento coinciden. Se mantienen las personalizaciones pedidas: `com.poligran.nutrimaster`, Java, API m\u00ednima 24, azul corporativo, proporci\u00f3n 30/70 y todas las funciones de SQLite. Los dos FrameLayout ahora est\u00e1n dentro de un LinearLayout horizontal; sus pesos 3/7 conservan el 30/70, no los pesos 1/2 del ejemplo.', '',
'El PDF es un ejemplo m\u00ednimo. Esta aplicaci\u00f3n conserva datos editables: el nombre, los estudios y la experiencia del perfil; las nueve recetas iniciales y las recetas propias. No se reemplazan por el perfil y las cuatro recetas ficticias de la maqueta. Los recursos receta1 a receta4 conservan los alias declarados en 2.0 (ensalada, avena, bowl y wrap); no son los platos exactos dibujados en el PDF. Estos valores de ejemplo no son nombres de variables ni eventos. La app completa a\u00f1ade m\u00e9todos, vistas y controles que el documento no nombr\u00f3.', '',
'El texto "Se mostr\u00f3 el mensaje de ejemplo." coincide con la figura de la p\u00e1gina 33. El Toast conserva "Ejemplo de mensaje mostrado con Toast". Los t\u00edtulos Perfil profesional, Galer\u00eda de recetas, Video: preparaci\u00f3n saludable, Navegador NutriMaster y Ejemplo de botones corresponden a las figuras.', '',
'La coincidencia de nombres no implica copia literal de todos los valores, dise\u00f1o verde, red sin validaci\u00f3n, recetas est\u00e1ticas ni ausencia de funciones adicionales. No se cambi\u00f3 el documento para disimular nombres diferentes: se modificaron los archivos del proyecto.', '',
'## Resultado de esta verificaci\u00f3n','', 'Se verifican adem\u00e1s las 98 declaraciones extra\u00eddas directamente de los siete ejemplos Java del PDF (p\u00e1ginas 18 a 24), incluyendo el tipo, la clase, el m\u00e9todo, las variables locales y los par\u00e1metros de las expresiones lambda. El contrato est\u00e1 en `docs/simbolos_java_documento.json`.','', f'PAS\u00d3: {len(results)} comprobaciones sobre el AST de Java, los XML y las cadenas. Ejecutar `python tools/check-document-names.py` vuelve a generar este informe y verifica los nombres. Se necesitan Python 3.9+ y JDK 17+. Las pruebas Android de `NombresDocumentoInstrumentedTest` se incluyen, pero no se reportan ejecutadas.','']
(P/'docs/NOMBRES_Y_EVENTOS.md').write_text('\n'.join(lines),encoding='utf-8')
