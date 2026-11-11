import { useCallback, useEffect, useRef, useState } from 'react'
import { Button, Input, Textarea } from '@maxhub/max-ui'
import { bridge } from '../bridge'
import { track } from '../analytics/track'
import { ApiError } from '../api/client'
import { submitPartnerLead, type LeadDetailsPayload, type PartnerType } from '../api/leads'
import { uploadPhoto } from '../api/uploads'
import { compressPhoto } from '../lib/image'
import { ru } from '../content/ru'
import { Chips } from '../components/Chips'
import { Checkbox } from '../components/Checkbox'
import { Screen } from '../components/Screen'
import { formatPhoneDisplay, isValidCompany, isValidName, normalizeRuPhone } from './validators'
import type { OfferType } from '../navigation'

type Status =
  | 'editing'
  | 'submitting'
  | 'success'
  | 'successNoMatch'
  | 'successPark'
  | 'duplicate'
  | 'error'
type Step = 1 | 2 | 3
type Direction = 'equipment' | 'materials'

function isDirection(offerType: OfferType): offerType is Direction {
  return offerType === 'equipment' || offerType === 'materials'
}

const DETAILS_PREFIX = 'details.'
const MAX_PHOTOS = 3

/** Приводит ключи ошибок вида `details.brand` к именам полей формы (`brand`). */
function normalizeFieldErrors(errors: Record<string, string>): Record<string, string> {
  const result: Record<string, string> = {}
  for (const [key, message] of Object.entries(errors)) {
    result[key.startsWith(DETAILS_PREFIX) ? key.slice(DETAILS_PREFIX.length) : key] = message
  }
  return result
}

export function LeadForm({
  offerType,
  initialPartnerType,
  onClose,
}: {
  offerType: OfferType
  initialPartnerType?: PartnerType
  onClose: () => void
}) {
  const wizard = isDirection(offerType)
  const direction: Direction = isDirection(offerType) ? offerType : 'equipment'
  const isMaterials = offerType === 'materials'
  const isRepair = offerType === 'repair'
  const isPark = offerType === 'park'

  const [step, setStep] = useState<Step>(1)
  const [name, setName] = useState('')
  const [partnerType, setPartnerType] = useState<PartnerType>(
    initialPartnerType ?? (isMaterials ? 'company' : 'individual'),
  )
  const [company, setCompany] = useState('')
  const [contactName, setContactName] = useState('')
  const [specialization, setSpecialization] = useState('')
  const [city, setCity] = useState('')
  const [phone, setPhone] = useState('')
  const [contactAuthDate, setContactAuthDate] = useState('')
  const [contactHash, setContactHash] = useState('')
  const [phoneLoading, setPhoneLoading] = useState(false)
  const [phoneError, setPhoneError] = useState(false)
  const [category, setCategory] = useState('')
  const [need, setNeed] = useState('')
  const [askNeed, setAskNeed] = useState(false)
  const [consent, setConsent] = useState(false)
  const [website, setWebsite] = useState('')

  const [brand, setBrand] = useState('')
  const [model, setModel] = useState('')
  const [capacity, setCapacity] = useState('')
  const [boomLength, setBoomLength] = useState('')
  const [description, setDescription] = useState('')
  const [fraction, setFraction] = useState('')
  const [price, setPrice] = useState('')
  const [priceUnit, setPriceUnit] = useState('hour')
  const [unit, setUnit] = useState('тонна')
  const [deliveryType, setDeliveryType] = useState('DELIVERY')
  const [district, setDistrict] = useState('')
  const [units, setUnits] = useState('')
  const [parkEquipment, setParkEquipment] = useState('')
  const [photos, setPhotos] = useState<string[]>([])
  const [photoUploading, setPhotoUploading] = useState(false)
  const [photoError, setPhotoError] = useState<string | null>(null)

  const [errors, setErrors] = useState<Record<string, string>>({})
  const [status, setStatus] = useState<Status>('editing')
  const startedRef = useRef(false)
  const inFlightRef = useRef(false)

  const dirty = Boolean(
    name ||
      company ||
      specialization ||
      category ||
      brand ||
      model ||
      capacity ||
      fraction ||
      price ||
      district ||
      photos.length > 0,
  )

  const addPhotos = async (files: FileList | null) => {
    if (!files || files.length === 0) {
      return
    }
    const free = MAX_PHOTOS - photos.length
    if (free <= 0) {
      return
    }
    const selected = Array.from(files).slice(0, free)
    setPhotoUploading(true)
    setPhotoError(null)
    try {
      const uploaded: string[] = []
      for (const file of selected) {
        const compressed = await compressPhoto(file)
        uploaded.push(await uploadPhoto(compressed, bridge.getInitData()))
      }
      setPhotos((prev) => [...prev, ...uploaded].slice(0, MAX_PHOTOS))
    } catch {
      setPhotoError(ru.form.errors.photo)
      bridge.hapticNotification('error')
    } finally {
      setPhotoUploading(false)
    }
  }

  const removePhoto = (url: string) => {
    setPhotos((prev) => prev.filter((item) => item !== url))
  }

  const markStart = (field: string) => {
    if (!startedRef.current) {
      startedRef.current = true
      track('form_start', { field })
    }
  }

  useEffect(() => {
    track('form_open')
  }, [])

  const loadPhone = useCallback(async () => {
    setPhoneLoading(true)
    setPhoneError(false)
    try {
      const contact = await bridge.requestContact()
      setPhone(contact.phone)
      setContactAuthDate(contact.authDate)
      setContactHash(contact.hash)
    } catch {
      setPhone('')
      setContactAuthDate('')
      setContactHash('')
      setPhoneError(true)
    } finally {
      setPhoneLoading(false)
    }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadPhone()
    }, 0)
    return () => window.clearTimeout(timer)
  }, [loadPhone])

  useEffect(() => {
    if (dirty && status === 'editing') {
      bridge.enableClosingConfirmation()
    } else {
      bridge.disableClosingConfirmation()
    }
    return () => bridge.disableClosingConfirmation()
  }, [dirty, status])

  const typeOptions =
    direction === 'equipment'
      ? ru.equipment.categories.map((item) => ({ value: item, label: item }))
      : ru.materials.types.map((item) => ({ value: item.value, label: item.label }))

  const companyFlow = partnerType === 'company'

  const validateStep1 = (): boolean => {
    const next: Record<string, string> = {}
    if (companyFlow) {
      if (!isValidCompany(company)) {
        next.company = ru.form.errors.companyName
      }
      if (contactName.trim() && !isValidName(contactName)) {
        next.contactName = ru.form.errors.contactName
      }
    } else if (!isValidName(name)) {
      next.name = ru.form.errors.name
    }
    if (!normalizeRuPhone(phone)) {
      next.phone = ru.form.errors.phone
    }
    if (!consent) {
      next.consent = ru.form.errors.consent
    }
    return applyErrors(next)
  }

  const validateRepair = (): boolean => {
    const next: Record<string, string> = {}
    if (partnerType === 'company') {
      if (!isValidCompany(company)) {
        next.company = ru.form.errors.companyName
      }
      if (contactName.trim() && !isValidName(contactName)) {
        next.contactName = ru.form.errors.contactName
      }
    } else if (!isValidName(name)) {
      next.name = ru.form.errors.name
    }
    if (!specialization.trim()) {
      next.specialization = ru.form.errors.specialization
    }
    if (!normalizeRuPhone(phone)) {
      next.phone = ru.form.errors.phone
    }
    if (!city) {
      next.city = ru.form.errors.city
    }
    if (!district) {
      next.district = ru.form.errors.district
    }
    if (!consent) {
      next.consent = ru.form.errors.consent
    }
    return applyErrors(next)
  }

  const validatePark = (): boolean => {
    const next: Record<string, string> = {}
    if (!isValidCompany(company)) {
      next.company = ru.form.errors.companyName
    }
    if (contactName.trim() && !isValidName(contactName)) {
      next.contactName = ru.form.errors.contactName
    }
    const unitsNumber = Number(units)
    if (units.trim() === '' || !Number.isFinite(unitsNumber) || unitsNumber <= 0) {
      next.units = ru.form.errors.required
    }
    if (!normalizeRuPhone(phone)) {
      next.phone = ru.form.errors.phone
    }
    if (!consent) {
      next.consent = ru.form.errors.consent
    }
    return applyErrors(next)
  }

  const validateStep2 = (): boolean => {
    const next: Record<string, string> = {}
    if (!category) {
      next.category = ru.form.errors.type
    }
    return applyErrors(next)
  }

  const validateStep3 = (): boolean => {
    const next: Record<string, string> = {}
    if (!district) {
      next.district = ru.form.errors.district
    }
    const priceNumber = Number(price)
    if (price.trim() === '' || !Number.isFinite(priceNumber) || priceNumber <= 0) {
      next.price = ru.form.errors.price
    }
    if (direction === 'equipment') {
      if (!brand.trim()) next.brand = ru.form.errors.required
      if (!model.trim()) next.model = ru.form.errors.required
      if (!(Number(capacity) > 0)) next.capacity = ru.form.errors.capacity
    }
    return applyErrors(next)
  }

  const applyErrors = (next: Record<string, string>): boolean => {
    setErrors(next)
    if (Object.keys(next).length > 0) {
      track('form_error', { field: Object.keys(next)[0], type: 'validation' })
      bridge.hapticNotification('error')
      return false
    }
    return true
  }

  const goNext = () => {
    if (step === 1 && !validateStep1()) {
      return
    }
    if (step === 2 && !validateStep2()) {
      return
    }
    setStep((prev) => (prev < 3 ? ((prev + 1) as Step) : prev))
  }

  const buildDetails = (): LeadDetailsPayload => {
    const priceNumber = Number(price)
    if (isPark) {
      return {
        units: Number(units),
        equipment: parkEquipment.trim() || undefined,
      }
    }
    if (direction === 'equipment') {
      return {
        category,
        brand: brand.trim(),
        model: model.trim(),
        capacity: Number(capacity),
        boomLength: boomLength.trim() || undefined,
        description: description.trim() || undefined,
        price: priceNumber,
        priceUnit,
        photos: photos.length > 0 ? photos : undefined,
      }
    }
    return {
      category,
      fraction: fraction || undefined,
      price: priceNumber,
      unit,
      deliveryType,
    }
  }

  const submit = async (details: LeadDetailsPayload, noMatch = false) => {
    if (inFlightRef.current) {
      return
    }
    inFlightRef.current = true
    track('form_submit', { noMatch })
    setStatus('submitting')
    try {
      const resolvedPartnerType: PartnerType = isPark ? 'company' : partnerType
      await submitPartnerLead(
        {
          name: name.trim() || company.trim(),
          phone: normalizeRuPhone(phone) ?? phone,
          offerTypes: [offerType],
          partnerType: resolvedPartnerType,
          companyName: resolvedPartnerType === 'company' ? company.trim() : undefined,
          contactName: contactName.trim() || undefined,
          district: district.trim() || undefined,
          details,
          consent: true,
          source: bridge.getStartParam(),
          contactAuthDate: contactAuthDate || undefined,
          contactHash: contactHash || undefined,
          website,
        },
        bridge.getInitData(),
      )
      bridge.hapticNotification('success')
      track('form_success', { noMatch })
      setStatus(noMatch ? 'successNoMatch' : isPark ? 'successPark' : 'success')
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        bridge.hapticNotification('warning')
        track('form_fail', { code: 409 })
        setStatus('duplicate')
        return
      }
      if (error instanceof ApiError && error.status === 400) {
        setErrors(normalizeFieldErrors(error.errors))
        bridge.hapticNotification('error')
        track('form_fail', { code: 400 })
        setStatus('editing')
        return
      }
      track('form_fail', { code: 'network' })
      setStatus('error')
    } finally {
      inFlightRef.current = false
    }
  }

  const submitStep3 = () => {
    if (!validateStep3()) {
      return
    }
    void submit(buildDetails())
  }

  const validateNeed = (): boolean => {
    const trimmed = need.trim()
    if (trimmed.length < 3 || trimmed.length > 500) {
      return applyErrors({ need: ru.form.errors.need })
    }
    return applyErrors({})
  }

  const submitNoMatch = () => {
    void submit({ noMatch: true, need: need.trim() }, true)
  }

  if (status === 'success' || status === 'successNoMatch' || status === 'successPark') {
    const content =
      status === 'successNoMatch'
        ? ru.form.successNoMatch
        : status === 'successPark'
          ? ru.form.successPark
          : ru.form.success
    return (
      <ResultScreen title={content.title} text={content.text} button={content.back} onClick={onClose} onBack={onClose} />
    )
  }

  if (status === 'duplicate') {
    return (
      <ResultScreen
        title={ru.form.duplicate.title}
        text={ru.form.duplicate.text}
        button={ru.form.duplicate.back}
        onClick={onClose}
        onBack={onClose}
      />
    )
  }

  if (status === 'error') {
    return (
      <ResultScreen
        title={ru.form.networkError.title}
        text={ru.form.networkError.text}
        button={ru.form.networkError.retry}
        onClick={() => {
          setStatus('editing')
          if (step === 3) {
            submitStep3()
          } else {
            submitNoMatch()
          }
        }}
        onBack={onClose}
      />
    )
  }

  const submitting = status === 'submitting'

  const handleBack = () => {
    if (askNeed) {
      setAskNeed(false)
      return
    }
    if (wizard && step > 1) {
      setStep((prev) => (prev - 1) as Step)
      return
    }
    onClose()
  }

  const stepTitle = isPark
    ? ru.form.park.title
    : askNeed
      ? ru.form.noMatchTitle
      : wizard
      ? step === 1
        ? ru.form.step1Title
        : step === 2
          ? ru.form.step2Title[direction]
          : ru.form.step3Title[direction]
      : ru.form.title

  return (
    <Screen title={stepTitle} onBack={handleBack}>
      <form
        className="lead-form"
        noValidate
        onChangeCapture={(event) => markStart((event.target as HTMLElement).id || 'form')}
        onSubmit={(event) => {
          event.preventDefault()
          if (isPark) {
            if (validatePark()) {
              void submit(buildDetails())
            }
            return
          }
          if (isRepair) {
            if (validateRepair()) {
              void submit({
                company: partnerType === 'company' ? company.trim() : undefined,
                city,
                specialization: specialization.trim(),
              })
            }
            return
          }
          if (!wizard) {
            return
          }
          if (step === 2 && askNeed) {
            if (validateNeed()) {
              submitNoMatch()
            }
            return
          }
          if (step === 1 || step === 2) {
            goNext()
            return
          }
          submitStep3()
        }}
      >
        {isPark ? (
          <ParkFields
            company={company}
            onCompany={setCompany}
            contactName={contactName}
            onContactName={setContactName}
            units={units}
            onUnits={setUnits}
            equipment={parkEquipment}
            onEquipment={setParkEquipment}
            phone={phone}
            phoneLoading={phoneLoading}
            phoneError={phoneError}
            onRetryPhone={() => void loadPhone()}
            consent={consent}
            onConsent={setConsent}
            errors={errors}
            website={website}
            onWebsite={setWebsite}
          />
        ) : isRepair ? (
          <RepairFields
            partnerType={partnerType}
            onPartnerType={setPartnerType}
            company={company}
            onCompany={setCompany}
            contactName={contactName}
            onContactName={setContactName}
            specialization={specialization}
            onSpecialization={setSpecialization}
            name={name}
            onName={setName}
            phone={phone}
            phoneLoading={phoneLoading}
            phoneError={phoneError}
            onRetryPhone={() => void loadPhone()}
            city={city}
            onCity={setCity}
            district={district}
            onDistrict={setDistrict}
            consent={consent}
            onConsent={setConsent}
            errors={errors}
            website={website}
            onWebsite={setWebsite}
          />
        ) : step === 1 ? (
          <ContactFields
            partnerType={partnerType}
            onPartnerType={setPartnerType}
            name={name}
            onName={setName}
            company={company}
            onCompany={setCompany}
            contactName={contactName}
            onContactName={setContactName}
            phone={phone}
            phoneLoading={phoneLoading}
            phoneError={phoneError}
            onRetryPhone={() => void loadPhone()}
            consent={consent}
            onConsent={setConsent}
            errors={errors}
            website={website}
            onWebsite={setWebsite}
          />
        ) : null}

        {wizard && step === 2 ? (
          askNeed ? (
            <>
              <p className="field__hint">{ru.form.noMatchSubtitle}</p>
              <div className="field">
                <label className="field__label" htmlFor="lead-need">
                  {ru.form.needLabel}
                </label>
                <Textarea
                  id="lead-need"
                  value={need}
                  placeholder={ru.form.needPlaceholder}
                  aria-invalid={Boolean(errors.need)}
                  onChange={(event) => setNeed(event.target.value)}
                />
                {errors.need ? (
                  <span className="field__error" role="alert">
                    {errors.need}
                  </span>
                ) : null}
              </div>
              <Button
                type="submit"
                variant="primary"
                size="large"
                stretched
                loading={submitting}
                disabled={submitting}
              >
                {submitting ? ru.form.submitting : ru.form.noMatchSubmit}
              </Button>
              <Button
                type="button"
                variant="secondary"
                size="large"
                stretched
                disabled={submitting}
                onClick={() => setAskNeed(false)}
              >
                {ru.common.back}
              </Button>
            </>
          ) : (
            <>
              <Chips
                options={typeOptions}
                selected={category ? [category] : []}
                onToggle={(value) => setCategory(category === value ? '' : value)}
                ariaLabel={ru.form.step2Title[direction]}
              />
              {errors.category ? (
                <span className="field__error" role="alert">
                  {errors.category}
                </span>
              ) : null}
              <Button type="submit" variant="primary" size="large" stretched>
                {ru.form.next}
              </Button>
              <Button
                type="button"
                variant="secondary"
                size="large"
                stretched
                disabled={submitting}
                onClick={() => setAskNeed(true)}
              >
                {ru.form.noMatch}
              </Button>
            </>
          )
        ) : null}

        {wizard && step === 3 ? (
          <>
            <DetailsFields
              direction={direction}
              category={category}
              errors={errors}
              photos={photos}
              photoUploading={photoUploading}
              photoError={photoError}
              onAddPhotos={addPhotos}
              onRemovePhoto={removePhoto}
              values={{
                brand,
                model,
                capacity,
                boomLength,
                description,
                fraction,
                price,
                priceUnit,
                unit,
                deliveryType,
                district,
              }}
              onChange={{
                brand: setBrand,
                model: setModel,
                capacity: setCapacity,
                boomLength: setBoomLength,
                description: setDescription,
                fraction: setFraction,
                price: setPrice,
                priceUnit: setPriceUnit,
                unit: setUnit,
                deliveryType: setDeliveryType,
                district: setDistrict,
              }}
            />
            <Button
              type="submit"
              variant="primary"
              size="large"
              stretched
              loading={submitting}
              disabled={submitting}
            >
              {submitting ? ru.form.submitting : ru.form.submit}
            </Button>
          </>
        ) : null}

        {!wizard || step === 1 ? (
          <Button
            type="submit"
            variant="primary"
            size="large"
            stretched
            loading={submitting}
            disabled={submitting}
          >
            {wizard ? ru.form.next : submitting ? ru.form.submitting : ru.form.submit}
          </Button>
        ) : null}
      </form>
    </Screen>
  )
}

function PhoneField({
  phone,
  phoneLoading,
  phoneError,
  onRetryPhone,
  error,
}: {
  phone: string
  phoneLoading: boolean
  phoneError: boolean
  onRetryPhone: () => void
  error?: string
}) {
  return (
    <div className="field">
      <span className="field__label">
        <label htmlFor="lead-phone">{ru.form.phoneLabel}</label>
        <FieldHelp text={ru.form.phoneHelp} />
      </span>
      <Input
        id="lead-phone"
        value={phoneLoading ? '' : formatPhoneDisplay(phone)}
        placeholder={phoneLoading ? 'Получаем номер…' : ''}
        readOnly
        aria-invalid={Boolean(error)}
      />
      {phoneError ? (
        <span className="field__hint field__hint--error">
          {ru.form.phoneUnavailable}{' '}
          <button type="button" className="link-button" onClick={onRetryPhone}>
            {ru.form.phoneRetry}
          </button>
        </span>
      ) : null}
      {error ? (
        <span className="field__error" role="alert">
          {error}
        </span>
      ) : null}
    </div>
  )
}

function ConsentField({
  consent,
  onConsent,
  error,
}: {
  consent: boolean
  onConsent: (value: boolean) => void
  error?: string
}) {
  return (
    <>
      <Checkbox id="lead-consent" checked={consent} onChange={onConsent}>
        {ru.form.consentText}{' '}
        <a href="/legal.html#privacy" onClick={(event) => event.stopPropagation()}>
          {ru.form.consentLink}
        </a>
      </Checkbox>
      {error ? (
        <span className="field__error" role="alert">
          {error}
        </span>
      ) : null}
    </>
  )
}

function Honeypot({
  website,
  onWebsite,
}: {
  website: string
  onWebsite: (value: string) => void
}) {
  return (
    <div className="honeypot" aria-hidden="true">
      <label htmlFor="lead-website">Website</label>
      <input
        id="lead-website"
        name="website"
        tabIndex={-1}
        autoComplete="off"
        value={website}
        onChange={(event) => onWebsite(event.target.value)}
      />
    </div>
  )
}

function PartnerTypeToggle({
  value,
  onChange,
}: {
  value: PartnerType
  onChange: (value: PartnerType) => void
}) {
  return (
    <div className="field">
      <span className="field__label">{ru.form.partnerTypeLabel}</span>
      <Chips
        options={[
          { value: 'individual', label: ru.form.partnerTypeIndividual },
          { value: 'company', label: ru.form.partnerTypeCompany },
        ]}
        selected={[value]}
        onToggle={(next) => onChange(next as PartnerType)}
        ariaLabel={ru.form.partnerTypeLabel}
      />
    </div>
  )
}

function CompanyFields({
  company,
  onCompany,
  contactName,
  onContactName,
  errors,
}: {
  company: string
  onCompany: (value: string) => void
  contactName: string
  onContactName: (value: string) => void
  errors: Record<string, string>
}) {
  return (
    <>
      <div className="field">
        <label className="field__label" htmlFor="lead-company">
          {ru.form.companyLabel}
        </label>
        <Input
          id="lead-company"
          value={company}
          placeholder={ru.form.companyPlaceholder}
          aria-invalid={Boolean(errors.company)}
          onChange={(event) => onCompany(event.target.value)}
        />
        {errors.company ? (
          <span className="field__error" role="alert">
            {errors.company}
          </span>
        ) : null}
      </div>

      <div className="field">
        <label className="field__label" htmlFor="lead-contact-name">
          {ru.form.contactNameLabel}
        </label>
        <Input
          id="lead-contact-name"
          value={contactName}
          placeholder={ru.form.contactNamePlaceholder}
          aria-invalid={Boolean(errors.contactName)}
          onChange={(event) => onContactName(event.target.value)}
        />
        {errors.contactName ? (
          <span className="field__error" role="alert">
            {errors.contactName}
          </span>
        ) : null}
      </div>
    </>
  )
}

function ContactFields({
  partnerType,
  onPartnerType,
  name,
  onName,
  company,
  onCompany,
  contactName,
  onContactName,
  phone,
  phoneLoading,
  phoneError,
  onRetryPhone,
  consent,
  onConsent,
  errors,
  website,
  onWebsite,
}: {
  partnerType: PartnerType
  onPartnerType: (value: PartnerType) => void
  name: string
  onName: (value: string) => void
  company: string
  onCompany: (value: string) => void
  contactName: string
  onContactName: (value: string) => void
  phone: string
  phoneLoading: boolean
  phoneError: boolean
  onRetryPhone: () => void
  consent: boolean
  onConsent: (value: boolean) => void
  errors: Record<string, string>
  website: string
  onWebsite: (value: string) => void
}) {
  const companyFlow = partnerType === 'company'
  return (
    <>
      <PartnerTypeToggle value={partnerType} onChange={onPartnerType} />

      {companyFlow ? (
        <CompanyFields
          company={company}
          onCompany={onCompany}
          contactName={contactName}
          onContactName={onContactName}
          errors={errors}
        />
      ) : (
        <div className="field">
          <label className="field__label" htmlFor="lead-name">
            {ru.form.nameLabel}
          </label>
          <Input
            id="lead-name"
            value={name}
            placeholder={ru.form.namePlaceholder}
            aria-invalid={Boolean(errors.name)}
            onChange={(event) => onName(event.target.value)}
          />
          {errors.name ? (
            <span className="field__error" role="alert">
              {errors.name}
            </span>
          ) : null}
        </div>
      )}

      <PhoneField
        phone={phone}
        phoneLoading={phoneLoading}
        phoneError={phoneError}
        onRetryPhone={onRetryPhone}
        error={errors.phone}
      />

      <ConsentField consent={consent} onConsent={onConsent} error={errors.consent} />
      <Honeypot website={website} onWebsite={onWebsite} />
    </>
  )
}

function RepairFields({
  partnerType,
  onPartnerType,
  company,
  onCompany,
  contactName,
  onContactName,
  specialization,
  onSpecialization,
  name,
  onName,
  phone,
  phoneLoading,
  phoneError,
  onRetryPhone,
  city,
  onCity,
  district,
  onDistrict,
  consent,
  onConsent,
  errors,
  website,
  onWebsite,
}: {
  partnerType: PartnerType
  onPartnerType: (value: PartnerType) => void
  company: string
  onCompany: (value: string) => void
  contactName: string
  onContactName: (value: string) => void
  specialization: string
  onSpecialization: (value: string) => void
  name: string
  onName: (value: string) => void
  phone: string
  phoneLoading: boolean
  phoneError: boolean
  onRetryPhone: () => void
  city: string
  onCity: (value: string) => void
  district: string
  onDistrict: (value: string) => void
  consent: boolean
  onConsent: (value: boolean) => void
  errors: Record<string, string>
  website: string
  onWebsite: (value: string) => void
}) {
  const companyFlow = partnerType === 'company'
  return (
    <>
      <PartnerTypeToggle value={partnerType} onChange={onPartnerType} />

      {companyFlow ? (
        <CompanyFields
          company={company}
          onCompany={onCompany}
          contactName={contactName}
          onContactName={onContactName}
          errors={errors}
        />
      ) : (
        <div className="field">
          <label className="field__label" htmlFor="lead-name">
            {ru.form.nameLabel}
          </label>
          <Input
            id="lead-name"
            value={name}
            placeholder={ru.form.namePlaceholder}
            aria-invalid={Boolean(errors.name)}
            onChange={(event) => onName(event.target.value)}
          />
          {errors.name ? (
            <span className="field__error" role="alert">
              {errors.name}
            </span>
          ) : null}
        </div>
      )}

      <div className="field">
        <label className="field__label" htmlFor="lead-specialization">
          {ru.form.specializationLabel}
        </label>
        <Input
          id="lead-specialization"
          value={specialization}
          placeholder={ru.form.specializationPlaceholder}
          aria-invalid={Boolean(errors.specialization)}
          onChange={(event) => onSpecialization(event.target.value)}
        />
        {errors.specialization ? (
          <span className="field__error" role="alert">
            {errors.specialization}
          </span>
        ) : null}
      </div>

      <PhoneField
        phone={phone}
        phoneLoading={phoneLoading}
        phoneError={phoneError}
        onRetryPhone={onRetryPhone}
        error={errors.phone}
      />

      <div className="field">
        <span className="field__label">{ru.form.cityLabel}</span>
        <Chips
          options={ru.form.cities}
          selected={city ? [city] : []}
          onToggle={onCity}
          ariaLabel={ru.form.cityLabel}
        />
        {errors.city ? (
          <span className="field__error" role="alert">
            {errors.city}
          </span>
        ) : null}
      </div>

      <div className="field">
        <span className="field__label">{ru.form.fields.district}</span>
        <Chips
          options={ru.form.districts}
          selected={district ? [district] : []}
          onToggle={onDistrict}
          ariaLabel={ru.form.fields.district}
        />
        {errors.district ? (
          <span className="field__error" role="alert">
            {errors.district}
          </span>
        ) : null}
      </div>

      <ConsentField consent={consent} onConsent={onConsent} error={errors.consent} />
      <Honeypot website={website} onWebsite={onWebsite} />
    </>
  )
}

function ParkFields({
  company,
  onCompany,
  contactName,
  onContactName,
  units,
  onUnits,
  equipment,
  onEquipment,
  phone,
  phoneLoading,
  phoneError,
  onRetryPhone,
  consent,
  onConsent,
  errors,
  website,
  onWebsite,
}: {
  company: string
  onCompany: (value: string) => void
  contactName: string
  onContactName: (value: string) => void
  units: string
  onUnits: (value: string) => void
  equipment: string
  onEquipment: (value: string) => void
  phone: string
  phoneLoading: boolean
  phoneError: boolean
  onRetryPhone: () => void
  consent: boolean
  onConsent: (value: boolean) => void
  errors: Record<string, string>
  website: string
  onWebsite: (value: string) => void
}) {
  return (
    <>
      <p className="field__hint">{ru.form.park.subtitle}</p>

      <CompanyFields
        company={company}
        onCompany={onCompany}
        contactName={contactName}
        onContactName={onContactName}
        errors={errors}
      />

      <div className="field">
        <label className="field__label" htmlFor="lead-park-units">
          {ru.form.park.unitsLabel}
        </label>
        <Input
          id="lead-park-units"
          inputMode="numeric"
          value={units}
          placeholder={ru.form.park.unitsPlaceholder}
          aria-invalid={Boolean(errors.units)}
          onChange={(event) => onUnits(event.target.value.replace(/\D/g, ''))}
        />
        {errors.units ? (
          <span className="field__error" role="alert">
            {errors.units}
          </span>
        ) : null}
      </div>

      <div className="field">
        <label className="field__label" htmlFor="lead-park-equipment">
          {ru.form.park.equipmentLabel}
        </label>
        <Input
          id="lead-park-equipment"
          value={equipment}
          placeholder={ru.form.park.equipmentPlaceholder}
          onChange={(event) => onEquipment(event.target.value)}
        />
      </div>

      <PhoneField
        phone={phone}
        phoneLoading={phoneLoading}
        phoneError={phoneError}
        onRetryPhone={onRetryPhone}
        error={errors.phone}
      />

      <ConsentField consent={consent} onConsent={onConsent} error={errors.consent} />
      <Honeypot website={website} onWebsite={onWebsite} />
    </>
  )
}

interface DetailValues {
  brand: string
  model: string
  capacity: string
  boomLength: string
  description: string
  fraction: string
  price: string
  priceUnit: string
  unit: string
  deliveryType: string
  district: string
}

interface DetailChange {
  brand: (v: string) => void
  model: (v: string) => void
  capacity: (v: string) => void
  boomLength: (v: string) => void
  description: (v: string) => void
  fraction: (v: string) => void
  price: (v: string) => void
  priceUnit: (v: string) => void
  unit: (v: string) => void
  deliveryType: (v: string) => void
  district: (v: string) => void
}

function DetailsFields({
  direction,
  category,
  values,
  onChange,
  errors,
  photos,
  photoUploading,
  photoError,
  onAddPhotos,
  onRemovePhoto,
}: {
  direction: Direction
  category: string
  values: DetailValues
  onChange: DetailChange
  errors: Record<string, string>
  photos: string[]
  photoUploading: boolean
  photoError: string | null
  onAddPhotos: (files: FileList | null) => void
  onRemovePhoto: (url: string) => void
}) {
  return (
    <>
      {direction === 'equipment' ? (
        <>
          <TextField
            id="lead-brand"
            label={ru.form.fields.brand}
            placeholder={ru.form.fields.brandPlaceholder}
            value={values.brand}
            onChange={onChange.brand}
            error={errors.brand}
          />
          <TextField
            id="lead-model"
            label={ru.form.fields.model}
            placeholder={ru.form.fields.modelPlaceholder}
            value={values.model}
            onChange={onChange.model}
            error={errors.model}
          />
          <TextField
            id="lead-capacity"
            label={ru.form.fields.capacity}
            placeholder={ru.form.fields.capacityPlaceholder}
            value={values.capacity}
            onChange={onChange.capacity}
            error={errors.capacity}
            numeric
          />
          {category === 'Манипулятор' ? (
            <TextField
              id="lead-boom-length"
              label={ru.form.fields.boomLength}
              placeholder={ru.form.fields.boomLengthPlaceholder}
              value={values.boomLength}
              onChange={onChange.boomLength}
              error={errors.boomLength}
            />
          ) : null}
          <div className="field">
            <label className="field__label" htmlFor="lead-description">
              {ru.form.fields.description}
            </label>
            <Textarea
              id="lead-description"
              value={values.description}
              placeholder={ru.form.fields.descriptionPlaceholder}
              onChange={(event) => onChange.description(event.target.value)}
            />
            <span className="field__hint">{ru.form.fields.descriptionHint}</span>
            {errors.description ? (
              <span className="field__error" role="alert">
                {errors.description}
              </span>
            ) : null}
          </div>
        </>
      ) : (ru.materials.noFractionTypes as readonly string[]).includes(category) ? null : (
        <div className="field">
          <span className="field__label">{ru.form.fields.fraction}</span>
          <Chips
            options={ru.form.fractions}
            selected={values.fraction ? [values.fraction] : []}
            onToggle={onChange.fraction}
            ariaLabel={ru.form.fields.fraction}
          />
          {errors.fraction ? (
            <span className="field__error" role="alert">
              {errors.fraction}
            </span>
          ) : null}
        </div>
      )}

      <div className="field">
        <label className="field__label" htmlFor="lead-price">
          {ru.form.fields.price}
        </label>
        <Input
          id="lead-price"
          inputMode="numeric"
          value={values.price}
          placeholder={ru.form.fields.pricePlaceholder}
          aria-invalid={Boolean(errors.price)}
          onChange={(event) => onChange.price(event.target.value.replace(/\D/g, ''))}
        />
        {errors.price ? (
          <span className="field__error" role="alert">
            {errors.price}
          </span>
        ) : null}
      </div>

      <div className="field">
        <span className="field__label">
          {direction === 'equipment' ? ru.form.fields.priceUnit : ru.form.fields.unit}
        </span>
        {direction === 'equipment' ? (
          <Chips
            options={ru.form.priceUnits}
            selected={[values.priceUnit]}
            onToggle={onChange.priceUnit}
            ariaLabel={ru.form.fields.priceUnit}
          />
        ) : (
          <Chips
            options={ru.form.materialUnits}
            selected={[values.unit]}
            onToggle={onChange.unit}
            ariaLabel={ru.form.fields.unit}
          />
        )}
      </div>

      {direction === 'materials' ? (
        <div className="field">
          <span className="field__label">{ru.form.fields.delivery}</span>
          <Chips
            options={ru.form.deliveryTypes}
            selected={[values.deliveryType]}
            onToggle={onChange.deliveryType}
            ariaLabel={ru.form.fields.delivery}
          />
        </div>
      ) : null}

      <div className="field">
        <span className="field__label">{ru.form.fields.district}</span>
        <Chips
          options={ru.form.districts}
          selected={values.district ? [values.district] : []}
          onToggle={onChange.district}
          ariaLabel={ru.form.fields.district}
        />
        {errors.district ? (
          <span className="field__error" role="alert">
            {errors.district}
          </span>
        ) : null}
      </div>

      {direction === 'equipment' ? (
        <div className="field">
          <span className="field__label">{ru.form.photoLabel}</span>
          <span className="field__hint">{ru.form.photoHint}</span>
          {photos.length > 0 || photoUploading ? (
            <div className="photos">
              {photos.map((url) => (
                <div className="photos__item" key={url}>
                  <img className="photos__img" src={url} alt="" />
                  <button
                    type="button"
                    className="photos__remove"
                    onClick={() => onRemovePhoto(url)}
                    aria-label={ru.form.photoRemove}
                  >
                    ×
                  </button>
                </div>
              ))}
              {photoUploading ? (
                <div className="photos__item photos__item--loading" aria-hidden="true" />
              ) : null}
            </div>
          ) : null}
          {photos.length < MAX_PHOTOS ? (
            <label className={`photo-add${photoUploading ? ' photo-add--disabled' : ''}`}>
              <input
                type="file"
                accept="image/*"
                multiple
                hidden
                disabled={photoUploading}
                onChange={(event) => {
                  onAddPhotos(event.target.files)
                  event.target.value = ''
                }}
              />
              {ru.form.photoAdd}
            </label>
          ) : null}
          {photoError ? (
            <span className="field__error" role="alert">
              {photoError}
            </span>
          ) : null}
        </div>
      ) : null}
    </>
  )
}

function TextField({
  id,
  label,
  placeholder,
  value,
  onChange,
  error,
  numeric = false,
}: {
  id: string
  label: string
  placeholder: string
  value: string
  onChange: (value: string) => void
  error?: string
  numeric?: boolean
}) {
  return (
    <div className="field">
      <label className="field__label" htmlFor={id}>
        {label}
      </label>
      <Input
        id={id}
        inputMode={numeric ? 'numeric' : undefined}
        value={value}
        placeholder={placeholder}
        aria-invalid={Boolean(error)}
        onChange={(event) =>
          onChange(numeric ? event.target.value.replace(/\D/g, '') : event.target.value)
        }
      />
      {error ? (
        <span className="field__error" role="alert">
          {error}
        </span>
      ) : null}
    </div>
  )
}

function FieldHelp({ text }: { text: string }) {
  const [open, setOpen] = useState(false)
  return (
    <span className="field__help-wrap">
      <button
        type="button"
        className={`field__help${open ? ' field__help--open' : ''}`}
        aria-label={text}
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        ?
      </button>
      <span className="field__tooltip" role="note">
        {text}
      </span>
    </span>
  )
}

function ResultScreen({
  title,
  text,
  button,
  onClick,
  onBack,
}: {
  title: string
  text: string
  button: string
  onClick: () => void
  onBack: () => void
}) {
  return (
    <Screen title={title} onBack={onBack}>
      <div className="result">
        <p>{text}</p>
        <Button variant="primary" size="large" stretched onClick={onClick}>
          {button}
        </Button>
      </div>
    </Screen>
  )
}
