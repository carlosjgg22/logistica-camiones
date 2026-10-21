import { useCallback, useEffect, useRef, useState } from 'react'
import { signOut, type User } from 'firebase/auth'
import { auth } from './firebase'
import { averiasNuevas, useDatos } from './datos'
import { erroresEscritura } from './escribir'
import { cantidad, hora } from './formato'
import { Icono, MensajeError, Snackbar } from './componentes/base'
import { Averias, Operadores, Programacion, Rutas, Tanques, Unidades, Vehiculos, Viajes } from './modulos/listas'
import {
  FormOperador, FormProgramar, FormRuta, FormTanque, FormUnidad, FormVehiculo, FormViaje,
} from './modulos/formularios'
import type { Averia, OperadorEstado, Ruta, TanqueDetalle, VehiculoDetalle, ViajeDetalle } from './tipos'

export type Modulo = 'operadores' | 'viajes' | 'programacion' | 'unidades' | 'rutas' | 'tanques' | 'vehiculos' | 'averias'

const MODULOS: { clave: Modulo; titulo: string; icono: string; accion: string | null }[] = [
  { clave: 'operadores', titulo: 'Operadores', icono: 'groups', accion: 'Agregar operador' },
  { clave: 'viajes', titulo: 'Viajes', icono: 'route', accion: 'Agregar viaje' },
  { clave: 'programacion', titulo: 'Programación', icono: 'edit_calendar', accion: 'Programar' },
  { clave: 'unidades', titulo: 'Unidades', icono: 'hub', accion: 'Conformar unidad' },
  { clave: 'rutas', titulo: 'Rutas', icono: 'map', accion: 'Agregar ruta' },
  { clave: 'tanques', titulo: 'Tanques', icono: 'propane_tank', accion: 'Agregar tanque' },
  { clave: 'vehiculos', titulo: 'Vehículos', icono: 'local_shipping', accion: 'Agregar vehículo' },
  { clave: 'averias', titulo: 'Averías', icono: 'warning', accion: null },
]

/** Formulario abierto: módulo y registro a editar (null = alta). */
type Edicion =
  | { modulo: 'operadores'; item: OperadorEstado | null }
  | { modulo: 'viajes'; item: ViajeDetalle | null }
  | { modulo: 'programacion' | 'unidades'; item: null }
  | { modulo: 'rutas'; item: Ruta | null }
  | { modulo: 'tanques'; item: TanqueDetalle | null }
  | { modulo: 'vehiculos'; item: VehiculoDetalle | null }

// El módulo elegido se recuerda en este navegador (si el navegador lo permite)
const leerModulo = (): Modulo => {
  try {
    const m = localStorage.getItem('modulo') as Modulo | null
    if (m && MODULOS.some((x) => x.clave === m)) return m
  } catch { /* sin almacenamiento: se usa el de siempre */ }
  return 'programacion'
}

const iniciales = (u: User) => {
  const base = u.displayName?.trim() || u.email?.split('@')[0] || '?'
  const partes = base.split(/[\s._-]+/).filter(Boolean)
  return (partes.length > 1 ? partes[0][0] + partes[1][0] : base.slice(0, 2)).toUpperCase()
}

export function Panel({ usuario }: { usuario: User }) {
  const datos = useDatos()
  const datosRef = useRef(datos)
  datosRef.current = datos
  const [modulo, setModulo] = useState<Modulo>(leerModulo)
  const [edicion, setEdicion] = useState<Edicion | null>(null)
  const [snack, setSnack] = useState<string | null>(null)
  const cerrarSnack = useCallback(() => setSnack(null), [])
  const cerrar = useCallback(() => setEdicion(null), [])

  useEffect(() => {
    try { localStorage.setItem('modulo', modulo) } catch { /* sin almacenamiento */ }
  }, [modulo])

  useEffect(() => {
    const oir = (e: Event) => setSnack((e as CustomEvent<string>).detail)
    erroresEscritura.addEventListener('error', oir)
    return () => erroresEscritura.removeEventListener('error', oir)
  }, [])

  // Aviso emergente cuando un operador reporta una avería (queda hasta que se cierra o se abre el módulo)
  const [alerta, setAlerta] = useState<string | null>(null)
  useEffect(() => {
    const oir = (e: Event) => {
      const a = (e as CustomEvent<Averia>).detail
      const o = datosRef.current.datos.operadores.find((x) => x.id === a.idOperador)
      setAlerta(`Avería ${a.gravedad === 'GRAVE' ? 'GRAVE' : 'leve'} reportada${o ? ` por ${o.nombre} ${o.apellido}` : ''}: ${a.codigo} ${a.punto}. “${a.observacion}”`)
    }
    averiasNuevas.addEventListener('nueva', oir)
    return () => averiasNuevas.removeEventListener('nueva', oir)
  }, [])
  const abiertas = datos.averias.filter((a) => !a.resuelta).length

  const resumen = (m: Modulo) => {
    switch (m) {
      case 'operadores': return cantidad(datos.operadores.length, 'operador', 'operadores')
      case 'viajes': return cantidad(datos.viajes.length, 'viaje', 'viajes')
      case 'programacion': return `${datos.enCurso.length} en curso`
      case 'unidades': return cantidad(datos.unidades.length, 'unidad', 'unidades')
      case 'rutas': return cantidad(datos.rutas.length, 'ruta', 'rutas')
      case 'tanques': return cantidad(datos.tanques.length, 'tanque', 'tanques')
      case 'vehiculos': return cantidad(datos.vehiculos.length, 'vehículo', 'vehículos')
      case 'averias': { const n = datos.averias.filter((a) => !a.resuelta).length; return n === 0 ? 'Sin abiertas' : cantidad(n, 'abierta', 'abiertas') }
    }
  }

  const actual = MODULOS.find((m) => m.clave === modulo)!
  const abrirAlta = () => setEdicion({ modulo, item: null } as Edicion)
  const listo = (texto: string) => setSnack(texto)
  const props = { datos, onListo: listo, onCerrar: cerrar }

  return (
    <div className="panel">
      <aside className="lateral">
        <div className="marca"><b>LogisticaGarcía</b><span>Oficina</span></div>
        <div className="pildora">
          <Icono nombre="sync" />
          {datos.ultimaSync ? `Datos actualizados a las ${hora(datos.ultimaSync)}` : 'Conectando con el servidor…'}
        </div>
        <nav className="nav">
          {MODULOS.map((m) => (
            <button key={m.clave} className={m.clave === modulo ? 'activo' : ''} onClick={() => setModulo(m.clave)}>
              <Icono nombre={m.icono} />
              <span><span className="titulo">{m.titulo}</span><span className="resumen">{resumen(m.clave)}</span></span>
              {m.clave === 'averias' && abiertas > 0 && <span className="contador" aria-label={`${abiertas} averías abiertas`}>{abiertas}</span>}
            </button>
          ))}
        </nav>
        <div className="usuario">
          <span className="avatar">{iniciales(usuario)}</span>
          <div className="datos"><b>Administrador</b><span title={usuario.email ?? ''}>{usuario.email}</span></div>
          <button className="boton-icono" onClick={() => signOut(auth)} aria-label="Cerrar sesión" title="Cerrar sesión">
            <Icono nombre="logout" />
          </button>
        </div>
      </aside>

      <main className="principal">
        <header className="encabezado">
          <Icono nombre={actual.icono} />
          <h1>{actual.titulo}</h1>
          {actual.accion && <button className="boton boton-accion" onClick={abrirAlta}><Icono nombre="add" relleno />{actual.accion}</button>}
        </header>
        <div className="contenido">
          {datos.error && <MensajeError texto={datos.error} />}
          {modulo === 'programacion' && <Programacion datos={datos} />}
          {modulo === 'viajes' && <Viajes datos={datos} onEditar={(item) => setEdicion({ modulo: 'viajes', item })} />}
          {modulo === 'operadores' && <Operadores datos={datos} onEditar={(item) => setEdicion({ modulo: 'operadores', item })} />}
          {modulo === 'unidades' && <Unidades datos={datos} onListo={listo} />}
          {modulo === 'rutas' && <Rutas datos={datos} onEditar={(item) => setEdicion({ modulo: 'rutas', item })} />}
          {modulo === 'tanques' && <Tanques datos={datos} onEditar={(item) => setEdicion({ modulo: 'tanques', item })} />}
          {modulo === 'vehiculos' && <Vehiculos datos={datos} onEditar={(item) => setEdicion({ modulo: 'vehiculos', item })} />}
          {modulo === 'averias' && <Averias datos={datos} quien={usuario.email} onListo={listo} />}
        </div>
      </main>

      {edicion?.modulo === 'operadores' && <FormOperador {...props} item={edicion.item} />}
      {edicion?.modulo === 'viajes' && <FormViaje {...props} item={edicion.item} />}
      {edicion?.modulo === 'programacion' && <FormProgramar {...props} />}
      {edicion?.modulo === 'unidades' && <FormUnidad {...props} />}
      {edicion?.modulo === 'rutas' && <FormRuta {...props} item={edicion.item} />}
      {edicion?.modulo === 'tanques' && <FormTanque {...props} item={edicion.item} />}
      {edicion?.modulo === 'vehiculos' && <FormVehiculo {...props} item={edicion.item} />}

      <Snackbar texto={snack} onFin={cerrarSnack} />
      {alerta && (
        <div className="snackbar alerta" role="alert">
          <Icono nombre="warning" relleno />
          <span>{alerta}</span>
          <button onClick={() => { setModulo('averias'); setAlerta(null) }}>Ver</button>
          <button onClick={() => setAlerta(null)} aria-label="Cerrar">✕</button>
        </div>
      )}
    </div>
  )
}
