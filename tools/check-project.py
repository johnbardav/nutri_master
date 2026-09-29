#!/usr/bin/env python3
"""Offline checks of resources, source links, SQL schema and bundled media.
This is NOT an Android build, Lint, or emulator test.
Run: python tools/check-project.py [--media]  (ffprobe is optional).
"""
from pathlib import Path
import xml.etree.ElementTree as ET
import json, re, sqlite3, tempfile, subprocess, sys, fnmatch
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'app/src/main/res'
checks=0
def check(ok,message):
    global checks
    checks+=1
    if not ok: raise AssertionError(message)
known={t:set() for t in ('layout','drawable','mipmap','raw','string','array','color','style','id','xml')}
xmls=list(RES.rglob('*.xml'))
for p in RES.rglob('*'):
    if not p.is_file():continue
    typ=p.parent.name.split('-')[0]
    if typ in known:known[typ].add(p.stem)
    if p.suffix=='.xml':
        tree=ET.parse(p);check(True,'XML parses: '+p.name)
        if typ=='values':
            for e in tree.getroot():
                t=e.attrib.get('type',e.tag) if e.tag=='item' else ('array' if e.tag.endswith('-array') else e.tag)
                if t in known and 'name' in e.attrib:known[t].add(e.attrib['name'])
        for name in re.findall(r'@\+id/(\w+)',p.read_text()):known['id'].add(name)
for p in (ROOT/'app/src').rglob('*.java'):
    for typ,name in re.findall(r'(?<![\w.])R\.(\w+)\.(\w+)',p.read_text()):
        if typ in known:check(name in known[typ],f'{p.name}: missing R.{typ}.{name}')
for p in xmls+[ROOT/'app/src/main/AndroidManifest.xml']:
    for typ,name in re.findall(r'@(?!android:)(\w+)/([\w.*]+)',p.read_text()):
        if typ in known:check(any(fnmatch.fnmatch(n,name) for n in known[typ]),f'{p.name}: missing @{typ}/{name}')
# Catch implicit Android style inheritance such as NM.Card -> undefined NM.
for p in (RES/'values').glob('*.xml'):
    for el in ET.parse(p).getroot().findall('style'):
        name=el.attrib['name']
        if '.' in name and 'parent' not in el.attrib:check(name.rsplit('.',1)[0] in known['style'],f'Undefined implicit style parent {name}')
main=(RES/'layout/activity_main.xml').read_text()
a='{http://schemas.android.com/apk/res/android}'
main_tree=ET.parse(RES/'layout/activity_main.xml').getroot()
panels={el.attrib.get(a+'id',''):el for el in main_tree.iter() if el.tag=='FrameLayout'}
left=panels.get('@+id/contenedorMenu'); right=panels.get('@+id/contenedorContenido')
check(left is not None and right is not None, 'Both named FrameLayout containers')
lw=float(left.attrib.get(a+'layout_weight',0)); rw=float(right.attrib.get(a+'layout_weight',0))
check(abs(lw/(lw+rw)-0.30)<1e-9, 'Master-detail split 30/70')
check(any(el.tag=='LinearLayout' and el.attrib.get(a+'orientation')=='horizontal' and left in list(el) and right in list(el) for el in main_tree.iter()), 'Document horizontal LinearLayout contains both panels')
check('contenedorMenu' in main and 'contenedorContenido' in main,'Both panel IDs')
manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text()
check(manifest.count('uses-permission')==1 and 'android.permission.INTERNET' in manifest,'Internet-only permission')
gradle=(ROOT/'app/build.gradle').read_text()
check("applicationId = 'com.poligran.nutrimaster'" in gradle,'Exact package ID')
check('minSdk = 24' in gradle and 'compileSdk = 37' in gradle,'SDK levels')
seed=json.loads((ROOT/'app/src/main/assets/recipes.json').read_text())
check(len(seed)==9,'Nine initial recipes')
for r in seed:
    check((RES/'drawable-nodpi'/(r['image']+'.webp')).is_file(),'Recipe image exists')
    check((RES/'raw'/(r['video']+'.mp4')).is_file(),'Recipe video exists')
    check(len(r['steps'].splitlines())==4,'Four matching tutorial steps')
# Execute the same SQL source that Android's SQLiteOpenHelper consumes.
with tempfile.TemporaryDirectory() as t:
    path=Path(t)/'checks.db';db=sqlite3.connect(path);db.execute('PRAGMA foreign_keys=ON')
    db.executescript((ROOT/'app/src/main/assets/schema.sql').read_text())
    for r in seed:
        row=dict(r);row['built_in']=1;row['created_at']=1
        cols=','.join(row);marks=','.join('?' for _ in row)
        db.execute(f'INSERT INTO recipes ({cols}) VALUES ({marks})',list(row.values()))
    check(db.execute('SELECT count(*) FROM recipes').fetchone()[0]==9,'SQL seed')
    db.execute('UPDATE recipes SET favorite=1-favorite,saved=1,notes=? WHERE id=1',('Persistent note',))
    db.commit();db.close();db=sqlite3.connect(path);db.execute('PRAGMA foreign_keys=ON')
    check(db.execute('SELECT favorite,saved,notes FROM recipes WHERE id=1').fetchone()==(1,1,'Persistent note'),'Flags and notes survive reopening')
    db.execute('UPDATE recipes SET title=? WHERE id=1',('Edited title',))
    check(db.execute('SELECT title FROM recipes WHERE id=1').fetchone()[0]=='Edited title','SQL update')
    for _ in range(2):db.execute('INSERT OR IGNORE INTO shopping(recipe_id,text) VALUES(1,?)',('One tomato',))
    check(db.execute('SELECT count(*) FROM shopping').fetchone()[0]==1,'No repeated ingredients for same recipe')
    db.execute('INSERT INTO calculations(recipe_id,title,portions,kcal,protein,carbs,fat,created_at) VALUES(1,?,2,780,28,96,30,1)',('Recorded title',))
    db.execute('DELETE FROM recipes WHERE id=1')
    check(db.execute('SELECT recipe_id,text FROM shopping').fetchone()==(None,'One tomato'),'Shopping retained after deleting recipe')
    check(db.execute('SELECT recipe_id,title FROM calculations').fetchone()==(None,'Recorded title'),'Historical snapshot retained')
    check(db.execute('SELECT count(*) FROM recipes').fetchone()[0]==8,'Deletion')
    db.execute('UPDATE shopping SET checked=1');db.execute('DELETE FROM shopping WHERE checked=1')
    check(db.execute('SELECT count(*) FROM shopping').fetchone()[0]==0,'Clear purchased')
    for field,value in [('base_servings',0),('kcal',-1),('favorite',3),('minutes',0),('saved',8)]:
        rejected=False
        try:db.execute('UPDATE recipes SET '+field+'=? WHERE id=2',(value,))
        except sqlite3.IntegrityError:rejected=True
        check(rejected,'SQL rejects invalid '+field)
    db.execute('INSERT INTO profile(id,name,role,education,experience,bio) VALUES(1,?,?,?,?,?)',('Name','Role','Studies','Experience','Bio'))
    db.execute('UPDATE profile SET name=? WHERE id=1',('Edited profile',))
    check(db.execute('SELECT name FROM profile').fetchone()[0]=='Edited profile','Profile update')
    db.close()
if '--media' in sys.argv:
    for r in seed:
        media=RES/'raw'/(r['video']+'.mp4')
        data=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-show_format','-of','json',str(media)]))
        stream=data['streams'][0]
        check(stream['codec_name']=='h264','H.264 video')
        check('Baseline' in stream['profile'],'Baseline profile')
        check(stream['pix_fmt']=='yuv420p','YUV 420p')
        check(float(data['format']['duration'])>=29.9,'Video is about 30 seconds')
        check(len(data['streams'])==1,'Guide intentionally has no audio')
print(f'PASS: {checks} structural, resource, SQL'+(' and media' if '--media' in sys.argv else '')+' checks.')
print('Android build, device performance and interaction tests NOT covered by this script.')
