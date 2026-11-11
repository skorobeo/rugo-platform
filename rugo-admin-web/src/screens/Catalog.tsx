import { useCallback, useState, type ReactNode } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Button, Input, Textarea } from '@maxhub/max-ui'
import {
  approveEquipment,
  createEquipment,
  createMaterial,
  createWorkshop,
  deleteEquipment,
  deleteMaterial,
  deleteWorkshop,
  fetchEquipmentList,
  fetchMaterials,
  fetchWorkshops,
  rejectEquipment,
  setMaterialStatus,
  updateEquipment,
  updateMaterial,
  updateWorkshop,
  type AdminEquipment,
  type AdminEquipmentInput,
  type AdminMaterial,
  type AdminMaterialInput,
  type AdminWorkshop,
  type AdminWorkshopInput,
} from '../api/admin'
import { ApiError } from '../api/client'
import { Modal } from '../components/Modal'
import { PhotoInput } from '../components/PhotoInput'
import { ErrorState, LoadingState } from '../components/States'
import { useToast } from '../components/Toast'
import { ru } from '../content/ru'
import { useAsync } from '../lib/useAsync'

type CatalogTab = 'equipment' | 'materials' | 'workshops'
type CatalogItem = AdminEquipment | AdminMaterial | AdminWorkshop

const TABS: { key: CatalogTab; label: string }[] = [
  { key: 'equipment', label: ru.catalog.tabs.equipment },
  { key: 'materials', label: ru.catalog.tabs.materials },
  { key: 'workshops', label: ru.catalog.tabs.workshops },
]

const BOOM_CATEGORIES: readonly string[] = ru.catalog.boomCategories

function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <label className="field">
      <span className="field__label">{label}</span>
      {children}
      {hint ? <span className="field__hint">{hint}</span> : null}
    </label>
  )
}

function Select({
  value,
  onChange,
  options,
  allowEmpty,
}: {
  value: string
  onChange: (value: string) => void
  options: readonly { value: string; label: string }[]
  allowEmpty?: boolean
}) {
  return (
    <select className="admin-select" value={value} onChange={(e) => onChange(e.target.value)}>
      {allowEmpty ? <option value="">— не выбрано —</option> : null}
      {options.map((o) => (
        <option key={o.value} value={o.value}>
          {o.label}
        </option>
      ))}
    </select>
  )
}

function errorText(e: unknown): string {
  if (e instanceof ApiError && Object.keys(e.errors).length > 0) {
    return Object.values(e.errors).join('; ')
  }
  return ru.catalog.error
}

function firstNumber(value: string | null | undefined): string {
  const match = value ? value.match(/\d+/) : null
  return match ? match[0] : ''
}

function districtOf(location: string | null | undefined): string {
  if (!location) {
    return ''
  }
  const comma = location.indexOf(',')
  return comma >= 0 ? location.slice(comma + 1).trim() : ''
}

function priceUnitOf(priceType: string | null | undefined): string {
  switch (priceType) {
    case 'DAY':
      return 'day'
    case 'SHIFT':
      return 'shift'
    default:
      return 'hour'
  }
}

// ---------------- Equipment ----------------

function EquipmentForm({
  initial,
  onClose,
  onSaved,
}: {
  initial?: AdminEquipment
  onClose: () => void
  onSaved: () => void
}) {
  const { notify } = useToast()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [f, setF] = useState({
    category: initial?.category ?? ru.catalog.categories[0],
    brand: initial?.brand ?? '',
    model: initial?.model ?? '',
    year: initial?.year != null ? String(initial.year) : '',
    capacity: firstNumber(initial?.capacity),
    boomLength: initial?.boomLength ?? '',
    price: initial?.price != null ? String(initial.price) : '',
    priceUnit: priceUnitOf(initial?.priceType),
    district: districtOf(initial?.location),
    description: initial?.description ?? '',
    ownerName: initial?.ownerPhone && initial.ownerPhone !== '+70000000000' ? (initial.ownerName ?? '') : '',
    ownerPhone: initial?.ownerPhone && initial.ownerPhone !== '+70000000000' ? initial.ownerPhone : '',
    imageUrls: initial?.imageUrls ?? (initial?.imageUrl ? [initial.imageUrl] : []),
  })

  const submit = async () => {
    if (!f.category) {
      setError('Выберите категорию')
      return
    }
    if (!f.capacity || Number(f.capacity) <= 0) {
      setError('Грузоподъёмность должна быть больше 0')
      return
    }
    if (!f.price || Number(f.price) <= 0) {
      setError('Цена должна быть больше 0')
      return
    }
    setBusy(true)
    setError('')
    const input: AdminEquipmentInput = {
      category: f.category,
      brand: f.brand,
      model: f.model,
      year: f.year ? Number(f.year) : undefined,
      capacity: Number(f.capacity),
      boomLength: f.boomLength || undefined,
      price: Number(f.price),
      priceUnit: f.priceUnit,
      district: f.district || undefined,
      description: f.description || undefined,
      ownerName: f.ownerName || undefined,
      ownerPhone: f.ownerPhone || undefined,
      imageUrls: f.imageUrls,
    }
    try {
      if (initial) {
        await updateEquipment(initial.id, input)
      } else {
        await createEquipment(input)
      }
      notify(ru.catalog.created)
      onSaved()
    } catch (e) {
      setError(errorText(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="card">
      <div className="card__body">
        <div className="card__title">{initial ? ru.catalog.tabs.equipment : ru.catalog.equipmentNew}</div>
        <Field label={ru.catalog.fields.category}>
          <Select
            value={f.category}
            onChange={(v) => setF({ ...f, category: v })}
            options={ru.catalog.categories.map((c) => ({ value: c, label: c }))}
          />
        </Field>
        <Field label={ru.catalog.fields.brand}>
          <Input value={f.brand} onChange={(e) => setF({ ...f, brand: e.target.value })} />
        </Field>
        <Field label={ru.catalog.fields.model}>
          <Input value={f.model} onChange={(e) => setF({ ...f, model: e.target.value })} />
        </Field>
        <Field label="Год выпуска">
          <Input
            inputMode="numeric"
            value={f.year}
            onChange={(e) => setF({ ...f, year: e.target.value.replace(/\D/g, '').slice(0, 4) })}
          />
        </Field>
        <Field label={ru.catalog.fields.capacity}>
          <Input
            inputMode="numeric"
            value={f.capacity}
            onChange={(e) => setF({ ...f, capacity: e.target.value.replace(/\D/g, '') })}
          />
        </Field>
        {BOOM_CATEGORIES.includes(f.category) ? (
          <Field label={ru.catalog.fields.boomLength}>
            <Input value={f.boomLength} onChange={(e) => setF({ ...f, boomLength: e.target.value })} />
          </Field>
        ) : null}
        <Field label={ru.catalog.fields.price}>
          <Input
            inputMode="numeric"
            value={f.price}
            onChange={(e) => setF({ ...f, price: e.target.value.replace(/\D/g, '') })}
          />
        </Field>
        <Field label={ru.catalog.fields.priceUnit}>
          <Select value={f.priceUnit} onChange={(v) => setF({ ...f, priceUnit: v })} options={ru.catalog.priceUnits} />
        </Field>
        <Field label={ru.catalog.fields.district}>
          <Select
            value={f.district}
            onChange={(v) => setF({ ...f, district: v })}
            options={ru.catalog.districts}
            allowEmpty
          />
        </Field>
        <Field label={ru.catalog.fields.description} hint={ru.catalog.fields.descriptionHint}>
          <Textarea value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} />
        </Field>
        <Field label={ru.catalog.fields.ownerName}>
          <Input value={f.ownerName} onChange={(e) => setF({ ...f, ownerName: e.target.value })} />
        </Field>
        <Field label={ru.catalog.fields.ownerPhone}>
          <Input
            inputMode="tel"
            value={f.ownerPhone}
            onChange={(e) => setF({ ...f, ownerPhone: e.target.value })}
          />
        </Field>
        <PhotoInput value={f.imageUrls} onChange={(urls) => setF({ ...f, imageUrls: urls })} />
        {error ? <div className="field__error">{error}</div> : null}
        <div className="card__actions">
          <Button variant="primary" size="medium" loading={busy} onClick={() => void submit()}>
            {ru.catalog.actions.save}
          </Button>
          <Button variant="secondary" size="medium" onClick={onClose}>
            {ru.catalog.actions.cancel}
          </Button>
        </div>
      </div>
    </div>
  )
}

// ---------------- Material ----------------

function MaterialForm({
  initial,
  onClose,
  onSaved,
}: {
  initial?: AdminMaterial
  onClose: () => void
  onSaved: () => void
}) {
  const { notify } = useToast()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [f, setF] = useState({
    category: initial?.type ?? ru.catalog.materialTypes[0],
    fraction: initial?.fraction ?? '',
    price: initial?.pricePerUnit != null ? String(initial.pricePerUnit) : '',
    unit: initial?.unit ?? 'тонна',
    deliveryType: initial?.deliveryType ?? 'DELIVERY',
    district: districtOf(initial?.location),
    supplierName: initial?.supplierPhone && initial.supplierPhone !== '+70000000000' ? (initial.supplierName ?? '') : '',
    supplierPhone: initial?.supplierPhone && initial.supplierPhone !== '+70000000000' ? initial.supplierPhone : '',
    imageUrls: initial?.imageUrls ?? (initial?.imageUrl ? [initial.imageUrl] : []),
  })

  const submit = async () => {
    if (!f.category) {
      setError('Выберите тип материала')
      return
    }
    if (!f.price || Number(f.price) <= 0) {
      setError('Цена должна быть больше 0')
      return
    }
    setBusy(true)
    setError('')
    const input: AdminMaterialInput = {
      category: f.category,
      fraction: f.fraction || undefined,
      price: Number(f.price),
      unit: f.unit,
      deliveryType: f.deliveryType,
      district: f.district || undefined,
      supplierName: f.supplierName || undefined,
      supplierPhone: f.supplierPhone || undefined,
      imageUrls: f.imageUrls,
    }
    try {
      if (initial) {
        await updateMaterial(initial.id, input)
      } else {
        await createMaterial(input)
      }
      notify(ru.catalog.created)
      onSaved()
    } catch (e) {
      setError(errorText(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="card">
      <div className="card__body">
        <div className="card__title">{initial ? ru.catalog.tabs.materials : ru.catalog.materialNew}</div>
        <Field label={ru.catalog.materialFields.category}>
          <Select
            value={f.category}
            onChange={(v) => setF({ ...f, category: v })}
            options={ru.catalog.materialTypes.map((t) => ({ value: t, label: t }))}
          />
        </Field>
        <Field label={ru.catalog.materialFields.fraction}>
          <Select
            value={f.fraction}
            onChange={(v) => setF({ ...f, fraction: v })}
            options={ru.catalog.fractions}
            allowEmpty
          />
        </Field>
        <Field label={ru.catalog.materialFields.price}>
          <Input
            inputMode="numeric"
            value={f.price}
            onChange={(e) => setF({ ...f, price: e.target.value.replace(/\D/g, '') })}
          />
        </Field>
        <Field label={ru.catalog.materialFields.unit}>
          <Select value={f.unit} onChange={(v) => setF({ ...f, unit: v })} options={ru.catalog.units} />
        </Field>
        <Field label={ru.catalog.materialFields.deliveryType}>
          <Select
            value={f.deliveryType}
            onChange={(v) => setF({ ...f, deliveryType: v })}
            options={ru.catalog.deliveryTypes}
          />
        </Field>
        <Field label={ru.catalog.materialFields.district}>
          <Select
            value={f.district}
            onChange={(v) => setF({ ...f, district: v })}
            options={ru.catalog.districts}
            allowEmpty
          />
        </Field>
        <Field label={ru.catalog.materialFields.supplierName}>
          <Input value={f.supplierName} onChange={(e) => setF({ ...f, supplierName: e.target.value })} />
        </Field>
        <Field label={ru.catalog.materialFields.supplierPhone}>
          <Input
            inputMode="tel"
            value={f.supplierPhone}
            onChange={(e) => setF({ ...f, supplierPhone: e.target.value })}
          />
        </Field>
        <PhotoInput value={f.imageUrls} onChange={(urls) => setF({ ...f, imageUrls: urls })} max={1} />
        {error ? <div className="field__error">{error}</div> : null}
        <div className="card__actions">
          <Button variant="primary" size="medium" loading={busy} onClick={() => void submit()}>
            {ru.catalog.actions.save}
          </Button>
          <Button variant="secondary" size="medium" onClick={onClose}>
            {ru.catalog.actions.cancel}
          </Button>
        </div>
      </div>
    </div>
  )
}

// ---------------- Workshop ----------------

function WorkshopForm({
  initial,
  onClose,
  onSaved,
}: {
  initial?: AdminWorkshop
  onClose: () => void
  onSaved: () => void
}) {
  const { notify } = useToast()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [f, setF] = useState({
    name: initial?.name ?? '',
    specialization: initial?.specialization ?? '',
    city: 'Воронеж',
    district: districtOf(initial?.location),
    phone: initial?.phone ?? '',
  })

  const submit = async () => {
    if (!f.name.trim()) {
      setError('Укажите название компании')
      return
    }
    if (!f.specialization.trim()) {
      setError('Укажите специализацию')
      return
    }
    if (!f.phone.trim()) {
      setError('Укажите телефон')
      return
    }
    setBusy(true)
    setError('')
    const input: AdminWorkshopInput = {
      name: f.name,
      specialization: f.specialization,
      city: f.city,
      district: f.district || undefined,
      phone: f.phone,
    }
    try {
      if (initial) {
        await updateWorkshop(initial.id, input)
      } else {
        await createWorkshop(input)
      }
      notify(ru.catalog.created)
      onSaved()
    } catch (e) {
      setError(errorText(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="card">
      <div className="card__body">
        <div className="card__title">{initial ? ru.catalog.tabs.workshops : ru.catalog.workshopNew}</div>
        <Field label={ru.catalog.workshopFields.name}>
          <Input value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} />
        </Field>
        <Field label={ru.catalog.workshopFields.specialization}>
          <Textarea value={f.specialization} onChange={(e) => setF({ ...f, specialization: e.target.value })} />
        </Field>
        <Field label={ru.catalog.workshopFields.city}>
          <Select value={f.city} onChange={(v) => setF({ ...f, city: v })} options={ru.catalog.cities} />
        </Field>
        <Field label={ru.catalog.workshopFields.district}>
          <Select
            value={f.district}
            onChange={(v) => setF({ ...f, district: v })}
            options={ru.catalog.districts}
            allowEmpty
          />
        </Field>
        <Field label={ru.catalog.workshopFields.phone}>
          <Input value={f.phone} onChange={(e) => setF({ ...f, phone: e.target.value })} />
        </Field>
        {error ? <div className="field__error">{error}</div> : null}
        <div className="card__actions">
          <Button variant="primary" size="medium" loading={busy} onClick={() => void submit()}>
            {ru.catalog.actions.save}
          </Button>
          <Button variant="secondary" size="medium" onClick={onClose}>
            {ru.catalog.actions.cancel}
          </Button>
        </div>
      </div>
    </div>
  )
}

// ---------------- List rows ----------------

function RowActions({ onEdit, onDelete }: { onEdit: () => void; onDelete: () => void }) {
  return (
    <div className="card__actions">
      <Button variant="secondary" size="small" onClick={onEdit}>
        {ru.catalog.actions.edit}
      </Button>
      <Button variant="destructive" size="small" onClick={onDelete}>
        {ru.catalog.actions.delete}
      </Button>
    </div>
  )
}

function EquipmentRow({ item, reload }: { item: AdminEquipment; reload: () => void }) {
  const { notify } = useToast()
  const [editing, setEditing] = useState(false)
  const image = item.imageUrls?.[0] ?? item.imageUrl
  if (editing) {
    return (
      <Modal title="Редактирование техники" onClose={() => setEditing(false)}>
        <EquipmentForm initial={item} onClose={() => setEditing(false)} onSaved={() => { setEditing(false); reload() }} />
      </Modal>
    )
  }
  const remove = async () => {
    if (!window.confirm(ru.catalog.actions.confirmDelete)) return
    try {
      await deleteEquipment(item.id)
      notify(ru.catalog.deleted)
      reload()
    } catch (e) {
      notify(errorText(e))
    }
  }
  return (
    <article className="card">
      {image ? <img className="card__image" src={image} alt="" /> : null}
      <div className="card__body">
        <div className="card__title">{item.type}</div>
        <div className="card__category">
          {item.category}
          {item.location ? ` · ${item.location}` : ''}
        </div>
        <div className="card__rows">
          {item.year ? <div className="card__row"><span className="card__row-label">Год</span><span className="card__row-value">{item.year}</span></div> : null}
          {item.capacity ? <div className="card__row"><span className="card__row-label">{ru.catalog.fields.capacity}</span><span className="card__row-value">{item.capacity}</span></div> : null}
          {item.boomLength ? <div className="card__row"><span className="card__row-label">{ru.catalog.fields.boomLength}</span><span className="card__row-value">{item.boomLength}</span></div> : null}
          {item.price ? <div className="card__row"><span className="card__row-label">{ru.catalog.fields.price}</span><span className="card__row-value">{item.price} ₽</span></div> : null}
          {item.ownerName ? <div className="card__row"><span className="card__row-label">{ru.catalog.fields.ownerName}</span><span className="card__row-value">{item.ownerName}{item.ownerPhone ? `, ${item.ownerPhone}` : ''}</span></div> : null}
        </div>
        <RowActions onEdit={() => setEditing(true)} onDelete={() => void remove()} />
      </div>
    </article>
  )
}

function ModerationRow({ item, reload }: { item: AdminEquipment; reload: () => void }) {
  const { notify } = useToast()
  const [busy, setBusy] = useState(false)
  const image = item.imageUrls?.[0] ?? item.imageUrl
  const run = async (action: () => Promise<string>) => {
    setBusy(true)
    try {
      const result = await action()
      notify(result === 'отклонено' ? ru.catalog.actions.rejected : ru.catalog.actions.approved)
      reload()
    } catch (e) {
      notify(errorText(e))
    } finally {
      setBusy(false)
    }
  }
  return (
    <article className="card">
      {image ? <img className="card__image" src={image} alt="" /> : null}
      <div className="card__body">
        <div className="card__title">{item.type}</div>
        <div className="card__category">
          {item.category}
          {item.location ? ` · ${item.location}` : ''}
        </div>
        <div className="card__rows">
          {item.capacity ? <div className="card__row"><span className="card__row-label">{ru.catalog.fields.capacity}</span><span className="card__row-value">{item.capacity}</span></div> : null}
          {item.price ? <div className="card__row"><span className="card__row-label">{ru.catalog.fields.price}</span><span className="card__row-value">{item.price} ₽</span></div> : null}
          {item.ownerName ? <div className="card__row"><span className="card__row-label">{ru.catalog.fields.ownerName}</span><span className="card__row-value">{item.ownerName}{item.ownerPhone ? `, ${item.ownerPhone}` : ''}</span></div> : null}
          {item.description ? <div className="card__row"><span className="card__row-label">{ru.catalog.fields.description}</span><span className="card__row-value">{item.description}</span></div> : null}
        </div>
        <div className="card__actions">
          <Button variant="primary" size="medium" loading={busy} onClick={() => void run(() => approveEquipment(item.id))}>
            {ru.catalog.actions.approve}
          </Button>
          <Button variant="destructive" size="medium" disabled={busy} onClick={() => void run(() => rejectEquipment(item.id))}>
            {ru.catalog.actions.reject}
          </Button>
        </div>
      </div>
    </article>
  )
}

function MaterialRow({ item, reload }: { item: AdminMaterial; reload: () => void }) {
  const { notify } = useToast()
  const [editing, setEditing] = useState(false)
  if (editing) {
    return (
      <Modal title="Редактирование материала" onClose={() => setEditing(false)}>
        <MaterialForm initial={item} onClose={() => setEditing(false)} onSaved={() => { setEditing(false); reload() }} />
      </Modal>
    )
  }
  const remove = async () => {
    if (!window.confirm(ru.catalog.actions.confirmDelete)) return
    try {
      await deleteMaterial(item.id)
      notify(ru.catalog.deleted)
      reload()
    } catch (e) {
      notify(errorText(e))
    }
  }
  const toggleStatus = async () => {
    try {
      const next = item.status === 'HIDDEN' ? 'APPROVED' : 'HIDDEN'
      await setMaterialStatus(item.id, next)
      notify(next === 'HIDDEN' ? 'Скрыто' : 'Опубликовано')
      reload()
    } catch (e) {
      notify(errorText(e))
    }
  }
  return (
    <article className="card">
      {(item.imageUrls?.[0] ?? item.imageUrl) ? (
        <img className="card__image" src={item.imageUrls?.[0] ?? item.imageUrl ?? ''} alt="" />
      ) : null}
      <div className="card__body">
        <div className="card__title">{item.type}{item.fraction ? ` ${item.fraction}` : ''}</div>
        <div className="card__category">{item.location}</div>
        <div className="card__rows">
          {item.pricePerUnit ? <div className="card__row"><span className="card__row-label">{ru.catalog.materialFields.price}</span><span className="card__row-value">{item.pricePerUnit} ₽ / {item.unit}</span></div> : null}
          {item.supplierName ? <div className="card__row"><span className="card__row-label">{ru.catalog.materialFields.supplierName}</span><span className="card__row-value">{item.supplierName}{item.supplierPhone ? `, ${item.supplierPhone}` : ''}</span></div> : null}
          <div className="card__row"><span className="card__row-label">Статус</span><span className="card__row-value">{item.status ?? '—'}</span></div>
        </div>
        <div className="card__actions">
          <Button variant="secondary" size="small" onClick={() => void toggleStatus()}>
            {item.status === 'HIDDEN' ? 'Опубликовать' : 'Скрыть'}
          </Button>
          <RowActions onEdit={() => setEditing(true)} onDelete={() => void remove()} />
        </div>
      </div>
    </article>
  )
}

function WorkshopRow({ item, reload }: { item: AdminWorkshop; reload: () => void }) {
  const { notify } = useToast()
  const [editing, setEditing] = useState(false)
  if (editing) {
    return (
      <Modal title="Редактирование мастерской" onClose={() => setEditing(false)}>
        <WorkshopForm initial={item} onClose={() => setEditing(false)} onSaved={() => { setEditing(false); reload() }} />
      </Modal>
    )
  }
  const remove = async () => {
    if (!window.confirm(ru.catalog.actions.confirmDelete)) return
    try {
      await deleteWorkshop(item.id)
      notify(ru.catalog.deleted)
      reload()
    } catch (e) {
      notify(errorText(e))
    }
  }
  return (
    <article className="card">
      <div className="card__body">
        <div className="card__title">{item.name}</div>
        <div className="card__category">{item.location}</div>
        <div className="card__rows">
          <div className="card__row"><span className="card__row-label">{ru.catalog.workshopFields.phone}</span><span className="card__row-value">{item.phone}</span></div>
        </div>
        <RowActions onEdit={() => setEditing(true)} onDelete={() => void remove()} />
      </div>
    </article>
  )
}

// ---------------- Screen ----------------

export function Catalog() {
  const [searchParams] = useSearchParams()
  const requestedType = searchParams.get('type')
  const requestedStatus = searchParams.get('status')
  const [tab, setTab] = useState<CatalogTab>(
    requestedType === 'materials' || requestedType === 'workshops' ? requestedType : 'equipment',
  )
  const [creating, setCreating] = useState(false)
  const [statusFilter, setStatusFilter] = useState(
    requestedStatus === 'HIDDEN' || requestedStatus === 'PENDING' || requestedStatus === 'all'
      ? requestedStatus
      : '',
  )
  const [query, setQuery] = useState('')

  const load = useCallback((): Promise<CatalogItem[]> => {
    const status = statusFilter || undefined
    if (tab === 'equipment') {
      return fetchEquipmentList(undefined, status)
    }
    if (tab === 'materials') {
      return fetchMaterials(undefined, status)
    }
    return fetchWorkshops(status)
  }, [tab, statusFilter])
  const { data, loading, error, reload } = useAsync(load)

  const switchTab = (next: CatalogTab) => {
    setCreating(false)
    setStatusFilter('')
    setQuery('')
    setTab(next)
  }

  const statusOptions =
    tab === 'equipment'
      ? [
          { value: '', label: ru.catalog.filters.published },
          { value: 'PENDING', label: ru.catalog.filters.pending },
          { value: 'HIDDEN', label: ru.catalog.filters.hidden },
          { value: 'all', label: 'Все' },
        ]
      : tab === 'materials'
        ? [
            { value: '', label: ru.catalog.filters.approved },
            { value: 'HIDDEN', label: ru.catalog.filters.hidden },
            { value: 'PENDING', label: ru.catalog.filters.pending },
            { value: 'all', label: 'Все' },
          ]
        : [
            { value: '', label: ru.catalog.filters.approved },
            { value: 'PENDING', label: ru.catalog.filters.pending },
            { value: 'all', label: 'Все' },
          ]

  const close = () => setCreating(false)
  const saved = () => {
    setCreating(false)
    reload()
  }

  const renderForm = () => {
    if (!creating) {
      return null
    }
    if (tab === 'equipment') {
      return <Modal title={ru.catalog.equipmentNew} onClose={close}><EquipmentForm onClose={close} onSaved={saved} /></Modal>
    }
    if (tab === 'materials') {
      return <Modal title={ru.catalog.materialNew} onClose={close}><MaterialForm onClose={close} onSaved={saved} /></Modal>
    }
    return <Modal title={ru.catalog.workshopNew} onClose={close}><WorkshopForm onClose={close} onSaved={saved} /></Modal>
  }

  const renderList = () => {
    if (loading) return <LoadingState />
    if (error) return <ErrorState onRetry={reload} />
    if (!data || data.length === 0) return <div className="state">{ru.catalog.emptyList}</div>
    const normalized = query.trim().toLowerCase()
    const visible = !normalized ? data : data.filter((item) => {
      if (tab === 'equipment') {
        const equipment = item as AdminEquipment
        return [equipment.type, equipment.category, equipment.ownerName, equipment.ownerPhone]
          .filter(Boolean).some((value) => String(value).toLowerCase().includes(normalized))
      }
      if (tab === 'materials') {
        const material = item as AdminMaterial
        return [material.type, material.fraction, material.supplierName]
          .filter(Boolean).some((value) => String(value).toLowerCase().includes(normalized))
      }
      const workshop = item as AdminWorkshop
      return [workshop.name, workshop.specialization, workshop.phone]
        .filter(Boolean).some((value) => String(value).toLowerCase().includes(normalized))
    })
    if (visible.length === 0) return <div className="state">{ru.catalog.emptyList}</div>
    if (tab === 'equipment') {
      return (
        <div className="cards-grid">
          {visible.map((i) => {
            const equipment = i as AdminEquipment
            return equipment.status === 'PENDING'
              ? <ModerationRow key={equipment.id} item={equipment} reload={reload} />
              : <EquipmentRow key={equipment.id} item={equipment} reload={reload} />
          })}
        </div>
      )
    }
    if (tab === 'materials') {
      return <div className="cards-grid">{visible.map((i) => <MaterialRow key={i.id} item={i as AdminMaterial} reload={reload} />)}</div>
    }
    return <div className="cards-grid">{visible.map((i) => <WorkshopRow key={i.id} item={i as AdminWorkshop} reload={reload} />)}</div>
  }

  return (
    <div className="page">
      <div className="page__head">
        <div>
          <h1 className="page__title">{ru.tabs.catalog}</h1>
          <p className="page__subtitle">Карточки каталога без заявок</p>
        </div>
        <div className="toolbar">
          <input
            className="search-input"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Поиск по каталогу"
          />
          <Button variant="primary" size="medium" onClick={() => setCreating(true)}>
            {ru.catalog.actions.add}
          </Button>
        </div>
      </div>

      <div className="tabs">
        {TABS.map((item) => (
          <button
            key={item.key}
            type="button"
            className={item.key === tab ? 'tabs__item tabs__item--active' : 'tabs__item'}
            onClick={() => switchTab(item.key)}
          >
            {item.label}
          </button>
        ))}
      </div>

      <div className="field field--inline">
        <span className="field__label">{ru.catalog.filters.label}</span>
        <Select value={statusFilter} onChange={setStatusFilter} options={statusOptions} />
      </div>

      {renderForm()}
      {renderList()}
    </div>
  )
}
