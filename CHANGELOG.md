# Cambios - NutriMaster 2.1.0

Base de esta revision: ZIP NutriMaster 2.0.0 ReferenciasCompletas. Version de aplicacion 2.1.0, versionCode 5. Es un proyecto completo, no un parche.

## Ajustes de nombres y referencias

- `View vista` en los seis `onCreateView`, conservando referencias de clase para la liberacion de vistas.
- `String[] recetas`, `String[] descripciones`, `int[] imagenes` y `ArrayAdapter<String> adapter` en FotosFragment. El adaptador conserva miniaturas, SQLite y filtros.
- Variables locales `adapter`, `controles` y `uri` en los metodos del documento; parametros de la lista `(parent, view, position, id)` y metodo `onItemClick`.
- Textos y titulos tomados de las figuras del documento, incluyendo Perfil profesional, Video: preparacion saludable y Se mostro el mensaje de ejemplo.
- Cabecera con el nombre completo del recetario y botones sin transformacion automatica a mayusculas.
- LinearLayout horizontal con contenedorMenu/contenedorContenido y pesos 3/7 para conservar el 30/70 pedido.
- Auditoria del arbol de sintaxis Java: declara y conecta las variables reales, no nombres agregados solo a comentarios. Contrasta las 98 declaraciones extraidas del PDF y los eventos de su numeral 7.
- Nuevas pruebas Android de etiquetas, nombres y metodos, incluidas para ejecucion en el dispositivo.

## Conservado

MainActivity, MenuFragment y los cinco Fragment de contenido con sus siete XML. Interfaz OnOpcionSeleccionadaListener, metodos mostrarPerfil/mostrarFotos/mostrarVideo/mostrarWeb/mostrarBotones/cambiarContenido, reproducirVideo/pausarVideo, cargarPagina y mostrarReceta/mostrarMensaje/limpiar.

Las recetas siguen en Fotos, no en el menu. Se conservan las nueve recetas y sus recursos, SQLite, CRUD, notas, favoritos y guardados independientes, calculadora, compartir, compras, historial, exportacion JSON, perfil editable, videos locales/propios, WebView y todas las funciones del proyecto 2.0.

No se modificaron el paquete com.poligran.nutrimaster, el azul corporativo, minSdk 24 ni las versiones de Gradle/SDK. No se copian literalmente los datos de ejemplo del perfil y los platos de las maquetas: los datos de la app siguen siendo editables.

## Documento actualizado

Se incluye Entrega_02_Semana5_NutriMaster_v2_1_0.docx con 78 archivos fuente completos y todas las secciones revisadas. Los archivos del ZIP son la fuente de cada bloque. Se agrega un generador reproducible y un comprobador de identidad documental; no intervienen en la app Android.

## Validacion

Consultar docs/validacion/RESUMEN.md. Los controles de nombres, XML, SQL y sintaxis no sustituyen la compilacion ni las pruebas Android. No se entrega un APK acreditado.
