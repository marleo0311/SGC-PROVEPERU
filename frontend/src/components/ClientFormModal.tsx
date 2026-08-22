import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { createClient, updateClient } from '../services/client.service'
import { getApiErrorDetails } from '../services/api'
import type { Cliente, ClienteGuardarRequest, TipoPersona } from '../types/client'

interface ClientFormModalProps {
  mode: 'create' | 'edit'
  client?: Cliente
  onClose: () => void
  onSaved: (client: Cliente) => void
}

interface ClientFormValues {
  tipoPersona: TipoPersona
  numeroDocumento: string
  nombres: string
  apellidos: string
  razonSocial: string
  nombreComercial: string
  direccion: string
  telefono: string
  whatsapp: string
  correo: string
  permiteCredito: boolean
}

type ClientFormErrors = Partial<Record<keyof ClientFormValues, string>>

function initialValues(client?: Cliente): ClientFormValues {
  return {
    tipoPersona: client?.tipoPersona ?? 'NATURAL',
    numeroDocumento: client?.numeroDocumento ?? '',
    nombres: client?.nombres ?? '',
    apellidos: client?.apellidos ?? '',
    razonSocial: client?.razonSocial ?? '',
    nombreComercial: client?.nombreComercial ?? '',
    direccion: client?.direccion ?? '',
    telefono: client?.telefono ?? '',
    whatsapp: client?.whatsapp ?? '',
    correo: client?.correo ?? '',
    permiteCredito: client?.permiteCredito ?? false,
  }
}

function validateForm(values: ClientFormValues): ClientFormErrors {
  const errors: ClientFormErrors = {}
  const isNatural = values.tipoPersona === 'NATURAL'
  const expectedDigits = isNatural ? 8 : 11

  if (!new RegExp(`^\\d{${expectedDigits}}$`).test(values.numeroDocumento)) {
    errors.numeroDocumento = `${isNatural ? 'El DNI' : 'El RUC'} debe tener ${expectedDigits} dígitos.`
  }
  if (isNatural && !values.nombres.trim()) errors.nombres = 'Ingresa los nombres.'
  if (isNatural && !values.apellidos.trim()) errors.apellidos = 'Ingresa los apellidos.'
  if (!isNatural && !values.razonSocial.trim()) errors.razonSocial = 'Ingresa la razón social.'
  if (values.nombres.length > 120) errors.nombres = 'Admite hasta 120 caracteres.'
  if (values.apellidos.length > 120) errors.apellidos = 'Admite hasta 120 caracteres.'
  if (values.razonSocial.length > 200) errors.razonSocial = 'Admite hasta 200 caracteres.'
  if (values.nombreComercial.length > 180) errors.nombreComercial = 'Admite hasta 180 caracteres.'
  if (values.direccion.length > 250) errors.direccion = 'Admite hasta 250 caracteres.'
  if (values.telefono.length > 30) errors.telefono = 'Admite hasta 30 caracteres.'
  if (values.whatsapp.length > 30) errors.whatsapp = 'Admite hasta 30 caracteres.'
  if (values.correo && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.correo)) errors.correo = 'Ingresa un correo válido.'
  if (values.correo.length > 180) errors.correo = 'Admite hasta 180 caracteres.'
  return errors
}

function textOrNull(value: string) {
  return value.trim() || null
}

function toRequest(values: ClientFormValues): ClienteGuardarRequest {
  const isNatural = values.tipoPersona === 'NATURAL'
  return {
    tipoPersona: values.tipoPersona,
    tipoDocumento: isNatural ? 'DNI' : 'RUC',
    numeroDocumento: values.numeroDocumento,
    nombres: isNatural ? textOrNull(values.nombres) : null,
    apellidos: isNatural ? textOrNull(values.apellidos) : null,
    razonSocial: isNatural ? null : textOrNull(values.razonSocial),
    nombreComercial: textOrNull(values.nombreComercial),
    direccion: textOrNull(values.direccion),
    telefono: textOrNull(values.telefono),
    whatsapp: textOrNull(values.whatsapp),
    correo: textOrNull(values.correo),
    permiteCredito: values.permiteCredito,
  }
}

export function ClientFormModal({ mode, client, onClose, onSaved }: ClientFormModalProps) {
  const [values, setValues] = useState<ClientFormValues>(() => initialValues(client))
  const [errors, setErrors] = useState<ClientFormErrors>({})
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !isSubmitting) onClose()
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [isSubmitting, onClose])

  function handleChange(event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: undefined }))
  }

  function choosePersonType(tipoPersona: TipoPersona) {
    setValues((current) => ({
      ...current,
      tipoPersona,
      numeroDocumento: '',
      nombres: tipoPersona === 'NATURAL' ? current.nombres : '',
      apellidos: tipoPersona === 'NATURAL' ? current.apellidos : '',
      razonSocial: tipoPersona === 'JURIDICA' ? current.razonSocial : '',
    }))
    setErrors({})
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const validationErrors = validateForm(values)
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors)
      return
    }

    setSubmitError('')
    setIsSubmitting(true)
    try {
      const request = toRequest(values)
      const savedClient = mode === 'create'
        ? await createClient(request)
        : await updateClient(client!.id, request)
      onSaved(savedClient)
    } catch (requestError) {
      const details = getApiErrorDetails(requestError)
      setSubmitError(details.message)
      setErrors(details.fieldErrors as ClientFormErrors)
    } finally {
      setIsSubmitting(false)
    }
  }

  const isNatural = values.tipoPersona === 'NATURAL'
  const title = mode === 'create' ? 'Registrar cliente' : 'Editar cliente'

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && !isSubmitting && onClose()}>
      <section className="form-modal client-form-modal" role="dialog" aria-modal="true" aria-labelledby="client-form-title">
        <header className="form-modal__header">
          <div>
            <span className="form-modal__icon client-form-modal__icon"><i className={`bi ${mode === 'create' ? 'bi-person-plus' : 'bi-person-gear'}`} /></span>
            <span><small>Gestión comercial</small><h2 id="client-form-title">{title}</h2></span>
          </div>
          <button className="icon-button" type="button" onClick={onClose} disabled={isSubmitting} aria-label="Cerrar formulario"><i className="bi bi-x-lg" /></button>
        </header>

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-modal__body">
            {submitError && <div className="alert-message alert-message--danger" role="alert"><i className="bi bi-exclamation-circle-fill" /><span>{submitError}</span></div>}

            <fieldset className="product-form-section">
              <legend><span>1</span> Identificación</legend>
              <div className="client-type-selector" role="group" aria-label="Tipo de persona">
                <button className={isNatural ? 'active' : ''} type="button" onClick={() => choosePersonType('NATURAL')} disabled={mode === 'edit'}>
                  <i className="bi bi-person" /><span><strong>Persona natural</strong><small>Identificación con DNI</small></span><i className="bi bi-check-circle-fill" />
                </button>
                <button className={!isNatural ? 'active' : ''} type="button" onClick={() => choosePersonType('JURIDICA')} disabled={mode === 'edit'}>
                  <i className="bi bi-building" /><span><strong>Persona jurídica</strong><small>Identificación con RUC</small></span><i className="bi bi-check-circle-fill" />
                </button>
              </div>
              <div className="product-form-grid client-form-identification">
                <FormField label={isNatural ? 'DNI' : 'RUC'} name="numeroDocumento" error={errors.numeroDocumento} required>
                  <div className="client-document-input"><span>{isNatural ? 'DNI' : 'RUC'}</span><input id="numeroDocumento" name="numeroDocumento" inputMode="numeric" value={values.numeroDocumento} onChange={(event) => { if (/^\d*$/.test(event.target.value)) handleChange(event) }} maxLength={isNatural ? 8 : 11} placeholder={isNatural ? '8 dígitos' : '11 dígitos'} autoFocus /></div>
                </FormField>
                {isNatural ? (
                  <>
                    <FormField label="Nombres" name="nombres" error={errors.nombres} required><input id="nombres" name="nombres" value={values.nombres} onChange={handleChange} maxLength={120} placeholder="Nombres del cliente" /></FormField>
                    <FormField label="Apellidos" name="apellidos" error={errors.apellidos} required><input id="apellidos" name="apellidos" value={values.apellidos} onChange={handleChange} maxLength={120} placeholder="Apellidos del cliente" /></FormField>
                  </>
                ) : (
                  <FormField label="Razón social" name="razonSocial" error={errors.razonSocial} required wide><input id="razonSocial" name="razonSocial" value={values.razonSocial} onChange={handleChange} maxLength={200} placeholder="Razón social registrada" /></FormField>
                )}
                <FormField label="Nombre comercial" name="nombreComercial" error={errors.nombreComercial} hint="Opcional"><input id="nombreComercial" name="nombreComercial" value={values.nombreComercial} onChange={handleChange} maxLength={180} placeholder="Nombre conocido del negocio" /></FormField>
              </div>
            </fieldset>

            <fieldset className="product-form-section">
              <legend><span>2</span> Datos de contacto</legend>
              <div className="product-form-grid product-form-grid--three">
                <FormField label="Teléfono" name="telefono" error={errors.telefono} hint="Opcional"><input id="telefono" name="telefono" value={values.telefono} onChange={handleChange} maxLength={30} placeholder="Ej. 01 555 0101" /></FormField>
                <FormField label="WhatsApp" name="whatsapp" error={errors.whatsapp} hint="Opcional"><input id="whatsapp" name="whatsapp" value={values.whatsapp} onChange={handleChange} maxLength={30} placeholder="Ej. 999 999 999" /></FormField>
                <FormField label="Correo electrónico" name="correo" error={errors.correo} hint="Opcional"><input id="correo" name="correo" type="email" value={values.correo} onChange={handleChange} maxLength={180} placeholder="cliente@correo.com" /></FormField>
                <FormField label="Dirección" name="direccion" error={errors.direccion} hint={`${values.direccion.length}/250`} wide><textarea id="direccion" name="direccion" value={values.direccion} onChange={handleChange} maxLength={250} rows={2} placeholder="Dirección fiscal o de entrega" /></FormField>
              </div>
            </fieldset>

            <fieldset className="product-form-section">
              <legend><span>3</span> Condición comercial</legend>
              <label className={`client-credit-option ${values.permiteCredito ? 'active' : ''}`}>
                <input type="checkbox" checked={values.permiteCredito} onChange={(event) => setValues((current) => ({ ...current, permiteCredito: event.target.checked }))} />
                <span><i className="bi bi-credit-card-2-front" /></span>
                <span><strong>Autorizar ventas a crédito</strong><small>El cliente podrá generar operaciones con saldo pendiente.</small></span>
                <span className="client-credit-switch" aria-hidden="true" />
              </label>
            </fieldset>
          </div>

          <footer className="form-modal__footer">
            <span><i className="bi bi-shield-check" /> Los campos marcados con * son obligatorios.</span>
            <div>
              <button className="secondary-button" type="button" onClick={onClose} disabled={isSubmitting}>Cancelar</button>
              <button className="primary-button primary-button--inline" type="submit" disabled={isSubmitting}>
                {isSubmitting ? <><span className="spinner-border spinner-border-sm" /> Guardando…</> : <><i className="bi bi-check2" /> {mode === 'create' ? 'Registrar cliente' : 'Guardar cambios'}</>}
              </button>
            </div>
          </footer>
        </form>
      </section>
    </div>
  )
}

interface FormFieldProps {
  label: string
  name: string
  error?: string
  hint?: string
  required?: boolean
  wide?: boolean
  children: React.ReactNode
}

function FormField({ label, name, error, hint, required, wide, children }: FormFieldProps) {
  return (
    <label className={`product-form-field ${wide ? 'product-form-field--wide' : ''} ${error ? 'product-form-field--error' : ''}`} htmlFor={name}>
      <span className="product-form-field__label">{label}{required && <b> *</b>}{hint && <small>{hint}</small>}</span>
      {children}
      {error && <span className="product-form-field__error"><i className="bi bi-exclamation-circle" /> {error}</span>}
    </label>
  )
}
