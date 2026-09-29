#!/usr/bin/env python3
"""Generate the academic DOCX from the ACTUAL sources in this project.
Requires python-docx. Does not build Android or manufacture execution screenshots.
Usage: python tools/generate-submission.py /path/to/output.docx
"""
from pathlib import Path
import sys, json, hashlib, re
import xml.etree.ElementTree as ET
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

P = Path(__file__).resolve().parents[1]
OUT = Path(sys.argv[1]) if len(sys.argv) > 1 else P/'docs/Entrega_02_Semana5_NutriMaster_v2_1_0.docx'
OUT.parent.mkdir(parents=True, exist_ok=True)
C = json.loads((P/'docs/contrato_documento.json').read_text())
RES = P/'app/src/main/res'
JROOT = 'app/src/main/java/com/poligran/nutrimaster/'
A = '{http://schemas.android.com/apk/res/android}'
NAVY='112746'; GRAY='536477'; LIGHT='F5F7FA'; WHITE='FFFFFF'
doc = Document()
# Remove decorative rules inherited from the template's title styles.
for border in list(doc.styles.element.iter(qn('w:pBdr'))):
 border.getparent().remove(border)
sec = doc.sections[0]
sec.page_width=Inches(8.5); sec.page_height=Inches(11)
sec.top_margin=sec.bottom_margin=sec.left_margin=sec.right_margin=Inches(1)
sec.header_distance=sec.footer_distance=Inches(.48)

for name in ['Normal','Body Text']:
 st=doc.styles[name];st.font.name='Arial';st.font.size=Pt(11)
 st.paragraph_format.line_spacing=1.35;st.paragraph_format.space_after=Pt(7)
for name,size in [('Title',25),('Subtitle',13),('Heading 1',16),('Heading 2',12),('Heading 3',11)]:
 st=doc.styles[name];st.font.name='Arial';st.font.size=Pt(size);st.font.color.rgb=RGBColor.from_string(NAVY)
 st.paragraph_format.space_before=Pt(14 if name!='Title' else 0);st.paragraph_format.space_after=Pt(7)
 st.paragraph_format.keep_with_next=True
for name,size in [('NMCode',8.5),('NMPath',9),('NMNote',9),('NMTable',9.2)]:
 st=doc.styles.add_style(name,1)
 st.font.name='Liberation Mono' if name=='NMCode' else 'Arial';st.font.size=Pt(size)
 st.paragraph_format.space_after=Pt(0 if name in ['NMCode','NMTable'] else 5)
 st.paragraph_format.space_before=Pt(0)
 st.paragraph_format.line_spacing=Pt(10.2) if name=='NMCode' else 1.14
 st.paragraph_format.keep_with_next=False
 st.paragraph_format.widow_control=True
 if name=='NMCode':
  st.paragraph_format.widow_control=False
  rpr=st.element.get_or_add_rPr();fonts=OxmlElement('w:rFonts')
  for k in ['ascii','hAnsi','eastAsia','cs']:fonts.set(qn('w:'+k),'Liberation Mono')
  rpr.append(fonts)
  ppr=st.element.get_or_add_pPr();shade=OxmlElement('w:shd');shade.set(qn('w:fill'),LIGHT);ppr.append(shade)
  lang=OxmlElement('w:lang');lang.set(qn('w:val'),'en-US');rpr.append(lang)
 if name in ['NMPath','NMNote']:st.font.color.rgb=RGBColor.from_string(GRAY)
# Spanish spelling; no red spellcheck marks on code.
rpr=doc.styles['Normal'].element.get_or_add_rPr();lang=OxmlElement('w:lang');lang.set(qn('w:val'),'es-CO');rpr.append(lang)
settings=doc.settings.element
for key in ['doNotAutoHyphenateCaps','doNotHyphenate']:
 pass
update=OxmlElement('w:updateFields');update.set(qn('w:val'),'true');settings.append(update)
doc.core_properties.title='NutriMaster | Entrega 2 - Semana 5 | Version 2.1.0'
doc.core_properties.author='Subgrupo 12 - Politecnico Grancolombiano'
doc.core_properties.subject='Implementacion Android y codigo fuente completo: Java, XML y SQLite'
doc.core_properties.keywords='NutriMaster, Java, XML, SQLite, Entrega 2, Semana 5, 2.1.0'

h=sec.header.paragraphs[0];h.text='NUTRIMASTER 2.1.0  |  ENTREGA 2 - SEMANA 5';h.style=doc.styles['NMNote']
f=sec.footer.paragraphs[0];f.alignment=WD_ALIGN_PARAGRAPH.RIGHT
r=f.add_run('Subgrupo 12  |  ');r.font.name='Arial';r.font.size=Pt(9)
fld=OxmlElement('w:fldSimple');fld.set(qn('w:instr'),'PAGE');f._p.append(fld)

anchor_id=1; sections=[]; manifest=[]; emitted=set()
def bookmark(par,name):
 global anchor_id
 b=OxmlElement('w:bookmarkStart');b.set(qn('w:id'),str(anchor_id));b.set(qn('w:name'),name)
 e=OxmlElement('w:bookmarkEnd');e.set(qn('w:id'),str(anchor_id));anchor_id+=1
 par._p.insert(0,b);par._p.append(e)
def heading(text,level=1,anchor=None):
 p=doc.add_paragraph(text,'Heading '+str(level))
 if anchor:bookmark(p,anchor)
 return p
def para(text,style=None,boldlead=None):
 p=doc.add_paragraph(style=style)
 if boldlead and text.startswith(boldlead):p.add_run(boldlead).bold=True;p.add_run(text[len(boldlead):])
 else:p.add_run(text)
 return p
def note(text):return para(text,'NMNote')
def table(headers,rows,widths):
 t=doc.add_table(rows=1, cols=len(headers));t.alignment=WD_TABLE_ALIGNMENT.CENTER;t.autofit=False
 for col,w in zip(t.columns,widths):col.width=Inches(w)
 def fill(cell,text,ishead=False):
  cell.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER
  p=cell.paragraphs[0];p.style=doc.styles['NMTable'];r=p.add_run(str(text));r.bold=ishead
  if ishead:r.font.color.rgb=RGBColor.from_string(WHITE)
  pr=cell._tc.get_or_add_tcPr()
  mar=OxmlElement('w:tcMar')
  for side,val in [('top','65'),('bottom','65'),('left','95'),('right','95')]:
   el=OxmlElement('w:'+side);el.set(qn('w:w'),val);el.set(qn('w:type'),'dxa');mar.append(el)
  pr.append(mar)
  if ishead:
   el=OxmlElement('w:shd');el.set(qn('w:fill'),NAVY);pr.append(el)
 for i,text in enumerate(headers):fill(t.rows[0].cells[i],text,True);t.rows[0].cells[i].width=Inches(widths[i])
 trpr=t.rows[0]._tr.get_or_add_trPr();repeat=OxmlElement('w:tblHeader');trpr.append(repeat)
 for row in rows:
  cells=t.add_row().cells
  for i,text in enumerate(row):fill(cells[i],text);cells[i].width=Inches(widths[i])
 for row in t.rows:
  pr=row._tr.get_or_add_trPr();ns=OxmlElement('w:cantSplit');pr.append(ns)
 # Only horizontal table rules, with a light outer boundary.
 pr=t._tbl.tblPr;borders=OxmlElement('w:tblBorders')
 for edge in ['top','bottom','left','right','insideH','insideV']:
  el=OxmlElement('w:'+edge);el.set(qn('w:val'),'single' if edge!='insideV' else 'nil');el.set(qn('w:sz'),'4');el.set(qn('w:color'),'D7DEE7');borders.append(el)
 pr.append(borders)
 para('','NMNote')
 return t

def code(rel,title,explanation=''):
 rel=str(rel)
 assert rel not in emitted, rel
 emitted.add(rel)
 data=(P/rel).read_bytes();text=data.decode('utf-8')
 heading(title,2,anchor='src_'+str(len(manifest)))
 path_par=para(rel,'NMPath');path_par.paragraph_format.keep_with_next=True
 if explanation:
  desc=para(explanation);desc.paragraph_format.keep_with_next=True
 caption=note('C\u00f3digo completo del archivo. SHA-256: '+hashlib.sha256(data).hexdigest()[:16]+'...')
 caption.paragraph_format.keep_with_next=True
 sdt=OxmlElement('w:sdt');pr=OxmlElement('w:sdtPr');tag=OxmlElement('w:tag');tag.set(qn('w:val'),'source:'+rel);pr.append(tag)
 alias=OxmlElement('w:alias');alias.set(qn('w:val'),rel);pr.append(alias);sdt.append(pr);content=OxmlElement('w:sdtContent');sdt.append(content)
 for line in text.splitlines():
  p=doc.add_paragraph(style='NMCode');run=p.add_run(line)
  rpr=run._r.get_or_add_rPr();no=OxmlElement('w:noProof');rpr.append(no)
  content.append(p._p)
 doc._element.body.insert(len(doc._element.body)-1,sdt)
 manifest.append({'file':rel,'section':title,'sha256':hashlib.sha256(data).hexdigest(),'lines':len(text.splitlines()),'trailing_newline':text.endswith('\n')})
 para('','NMNote')

def chapter(title,key):
 par=heading(title,1,key)
 if key in ['s1','anexo_a','anexo_b','anexo_c','anexo_d']:
  par.paragraph_format.page_break_before=True
 sections.append((title,key))

def resolve(v):
 return strings.get(v[8:],v).replace('\\n',' / ') if v.startswith('@string/') else v
strings={el.attrib['name']:''.join(el.itertext()) for p in (RES/'values').glob('*.xml') for el in ET.parse(p).getroot() if el.tag=='string'}

# Cover. The academic names are retained from the submitted PDF.
p=para('POLIT\u00c9CNICO GRANCOLOMBIANO');p.alignment=WD_ALIGN_PARAGRAPH.CENTER;p.runs[0].bold=True
p=para('Ingenier\u00eda de Software\nHerramientas de Programaci\u00f3n M\u00f3vil I');p.alignment=WD_ALIGN_PARAGRAPH.CENTER
para('')
p=doc.add_paragraph('NutriMaster','Title');p.alignment=WD_ALIGN_PARAGRAPH.CENTER
p=doc.add_paragraph('Aplicaci\u00f3n M\u00f3vil de Recetario Saludable Interactivo','Subtitle');p.alignment=WD_ALIGN_PARAGRAPH.CENTER
p=para('Entrega 2 - Semana 5\nImplementaci\u00f3n Android y c\u00f3digo fuente\nVersi\u00f3n 2.1.0');p.alignment=WD_ALIGN_PARAGRAPH.CENTER
para('')
p=para('Subgrupo 12');p.alignment=WD_ALIGN_PARAGRAPH.CENTER;p.runs[0].bold=True
for name in ['Cristian Camilo Arango Giraldo','Cristian Augusto Acu\u00f1a Hern\u00e1ndez','Yony Arbey R\u00faa Jim\u00e9nez','Darwin Javier Gamba S\u00e1nchez','John Mario Barrientos David']:
 p=para(name);p.alignment=WD_ALIGN_PARAGRAPH.CENTER;p.paragraph_format.space_after=Pt(3)
para('')
p=para('Profesor: Jos\u00e9 Le\u00f3n Le\u00f3n\n29 de septiembre de 2026');p.alignment=WD_ALIGN_PARAGRAPH.CENTER

# One deterministic internal index; its hyperlinks do not require Word field updates.
doc.add_page_break();heading('Contenido',1,'contenido')
note('Los t\u00edtulos del \u00edndice enlazan a cada secci\u00f3n. El panel de navegaci\u00f3n de Word tambi\u00e9n permite localizar clases y archivos.')
toc_marker=doc.add_paragraph('')

chapter('1. Introducci\u00f3n','s1')
para('NutriMaster es una aplicaci\u00f3n Android de consulta y gesti\u00f3n de recetas saludables. La versi\u00f3n 2.1.0 mantiene el recetario, la persistencia local y las funciones multimedia, y consolida la correspondencia entre los nombres de la especificaci\u00f3n, las interfaces XML y las declaraciones Java.')
para('El men\u00fa ofrece Perfil, Fotos, Video, Web y Botones. La pantalla conserva dos paneles simult\u00e1neos: el izquierdo ocupa el 30 % del ancho y el derecho el 70 %. Las recetas se consultan dentro de Fotos, no como opciones adicionales del men\u00fa lateral.')
para('La gu\u00eda acad\u00e9mica solicita identificar actividades y fragmentos, interfaces, propiedades, variables vinculadas, eventos y m\u00e9todos, y acompa\u00f1arlos con XML, Java y capturas de ejecuci\u00f3n (Polit\u00e9cnico Grancolombiano, s. f., pp. 2-4). Este documento relaciona esos elementos con los archivos de la versi\u00f3n 2.1.0.')
table(['Identificaci\u00f3n','Valor'],[
('Aplicaci\u00f3n / paquete','NutriMaster / com.poligran.nutrimaster'),('Versi\u00f3n de la aplicaci\u00f3n','2.1.0; versionCode = 5'),('Lenguaje / interfaces / almacenamiento','Java 17 / XML / SQLite'),('Configuraci\u00f3n Android','minSdk 24; compileSdk y targetSdk 37'),('Herramientas configuradas','Android Gradle Plugin 9.4.0; Gradle 9.6.0'),('Estado de validaci\u00f3n','Nombres y fuentes verificados est\u00e1ticamente; compilaci\u00f3n y ejecuci\u00f3n Android pendientes.')],[2.05,4.45])
para('La revisi\u00f3n de nombres no cambia el esquema SQLite ni reemplaza los datos del usuario. La galer\u00eda conserva b\u00fasqueda y filtros; Guardadas y Favoritas siguen siendo colecciones independientes. Las acciones acad\u00e9micas de mostrar texto, mostrar un Toast y limpiar coexisten con las acciones completas del recetario.')

chapter('2. Arquitectura de la aplicaci\u00f3n','s2')
para('MainActivity hospeda MenuFragment en contenedorMenu y uno de los cinco fragmentos de contenido en contenedorContenido. Existen seis clases de fragmento, pero se muestran dos instancias principales a la vez: men\u00fa y contenido. Cada fragmento tiene su XML y administra sus vistas y eventos; esta separaci\u00f3n corresponde al papel de los fragmentos en Android (Android Developers, s. f.-b).')
table(['Paso','Responsable y flujo'],[
('1. Selecci\u00f3n','MenuFragment registra setOnClickListener en btnPerfil, btnFotos, btnVideo, btnWeb y btnBotones.'),
('2. Callback','La interfaz MenuFragment.OnOpcionSeleccionadaListener entrega onOpcionSeleccionada(String opcion) a MainActivity.'),
('3. Estado de navegaci\u00f3n','MainActivity traduce la opci\u00f3n al m\u00f3dulo de AppViewModel. El observador invoca mostrarModulo(String opcion).'),
('4. M\u00e9todo de pantalla','mostrarPerfil(), mostrarFotos(), mostrarVideo(), mostrarWeb() o mostrarBotones() crea el fragmento correspondiente.'),
('5. Reemplazo','cambiarContenido(Fragment fragmento) usa replace(R.id.contenedorContenido, fragmento, tag). No reemplaza el men\u00fa.'),
('6. Datos compartidos','AppViewModel comparte selecci\u00f3n y estado; Repository ejecuta operaciones de NutriDatabase y publica un Snapshot.')],[1.2,5.3])
para('Las transacciones permiten reemplazar un fragmento dentro de un contenedor sin iniciar otra Activity (Android Developers, s. f.-c). El proyecto comprueba el estado del FragmentManager para aplazar un cambio cuando ya se guard\u00f3 el estado; onPostResume aplica la selecci\u00f3n vigente.')
para('AppViewModel utiliza el alcance de MainActivity, de modo que los fragmentos reciben una instancia compartida (Android Developers, s. f.-a). Su funci\u00f3n es conservar datos y estado transitorio; no sustituye los nombres ni las clases de los fragmentos del documento. La escritura duradera reside en SQLite.')
heading('2.1 Persistencia y estado',2)
para('NutriDatabase administra las tablas recipes, profile, shopping y calculations. Repository concentra el acceso as\u00edncrono. El perfil, las recetas, los indicadores de guardado/favorito, las notas, las compras y los c\u00e1lculos se escriben en esa base. Los filtros, la receta seleccionada, los borradores y posiciones de las vistas se gestionan por separado mediante SavedStateHandle.')
para('SQLiteOpenHelper gestiona la creaci\u00f3n y versi\u00f3n de la base de datos (Android Developers, s. f.-d). En esta revisi\u00f3n no se modifica schema.sql ni se introduce una migraci\u00f3n de datos. Al instalar sobre una versi\u00f3n previa se requiere el mismo paquete y certificado; no se debe desinstalar ni borrar los datos para probar una actualizaci\u00f3n.')

chapter('3. Actividades y fragmentos','s3')
para('Las siete clases principales est\u00e1n en app/src/main/java/com/poligran/nutrimaster/. Sus interfaces se encuentran en app/src/main/res/layout/. Los nombres mantienen las referencias de la especificaci\u00f3n de Semana 5 (Subgrupo 12, 2026, pp. 5-7).')
funcs={
'MainActivity':'Contenedor general, callback de navegaci\u00f3n y selecci\u00f3n de archivos.',
'MenuFragment':'Men\u00fa persistente con cinco botones; no contiene la lista de recetas.',
'PerfilFragment':'Foto, nombre, estudios, experiencia, informaci\u00f3n adicional y edici\u00f3n del perfil.',
'FotosFragment':'Lista y cuadr\u00edcula de recetas, filtros, selecci\u00f3n, detalle y creaci\u00f3n.',
'VideoFragment':'Video de la receta seleccionada, controles y vinculaci\u00f3n de archivo propio.',
'WebFragment':'URL escrita, WebView, historial de navegaci\u00f3n, gu\u00eda local y errores.',
'BotonesFragment':'Ejemplo acad\u00e9mico y operaciones completas sobre recetas y colecciones.'}
table(['Clase / tipo','XML','Responsabilidad'],[(k+'.java\n'+('Activity' if k=='MainActivity' else 'Fragment'),v+'.xml',funcs[k]) for k,v in C['classes'].items()],[1.62,1.8,3.08])
heading('3.1 Estructura complementaria de archivos',2)
table(['Carpeta','Archivos o responsabilidad'],[
('java/.../ui/','AppViewModel.java, Editors.java, CollectionDialogs.java, FotosAdapter.java y RecipeAdapter.java.'),
('java/.../data/','NutriDatabase.java, Repository.java y JsonExporter.java.'),
('java/.../domain/','NutritionCalculator.java, RecipeFilter.java, UrlPolicy.java y Validation.java.'),
('java/.../model/','Recipe.java, Profile.java, Snapshot.java, ShoppingItem.java y Calculation.java.'),
('java/.../util/','Format.java, ImageLoader.java, Streams.java y TextWatcherAdapter.java.'),
('res/ y assets/','XML de interfaces, estilos, textos, alias, ilustraciones, MP4, recipes.json y schema.sql.'),
('test/ y androidTest/','Pruebas de dominio, base de datos, navegaci\u00f3n y nombres del documento.'),
('tools/ y docs/','Validadores, generadores del inventario y del documento, correspondencias y registros.')],[1.55,4.95])
para('No se usa DetailFragment ni se mantienen los controladores de pantalla de las primeras versiones. Los auxiliares anteriores complementan a los fragmentos: no son sustitutos de las clases exigidas. Los anexos contienen su c\u00f3digo fuente.')

chapter('4. Dise\u00f1o de interfaces','s4')
para('El dise\u00f1o conserva la paleta azul oscuro corporativa y los dos paneles. El contenedor horizontal asigna pesos 3 y 7 a contenedorMenu y contenedorContenido. El encabezado general muestra NutriMaster y la descripci\u00f3n del recetario; los botones del men\u00fa conservan sus r\u00f3tulos en espa\u00f1ol.')
for title,body in [
('4.1 Perfil','La secci\u00f3n se titula Perfil profesional. scrollPerfil contiene la fotograf\u00eda o avatar, txtNombre, los apartados Estudios, Experiencia e Informaci\u00f3n adicional, y los controles para editar el perfil o elegir una foto. Los contadores muestran recetas, guardadas, favoritas y compras pendientes.'),
('4.2 Fotos','Galer\u00eda de recetas presenta lstFotos como ListView con miniaturas y un ArrayAdapter<String> especializado. La vista alternativa Cuadr\u00edcula conserva tres columnas. Buscar, filtrar por categor\u00eda o colecci\u00f3n y elegir una receta actualiza su imagen, descripci\u00f3n, ingredientes, pasos y datos registrados. Crear receta sigue abriendo el editor.'),
('4.3 Video','Video: preparaci\u00f3n saludable muestra el video asociado a la receta activa. Reproducir, Pausar y Detener mantienen sus acciones; el MediaController aporta la barra de avance. Elegir receta en Fotos, Abrir video del dispositivo y Usar gu\u00eda incluida conservan la navegaci\u00f3n y la selecci\u00f3n de medios.'),
('4.4 Web','Navegador NutriMaster contiene edtUrl, Cargar, el estado y webView. La p\u00e1gina externa se carga al pulsar Cargar; la gu\u00eda local no es evidencia de una carga de red. Se mantienen Atr\u00e1s, Adelante, Gu\u00eda local, Abrir en navegador y Permitir JavaScript en esta sesi\u00f3n. UrlPolicy conserva la validaci\u00f3n HTTPS.'),
('4.5 Botones','Ejemplo de botones contiene edtReceta, Mostrar receta, Mostrar mensaje, Limpiar y txtResultadoBotones. Debajo permanece el recetario completo: guardar, favorita, notas, porciones, compartir, editar, eliminar, compras, historial y exportar. Limpiar solo restablece el ejemplo; no elimina recetas ni notas.')]:
 heading(title,2);para(body)
heading('4.6 Recursos y contenido editable',2)
para('Las nueve recetas iniciales, sus ilustraciones y los valores nutricionales son datos demostrativos. Los recursos receta1, receta2, receta3 y receta4 son alias de las ilustraciones de ensalada, avena, bowl y wrap; no reemplazan el recetario por los cuatro platos de la maqueta. video_nutrimaster.mp4 conserva los mismos bytes de la gu\u00eda local video_bowl.mp4.')
para('Las gu\u00edas MP4 muestran texto e ilustraciones y no tienen audio. El perfil inicial es editable y su avatar no constituye una fotograf\u00eda real de una persona. La calculadora escala las cantidades registradas; no calcula necesidades personales ni valida datos nutricionales. La exportaci\u00f3n JSON no incluye archivos multimedia externos ni implementa importaci\u00f3n de copias.')

chapter('5. Identificadores y propiedades','s5')
para('Las siguientes tablas mantienen los identificadores del documento y los relacionan con las vistas reales. Los tipos de botones en Java son Button; las interfaces usan MaterialButton, que permite conservar el dise\u00f1o existente. Las propiedades completas, incluidas las vistas adicionales, se encuentran en el XML del apartado 8 y en docs/INVENTARIO_VISTAS.html.')
table(['ID','Tipo','Funci\u00f3n / propiedad'],[('contenedorMenu','FrameLayout','Panel izquierdo: ancho 0dp y peso 3.'),('contenedorContenido','FrameLayout','Panel derecho: ancho 0dp y peso 7.')],[2,1.2,3.3])
meanings={
'imgLogo':'Logo de NutriMaster.','txtMenuTitulo':'NutriMaster / Recetario Saludable.',
'btnPerfil':'Seleccionar Perfil.','btnFotos':'Seleccionar Fotos.','btnVideo':'Seleccionar Video.','btnWeb':'Seleccionar Web.','btnBotones':'Seleccionar Botones.',
'imgPerfil':'Foto o avatar del perfil.','txtNombre':'Nombre editable de la persona.','txtEstudios':'Formaci\u00f3n del perfil.','txtExperiencia':'Experiencia del perfil.','txtInfoAdicional':'Biograf\u00eda o informaci\u00f3n adicional.','scrollPerfil':'Desplazamiento vertical del perfil.',
'lstFotos':'Lista desplazable con selecci\u00f3n de receta.','imgFotoDetalle':'Ilustraci\u00f3n de la receta seleccionada.','txtDescripcionFoto':'Descripci\u00f3n de la receta.',
'videoView':'Reproductor multimedia.','btnReproducir':'Iniciar o reanudar video.','btnPausar':'Pausar video.',
'edtUrl':'Direcci\u00f3n HTTPS escrita.','btnCargar':'Validar y cargar la direcci\u00f3n.','webView':'Contenido web dentro de la app.','txtEstadoWeb':'Progreso, informaci\u00f3n o error de carga.',
'edtReceta':'Texto del ejemplo de botones.','btnMostrarReceta':'Mostrar el texto escrito.','btnMostrarMensaje':'Mostrar Toast y actualizar resultado.','btnLimpiar':'Limpiar el ejemplo.','txtResultadoBotones':'Resultado visible del ejemplo.'}
for n,(cls,fields) in enumerate(C['fields'].items(),1):
 heading('5.'+str(n)+' '+cls,2)
 table(['ID / variable','Tipo Java','Funci\u00f3n'],[(k,t,meanings[k]) for k,t in fields.items()],[2,1.2,3.3])
heading('5.7 Controles adicionales conservados',2)
table(['Grupo','Identificadores de las funciones ampliadas'],[
('Perfil','edit_profile, change_photo, profile_explore, profile_stats.'),
('Fotos','recipe_search, category_filter, collection_filter, gallery_display_mode, gallery_mode_list, gallery_mode_grid, recipe_grid, create_recipe, recipe_actions y recipe_video.'),
('Video','video_select_recipe, video_stop, video_import, video_reset, video_loading y video_state.'),
('Web','web_back, web_forward, web_home, web_external, web_javascript y web_progress.'),
('Botones','actions_select_recipe, save_recipe, favorite_recipe, recipe_notes, save_notes, portions_input, calculate, calorie_result, share_recipe, edit_recipe y delete_recipe.'),
('Colecciones','add_to_shopping, open_shopping, open_history, export_data y shopping_count.')],[1.12,5.38])

chapter('6. Variables y v\u00ednculos con los identificadores','s6')
para('Los fragmentos declaran las variables de vista con los mismos nombres de sus IDs. En onCreateView se crea View vista, se infla el XML correspondiente y se enlazan las variables mediante vista.findViewById(R.id.identificador). Por ejemplo, la variable btnPerfil se enlaza con R.id.btnPerfil, y txtResultadoBotones con R.id.txtResultadoBotones.')
para('Las declaraciones completas y los enlaces ejecutables figuran en el apartado 9. El c\u00f3digo conserva adem\u00e1s this.vista para gestionar el ciclo de vida y liberar referencias en onDestroyView. Esas referencias de clase no sustituyen la variable local vista de los ejemplos acad\u00e9micos.')
table(['Ubicaci\u00f3n','Declaraciones conservadas','Uso'],[
('Todos los fragmentos','View vista; LayoutInflater inflater; ViewGroup container; Bundle savedInstanceState.','Inflar la interfaz y recibir el estado del ciclo de vida.'),
('MenuFragment','OnOpcionSeleccionadaListener listener; Context context; String opcion.','Vincular el callback con la actividad y comunicar la opci\u00f3n.'),
('MainActivity','String opcion; Fragment fragmento.','Recibir la elecci\u00f3n y reemplazar el contenido derecho.'),
('FotosFragment','String[] recetas; String[] descripciones; int[] imagenes.','Representar la selecci\u00f3n filtrada; sus valores se obtienen de las recetas de SQLite.'),
('FotosFragment','ArrayAdapter<String> adapter; par\u00e1metros parent, view, position e id.','Adaptar la lista y recibir onItemClick. FotosAdapter conserva las miniaturas y los IDs de recetas.'),
('VideoFragment','MediaController controles; Uri uri.','Conectar el reproductor y preparar la referencia al video local.'),
('WebFragment','String url.','Leer, validar y normalizar edtUrl en cargarPagina().'),
('BotonesFragment','String receta.','Leer edtReceta en mostrarReceta(), sin modificar la receta de SQLite.')],[1.35,2.55,2.6])
para('Las 98 declaraciones extra\u00eddas de los siete ejemplos Java del PDF se contrastan por clase, m\u00e9todo, tipo y nombre mediante tools/check-document-names.py. El contrato de referencia est\u00e1 en docs/simbolos_java_documento.json. Esto verifica declaraciones reales del \u00e1rbol de sintaxis, no coincidencias aisladas dentro de comentarios.')

chapter('7. Eventos y m\u00e9todos','s7')
para('El identificador, la variable y el texto de un bot\u00f3n cumplen funciones distintas y se mantienen alineados. Por ejemplo: ID btnMostrarMensaje, variable Button btnMostrarMensaje, r\u00f3tulo Mostrar mensaje y m\u00e9todo mostrarMensaje(). No se usan nombres t\u00e9cnicos como r\u00f3tulos de la interfaz.')
rows=[
('btnPerfil / Perfil','setOnClickListener','onOpcionSeleccionada("perfil") \u2192 mostrarPerfil()'),
('btnFotos / Fotos','setOnClickListener','onOpcionSeleccionada("fotos") \u2192 mostrarFotos()'),
('btnVideo / Video','setOnClickListener','onOpcionSeleccionada("video") \u2192 mostrarVideo()'),
('btnWeb / Web','setOnClickListener','onOpcionSeleccionada("web") \u2192 mostrarWeb()'),
('btnBotones / Botones','setOnClickListener','onOpcionSeleccionada("botones") \u2192 mostrarBotones()'),
('lstFotos','setOnItemClickListener','onItemClick(parent, view, position, id)'),
('btnReproducir / Reproducir','setOnClickListener','reproducirVideo() \u2192 videoView.start()'),
('btnPausar / Pausar','setOnClickListener','pausarVideo() \u2192 videoView.pause()'),
('btnCargar / Cargar','setOnClickListener','cargarPagina() \u2192 UrlPolicy.normalize(url) \u2192 webView.loadUrl(url)'),
('btnMostrarReceta / Mostrar receta','setOnClickListener','mostrarReceta() \u2192 txtResultadoBotones.setText(...)'),
('btnMostrarMensaje / Mostrar mensaje','setOnClickListener','mostrarMensaje() \u2192 Toast y cambio de txtResultadoBotones'),
('btnLimpiar / Limpiar','setOnClickListener','limpiar() \u2192 borra edtReceta y restablece txtResultadoBotones')]
table(['Vista / texto visible','Registro del evento','M\u00e9todo y respuesta'],rows,[1.75,1.5,3.25])
para('Las cinco acciones del men\u00fa terminan en cambiarContenido(Fragment fragmento). Los eventos de la lista conservan los par\u00e1metros del documento y delegan en un m\u00e9todo onItemClick expl\u00edcito. Las comprobaciones de preparaci\u00f3n y ciclo de vida del video permanecen: cambiar un nombre no debe eliminar el manejo de errores ni la liberaci\u00f3n del reproductor (Android Developers, s. f.-e).')
heading('7.1 Acciones del recetario y persistencia',2)
table(['Acci\u00f3n / ID','Ruta de implementaci\u00f3n'],[
('Guardar / save_recipe','AppViewModel.toggleSaved(id) \u2192 NutriDatabase.toggle(id, false).'),
('Favorita / favorite_recipe','AppViewModel.toggleFavorite(id) \u2192 NutriDatabase.toggle(id, true).'),
('Notas / save_notes','AppViewModel.saveNotes(id, notes) \u2192 NutriDatabase.saveNotes(...).'),
('Calcular / calculate','BotonesFragment.calculate() \u2192 NutritionCalculator.calculate(...) \u2192 AppViewModel.calculate(...).'),
('Compartir / share_recipe','BotonesFragment.share() \u2192 Intent.ACTION_SEND con texto.'),
('Crear o editar','Editors.recipe(...) \u2192 AppViewModel.saveRecipe(...) \u2192 NutriDatabase.saveRecipe(...).'),
('Eliminar / delete_recipe','Confirmaci\u00f3n \u2192 AppViewModel.deleteRecipe(id). No es la acci\u00f3n Limpiar.'),
('Compras','addIngredients(...), addShopping(...), checkShopping(...) y clearShopping().'),
('Historial','CollectionDialogs.history(...) muestra los c\u00e1lculos; clearHistory() los elimina tras confirmaci\u00f3n.'),
('Exportar / export_data','MainActivity.exportData() \u2192 AppViewModel.export(uri) \u2192 JsonExporter.export(...).'),
('Elegir receta en Fotos','AppViewModel.openRecipeLibrary() restablece filtros y abre Fotos; no borra ni modifica SQLite.')],[1.65,4.85])

chapter('8. C\u00f3digo XML','s8')
para('Los siguientes bloques contienen los siete layouts principales completos de esta versi\u00f3n. Se conservan los nombres de la especificaci\u00f3n y se incluyen las vistas ampliadas del recetario. Los di\u00e1logos, \u00edtems, estilos, cadenas y dem\u00e1s XML utilizados se transcriben en el anexo B.')
xml_desc={
'activity_main':'Define el encabezado y los dos FrameLayout. El LinearLayout horizontal asigna pesos 3/7 a contenedorMenu y contenedorContenido.',
'fragment_menu':'Contiene imgLogo, txtMenuTitulo y los cinco botones del men\u00fa. No incluye recetas como opciones laterales.',
'fragment_perfil':'Contiene scrollPerfil, los campos de perfil y las acciones de edici\u00f3n y selecci\u00f3n de fotograf\u00eda.',
'fragment_fotos':'Contiene lstFotos, la cuadr\u00edcula alternativa, filtros y el detalle de receta. Las dos presentaciones comparten la misma selecci\u00f3n.',
'fragment_video':'Integra videoView, Reproducir, Pausar y las acciones ampliadas de gesti\u00f3n del video.',
'fragment_web':'Define edtUrl, btnCargar, txtEstadoWeb, webView y los controles de navegaci\u00f3n adicionales.',
'fragment_botones':'Incluye el ejemplo acad\u00e9mico y las acciones persistentes. Los controles adicionales permanecen independientes de Mostrar receta, Mostrar mensaje y Limpiar.'}
for i,layout in enumerate(C['classes'].values(),1):code('app/src/main/res/layout/'+layout+'.xml',f'8.{i} {layout}.xml',xml_desc[layout])

chapter('9. C\u00f3digo Java','s9')
para('Cada clase declara, vincula y administra las vistas de su XML. Los nombres del apartado 7 se mantienen en los m\u00e9todos ejecutables. El paquete es com.poligran.nutrimaster en las siete clases. El anexo A contiene todas las clases complementarias usadas por estos archivos; el anexo D contiene las pruebas.')
for i,cls in enumerate(C['classes'],1):code(JROOT+cls+'.java',f'9.{i} {cls}.java',funcs[cls])
code('app/src/main/AndroidManifest.xml','9.8 AndroidManifest.xml','Declara MainActivity como entrada, el permiso INTERNET, el tema y la configuraci\u00f3n de la aplicaci\u00f3n. No solicita acceso amplio al almacenamiento; los archivos propios se eligen mediante el selector del sistema.')

chapter('10. Evidencias de funcionamiento y validaci\u00f3n','s10')
para('Las maquetas de la especificaci\u00f3n inicial no son capturas de ejecuci\u00f3n. Las siguientes evidencias deben obtenerse de NutriMaster 2.1.0 ejecut\u00e1ndose en el dispositivo o emulador. Hasta incorporarlas, su estado es pendiente. No se presentan im\u00e1genes generadas como pantallazos de funcionamiento.')
evidence=[
('Principal','MainActivity.java / activity_main.xml','Abrir la app: mostrar ambos paneles, los cinco botones y el perfil inicial.','01_principal.png'),
('Men\u00fa','MenuFragment.java / fragment_menu.xml','Mostrar Perfil, Fotos, Video, Web y Botones, sin recetas laterales.','02_menu.png'),
('Perfil','PerfilFragment.java / fragment_perfil.xml','Configurar una foto autorizada; mostrar nombre, estudios, experiencia y desplazamiento.','03_perfil.png'),
('Fotos','FotosFragment.java / fragment_fotos.xml','Seleccionar una fila y evidenciar imagen y descripci\u00f3n; repetir en Cuadr\u00edcula.','04_fotos.png'),
('Video','VideoFragment.java / fragment_video.xml','Reproducir un MP4 con controles visibles; probar pausa, avance y detenci\u00f3n.','05_video.png'),
('Web','WebFragment.java / fragment_web.xml','Digitar una URL HTTPS y pulsar Cargar; mostrar la p\u00e1gina realmente cargada.','06_web.png'),
('Mostrar receta','BotonesFragment.java / fragment_botones.xml','Escribir un nombre y pulsar Mostrar receta para cambiar txtResultadoBotones.','07_botones.png'),
('Toast','BotonesFragment.java / fragment_botones.xml','Pulsar Mostrar mensaje y capturar el Toast; probar Limpiar sin perder notas.','08_toast.png'),
('Navegaci\u00f3n','MainActivity.java / activity_main.xml','Comparar dos opciones con el men\u00fa siempre visible y el contenido derecho diferente.','09_navegacion.png'),
('Persistencia','BotonesFragment, AppViewModel y NutriDatabase','Guardar una receta, marcar favorita y escribir una nota; cerrar y abrir, y verificar los datos.','10_persistencia.png')]
for i,(label,source,action,filename) in enumerate(evidence,1):
 heading(f'10.{i} {label}',2)
 note('CAPTURA REAL PENDIENTE  |  docs/capturas/'+filename)
 para(action);note('Referencia: '+source+'.')
heading('10.11 Comprobaciones ejecutadas',2)
table(['Comprobaci\u00f3n','Resultado registrado'],[
('Declaraciones, v\u00ednculos, etiquetas y eventos','326 comprobaciones aprobadas, incluyendo las 98 declaraciones del Java del PDF.'),
('Correspondencia de recursos y conservaci\u00f3n','249 comprobaciones aprobadas; 80 referencias de recursos y 33 archivos de datos/l\u00f3gica/multimedia conservados.'),
('XML, recursos, SQL y multimedia','853 comprobaciones aprobadas.'),
('L\u00f3gica de dominio Java','55 aserciones aprobadas.'),
('Sintaxis Java y retornos','33 archivos analizados; sin resoluci\u00f3n de tipos Android.'),
('Integridad documental','Los bloques de c\u00f3digo se extraen directamente de los archivos y se comparan con el manifiesto de fuentes.')],[2.7,3.8])
para('Estos resultados son controles est\u00e1ticos y pruebas de l\u00f3gica sin Android. No demuestran reproducci\u00f3n en el dispositivo, apariencia final, rendimiento ni persistencia mediante interacciones de usuario. La compilaci\u00f3n Android se intent\u00f3, pero se detuvo durante la descarga del wrapper de Gradle; no lleg\u00f3 a compilar el m\u00f3dulo app. No hay un APK validado.')
heading('10.12 Ejecuci\u00f3n y pruebas pendientes',2)
para('Abrir la carpeta NutriMaster que contiene settings.gradle, instalar el SDK configurado, sincronizar Gradle y ejecutar app en API 24. Los archivos README.md y docs/INSTALACION_Y_SOLUCIONES.md describen la instalaci\u00f3n y el primer arranque del wrapper. No mezclar esta versi\u00f3n con carpetas de revisiones anteriores.')
para('Ejecutar las pruebas de androidTest, comprobar cambios de orientaci\u00f3n, selecci\u00f3n de archivos, ausencia de conexi\u00f3n, persistencia y cambios r\u00e1pidos de m\u00f3dulo. El objetivo de cambio de pantalla inferior a un segundo requiere medici\u00f3n en el dispositivo; no se deduce del an\u00e1lisis del c\u00f3digo.')
table(['Comando en Windows','Prop\u00f3sito'],[(r'.\gradlew.bat :app:assembleDebug','Compilar el APK de depuraci\u00f3n.'),(r'.\gradlew.bat :app:testDebugUnitTest','Ejecutar las pruebas unitarias del m\u00f3dulo.'),(r'.\gradlew.bat :app:connectedDebugAndroidTest','Ejecutar pruebas instrumentadas con un dispositivo disponible.')],[3.5,3])

chapter('11. Sustentaci\u00f3n','s11')
para('La sustentaci\u00f3n debe recorrer el flujo desde el nombre que aparece en pantalla hasta el m\u00e9todo y la escritura o actualizaci\u00f3n que ejecuta. Mostrar un r\u00f3tulo no basta: conviene localizar el ID en el XML, la variable Java, findViewById, el listener y el m\u00e9todo que responde.')
heading('11.1 Recorrido sugerido',2)
para('Presentar el nombre y objetivo de NutriMaster; se\u00f1alar los dos contenedores; mostrar los cinco botones de MenuFragment; seguir onOpcionSeleccionada hasta cambiarContenido; abrir Fotos y seleccionar una receta; reproducir su video; cargar una URL; demostrar Mostrar receta, Mostrar mensaje y Limpiar; finalmente guardar una nota o favorita y comprobarla tras reabrir la app.')
heading('11.2 Preguntas de verificaci\u00f3n',2)
table(['Pregunta','Respuesta vinculada al proyecto'],[
('\u00bfPor qu\u00e9 seis clases de Fragment?','Una es el men\u00fa y cinco son alternativas de contenido. Solo men\u00fa y una alternativa se muestran simult\u00e1neamente.'),
('\u00bfC\u00f3mo se comunica el men\u00fa?','Con MenuFragment.OnOpcionSeleccionadaListener, implementada por MainActivity.'),
('\u00bfQu\u00e9 hace AppViewModel?','Comparte la receta seleccionada y el estado y coordina Repository. La actividad sigue siendo responsable de las transacciones.'),
('\u00bfD\u00f3nde est\u00e1n las recetas?','En SQLite y en la secci\u00f3n Fotos. No son botones del men\u00fa lateral.'),
('\u00bfQu\u00e9 diferencia Limpiar de Eliminar?','limpiar() restablece el ejemplo de texto. deleteRecipe(id) elimina una receta tras confirmaci\u00f3n.'),
('\u00bfPor qu\u00e9 hay String[] y SQLite?','Los arreglos describen el resultado filtrado para la lista; la base de datos sigue siendo el almacenamiento.'),
('\u00bfD\u00f3nde se conserva el nombre del video del PDF?','En res/raw/video_nutrimaster.mp4; el recurso se utiliza en VideoFragment.'),
('\u00bfQu\u00e9 se ha probado?','Los resultados ejecutados est\u00e1n en docs/validacion/. Las pruebas instrumentadas y las capturas deben completarse en Android.')],[2.1,4.4])

chapter('12. Conclusiones y recomendaciones','s12')
para('La versi\u00f3n 2.1.0 relaciona la especificaci\u00f3n con las clases, interfaces, identificadores y m\u00e9todos ejecutables. MainActivity, MenuFragment y las cinco clases de contenido mantienen la organizaci\u00f3n documentada, mientras que los auxiliares de datos y presentaci\u00f3n sostienen las funciones ampliadas del recetario.')
para('Separar la demostraci\u00f3n acad\u00e9mica de botones de las acciones de negocio permite conservar ambas: mostrar texto o limpiar un campo no equivale a guardar, modificar ni eliminar una receta. Esta distinci\u00f3n debe mantenerse en las capturas y en la sustentaci\u00f3n.')
para('La persistencia ya forma parte del alcance actual, no de una mejora futura. El esquema y los archivos de datos, dominio, modelos y multimedia preservados se verifican mediante huellas SHA-256. Para desarrollar nuevas funciones ser\u00e1 necesario mantener sincronizados el proyecto, las tablas de nombres y los bloques de c\u00f3digo de este documento.')
para('El cierre de la entrega requiere compilar la versi\u00f3n, ejecutar las pruebas en Android, registrar el dispositivo y los resultados, y reemplazar las indicaciones de capturas pendientes por evidencias reales. La verificaci\u00f3n est\u00e1tica reduce contradicciones de nombres, pero no sustituye ese proceso.')

chapter('13. Bibliograf\u00eda','s13')
refs=[
('Android Developers. (s. f.-a). Communicate with fragments.','https://developer.android.com/guide/fragments/communicate'),
('Android Developers. (s. f.-b). Fragments.','https://developer.android.com/guide/fragments'),
('Android Developers. (s. f.-c). Fragment transactions.','https://developer.android.com/guide/fragments/transactions'),
('Android Developers. (s. f.-d). Save data using SQLite.','https://developer.android.com/training/data-storage/sqlite'),
('Android Developers. (s. f.-e). VideoView.','https://developer.android.com/reference/android/widget/VideoView'),
('Android Developers. (s. f.-f). WebView.','https://developer.android.com/reference/android/webkit/WebView'),
('Polit\u00e9cnico Grancolombiano. (s. f.). Desarrollo de una aplicaci\u00f3n m\u00f3vil para Android [Gu\u00eda del proyecto grupal, Herramientas de programaci\u00f3n m\u00f3vil I].','Archivo: Instrucciones - Desarrollo de una aplicacion movil.pdf.'),
('Subgrupo 12. (2026). Entrega 2 - Semana 5: Implementaci\u00f3n Android de NutriMaster [Especificaci\u00f3n acad\u00e9mica del equipo].','Archivo: Entrega_02_Semana5_NutriMaster.docx.pdf.'),
('Subgrupo 12. (2026). NutriMaster: Aplicaci\u00f3n M\u00f3vil de Recetario Saludable Interactivo [Entrega 1].','Archivo: Entrega Sem 3 - Prog M\u00f3vil 1 - Subgrupo 12.docx.')]
for text,url in refs:
 p=para(text+' '+url);p.paragraph_format.left_indent=Inches(.25);p.paragraph_format.first_line_indent=Inches(-.25)
note('Documentaci\u00f3n t\u00e9cnica consultada el 29 de septiembre de 2026. Los ejemplos del documento inicial se utilizan como contrato de nombres; los bloques transcritos pertenecen al c\u00f3digo de NutriMaster 2.1.0.')

# All remaining application Java, XML, data, build configuration and test sources.
chapter('Anexo A. Clases Java complementarias','anexo_a')
para('C\u00f3digo completo de las capas y utilidades referenciadas en los apartados anteriores. Estos archivos no reemplazan los fragmentos; contienen estado compartido, editores, colecciones, adaptadores, acceso a datos, validaci\u00f3n y modelos.')
extra_java=sorted((P/'app/src/main/java').rglob('*.java'))
idx=0
for path in extra_java:
 rel=path.relative_to(P).as_posix()
 if rel in emitted:continue
 idx+=1;code(rel,f'A.{idx} {path.name}')

chapter('Anexo B. XML y recursos complementarios','anexo_b')
para('Se incluyen todos los XML restantes de res/: di\u00e1logos, filas, estados, estilos, colores, cadenas, alias y recursos vectoriales. Los archivos binarios WEBP y MP4 permanecen dentro del ZIP y se inventar\u00edan por nombre; no se transcriben como texto.')
idx=0
for path in sorted(RES.rglob('*.xml')):
 rel=path.relative_to(P).as_posix()
 if rel in emitted:continue
 idx+=1;code(rel,f'B.{idx} {path.parent.name}/{path.name}')
heading('B.'+str(idx+1)+' Inventario de recursos binarios',2)
binaries=sorted(p for p in RES.rglob('*') if p.is_file() and p.suffix in ['.webp','.mp4'])
table(['Archivo','Tipo / tama\u00f1o'],[(p.relative_to(RES).as_posix(),p.suffix[1:].upper()+' / '+format(p.stat().st_size/1024,'.1f')+' KiB') for p in binaries],[4.5,2])

chapter('Anexo C. Datos iniciales y configuraci\u00f3n','anexo_c')
para('schema.sql crea las tablas; recipes.json proporciona los datos iniciales de una instalaci\u00f3n nueva. Los archivos Gradle fijan la configuraci\u00f3n de la compilaci\u00f3n. No se incluye local.properties porque la ruta del SDK depende de cada computadora.')
configs=['app/src/main/assets/schema.sql','app/src/main/assets/recipes.json','app/build.gradle','build.gradle','settings.gradle','gradle.properties','gradle/wrapper/gradle-wrapper.properties']
for i,rel in enumerate(configs,1):code(rel,f'C.{i} {Path(rel).name}')

chapter('Anexo D. C\u00f3digo de las pruebas','anexo_d')
para('DomainChecks contiene las aserciones de l\u00f3gica que se ejecutan sin Android. DomainTest adapta esas comprobaciones a JUnit. Las tres clases de androidTest se incluyen para su ejecuci\u00f3n en el dispositivo; su presencia no significa que hayan sido aprobadas en este entorno.')
paths=sorted(p for p in (P/'app/src').rglob('*.java') if '/main/' not in p.as_posix())
for i,path in enumerate(paths,1):code(path.relative_to(P).as_posix(),f'D.{i} {path.name}')

# Fill the table of contents with bookmarks, not speculative page numbers.
for title,key in sections:
 p=doc.add_paragraph();p.paragraph_format.space_after=Pt(5);p.paragraph_format.line_spacing=1.1
 link=OxmlElement('w:hyperlink');link.set(qn('w:anchor'),key)
 run=OxmlElement('w:r');rpr=OxmlElement('w:rPr');color=OxmlElement('w:color');color.set(qn('w:val'),NAVY);rpr.append(color);run.append(rpr)
 txt=OxmlElement('w:t');txt.text=title;run.append(txt);link.append(run);p._p.append(link)
 toc_marker._p.addprevious(p._p)
toc_marker._p.getparent().remove(toc_marker._p)

# Confirm every text source in the generated HTML export has its own exact DOCX block.
expected={p.relative_to(P).as_posix() for p in (P/'app/src').rglob('*') if p.is_file() and p.suffix in ['.java','.xml','.json','.sql']}
expected.update(['app/build.gradle','build.gradle','settings.gradle','gradle.properties','gradle/wrapper/gradle-wrapper.properties'])
assert emitted == expected, {'missing': sorted(expected-emitted),'extra':sorted(emitted-expected)}
doc.save(OUT)
(P/'docs/FUENTES_DOCUMENTO.json').write_text(json.dumps({'version':'2.1.0','document':OUT.name,'blocks':manifest},indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
print('Generated',OUT,'with',len(manifest),'complete source files,',sum(x['lines'] for x in manifest),'source lines.')
