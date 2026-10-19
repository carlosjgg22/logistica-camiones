# Despachos: app Android (Kotlin + Jetpack Compose)

App offline-first para despacho de combustible. Todo se guarda primero en el teléfono (Room)
y se sincroniza con el servidor en los dos sentidos cuando hay señal (WorkManager).

## Abrir y correr
1. Android Studio (Koala o más nuevo): File > Open > carpeta `FlotaDespacho`.
2. Deja que sincronice Gradle.
3. Run en un emulador o teléfono con Android 8.0+.

Datos de prueba: operadores con cédula `12345678` (Juan), `87654321` (María) o `11223344` (Pedro).
Oficina (consulta): PIN `1234`.

Si ya habías instalado la versión anterior, la base local se recrea sola (cambió el esquema)
y los datos de prueba se cargan de nuevo.

## Roles
**Oficina (PIN)**: un menú de íconos con 7 módulos.
1. **Operadores**: alta y edición (nombre, apellido, teléfono, cédula). Muestra quién está
   disponible o en viaje, en qué unidad está y un botón para llamar.
2. **Viajes**: alta y edición (despacho y ruta, destino, unidad, fecha de salida, de llegada y de
   entrega del producto). La duración se calcula sola (salida a llegada).
3. **Programación**: los viajes en curso (programados que no salieron y en ruta). El botón
   **Programar** le asigna a un viaje la unidad, el tipo de carga y la fecha.
4. **Unidades**: vehículo + tanque + operador. Cada unidad conformada aparece en la lista y se
   puede asignar a un viaje. Al deshacerla, sus partes quedan libres para armar otra.
5. **Rutas**: número de despacho, recorrido y número de factura.
6. **Tanques**: matrícula, volumen, compartimientos y tipo de combustible.
7. **Vehículos**: matrícula, descripción y tipo de vehículo.

Tocar una tarjeta abre su edición. Los tipos de vehículo y de combustible son fijos (vienen del servidor).

**Operador (cédula)**
- Ve su próximo viaje programado, con la unidad que le asignó la oficina.
- Registra la salida, después la llegada y por último la entrega del producto.
- Las horas vienen en "ahora" y se pueden corregir con un reloj si se olvidó de registrar a tiempo.
  No se aceptan horas futuras ni una llegada anterior a la salida.

## Por qué hace falta Firebase
Cada teléfono tiene su propia base local. Para que el usuario de consulta vea a los operadores
desde otro teléfono, los datos tienen que pasar por Cloud Firestore. Sin `app/google-services.json`
la app compila y funciona, pero cada teléfono solo ve lo suyo.

## Conectar Firebase
1. En https://console.firebase.google.com crea un proyecto.
2. Agrega una app **Android** con el paquete `com.flota.despacho` y descarga `google-services.json`.
   Ponlo en `app/google-services.json`. No hace falta tocar nada más del código.
3. **Authentication > Método de acceso**: activa **Anónimo**.
4. **Firestore Database**: créala (modo producción) y en **Reglas** pega el contenido de `firestore.rules`.
5. Recompila e instala. La primera vez que un teléfono sincroniza con Firestore vacío, sube los
   datos de prueba (operadores, unidades, rutas…). Después edítalos desde la consola de Firebase.

## Cómo se guardan los datos en Firestore
Una colección por tabla, con sufijo `_v2`: `tiposVehiculo_v2`, `tiposCombustible_v2`, `operadores_v2`,
`vehiculos_v2`, `tanques_v2`, `unidades_v2`, `rutas_v2`, `viajes_v2`, `programaciones_v2`. El id de cada documento es el UUID del registro y
sus campos son los mismos de `data/db/Entidades.kt`, más `servidorTs` (hora del servidor).

- Si cargas registros a mano en la consola, incluye **todos** los campos de la entidad y
  `servidorTs` de tipo timestamp; sin `servidorTs` los teléfonos no lo descargan.
- Los ids son UUID generados en el teléfono, así que reenviar un registro por mala señal no
  duplica nada.
- Las colecciones sin sufijo (`operadores`, `viajes`…) son de la versión anterior, con otra
  estructura. La app ya no las usa: puedes borrarlas desde la consola.
- Ojo: dos teléfonos sin señal podrían usar la misma unidad a la vez; Firestore no lo impide.
  Si pasa, la oficina lo verá en la lista de viajes.

## Estructura
```
data/db/Entidades.kt     Tablas Room y vistas con JOIN
data/db/FlotaDao.kt      Consultas (disponibilidad, unidades, viajes en curso)
data/FlotaRepository.kt  Validaciones, programar, salida/llegada/entrega, sincronizar, demo
sync/                    Firestore (FirebaseSync) + SyncWorker
ui/operador/             Salida, llegada y entrega del producto
ui/oficina/              Menú de íconos y un archivo por módulo
ui/componentes/          Campo de hora, selector, botones, tarjetas
```
