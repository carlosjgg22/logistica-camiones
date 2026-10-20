# Handoff: FlotaDespacho — rediseño app Android + panel de oficina para PC

Repo destino: `carlosjgg22/logistica-camiones`, rama `rediseno-menu-iconos` (Kotlin + Jetpack Compose + Room + Firestore).

## Overview
Despacho de combustible de **LogisticaGarcía**. Tres superficies que comparten los mismos datos (Room local → Firestore, colecciones `*_v2`):
1. **App Android · Operador** (entra con cédula): ve su viaje programado y registra **salida → llegada → entrega del producto**.
2. **App Android · Administrador** (rol Oficina): menú de 7 módulos. **Cambio:** el PIN fijo `1234` se reemplaza por correo + contraseña de Firebase Auth.
3. **Panel web para PC** (NUEVO): los mismos 7 módulos en escritorio, mismo usuario admin, instalable como PWA.

## About the Design Files
Los archivos `.dc.html` de este paquete son **referencias de diseño hechas en HTML**: prototipos que muestran el aspecto y el comportamiento buscados, no código para producción. La tarea es **recrearlos**:
- En la app Android, con Compose/Material 3 y los componentes que ya existen en `ui/componentes/Componentes.kt`.
- En el panel web, con un framework nuevo. Recomendado: **Vite + React + TypeScript + Firebase JS SDK (Auth + Firestore)**, como PWA y publicado en Firebase Hosting.

Se abren directo en el navegador (necesitan `support.js` en la misma carpeta).

## Fidelity
**Alta fidelidad.** Colores, tipografía, espaciado, textos e interacciones son los finales. Los colores salen de `ui/theme/Tema.kt`. En Android se puede seguir usando la tipografía de sistema (Roboto); en el diseño se usa IBM Plex Sans solo como referencia de peso y tamaño.

---

## Cambios en la app Android

### 1. Íconos → Material Symbols Rounded
- Quitar `material-icons-extended`. Descargar los íconos desde fonts.google.com/icons en **Rounded, peso 500, opsz 24**, como Android Vector Drawable, y guardarlos en `res/drawable/`.
- **Rellenos (FILL 1):** íconos dentro de botones de acción (`login`, `local_shipping` de salida, `flag`, `inventory_2`, `edit_calendar`, `add`), `cloud_done`/`cloud_upload`, `error`, `task_alt`, `call`, y los íconos de módulo dentro del círculo `primaryContainer`.
- **Contorno (FILL 0):** íconos de `Dato` (`place`, `map`, `local_shipping`, `local_gas_station`, `calendar_today`, `timer`), `schedule`, `edit`, `logout`, `refresh`, `arrow_back`, `visibility`.
- Lista usada: groups, route, edit_calendar, hub, map, propane_tank, local_shipping, place, local_gas_station, calendar_today, timer, flag, inventory_2, schedule, edit, cloud_done, cloud_upload, error, task_alt, login, logout, refresh, arrow_back, add, call, apartment, check, check_box, visibility, visibility_off, info, chevron_right, sync, person.

### 2. Operador — `ui/operador/OperadorScreen.kt` (pantallas 3a–3g, interactivo 2a)
La lógica no cambia (OperadorViewModel y FlotaRepository se quedan igual). Cambios de UI:
- **Pie fijo:** el título del paso, el `CampoHora` y el `BotonGrande` van en una zona fija abajo (fuera del `verticalScroll`). Solo hace scroll la tarjeta del viaje. Así el botón siempre se ve.
  - Implementación: `Scaffold(bottomBar = { Column(Modifier.padding(16.dp, 0.dp, 16.dp, 18.dp), spacedBy(10.dp)) { título; CampoHora; BotonGrande } })`.
- **Seguimiento de 3 pasos** al pie de `TarjetaViaje`, debajo de un divisor `#E2E7EE`: tres columnas Salida · Llegada · Entrega, cada una con una barra de 4dp (verde `#2E7D32` si está hecho, `primary` si es el paso actual, `#E2E7EE` si está pendiente), la etiqueta (12sp semibold) y la hora HH:mm en monospace, o "—" si no hay.
- **Carga:** `Dato(local_gas_station, "Carga", "Diésel · 30.000 L en 3 compartimientos")`, armado con `combustible`, `volumenLitros` y `compartimientos` del tanque de la unidad. Hace falta agregar volumen y compartimientos a `ViajeDetalle` (JOIN con tanque).
- La fila "Ruta" se puede ocultar en pantallas bajas para que entre todo.
- **Viaje terminado** (después de la entrega): ícono `task_alt` de 64dp en verde, "Viaje terminado", "Despacho {n} a {destino}. Duración {d} · entrega a las {hh:mm}" y "Cuando la oficina te programe otro viaje aparecerá aquí." Hoy la pantalla pasa directo al estado vacío.
- **Selector de hora:** se usa el `TimePicker` M3 que ya existe. En el diseño se ve como hoja inferior con ▲▼ por hora y minuto y los botones "Usar hora actual", "Cancelar" y "Aceptar". Cualquiera de las dos versiones sirve.
- Botones por paso: Salida → `secondary #C25A00` con ícono `local_shipping`. Llegada → `primary` con `flag`. Entrega → `primary` con `inventory_2`. Alto mínimo 68dp, radio 18dp.
- Textos (ya están en el repo, se mantienen): "Registrar salida", "Hora de salida", "Ahora, HH:mm", "Hoy a las HH:mm", "Toca para cambiar la hora", "¿Registrar salida?", "Hora de salida: la hora actual.", snackbars "Salida registrada. Buen viaje." / "Llegada registrada. Viaje de X h Y min." / "Entrega registrada. Viaje terminado."
- Validaciones (FlotaRepository, sin cambios): "La hora de salida no puede ser posterior a la hora actual.", "La llegada no puede ser anterior a la salida.", "La entrega no puede ser anterior a la salida."

### 3. Login — `ui/login/LoginScreen.kt` (pantallas 3a y 4a)
- Selector segmentado **Operador | Administrador** (alto 40dp, borde `#72787E`, opción elegida con fondo `#D3E4FF`, texto `#001C38` e ícono `check`).
- Operador: campo Cédula (numérico, monospace 22sp, alto 60dp) y botón "Entrar". Errores: "Escribe tu cédula." / "No hay un operador con esa cédula."
- Administrador: Correo, Contraseña (con ícono para mostrar u ocultar), "Mantener la sesión iniciada" y "Entrar". Error: "Correo o contraseña incorrectos." Pie: "El mismo usuario sirve para el panel de la PC."
- Backend: `FirebaseAuth.signInWithEmailAndPassword`. Hoy se usa Auth anónimo; el operador puede seguir así. Hay que guardar el rol en `Sesion.kt`.

### 4. Oficina — `ui/oficina/OficinaScreen.kt` y módulos (pantallas 4b–4g)
- **Menú (4b):** arriba de la grilla, una tarjeta de acceso rápido en `primary` con texto blanco: ícono `local_shipping`, "Ahora" y "{n} en ruta · {m} por salir", que abre Programación. Grilla de 2 columnas, tarjetas de alto mínimo 104dp, círculo de 48dp `primaryContainer` con ícono de 26dp, título 15.5sp bold y resumen 13.5sp `onSurfaceVariant`.
- Las listas de Programación, Viajes, Operadores y Unidades se mantienen como están en el repo (tarjeta + ChipEstado + Secundario). Los textos coinciden con los de los mocks.
- **Programar viaje (4d):** tal como está en `FormularioProgramacion`, más un aviso informativo arriba del botón: "Al programar, {operador} verá el viaje en su teléfono."

### 5. Reglas de Firestore
- El admin (Auth por correo, custom claim `admin: true` o un documento en `admins/{uid}`) puede leer y escribir todo.
- El operador solo puede actualizar `fechaSalida`, `fechaLlegada` y `fechaEntrega` de sus viajes.

---

## Panel web para PC (NUEVO) — pantalla 2b
Mismos datos y mismas reglas. Lee y escribe en las colecciones `tiposVehiculo_v2`, `tiposCombustible_v2`, `operadores_v2`, `vehiculos_v2`, `tanques_v2`, `unidades_v2`, `rutas_v2`, `viajes_v2`, `programaciones_v2`, con los mismos campos de `data/db/Entidades.kt` más `servidorTs` (`serverTimestamp()` al escribir). Los ids son UUID generados en el cliente. Escucha en vivo con `onSnapshot`.

### Login (2b, sin sesión)
Grilla de 2 columnas a 1240px: panel izquierdo flexible en `#0B4F8A` y formulario derecho de 520px en blanco.
- Izquierda (padding 48px, texto blanco): logo (círculo de 48px `#D3E4FF` con `local_shipping`) + "LogisticaGarcía" 18px/700. Abajo: "Oficina de despachos" 40px/700, "Programa viajes, arma unidades y sigue en vivo la salida, llegada y entrega de cada operador." 17px con opacidad .85, y "Mismos datos que la app de Android" 14px con ícono `sync`.
- Derecha (padding 56px, centrado vertical, separación 18px): "Iniciar sesión" 26px/700, "Acceso para administradores de la oficina." 15px `#424850`. Campos de 52px de alto, borde 1.5px `#72787E` (`#BA1A1A` si hay error), radio 12px. "Mantener la sesión iniciada" con `check_box` y "¿Olvidaste tu contraseña?" (link `primary`, usa `sendPasswordResetEmail`). Botón "Entrar" de 56px, radio 16px, `primary`. Enter envía el formulario. Pie: "Los operadores entran desde la app de Android con su cédula."

### Panel (con sesión)
Grilla `248px | 1fr`, fondo `#F4F6F9`.
- **Barra lateral** (blanca, borde derecho `#E2E7EE`): "LogisticaGarcía" 16px/700 `primary` y "Oficina" 13px. Debajo, la píldora "Datos actualizados a las HH:mm" (`#E2E7EE`, 12.5px, ícono `sync`). Ítems de navegación (padding 10×12, radio 12px): ícono de 22px, título 14.5px/600 y resumen 12px ("5 operadores", "3 en curso"…). El ítem activo va en `#D3E4FF` con texto `#001C38`; hover `#EDF0F4`. Abajo, el usuario: avatar de 36px `#15181C` con iniciales, "Administrador", correo y botón `logout`.
- **Encabezado** (68px, blanco, borde inferior): ícono del módulo, título 22px/700 y botón de acción a la derecha (44px, radio 14px, `#D3E4FF`/`#001C38`, ícono `add`): "Agregar operador", "Agregar viaje", "Programar", "Conformar unidad", "Agregar ruta", "Agregar tanque", "Agregar vehículo".
- **Contenido** (padding 22×26, separación 16):
  - **Programación** (módulo por defecto): "Viajes en curso: {n} en ruta, {m} por salir" 20px/700. Grilla de 3 columnas con tarjetas (blancas, radio 14px, borde `#E2E7EE`): "Despacho {n}" 16px/700 + chip de estado, "Destino: …" 16px, unidad 14px y "Salió a las HH:mm, lleva X en ruta" o "Programado para …".
  - **Viajes:** chips de filtro "Todos (n)" más uno por estado (34px, radio 8px; el activo en `#D3E4FF`). Tabla con columnas `96px 1.4fr 1.5fr 90px 76px 76px 76px 100px 120px`: Despacho · Destino · Unidad·operador · Carga · Salida · Llegada · Entrega · Duración · Estado. Filas con padding 12×16, 14px, divisor `#EDF0F4`, hover `#F4F6F9`. Al hacer clic se abre la edición (FormularioViaje).
  - **Operadores:** "{n} disponibles, {m} en viaje", chips Todos/Disponibles/En viaje y tarjetas en 3 columnas: nombre + chip (Disponible verde / En viaje ámbar), "Cédula …", unidad, "Salió a las … rumbo a …, lleva …" y "Llamar al {tel}" (`tel:` link).
  - **Unidades:** tarjetas en 3 columnas: matrícula del vehículo en monospace + chip En ruta/Libre, y tres filas con ícono: vehículo · tanque (volumen, compartimientos, combustible) · operador.
  - **Rutas / Tanques / Vehículos:** tablas simples. Rutas: Despacho · Recorrido · Factura. Tanques: Matrícula · Volumen · Compartimientos · Combustible · Unidad. Vehículos: Matrícula · Descripción · Tipo · Unidad.
- Formularios de alta y edición: mismos campos y validaciones que los `Formulario*` de Compose del repo, en un panel lateral o modal.
- **PWA:** `manifest.webmanifest` (nombre "LogisticaGarcía Oficina", theme `#0B4F8A`, ícono local_shipping) y un service worker básico. Firestore web con `persistentLocalCache` para trabajar sin conexión.

---

## Estados y lógica (iguales en todas las superficies)
- `EstadoViaje` (Entidades.kt): llegada → COMPLETADO; salida → EN_RUTA; tiene programación → PROGRAMADO; si no, SIN_PROGRAMAR.
- Operador "en viaje" = tiene salida y le falta la entrega.
- Duración = llegada − salida. Formato `duracion()`: "X h Y min" o "Y min".
- Litros = `volumenLitros` del tanque de la unidad, formato `es` ("30.000 L"). **El operador no escribe litros.**
- Aviso de sincronización: si `pendientes == 0` → "Todo enviado" (`surfaceVariant`, `cloud_done`). Si no → "Guardado en el teléfono ({n} por enviar). Se envía solo cuando haya señal." (`secondaryContainer`, `cloud_upload`).

## Design tokens (Tema.kt, claro)
- primary `#0B4F8A` · onPrimary `#FFFFFF` · primaryContainer `#D3E4FF` · onPrimaryContainer `#001C38`
- secondary `#C25A00` · secondaryContainer `#FFDCC2` · onSecondaryContainer `#2E1500`
- background/surface `#F4F6F9` · onSurface `#15181C` · surfaceVariant `#E2E7EE` · onSurfaceVariant `#424850` · surfaceContainer `#EDF0F4` · outline `#72787E`
- errorContainer `#FFDAD6` · onErrorContainer `#410002` · error `#BA1A1A`
- Estados: Sin programar `#616870` · Programado `#3949AB` · En ruta `#B35300` · Completado / Disponible `#2E7D32`. Fondo del chip = color al 14% de alfa.
- Radios: chips 99px · campos 12 · tarjetas 14–16 · botón grande 18 · diálogos 28 · snackbar 8.
- Tipografía: títulos de pantalla 22/600, título de paso 20–22/700, dato destacado 22–24/700, cuerpo 16–17, secundario 14, etiqueta 12/500. Horas en monospace.
- Sombra de tarjeta: `0 1px 3px rgba(21,24,28,.12), 0 1px 2px rgba(21,24,28,.06)`.

## Capturas
En `screenshots/` hay una imagen por pantalla, con el mismo id que el prototipo:
- `screenshots/3a-operador-login.png`
- `screenshots/3b-operador-registrar-salida.png`
- `screenshots/3c-operador-corregir-hora.png`
- `screenshots/3d-operador-confirmar-salida.png`
- `screenshots/3e-operador-en-ruta-registrar-llegada.png`
- `screenshots/3f-operador-registrar-entrega.png`
- `screenshots/3g-operador-viaje-terminado.png`
- `screenshots/4a-admin-android-login.png`
- `screenshots/4b-admin-android-menu.png`
- `screenshots/4c-admin-android-programacion.png`
- `screenshots/4d-admin-android-programar-viaje.png`
- `screenshots/4e-admin-android-viajes.png`
- `screenshots/4f-admin-android-operadores.png`
- `screenshots/4g-admin-android-unidades.png`
- `screenshots/2b-pc-login.png`
- `screenshots/2b-pc-login-relleno.png`
- `screenshots/2b-pc-programacion.png`
- `screenshots/2b-pc-viajes.png`
- `screenshots/2b-pc-operadores.png`
- `screenshots/2b-pc-unidades.png`
- `screenshots/2b-pc-rutas.png`
- `screenshots/2b-pc-tanques.png`
- `screenshots/2b-pc-vehiculos.png`
- `screenshots/2c-oficina-movil-menu.png`

## Files
- `FlotaDespacho Rediseno.dc.html`: archivo principal. Ronda 4 = admin en Android (4a–4g), ronda 3 = operador paso a paso (3a–3g), ronda 2 = prototipos interactivos (2a operador, **2b panel PC**, 2c oficina móvil).
- `Operador Pantalla.dc.html`: pantallas estáticas del operador (prop `pantalla`).
- `Oficina Pantalla.dc.html`: pantallas estáticas del admin en Android (prop `pantalla`).
- `support.js`: runtime para abrir los `.dc.html`.

Acceso de prueba en el prototipo: operador con cédula `12345678` (Juan); admin `admin@logisticagarcia.com` / `admin123`.

## Orden sugerido para Claude Code
1. Cambio de íconos en Android.
2. Pie fijo en OperadorScreen, seguimiento de pasos, carga con litros y pantalla de viaje terminado.
3. Login de administrador con Firebase Auth y nuevas reglas de Firestore.
4. Tarjeta "Ahora" en el menú de Oficina.
5. Proyecto `web-oficina/` (Vite + React + TS + Firebase): login y módulo Programación primero, después el resto.
