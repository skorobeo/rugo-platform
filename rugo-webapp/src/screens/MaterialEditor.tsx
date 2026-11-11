import { useState } from 'react'
import { Button, Input } from '@maxhub/max-ui'
import { bridge } from '../bridge'
import { ru } from '../content/ru'
import { Screen } from '../components/Screen'
import { CategoryPicker } from '../components/CategoryPicker'
import { Checkbox } from '../components/Checkbox'
import type { Material } from '../api/catalog'
import type { PartnerProfile } from '../api/partner'
import { createMyMaterial, submitMaterialChange } from '../api/partner'
import { uploadPhoto } from '../api/uploads'
import { compressPhoto } from '../lib/image'
import { ApiError } from '../api/client'
import { firstUnhandledError, normalizeApiErrors } from '../lib/apiErrors'

const MAX_PHOTOS = 3
const HANDLED_FIELDS = ['category', 'price', 'photo', 'companyName'] as const

function districtOf(location: string | null | undefined): string {
  if (!location) {
    return ''
  }
  const comma = location.indexOf(',')
  return comma >= 0 ? location.slice(comma + 1).trim() : ''
}

function isNoFraction(type: string): boolean {
  return (ru.materials.noFractionTypes as readonly string[]).includes(type)
}

const MATERIAL_LABELS: Record<string, string> = Object.fromEntries(
  ru.materials.types.map((type) => [type.value, type.label]),
)

/** Создание/правка своего материала. Оба действия уходят на модерацию (заявкой). */
export function MaterialEditor({
  item,
  profile,
  onClose,
}: {
  item?: Material
  profile?: PartnerProfile
  onClose: () => void
}) {
  const alreadyCompany = profile?.partnerType === 'company'
  const [isCompany, setIsCompany] = useState(alreadyCompany)
  const [companyName, setCompanyName] = useState(alreadyCompany ? (profile?.name ?? '') : '')
  const [category, setCategory] = useState(item?.type ?? '')
  const [fraction, setFraction] = useState(item?.fraction ?? '')
  const [price, setPrice] = useState(item?.pricePerUnit != null ? String(item.pricePerUnit) : '')
  const [unit, setUnit] = useState(item?.unit ?? 'тонна')
  const [deliveryType, setDeliveryType] = useState(item?.deliveryType ?? 'DELIVERY')
  const [district, setDistrict] = useState(districtOf(item?.location))
  const [photos, setPhotos] = useState<string[]>(
    item?.imageUrls ?? (item?.imageUrl ? [item.imageUrl] : []),
  )
  const [uploading, setUploading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [done, setDone] = useState(false)

  const noFraction = isNoFraction(category)

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
    const fractionValue = !noFraction && fraction.trim() ? fraction.trim() : undefined
    try {
      if (item) {
        await submitMaterialChange(
          {
            materialId: item.id,
            action: 'update',
            category,
            fraction: fractionValue,
            price: Number(price),
            unit,
            deliveryType,
            district: district || undefined,
            photos: photos.length > 0 ? photos : undefined,
          },
          bridge.getInitData(),
        )
      } else {
        await createMyMaterial(
          {
            category,
            fraction: fractionValue,
            price: Number(price),
            unit,
            deliveryType,
            district: district || undefined,
            photos: photos.length > 0 ? photos : undefined,
            partnerType: isCompany ? 'company' : undefined,
            companyName: isCompany ? companyName.trim() : undefined,
          },
          bridge.getInitData(),
        )
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
          <p className="menu-item__title">
            {item ? ru.cabinet.materialSubmittedTitle : ru.cabinet.materialCreatedTitle}
          </p>
          <p className="prose__muted">
            {item ? ru.cabinet.materialSubmittedText : ru.cabinet.materialCreatedText}
          </p>
          <Button variant="primary" size="large" stretched onClick={onClose}>
            {ru.cabinet.submittedBack}
          </Button>
        </div>
      </Screen>
    )
  }

  return (
    <Screen
      title={item ? ru.cabinet.materialEditorTitleEdit : ru.cabinet.materialEditorTitleNew}
      onBack={onClose}
    >
      <div className="lead-form">
        <div className="field" role="group" aria-label={ru.cabinet.fieldMaterialType}>
          <span className="field__label">{ru.cabinet.fieldMaterialType}</span>
          <CategoryPicker
            value={category}
            onChange={setCategory}
            groups={ru.materials.groups}
            labels={MATERIAL_LABELS}
          />
          {errors.category ? <span className="field__error">{errors.category}</span> : null}
        </div>

        {!noFraction ? (
          <label className="field">
            <span className="field__label">{ru.cabinet.fieldFraction}</span>
            <Input value={fraction} onChange={(e) => setFraction(e.target.value)} />
          </label>
        ) : null}

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
          <span className="field__label">{ru.cabinet.fieldUnit}</span>
          <select className="lead-select" value={unit} onChange={(e) => setUnit(e.target.value)}>
            {ru.form.materialUnits.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span className="field__label">{ru.cabinet.fieldDelivery}</span>
          <select
            className="lead-select"
            value={deliveryType}
            onChange={(e) => setDeliveryType(e.target.value)}
          >
            {ru.form.deliveryTypes.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
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

        {!item ? (
          alreadyCompany ? (
            <div className="field">
              <span className="field__label">{ru.form.partnerTypeCompany}</span>
              <p className="prose__muted">{profile?.name}</p>
            </div>
          ) : (
            <>
              <div className="field">
                <Checkbox id="material-company" checked={isCompany} onChange={setIsCompany}>
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
