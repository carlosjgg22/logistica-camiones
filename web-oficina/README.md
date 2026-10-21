# Panel web de oficina (PC)

Los 7 módulos de la oficina en el navegador, con el mismo usuario administrador que la app de Android
y los mismos datos (Firestore, colecciones `*_v2`). Vite + React + TypeScript + Firebase JS SDK.
Se puede instalar como app (PWA) y sigue funcionando sin conexión.

## Probarlo en la PC
1. En la consola de Firebase registra una **app Web** en el proyecto `flota-despacho-3339`
   (Configuración del proyecto > Tus apps > Agregar app > Web) y copia `apiKey` y `appId`.
2. Copia `.env.example` como `.env` y completa esos dos datos.
3. En **Authentication** activa **Correo electrónico/contraseña** y crea el usuario administrador.
4. ```
   cd web-oficina
   npm install
   npm run dev
   ```
   y abre http://localhost:5173. Si cambias el `.env`, corta `npm run dev` y vuelve a arrancarlo.

## Publicarlo (Firebase Hosting)
```
cd web-oficina && npm run build && cd ..
npx firebase-tools deploy --only hosting
```
Queda en `https://flota-despacho-3339.web.app`. Agrega ese dominio en
Authentication > Configuración > Dominios autorizados si no aparece.

## Cómo está armado
```
src/tipos.ts       Registros iguales a data/db/Entidades.kt y nombres de colecciones
src/datos.ts       Lectura en vivo (onSnapshot) y vistas armadas (lo que en la app son JOIN)
src/escribir.ts    Altas y cambios con las mismas validaciones que FlotaRepository.kt
src/Login.tsx      Correo y contraseña de Firebase Auth
src/Panel.tsx      Barra lateral, encabezado y formularios
src/modulos/       Listas y formularios de cada módulo
public/            Manifest, service worker e íconos de la PWA (npm run iconos los regenera)
```
