#!/usr/bin/env python3
"""Audit the supplied PDF's code names against the complete project. Offline, NOT an Android build."""
from pathlib import Path
import json, re, hashlib, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
J=ROOT/'app/src/main/java/com/poligran/nutrimaster'; RES=ROOT/'app/src/main/res'
c=json.loads((ROOT/'docs/contrato_documento.json').read_text())
checks=0
results=[]
def check(ok, message):
    global checks
    checks+=1
    results.append(('PASS' if ok else 'FAIL', message))
    if not ok: raise AssertionError(message)
known={}
for p in RES.rglob('*'):
    if not p.is_file():continue
    typ=p.parent.name.split('-')[0]
    if typ!='values':known.setdefault(typ,set()).add(p.stem)
    if p.suffix=='.xml':
        tree=ET.parse(p).getroot()
        for el in tree.iter():
            value=el.attrib.get('{http://schemas.android.com/apk/res/android}id','')
            if value.startswith('@+id/'):known.setdefault('id',set()).add(value[5:])
        if typ=='values':
            for el in tree:
                t=el.attrib.get('type',el.tag) if el.tag=='item' else el.tag
                if 'name' in el.attrib:known.setdefault(t,set()).add(el.attrib['name'])
for symbol,pages in c['resources'].items():
    typ,name=symbol.split('.',1)
    check(name in known.get(typ,set()),'PDF pp. '+','.join(map(str,pages))+': '+symbol)
for cls,layout in c['classes'].items():
    p=J/(cls+'.java');check(p.exists(),cls+'.java in application package')
    s=p.read_text();base='AppCompatActivity' if cls=='MainActivity' else 'Fragment'
    check(bool(re.search(r'\bclass\s+'+cls+r'\s+extends\s+'+base+r'\b',s)),cls+' is a real '+base)
    check('R.layout.'+layout in s,cls+' inflates '+layout)
    check((RES/'layout'/(layout+'.xml')).exists(),layout+'.xml exists')
    for method in c['methods'][cls]:
        check(bool(re.search(r'\b(?:void|View|boolean)\s+'+method+r'\s*\(',s)),cls+'.'+method+' declared')
    for field,typ in c['fields'].get(cls,{}).items():
        check(bool(re.search(r'\b'+typ+r'\s+[^;{}]*\b'+field+r'\b[^;{}]*;',s)),cls+': '+typ+' '+field+' declared')
        check(bool(re.search(r'\b'+field+r'\s*=\s*\w+\.findViewById\(R\.id\.'+field+r'\)',s)),cls+': '+field+' bound to exact ID')
menu=ET.parse(RES/'layout/fragment_menu.xml').getroot()
buttons=[el for el in menu.iter() if el.tag.endswith('Button')]
check(len(buttons)==5,'Exactly five real menu buttons')
check(not any('RecyclerView' in e.tag or e.tag=='ListView' for e in menu.iter()),'No recipes in the left menu')
main=(J/'MainActivity.java').read_text()
check('implements MenuFragment.OnOpcionSeleccionadaListener' in main,'Activity implements exact callback')
check('.replace(R.id.contenedorContenido,fragmento,tag)' in main,'Real FragmentTransaction.replace in right panel')
for retired in ['ui/DetailFragment.java','ui/MenuFragment.java','ui/sections/ProfileController.java','ui/sections/GalleryController.java','ui/sections/VideoController.java','ui/sections/WebController.java','ui/sections/ActionsController.java']:
    check(not (J/retired).exists(),'Retired implementation absent: '+retired)
for frag,id_,method in [('BotonesFragment','btnMostrarReceta','mostrarReceta'),('BotonesFragment','btnMostrarMensaje','mostrarMensaje'),('BotonesFragment','btnLimpiar','limpiar'),('WebFragment','btnCargar','cargarPagina'),('VideoFragment','btnReproducir','reproducirVideo'),('VideoFragment','btnPausar','pausarVideo')]:
    check(id_+'.setOnClickListener(v -> '+method+'())' in (J/(frag+'.java')).read_text(),id_+' invokes '+method)
for field,tag in [('lstFotos','ListView'),('videoView','VideoView'),('webView','WebView'),('scrollPerfil','ScrollView')]:
    matches=[e for p in (RES/'layout').glob('fragment_*.xml') for e in ET.parse(p).getroot().iter() if e.attrib.get('{http://schemas.android.com/apk/res/android}id')=='@+id/'+field]
    check(len(matches)==1 and matches[0].tag==tag,field+' has exact XML type '+tag)
for name,sha in c['unchanged_v1_1_sha256'].items():
    check((ROOT/name).is_file() and hashlib.sha256((ROOT/name).read_bytes()).hexdigest()==sha,'Preserved byte-for-byte: '+name)
check('Toast.makeText(requireContext(), R.string.botones_toast, Toast.LENGTH_SHORT).show()' in (J/'BotonesFragment.java').read_text(),'Actual Toast implemented')
check('class FotosAdapter extends ArrayAdapter<String>' in (J/'ui/FotosAdapter.java').read_text(),'Real ArrayAdapter for lstFotos')
check('onItemClick(parent, view, position, id)' in (J/'FotosFragment.java').read_text(),'onItemClick is registered through the document lambda')
check('new GridLayoutManager(requireContext(), 3)' in (J/'FotosFragment.java').read_text(),'Original grid preserved as alternate view')
check((RES/'raw/video_nutrimaster.mp4').read_bytes()==(RES/'raw/video_bowl.mp4').read_bytes(),'Document video file contains unchanged original default guide')
vm=(J/'ui/AppViewModel.java').read_text();body=vm.split('public void openRecipeLibrary() {',1)[1].split('\n    }',1)[0]
check('navigate(GALLERY)' in body and 'select(' not in body,'Recipe chooser preserves active selection')
check('repository' not in body and 'db.' not in body,'Chooser does not mutate SQLite')
check("versionName = '2.1.0'" in (ROOT/'app/build.gradle').read_text(),'New application version 2.1.0')
for status,message in results:print(status+': '+message)
print('PASS:',checks,'document-alignment and preservation assertions. Android execution NOT covered.')
