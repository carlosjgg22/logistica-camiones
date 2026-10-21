import { useState } from 'react'
import type { DatosPanel } from '../datos'
import {
  conformarUnidad, guardarOperador, guardarRuta, guardarTanque, guardarVehiculo, guardarViaje, nuevoId, programarViaje,
} from '../escribir'
import { aInputFecha, deInputFecha, litros } from '../formato'
import type { OperadorEstado, Ruta, TanqueDetalle, VehiculoDetalle, ViajeDetalle } from '../tipos'
import { Cajon, Campo, CampoFechaHora, CampoTexto, Duracion, MensajeError, Selector } from '../componentes/base'

// Mismos campos y validaciones que los Formulario* de ui/oficina en la app.

interface Props<T> { datos: DatosPanel; item?: T | null; onListo: (texto: string) => void; onCerrar: () => void }

/** Guarda y, si salió bien, avisa con [texto]. */
const conAviso = (error: string | null, texto: string, onListo: (t: string) => void) => {
  if (error == null) onListo(texto)
  return error
}

// ---------------- Operador ----------------

export function FormOperador({ datos, item, onListo, onCerrar }: Props<OperadorEstado>) {
  const [nombre, setNombre] = useState(item?.nombre ?? '')
  const [apellido, setApellido] = useState(item?.apellido ?? '')
  const [telefono, setTelefono] = useState(item?.telefono ?? '')
  const [cedula, setCedula] = useState(item?.cedula ?? '')
  const valido = [nombre, apellido, telefono, cedula].every((x) => x.trim())

  return (
    <Cajon
      titulo={item ? 'Editar operador' : 'Nuevo operador'} textoBoton={item ? 'Guardar cambios' : 'Agregar operador'}
      habilitado={valido} onCerrar={onCerrar}
      aviso="Con la cédula el operador entra a la app en su teléfono."
      onGuardar={() => conAviso(guardarOperador(datos.datos, {
        id: item?.id ?? nuevoId(), nombre: nombre.trim(), apellido: apellido.trim(),
        telefono: telefono.trim(), cedula: cedula.trim(), activo: item?.activo ?? true,
      }), item ? 'Operador actualizado' : 'Operador agregado', onListo)}
    >
      <CampoTexto etiqueta="Nombre" valor={nombre} onCambio={setNombre} />
      <CampoTexto etiqueta="Apellido" valor={apellido} onCambio={setApellido} />
      <CampoTexto etiqueta="Teléfono" valor={telefono} onCambio={setTelefono} numerico />
      <CampoTexto etiqueta="Cédula" valor={cedula} onCambio={setCedula} numerico />
    </Cajon>
  )
}

// ---------------- Ruta, tanque, vehículo ----------------

export function FormRuta({ item, onListo, onCerrar }: Props<Ruta>) {
  const [despacho, setDespacho] = useState(item?.numeroDespacho ?? '')
  const [recorrido, setRecorrido] = useState(item?.recorrido ?? '')
  const [factura, setFactura] = useState(item?.numeroFactura ?? '')
  return (
    <Cajon
      titulo={item ? 'Editar ruta' : 'Nueva ruta'} textoBoton={item ? 'Guardar cambios' : 'Agregar ruta'}
      habilitado={[despacho, recorrido, factura].every((x) => x.trim())} onCerrar={onCerrar}
      onGuardar={() => conAviso(guardarRuta({
        id: item?.id ?? nuevoId(), numeroDespacho: despacho.trim(), recorrido: recorrido.trim(), numeroFactura: factura.trim(),
      }), item ? 'Cambios guardados' : 'Ruta agregada', onListo)}
    >
      <CampoTexto etiqueta="Número de despacho" valor={despacho} onCambio={setDespacho} />
      <CampoTexto etiqueta="Recorrido" valor={recorrido} onCambio={setRecorrido} />
      <CampoTexto etiqueta="Número de factura" valor={factura} onCambio={setFactura} />
    </Cajon>
  )
}

export function FormTanque({ datos, item, onListo, onCerrar }: Props<TanqueDetalle>) {
  const [matricula, setMatricula] = useState(item?.matricula ?? '')
  const [volumen, setVolumen] = useState(item ? String(item.volumenLitros) : '')
  const [compartimientos, setCompartimientos] = useState(item ? String(item.compartimientos) : '')
  const [idCombustible, setIdCombustible] = useState<string | null>(item?.idTipoCombustible ?? null)
  const vol = Number(volumen), comp = Number(compartimientos)
  const valido = matricula.trim() !== '' && vol > 0 && comp > 0 && idCombustible != null
  return (
    <Cajon
      titulo={item ? 'Editar tanque' : 'Nuevo tanque'} textoBoton={item ? 'Guardar cambios' : 'Agregar tanque'}
      habilitado={valido} onCerrar={onCerrar}
      onGuardar={() => conAviso(guardarTanque(datos.datos, {
        id: item?.id ?? nuevoId(), matricula: matricula.trim().toUpperCase(),
        volumenLitros: vol, compartimientos: comp, idTipoCombustible: idCombustible!,
      }), item ? 'Cambios guardados' : 'Tanque agregado', onListo)}
    >
      <CampoTexto etiqueta="Matrícula" valor={matricula} onCambio={setMatricula} />
      <CampoTexto etiqueta="Volumen" valor={volumen} onCambio={setVolumen} numerico sufijo="L" />
      <CampoTexto etiqueta="Compartimientos" valor={compartimientos} onCambio={setCompartimientos} numerico />
      <Selector
        etiqueta="Tipo de combustible" opciones={datos.combustibles} valor={idCombustible}
        clave={(c) => c.id} texto={(c) => c.nombre} onCambio={setIdCombustible}
      />
    </Cajon>
  )
}

export function FormVehiculo({ datos, item, onListo, onCerrar }: Props<VehiculoDetalle>) {
  const [matricula, setMatricula] = useState(item?.matricula ?? '')
  const [descripcion, setDescripcion] = useState(item?.descripcion ?? '')
  const [idTipo, setIdTipo] = useState<string | null>(item?.idTipoVehiculo ?? null)
  return (
    <Cajon
      titulo={item ? 'Editar vehículo' : 'Nuevo vehículo'} textoBoton={item ? 'Guardar cambios' : 'Agregar vehículo'}
      habilitado={matricula.trim() !== '' && descripcion.trim() !== '' && idTipo != null} onCerrar={onCerrar}
      onGuardar={() => conAviso(guardarVehiculo(datos.datos, {
        id: item?.id ?? nuevoId(), matricula: matricula.trim().toUpperCase(), descripcion: descripcion.trim(), idTipoVehiculo: idTipo!,
      }), item ? 'Cambios guardados' : 'Vehículo agregado', onListo)}
    >
      <CampoTexto etiqueta="Matrícula" valor={matricula} onCambio={setMatricula} />
      <CampoTexto etiqueta="Descripción" valor={descripcion} onCambio={setDescripcion} />
      <Selector
        etiqueta="Tipo de vehículo" opciones={datos.tiposVehiculo} valor={idTipo}
        clave={(t) => t.id} texto={(t) => t.nombre} onCambio={setIdTipo}
      />
    </Cajon>
  )
}

// ---------------- Unidad ----------------

export function FormUnidad({ datos, onListo, onCerrar }: Props<never>) {
  // Solo se ofrece lo que no está en otra unidad
  const vehiculos = datos.vehiculos.filter((v) => !v.enUnidad)
  const tanques = datos.tanques.filter((t) => !t.enUnidad)
  const operadores = datos.operadores.filter((o) => !o.enUnidad)
  const [idVehiculo, setIdVehiculo] = useState<string | null>(null)
  const [idTanque, setIdTanque] = useState<string | null>(null)
  const [idOperador, setIdOperador] = useState<string | null>(null)
  const faltan = [
    vehiculos.length === 0 && 'vehículos', tanques.length === 0 && 'tanques', operadores.length === 0 && 'operadores',
  ].filter(Boolean)

  return (
    <Cajon
      titulo="Conformar unidad" textoBoton="Conformar unidad" iconoBoton="hub" onCerrar={onCerrar}
      habilitado={idVehiculo != null && idTanque != null && idOperador != null}
      onGuardar={() => conAviso(conformarUnidad(datos.datos, idVehiculo!, idTanque!, idOperador!),
        'Unidad conformada. Ya se puede asignar a un viaje.', onListo)}
    >
      {faltan.length > 0 && (
        <MensajeError texto={`No hay ${faltan.join(' ni ')} libres. Agrégalos en su módulo o deshaz otra unidad.`} />
      )}
      <Selector
        etiqueta="Vehículo" opciones={vehiculos} valor={idVehiculo} clave={(v) => v.id}
        texto={(v) => `${v.matricula}, ${v.descripcion} (${v.tipo})`} onCambio={setIdVehiculo}
      />
      <Selector
        etiqueta="Tanque" opciones={tanques} valor={idTanque} clave={(t) => t.id}
        texto={(t) => `${t.matricula}, ${litros(t.volumenLitros)} de ${t.combustible}`} onCambio={setIdTanque}
      />
      <Selector
        etiqueta="Operador" opciones={operadores} valor={idOperador} clave={(o) => o.id}
        texto={(o) => `${o.nombreCompleto}, cédula ${o.cedula}`} onCambio={setIdOperador}
      />
    </Cajon>
  )
}

// ---------------- Viaje ----------------

const textoUnidad = (u: DatosPanel['unidades'][number]) => `${u.vehiculo} + ${u.tanque} · ${u.operador}`

export function FormViaje({ datos, item, onListo, onCerrar }: Props<ViajeDetalle>) {
  const [idRuta, setIdRuta] = useState<string | null>(item?.idRuta ?? null)
  const [destino, setDestino] = useState(item?.destino ?? '')
  const [idUnidad, setIdUnidad] = useState<string | null>(item?.idUnidad ?? null)
  const [salida, setSalida] = useState<number | null>(item?.fechaSalida ?? null)
  const [llegada, setLlegada] = useState<number | null>(item?.fechaLlegada ?? null)
  const [entrega, setEntrega] = useState<number | null>(item?.fechaEntrega ?? null)

  const ruta = datos.rutas.find((r) => r.id === idRuta)
  const unidadVigente = datos.unidades.some((u) => u.id === idUnidad)
  const programado = item?.idProgramacion != null
  const valido = ruta != null && destino.trim() !== '' && (!programado || idUnidad != null)

  return (
    <Cajon
      titulo={item ? 'Editar viaje' : 'Nuevo viaje'} textoBoton={item ? 'Guardar cambios' : 'Agregar viaje'}
      iconoBoton="route" habilitado={valido} onCerrar={onCerrar}
      onGuardar={() => conAviso(guardarViaje(datos.datos, {
        id: item?.id ?? nuevoId(), idRuta: idRuta!, idUnidad, idOperador: item?.idOperador ?? null,
        destino: destino.trim(), fechaSalida: salida, fechaLlegada: llegada, fechaEntrega: entrega,
      }), item ? 'Viaje actualizado' : 'Viaje agregado', onListo)}
    >
      {datos.rutas.length === 0 && (
        <MensajeError texto="Primero carga una ruta en el módulo Rutas (número de despacho, recorrido y factura)." />
      )}
      <Selector
        etiqueta="Despacho y ruta" opciones={datos.rutas} valor={idRuta} clave={(r) => r.id}
        texto={(r) => `Despacho ${r.numeroDespacho}, ${r.recorrido}`} onCambio={setIdRuta}
        ayuda={ruta ? `Factura ${ruta.numeroFactura}` : null}
      />
      <CampoTexto etiqueta="Destino" valor={destino} onCambio={setDestino} />
      <Selector
        etiqueta="Unidad" opciones={datos.unidades} valor={unidadVigente ? idUnidad : null} clave={(u) => u.id}
        texto={textoUnidad} onCambio={setIdUnidad} vacio={programado ? undefined : 'Sin asignar'}
        ayuda={idUnidad != null && !unidadVigente ? 'La unidad que tenía este viaje ya fue deshecha.'
          : programado ? 'El viaje está programado: necesita una unidad.'
            : 'Opcional. También se asigna al programar el viaje.'}
      />
      <CampoFechaHora etiqueta="Fecha de salida" valor={salida} onCambio={setSalida} />
      <CampoFechaHora etiqueta="Fecha de llegada" valor={llegada} onCambio={setLlegada} />
      <CampoFechaHora etiqueta="Fecha de entrega del producto" valor={entrega} onCambio={setEntrega} />
      <Duracion salida={salida} llegada={llegada} />
    </Cajon>
  )
}

// ---------------- Programar ----------------

export function FormProgramar({ datos, onListo, onCerrar }: Props<never>) {
  const sinProgramar = datos.viajes.filter((v) => v.estado === 'SIN_PROGRAMAR')
  const [idViaje, setIdViaje] = useState<string | null>(null)
  const [idUnidad, setIdUnidad] = useState<string | null>(null)
  const [idCarga, setIdCarga] = useState<string | null>(null)
  const [fecha, setFecha] = useState(Date.now())
  const unidad = datos.unidades.find((u) => u.id === idUnidad)
  const carga = datos.combustibles.find((c) => c.id === idCarga)

  const elegirViaje = (id: string | null) => {
    setIdViaje(id)
    // Si el viaje ya traía unidad, viene elegida
    const v = sinProgramar.find((x) => x.id === id)
    const u = datos.unidades.find((x) => x.id === v?.idUnidad)
    if (u) { setIdUnidad(u.id); if (idCarga == null) setIdCarga(u.idTipoCombustible) }
  }
  const elegirUnidad = (id: string | null) => {
    setIdUnidad(id)
    const u = datos.unidades.find((x) => x.id === id)
    if (u) setIdCarga(u.idTipoCombustible) // la carga que lleva el tanque, se puede cambiar
  }

  return (
    <Cajon
      titulo="Programar viaje" textoBoton="Programar viaje" iconoBoton="edit_calendar" onCerrar={onCerrar}
      habilitado={idViaje != null && unidad != null && carga != null}
      aviso={`Al programar, ${unidad?.nombreOperador ?? 'el operador de la unidad'} verá el viaje en su teléfono.`}
      onGuardar={() => conAviso(programarViaje(datos.datos, idViaje!, idUnidad!, idCarga!, fecha),
        'Viaje programado. El operador ya lo ve en su teléfono.', onListo)}
    >
      {sinProgramar.length === 0 && <MensajeError texto="No hay viajes sin programar. Agrega uno en el módulo Viajes." />}
      {datos.unidades.length === 0 && <MensajeError texto="No hay unidades. Conforma una en el módulo Unidades." />}
      <Selector
        etiqueta="Viaje" opciones={sinProgramar} valor={idViaje} clave={(v) => v.id}
        texto={(v) => `Despacho ${v.numeroDespacho} a ${v.destino}`} onCambio={elegirViaje}
      />
      <Selector
        etiqueta="Unidad" opciones={datos.unidades} valor={idUnidad} clave={(u) => u.id}
        texto={(u) => textoUnidad(u) + (u.enUso ? ' (en ruta)' : '')} onCambio={elegirUnidad}
        ayuda={unidad ? `Tanque de ${litros(unidad.volumenLitros)} para ${unidad.combustible}` : null}
      />
      <Selector
        etiqueta="Tipo de carga" opciones={datos.combustibles} valor={idCarga} clave={(c) => c.id}
        texto={(c) => c.nombre} onCambio={setIdCarga}
        alerta={unidad != null && carga != null && carga.id !== unidad.idTipoCombustible}
        ayuda={unidad && carga && carga.id !== unidad.idTipoCombustible ? `Ojo: el tanque de esta unidad es para ${unidad.combustible}.`
          : unidad ? 'La carga que lleva el tanque; se puede cambiar.' : null}
      />
      <Campo etiqueta="Fecha">
        {(id) => (
          <div className="entrada">
            <input id={id} type="date" value={aInputFecha(fecha)} onChange={(e) => e.target.value && setFecha(deInputFecha(e.target.value))} />
          </div>
        )}
      </Campo>
    </Cajon>
  )
}
