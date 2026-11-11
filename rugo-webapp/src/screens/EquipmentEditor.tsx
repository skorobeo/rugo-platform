import { useState } from 'react'
import { Button, Input, Textarea } from '@maxhub/max-ui'
import { bridge } from '../bridge'
import { ru } from '../content/ru'
import { Screen } from '../components/Screen'
import { CategoryPicker } from '../components/CategoryPicker'
import { Checkbox } from '../components/Checkbox'
import type { Equipment } from '../api/catalog'
import type { PartnerProfile } from '../api/partner'
import { createMyEquipment, updateMyEquipment, type PartnerEquipmentInput } from '../api/partner'
import { uploadPhoto } from '../api/uploads'
import { compressPhoto } from '../lib/image'
import { ApiError } from '../api/client'
import { firstUnhandledError, normalizeApiErrors } from '../lib/apiErrors'

const MAX_PHOTOS = 3
const HANDLED_FIELDS = ['category', 'capacity', 'price', 'photo', 'companyName'] as const

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

export function EquipmentEditor({
  item,
  profile,
  onClose,
}: {
  item?: Equipment
  profile?: PartnerProfile
  onClose: () => void
}) {
  const alreadyCompany = profile?.partnerType === 'company'
  const [isCompany, setIsCompany] = useState(alreadyCompany)
  const [companyName, setCompanyName] = useState(alreadyCompany ? (profile?.name ?? '') : '')
  const [category, setCategory] = useState(item?.category ?? '')
  const [brand, setBrand] = useState(item?.brand ?? '')
  const [model, setModel] = useState(item?.model ?? '')
  const [capacity, setCapacity] = useState(firstNumber(item?.capacity))
  const [price, setPrice] = useState(item?.price != null ? String(item.price) : '')
  const [priceUnit, setPriceUnit] = useState(priceUnitOf(item?.priceType))
  const [district, setDistrict] = useState(districtOf(item?.location))
  const [description, setDescription] = useState(item?.description ?? '')
  const [photos, setPhotos] = useState<string[]>(item?.imageUrls ?? (item?.imageUrl ? [item.imageUrl] : []))
  const [uploading, setUploading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [done, setDone] = useState(false)

  const addPhotos = async (files: FileList | null) => {
    if (!files || files.length === 0) {
      return
    }
    const free = MAX_PHOTOS - photos.length
    if (free <= 0) {
      return
    }
    setUploading(true)
    try {
      const uploaded: string[] = []
      for (const file of Array.from(files).slice(0, free)) {
        const compressed = await compressPhoto(file)
        uploaded.push(await uploadPhoto(compressed, bridge.getInitData()))
      }
      setPhotos((prev) => [...prev, ...uploaded].slice(0, MAX_PHOTOS))
      setErrors((prev) => ({ ...prev, photo: '' }))
    } catch {
      setErrors((prev) => ({ ...prev, photo: ru.cabinet.errPhoto }))
    } finally {
      setUploading(false)
    }
  }

  const validate = (): Record<string, string> => {
    const next: Record<string, string> = {}
    if (!category) {
      next.category = ru.cabinet.errCategory
    }
    if (!capacity || Number(capacity) <= 0) {
      next.capacity = ru.cabinet.errCapacity
    }
    if (!price || Number(price) <= 0) {
      next.price = ru.cabinet.errPrice
    }
    if (isCompany && !companyName.trim()) {
      next.companyName = 'Укажите название компании'
    }
    return next
  }

  const submit = async () => {
    const next = validate()
    setErrors(next)
    if (Object.keys(next).length > 0) {
      return
    }
    setBusy(true)
    const input: PartnerEquipmentInput = {
      category,
      brand: brand.trim() || undefined,
      model: model.trim() || undefined,
      capacity: Number(capacity),
      price: Number(price),
      priceUnit,
      district: district || undefined,
      description: description.trim() || undefined,
      imageUrls: photos.length > 0 ? photos : undefined,
      partnerType: isCompany ? 'company' : undefined,
      companyName: isCompany ? companyName.trim() : undefined,
    }
    try {
      if (item) {
        await updateMyEquipment(item.id, input, bridge.getInitData())
      } else {
        await createMyEquipment(input, bridge.getInitData())
      }
      setDone(true)
    } catch (e) {
      if (e instanceof ApiError && Object.keys(e.errors).length > 0) {
        setErrors(normalizeApiErrors(e.errors))
      } else {
        setErrors({ form: ru.cabinet.loadError })
      }
    } finally {
      setBusy(false)
    }
  }

  if (done) {
    return (
      <Screen title={ru.cabinet.title} onBack={onClose}>
        <div className="prose">
          <p className="menu-item__title">{ru.cabinet.submittedTitle}</p>
          <p className="prose__muted">{ru.cabinet.submittedText}</p>
          <Button variant="primary" size="large" stretched onClick={onClose}>
            {ru.cabinet.submittedBack}
          </Button>
        </div>
      </Screen>
    )
  }

  return (
    <Screen title={item ? ru.cabinet.editorTitleEdit : ru.cabinet.editorTitleNew} onBack={onClose}>
      <div className="lead-form">
        <div className="field" role="group" aria-label={ru.cabinet.fieldCategory}>
          <span className="field__label">{ru.cabinet.fieldCategory}</span>
          <CategoryPicker value={category} onChange={setCategory} />
          {errors.category ? <span className="field__error">{errors.category}</span> : null}
        </div>

        <label className="field">
          <span className="field__label">{ru.cabinet.fieldBrand}</span>
          <Input value={brand} onChange={(e) => setBrand(e.target.value)} />
        </label>

        <label className="field">
          <span className="field__label">{ru.cabinet.fieldModel}</span>
          <Input value={model} onChange={(e) => setModel(e.target.value)} />
        </label>

        <label className="field">
          <span className="field__label">{ru.cabinet.fieldCapacity}</span>
          <Input
            inputMode="numeric"
            value={capacity}
            onChange={(e) => setCapacity(e.target.value.replace(/\D/g, ''))}
          />
          {errors.capacity ? <span className="field__error">{errors.capacity}</span> : null}
        </label>

        <label className="field">
          <span className="field__label">{ru.cabinet.fieldPrice}</span>
          <Input
            inputMode="numeric"
            value={price}
            onChange={(e) => setPrice(e.target.value.replace(/\D/g, ''))}
          />
          {errors.price ? <span className="field__error">{errors.price}</span> : null}
        </label>

        <label className="field">
          <span className="field__label">{ru.cabinet.fieldPriceUnit}</span>
          <select className="lead-select" value={priceUnit} onChange={(e) => setPriceUnit(e.target.value)}>
            {ru.form.priceUnits.map((unit) => (
              <option key={unit.value} value={unit.value}>
                {unit.label}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span className="field__label">{ru.cabinet.fieldDistrict}</span>
          <select
            className="lead-select"
            value={district}
            onChange={(e) => setDistrict(e.target.value)}
          >
            <option value="">— не выбрано —</option>
            {ru.form.districts.map((d) => (
              <option key={d.value} value={d.value}>
                {d.label}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span className="field__label">{ru.cabinet.fieldDescription}</span>
          <Textarea value={description} onChange={(e) => setDescription(e.target.value)} />
        </label>

        {!item ? (
          alreadyCompany ? (
            <div className="field">
              <span className="field__label">{ru.form.partnerTypeCompany}</span>
              <p className="prose__muted">{profile?.name}</p>
            </div>
          ) : (
            <>
              <div className="field">
                <Checkbox id="equipment-company" checked={isCompany} onChange={setIsCompany}>
                  {ru.form.partnerTypeCompany}
                </Checkbox>
              </div>
              {isCompany ? (
                <label className="field">
                  <span className="field__label">{ru.form.companyLabel}</span>
                  <Input
                    value={companyName}
                    onChange={(e) => setCompanyName(e.target.value)}
                    placeholder={ru.form.companyPlaceholder}
                  />
                  {errors.companyName ? <span className="field__error">{errors.companyName}</span> : null}
                </label>
              ) : null}
            </>
          )
        ) : null}

        <div className="field">
          <span className="field__label">{ru.cabinet.fieldPhotos}</span>
          {photos.length > 0 ? (
            <div className="photos">
              {photos.map((src) => (
                <div className="photos__item" key={src}>
                  <img className="photos__img" src={src} alt="" />
                  <button
                    type="button"
                    className="photos__remove"
                    onClick={() => setPhotos((prev) => prev.filter((p) => p !== src))}
                    aria-label={ru.cabinet.removePhoto}
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          ) : null}
          {photos.length < MAX_PHOTOS ? (
            <label className={uploading ? 'photo-add photo-add--disabled' : 'photo-add'}>
              <input
                type="file"
                accept="image/*"
                multiple
                hidden
                disabled={uploading}
                onChange={(e) => {
                  void addPhotos(e.target.files)
                  e.target.value = ''
                }}
              />
              {uploading ? ru.cabinet.uploading : ru.cabinet.addPhotos}
            </label>
          ) : null}
          {errors.photo ? <span className="field__error">{errors.photo}</span> : null}
        </div>

        {(() => {
          const generalError = firstUnhandledError(errors, HANDLED_FIELDS)
          return generalError ? <span className="field__error">{generalError}</span> : null
        })()}

        <Button variant="primary" size="large" stretched loading={busy} disabled={busy} onClick={() => void submit()}>
          {busy ? ru.cabinet.saving : ru.cabinet.save}
        </Button>
      </div>
    </Screen>
  )
}
