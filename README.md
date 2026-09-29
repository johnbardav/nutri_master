# NutriMaster

Hola, bienvenido a **NutriMaster**. Este proyecto es una aplicación móvil de recetario saludable para Android, desarrollada como parte de la asignatura **Herramientas de Programación Móvil I** del **Politécnico Grancolombiano**.

La idea de la aplicación es que puedas consultar recetas, revisar ingredientes, ver imágenes y videos, cargar contenido web y ejecutar diferentes acciones sobre las recetas desde una sola pantalla.

**Versión del proyecto:** `2.1.0`  
**Nombre de la aplicación:** `NutriMaster`  
**Paquete:** `com.poligran.nutrimaster`  
**Lenguaje:** Java  
**Interfaz:** XML  
**Persistencia:** SQLite

## ¿Cómo funciona NutriMaster?

La aplicación trabaja con una estructura de doble panel.

A la izquierda permanece el menú principal y, a la derecha, cambia el contenido según la opción que selecciones. De esta forma no es necesario abrir una pantalla completamente nueva cada vez.

El diseño conserva una distribución aproximada de **30 % para el menú** y **70 % para el contenido**, con una apariencia en tonos azul oscuro.

Las cinco secciones principales son:

| Sección | ¿Qué puedes hacer? |
| --- | --- |
| **Perfil** | Consultar y editar información personal, estudios, experiencia, biografía y fotografía. |
| **Fotos** | Consultar la biblioteca de recetas, buscar, filtrar, seleccionar una receta y ver su imagen, descripción, ingredientes y preparación. |
| **Video** | Reproducir videos relacionados con la receta seleccionada, pausar la reproducción y utilizar los controles del reproductor. |
| **Web** | Escribir una dirección web y visualizar su contenido dentro de la aplicación mediante un WebView. |
| **Botones** | Guardar recetas, marcar favoritas, calcular información por porciones, escribir notas, compartir y ejecutar otras acciones. |

Las recetas **no aparecen en el menú lateral**. Se seleccionan desde la sección **Fotos**. Desde Video y Botones también puedes regresar a Fotos para elegir otra receta.

La aplicación incluye recetas iniciales y permite crear, editar y eliminar recetas propias.

También diferencia entre **recetas guardadas** y **recetas favoritas**. Una receta puede estar en una categoría sin estar necesariamente en la otra.

En la sección Botones se mantienen además los ejemplos académicos **Mostrar receta**, **Mostrar mensaje** y **Limpiar**, junto con las funcionalidades adicionales de la aplicación.

## ¿Dónde se guarda la información?

NutriMaster utiliza **SQLite**, que viene integrado en Android.

Esto significa que no necesitas instalar MySQL, PostgreSQL ni ningún programa adicional para manejar la base de datos.

La información se guarda localmente en el dispositivo, incluyendo datos como recetas, favoritos, elementos guardados, notas, compras, historial y perfil.

Si desinstalas la aplicación o borras manualmente sus datos desde Android, esa información local también se elimina.

La aplicación incluye recursos locales para que varias funciones puedan probarse sin conexión. La sección Web sí necesita internet para visitar páginas externas.

Los valores nutricionales incluidos son demostrativos y la calculadora trabaja a partir de los datos registrados para cada receta y la cantidad de porciones indicada.

## ¿Cómo está organizado el proyecto?

La aplicación utiliza una `MainActivity` y varios fragmentos.

`MenuFragment` permanece a la izquierda y comunica la opción seleccionada a `MainActivity` mediante `OnOpcionSeleccionadaListener`.

`MainActivity` recibe el evento en `onOpcionSeleccionada()` y llama al método correspondiente:

```text
mostrarPerfil()
mostrarFotos()
mostrarVideo()
mostrarWeb()
mostrarBotones()
```

Después, `cambiarContenido()` reemplaza únicamente el fragmento del panel derecho.

Los nombres principales del código se mantienen alineados con la entrega académica:

```text
MainActivity.java
MenuFragment.java
PerfilFragment.java
FotosFragment.java
VideoFragment.java
WebFragment.java
BotonesFragment.java
```

Cada fragmento principal tiene además su archivo XML correspondiente:

```text
activity_main.xml
fragment_menu.xml
fragment_perfil.xml
fragment_fotos.xml
fragment_video.xml
fragment_web.xml
fragment_botones.xml
```

Algunos ejemplos de nombres utilizados en la interfaz y en Java son:

```text
contenedorMenu
contenedorContenido
btnPerfil
btnFotos
btnVideo
btnWeb
btnBotones
imgPerfil
txtNombre
lstFotos
imgFotoDetalle
txtDescripcionFoto
videoView
btnReproducir
btnPausar
edtUrl
btnCargar
webView
edtReceta
btnMostrarReceta
btnMostrarMensaje
btnLimpiar
```

También se conservan métodos como:

```text
onOpcionSeleccionada()
mostrarPerfil()
mostrarFotos()
mostrarVideo()
mostrarWeb()
mostrarBotones()
cambiarContenido()
reproducirVideo()
pausarVideo()
cargarPagina()
mostrarReceta()
mostrarMensaje()
limpiar()
```

## Estructura principal del repositorio

Esta es una vista resumida del proyecto:

```text
NutriMaster/
|-- README.md
|-- app/
|   |-- build.gradle
|   `-- src/
|       |-- main/
|       |   |-- AndroidManifest.xml
|       |   |-- java/com/poligran/nutrimaster/
|       |   |   |-- MainActivity.java
|       |   |   |-- MenuFragment.java
|       |   |   |-- PerfilFragment.java
|       |   |   |-- FotosFragment.java
|       |   |   |-- VideoFragment.java
|       |   |   |-- WebFragment.java
|       |   |   |-- BotonesFragment.java
|       |   |   |-- data/
|       |   |   |-- domain/
|       |   |   |-- model/
|       |   |   |-- ui/
|       |   |   `-- util/
|       |   |-- assets/
|       |   |   |-- recipes.json
|       |   |   `-- schema.sql
|       |   `-- res/
|       |       |-- layout/
|       |       |-- drawable/
|       |       |-- drawable-nodpi/
|       |       |-- raw/
|       |       `-- values/
|       |-- test/
|       `-- androidTest/
|-- tools/
|-- gradle/wrapper/
|-- build.gradle
|-- settings.gradle
|-- gradle.properties
|-- gradlew
|-- gradlew.bat
`-- .gitignore
```

Si quieres cambiar una pantalla, normalmente debes comenzar por su clase `Fragment` y su archivo XML.

Si quieres revisar la persistencia o SQLite, entra en `data/`.

Los modelos principales están en `model/`, la lógica de cálculos y validaciones en `domain/`, y los adaptadores y componentes compartidos de interfaz en `ui/`.

## Estado actual del proyecto

El proyecto contiene la estructura, lógica, recursos y funcionalidades principales de NutriMaster.

Se han realizado validaciones sobre nombres de clases, identificadores, recursos, lógica Java y estructura del proyecto.

Aun así, antes de entregar o publicar una versión final, lo recomendable es abrir esta versión en Android Studio, sincronizar Gradle, compilarla y recorrer manualmente todas las pantallas en un dispositivo o emulador.

También es importante comprobar el comportamiento de SQLite después de cerrar y volver a abrir la aplicación, así como probar Video y Web en el dispositivo donde se vaya a presentar.

## Equipo

**Subgrupo 12 - Ingeniería de Software - Politécnico Grancolombiano**

- Cristian Camilo Arango Giraldo
- Cristian Augusto Acuña Hernández
- Yony Arbey Rúa Jiménez
- Darwin Javier Gamba Sánchez
- John Mario Barrientos David

**Docente:** José León León

---

# ¿Cómo instalar y ejecutar el proyecto?

Esta parte es sencilla. Si acabas de descargar el repositorio y quieres ponerlo a funcionar, sigue estos pasos.

## Primero, revisa las versiones

La versión actual del proyecto fue preparada con esta configuración:

| Herramienta o componente | Versión |
| --- | --- |
| Android Studio | **Quail 4 - 2026.1.4 Patch 1** |
| Runtime/JBR de Android Studio | **25.0.3** |
| Lenguaje del proyecto | **Java** |
| Nivel del código Java | **17** |
| Android Gradle Plugin | **9.4.0** |
| Gradle | **9.6.0** |
| `compileSdk` | **37** |
| `targetSdk` | **37** |
| Android Build Tools | **36.0.0** |
| `minSdk` | **24 - Android 7.0 Nougat** |
| Base de datos | **SQLite** |

No necesitas instalar Gradle de forma global. El proyecto utiliza el wrapper incluido.

Tampoco necesitas instalar SQLite por separado porque forma parte de Android.

## 1. Descarga el proyecto

Si estás en GitHub, pulsa:

```text
Code > Download ZIP
```

Después descomprime el archivo en una carpeta de tu computador.

Si prefieres usar Git, también puedes clonar el repositorio normalmente.

## 2. Abre la carpeta correcta en Android Studio

Abre Android Studio y selecciona:

```text
Open
```

Busca la carpeta raíz del proyecto, es decir, la carpeta donde encuentras:

```text
settings.gradle
build.gradle
gradlew
gradlew.bat
app/
```

Abre esa carpeta completa.

No abras solamente la carpeta `app`.

## 3. Espera la sincronización de Gradle

Cuando Android Studio abra el proyecto, normalmente comenzará la sincronización de Gradle.

La primera vez puede tardar un poco porque necesita descargar dependencias.

Si Android Studio te pregunta si deseas confiar en el proyecto, confirma que sí si lo descargaste de este repositorio.

## 4. Revisa el SDK de Android

En Android Studio abre:

```text
Tools > SDK Manager
```

En **SDK Platforms**, comprueba que tengas instalada la plataforma utilizada por el proyecto.

Para esta versión se utiliza:

```text
Android SDK Platform 37
```

Después entra en **SDK Tools** y revisa que tengas instalados al menos:

```text
Android SDK Build-Tools 36.0.0
Android SDK Platform-Tools
Android Emulator
```

El emulador solo es necesario si no vas a probar la aplicación en un teléfono físico.

## 5. Revisa el JDK de Gradle

En Windows puedes revisar esto desde:

```text
File > Settings
> Build, Execution, Deployment
> Build Tools
> Gradle
```

En **Gradle JDK**, utiliza preferiblemente el JBR incluido con Android Studio.

En el entorno utilizado para este proyecto corresponde a JBR 25.

El código fuente de la aplicación se mantiene en Java 17.

## 6. Elige dónde ejecutar la aplicación

Tienes dos opciones.

### Opción A: usar un emulador

Abre:

```text
Tools > Device Manager
```

Crea un dispositivo virtual y selecciona una imagen de Android compatible.

El proyecto admite como mínimo:

```text
Android 7.0 Nougat - API 24
```

Puedes utilizar API 24 o una versión posterior.

### Opción B: usar un teléfono Android

En el teléfono activa las **Opciones de desarrollador** y luego la **Depuración USB**.

Conecta el teléfono al computador mediante USB y acepta el mensaje de autorización que aparece en el dispositivo.

Cuando Android Studio reconozca el teléfono, aparecerá en la lista de dispositivos disponibles.

## 7. Ejecuta NutriMaster

En la parte superior de Android Studio selecciona el módulo:

```text
app
```

Luego selecciona el emulador o teléfono y pulsa **Run**, el botón con el triángulo verde.

Android Studio compilará el proyecto, instalará la aplicación y tratará de abrir NutriMaster automáticamente.

## 8. Haz una prueba rápida

Cuando abra la aplicación, puedes hacer este recorrido:

1. Entra en **Perfil** y comprueba que se muestre la información.
2. Entra en **Fotos** y selecciona una receta.
3. Revisa que aparezcan su imagen y descripción.
4. Entra en **Video** y prueba Reproducir y Pausar.
5. Entra en **Web**, escribe una URL y pulsa Cargar.
6. Entra en **Botones** y prueba Mostrar receta, Mostrar mensaje y Limpiar.
7. Prueba también guardar una receta, marcarla como favorita y utilizar el cálculo por porciones.
8. Cierra la aplicación y vuelve a abrirla para comprobar que los datos guardados continúan allí.

## Si algo no funciona

No te preocupes. Normalmente los primeros problemas tienen que ver con el SDK, Gradle o Java.

### Android Studio no encuentra el SDK

Abre SDK Manager y revisa que la plataforma esté instalada correctamente.

Evita copiar el archivo `local.properties` desde otro computador, porque allí normalmente se guarda una ruta local distinta para cada máquina.

### Gradle muestra un problema con Java

Revisa **Gradle JDK** en la configuración de Android Studio y selecciona el JBR incluido con el IDE.

### No se descargan las dependencias

Comprueba que tengas conexión a internet y vuelve a sincronizar el proyecto.

Puedes usar:

```text
File > Sync Project with Gradle Files
```

### Android Studio presenta un problema con el wrapper

El proyecto incluye herramientas auxiliares para instalar el wrapper oficial de Gradle.

En Windows puedes ejecutar desde la raíz del proyecto:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\install-official-wrapper.ps1
```

En macOS o Linux:

```bash
sh tools/install-official-wrapper.sh
```

Después vuelve a Android Studio y sincroniza nuevamente.

---

Si llegaste hasta aquí y NutriMaster abrió correctamente, ya tienes el proyecto listo para comenzar a probarlo, modificarlo y continuar iterando sobre él.
