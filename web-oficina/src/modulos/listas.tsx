import { useEffect, useState, type ReactNode } from 'react'
import type { DatosPanel } from '../datos'
import { deshacerUnidad } from '../escribir'
import { diaRelativo, duracion, hora, litros } from '../formato'
import { ESTADOS, ETIQUETA_ESTADO, type EstadoViaje, type OperadorEstado, type Ruta, type TanqueDetalle, type VehiculoDetalle, type ViajeDetalle } from '../tipos'
import { ChipEstado, ChipLibre, EstadoVacio, Icono } from '../componentes/base'

/** Hora actual que se refresca sola, para "lleva X en ruta". */
function useAhora(cadaMs = 30_000) {
  const [ahora, setAhora] = useState(Date.now())
  useEffect(() => {
    const t = setInterval(() => setAhora(Date.now()), cadaMs)
    return () => clearInterval(t)
  }, [cadaMs])
  return ahora
}

const textoUnidad = (v: ViajeDetalle) =>
  v.vehiculo ? `Unidad ${v.vehiculo} + tanque ${v.tanque}` + (v.operador ? `, ${v.operador}` : '') : 'Sin unidad asignada'

const Hora = ({ ms }: { ms: number | null }) =>
  ms != null ? <span className="mono">{hora(ms)}</span> : <span className="vacio-celda">—</span>

const comp = (n: number) => (n === 1 ? '1 comp.' : `${n} comp.`)

// ---------------- Programación ----------------

export function Programacion({ datos }: { datos: DatosPanel }) {
  const ahora = useAhora()
  const enRuta = datos.enCurso.filter((v) => v.estado === 'EN_RUTA').length
  return (
    <>
      <h2>Viajes en curso: {enRuta} en ruta, {datos.enCurso.length - enRuta} por salir</h2>
      {datos.enCurso.length === 0 ? (
        <EstadoVacio icono="edit_calendar" titulo="No hay viajes en curso" texto='Toca "Programar" para asignarle unidad y carga a un viaje.' />
      ) : (
        <div className="grilla">
          {datos.enCurso.map((v) => (
            <div key={v.id} className="tarjeta">
              <div className="cabeza"><b>Despacho {v.numeroDespacho}</b><ChipEstado estado={v.estado} /></div>
              <span className="linea">Destino: {v.destino}</span>
              <span className="sec">{textoUnidad(v)}</span>
              <span className="sec">
                {v.fechaSalida != null
                  ? `Salió a las ${hora(v.fechaSalida)}, lleva ${duracion(ahora - v.fechaSalida)} en ruta`
                  : `Programado para ${diaRelativo(v.fechaProgramada ?? ahora, ahora)}` + (v.carga ? `, carga ${v.carga}` : '')}
              </span>
            </div>
          ))}
        </div>
      )}
    </>
  )
}

// ---------------- Viajes ----------------

export function Viajes({ datos, onEditar }: { datos: DatosPanel; onEditar: (v: ViajeDetalle) => void }) {
  const [filtro, setFiltro] = useState<EstadoViaje | null>(null)
  const ahora = useAhora()
  const visibles = datos.viajes.filter((v) => filtro == null || v.estado === filtro)
  return (
    <>
      <div className="filtros">
        <button className={'filtro' + (filtro == null ? ' activo' : '')} onClick={() => setFiltro(null)}>Todos ({datos.viajes.length})</button>
        {ESTADOS.map((e) => (
          <button key={e} className={'filtro' + (filtro === e ? ' activo' : '')} onClick={() => setFiltro(filtro === e ? null : e)}>
            {ETIQUETA_ESTADO[e]} ({datos.viajes.filter((v) => v.estado === e).length})
          </button>
        ))}
      </div>
      {visibles.length === 0 ? (
        <EstadoVacio icono="inbox" titulo="No hay viajes aquí" texto='Toca "Agregar viaje" para cargar uno.' />
      ) : (
        <div className="tabla">
          <table>
            <colgroup>
              {['96px', '1.4fr', '1.5fr', '90px', '76px', '76px', '76px', '100px', '120px'].map((w, i) => <col key={i} style={{ width: w }} />)}
            </colgroup>
            <thead>
              <tr><th>Despacho</th><th>Destino</th><th>Unidad · operador</th><th>Carga</th><th>Salida</th><th>Llegada</th><th>Entrega</th><th>Duración</th><th>Estado</th></tr>
            </thead>
            <tbody>
              {visibles.map((v) => (
                <tr key={v.id} onClick={() => onEditar(v)} tabIndex={0} onKeyDown={(e) => { if (e.key === 'Enter') onEditar(v) }}>
                  <td className="mono"><b>{v.numeroDespacho}</b></td>
                  <td>{v.destino}</td>
                  <td>{v.vehiculo ? `${v.vehiculo} · ${v.operador ?? ''}` : <span className="vacio-celda">Sin asignar</span>}</td>
                  <td>{v.carga ?? <span className="vacio-celda">—</span>}</td>
                  <td><Hora ms={v.fechaSalida} /></td>
                  <td><Hora ms={v.fechaLlegada} /></td>
                  <td><Hora ms={v.fechaEntrega} /></td>
                  <td>
                    {v.duracionMs != null ? duracion(v.duracionMs)
                      : v.fechaSalida != null ? `Lleva ${duracion(ahora - v.fechaSalida)}`
                        : <span className="vacio-celda">—</span>}
                  </td>
                  <td><ChipEstado estado={v.estado} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}

// ---------------- Operadores ----------------

type FiltroOperador = 'todos' | 'disponibles' | 'enViaje'

export function Operadores({ datos, onEditar }: { datos: DatosPanel; onEditar: (o: OperadorEstado) => void }) {
  const [filtro, setFiltro] = useState<FiltroOperador>('todos')
  const ahora = useAhora()
  const lista = datos.operadores
  const disponibles = lista.filter((o) => !o.enViaje).length
  const visibles = lista.filter((o) => filtro === 'todos' || (filtro === 'enViaje') === o.enViaje)
  const filtros: [FiltroOperador, string][] = [['todos', 'Todos'], ['disponibles', 'Disponibles'], ['enViaje', 'En viaje']]
  return (
    <>
      <h2>{disponibles} disponibles, {lista.length - disponibles} en viaje</h2>
      <div className="filtros">
        {filtros.map(([f, t]) => (
          <button key={f} className={'filtro' + (filtro === f ? ' activo' : '')} onClick={() => setFiltro(f)}>{t}</button>
        ))}
      </div>
      {visibles.length === 0 ? (
        <EstadoVacio icono="groups" titulo="Nadie en esta lista" texto="Agrega un operador o prueba con otro filtro." />
      ) : (
        <div className="grilla">
          {visibles.map((o) => (
            <div
              key={o.id} className="tarjeta" role="button" tabIndex={0} title="Editar operador"
              onClick={() => onEditar(o)} onKeyDown={(e) => { if (e.key === 'Enter') onEditar(o) }}
            >
              <div className="cabeza">
                <b>{o.nombreCompleto}</b>
                <ChipLibre ocupado={o.enViaje} textoOcupado="En viaje" textoLibre="Disponible" />
              </div>
              <span className="sec">Cédula {o.cedula}</span>
              <span className="sec">{o.vehiculo ? `En la unidad del vehículo ${o.vehiculo}` : 'Sin unidad asignada'}</span>
              {o.fechaSalida != null && (
                <span className="sec">
                  Salió a las {hora(o.fechaSalida)}{o.destino ? ` rumbo a ${o.destino}` : ''}, lleva {duracion(ahora - o.fechaSalida)}
                </span>
              )}
              <a className="link" href={`tel:${o.telefono}`} onClick={(e) => e.stopPropagation()}>
                <Icono nombre="call" relleno tam={18} /> Llamar al {o.telefono}
              </a>
            </div>
          ))}
        </div>
      )}
    </>
  )
}

// ---------------- Unidades ----------------

export function Unidades({ datos, onListo }: { datos: DatosPanel; onListo: (texto: string) => void }) {
  const deshacer = (id: string, nombre: string) => {
    if (!window.confirm(`¿Deshacer la unidad ${nombre}? El vehículo, el tanque y el operador quedan libres para armar otra unidad.`)) return
    onListo(deshacerUnidad(datos.datos, id) ?? 'Unidad deshecha')
  }
  return datos.unidades.length === 0 ? (
    <EstadoVacio icono="hub" titulo="No hay unidades" texto='Toca "Conformar unidad" para armar la primera.' />
  ) : (
    <div className="grilla">
      {datos.unidades.map((u) => (
        <div key={u.id} className="tarjeta">
          <div className="cabeza"><b className="mono">{u.vehiculo}</b><ChipLibre ocupado={u.enUso} textoOcupado="En ruta" /></div>
          <span className="dato"><Icono nombre="local_shipping" />{u.descripcionVehiculo}{u.tipoVehiculo ? ` · ${u.tipoVehiculo}` : ''}</span>
          <span className="dato">
            <Icono nombre="propane_tank" />
            Tanque {u.tanque} · {litros(u.volumenLitros)} · {comp(u.compartimientos)} · {u.combustible}
          </span>
          <span className="dato"><Icono nombre="person" />{u.operador}</span>
          {!u.enUso && (
            <div><button className="boton boton-texto" onClick={() => deshacer(u.id, u.vehiculo)}><Icono nombre="link_off" relleno tam={18} />Deshacer unidad</button></div>
          )}
        </div>
      ))}
    </div>
  )
}

// ---------------- Rutas, Tanques, Vehículos (tablas simples) ----------------

function Tabla<T extends { id: string }>({ columnas, filas, celdas, onEditar, vacio }: {
  columnas: string[]; filas: T[]; celdas: (x: T) => ReactNode[]; onEditar: (x: T) => void
  vacio: { icono: string; titulo: string; texto: string }
}) {
  if (filas.length === 0) return <EstadoVacio {...vacio} />
  return (
    <div className="tabla">
      <table>
        <thead><tr>{columnas.map((c) => <th key={c}>{c}</th>)}</tr></thead>
        <tbody>
          {filas.map((f) => (
            <tr key={f.id} onClick={() => onEditar(f)} tabIndex={0} onKeyDown={(e) => { if (e.key === 'Enter') onEditar(f) }}>
              {celdas(f).map((c, i) => <td key={i}>{c}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export const Rutas = ({ datos, onEditar }: { datos: DatosPanel; onEditar: (r: Ruta) => void }) => (
  <Tabla
    columnas={['Despacho', 'Recorrido', 'Factura']} filas={datos.rutas} onEditar={onEditar}
    celdas={(r) => [<b className="mono">{r.numeroDespacho}</b>, r.recorrido, <span className="mono">{r.numeroFactura}</span>]}
    vacio={{ icono: 'map', titulo: 'Todavía no hay rutas', texto: 'Toca "Agregar ruta" para empezar.' }}
  />
)

export const Tanques = ({ datos, onEditar }: { datos: DatosPanel; onEditar: (t: TanqueDetalle) => void }) => (
  <Tabla
    columnas={['Matrícula', 'Volumen', 'Compartimientos', 'Combustible', 'Unidad']} filas={datos.tanques} onEditar={onEditar}
    celdas={(t) => [<b className="mono">{t.matricula}</b>, litros(t.volumenLitros), t.compartimientos, t.combustible,
      <ChipLibre ocupado={t.enUnidad} textoOcupado="En unidad" />]}
    vacio={{ icono: 'propane_tank', titulo: 'Todavía no hay tanques', texto: 'Toca "Agregar tanque" para empezar.' }}
  />
)

export const Vehiculos = ({ datos, onEditar }: { datos: DatosPanel; onEditar: (v: VehiculoDetalle) => void }) => (
  <Tabla
    columnas={['Matrícula', 'Descripción', 'Tipo', 'Unidad']} filas={datos.vehiculos} onEditar={onEditar}
    celdas={(v) => [<b className="mono">{v.matricula}</b>, v.descripcion, v.tipo, <ChipLibre ocupado={v.enUnidad} textoOcupado="En unidad" />]}
    vacio={{ icono: 'local_shipping', titulo: 'Todavía no hay vehículos', texto: 'Toca "Agregar vehículo" para empezar.' }}
  />
)
