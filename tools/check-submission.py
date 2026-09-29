#!/usr/bin/env python3
"""Compare every DOCX source block with the corresponding file; no Android SDK needed."""
from pathlib import Path
import sys, json, hashlib, zipfile
import xml.etree.ElementTree as E
P=Path(__file__).resolve().parents[1]
DOC=Path(sys.argv[1]) if len(sys.argv)>1 else P/'docs/Entrega_02_Semana5_NutriMaster_v2_1_0.docx'
NS={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
W='{'+NS['w']+'}'
M=json.loads((P/'docs/FUENTES_DOCUMENTO.json').read_text())
with zipfile.ZipFile(DOC) as z: root=E.fromstring(z.read('word/document.xml'))
blocks={}
for sdt in root.findall('.//w:sdt',NS):
 tag=sdt.find('w:sdtPr/w:tag',NS)
 if tag is None:continue
 value=tag.attrib.get(W+'val','')
 if not value.startswith('source:'):continue
 path=value[len('source:'):]
 if path in blocks:raise AssertionError('Duplicate source block: '+path)
 lines=[]
 for p in sdt.findall('w:sdtContent/w:p',NS):
  text=''
  for el in p.iter():
   if el.tag==W+'t':text+=el.text or ''
   elif el.tag==W+'tab':text+='\t'
   elif el.tag==W+'br':text+='\n'
  lines.append(text)
 blocks[path]=lines
expected={row['file'] for row in M['blocks']}
assert set(blocks)==expected, {'missing':sorted(expected-set(blocks)), 'extra':sorted(set(blocks)-expected)}
for row in M['blocks']:
 path=row['file'];raw=(P/path).read_bytes();source=raw.decode('utf-8')
 assert hashlib.sha256(raw).hexdigest()==row['sha256'], 'Changed source after Word generation: '+path
 assert blocks[path]==source.splitlines(), 'DOCX block differs from source: '+path
 print('PASS: exact DOCX source block: '+path)
assert M['version']=='2.1.0'
assert "versionName = '2.1.0'" in (P/'app/build.gradle').read_text()
assert len(expected)==78
print('PASS: 78 complete source blocks equal the project files; version 2.1.0. Android execution NOT covered.')
