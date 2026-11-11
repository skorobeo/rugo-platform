import { useState } from 'react'
import { Button } from '@maxhub/max-ui'
import { bridge } from '../bridge'
import { ApiError } from '../api/client'
import { ru } from '../content/ru'
import { Screen } from '../components/Screen'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import { useAsync } from '../lib/useAsync'
import {
  confirmPhone,
  deleteMyEquipment,
  fetchMyEquipment,
  fetchMyLeads,
  fetchMyMaterials,
  fetchProfile,
  deleteMyMaterial,
  type MyLead,
  type PartnerProfile,
} from '../api/partner'
import type { Equipment, Material } from '../api/catalog'

type State = 'loading' | 'error' | 'phone' | 'ready'

type CabinetData = {
  profile: PartnerProfile
  items: Equipment[]
  materials: Material[]
  leads: MyLead[]
}

const PRICE_UNITS: Record<string, string> = {
  HOUR: ru.catalog.unitHour,
  DAY: ru.catalog.unitDay,
  SHIFT: ru.catalog.unitShift,
}

const DIRECTION_LABELS: Record<string, string> = {
  equipment: ru.cabinet.pendingEquipment,
  materials: ru.cabinet.pendingMaterials,
  repair: ru.cabinet.pendingRepair,
  park: ru.cabinet.pendingPark,
}

function priceLabel(item: Equipment): string {
  if (item.price == null) {
    return ru.catalog.byRequest
  }
  const unit = item.priceType ? PRICE_UNITS[item.priceType] ?? ru.catalog.unitHour : ru.catalog.unitHour
  return `${ru.catalog.from} ${item.price} ${unit}`
}

function photoOf(item: Equipment): string | undefined {
  return item.imageUrls?.[0] ?? item.imageUrl ?? undefined
}

function materialTitle(item: Material): string {
  return item.fraction ? `${item.type} (${item.fraction})` : item.type
}

function materialPrice(item: Material): string {
  if (item.pricePerUnit == null) {
    return ru.catalog.byRequest
  }
  return `${ru.catalog.from} ${item.pricePerUnit} ₽/${item.unit ?? 'тонна'}`
}

function leadDirection(lead: MyLead): string {
  const first = lead.offerTypes[0]
  return DIRECTION_LABELS[first] ?? first ?? ''
}

async function loadCabinet(): Promise<CabinetData> {
  const initData = bridge.getInitData()
  const profile = await fetchProfile(initData)
  if (!profile.registered) {
    return { profile, items: [], materials: [], leads: [] }
  }
  const [items, materials, leads] = await Promise.all([
    fetchMyEquipment(initData),
    fetchMyMaterials(initData),
    fetchMyLeads(initData),
  ])
  return { profile, items, materials, leads }
}

export function Cabinet({
  onBack,
  onAdd,
  onEdit,
  onAddMaterial,
  onEditMaterial,
}: {
  onBack: () => void
  onAdd: (profile: PartnerProfile) => void
  onEdit: (item: Equipment, profile: PartnerProfile) => void
  onAddMaterial: (profile: PartnerProfile) => void
  onEditMaterial: (item: Material, profile: PartnerProfile) => void
}) {
  const [phoneBusy, setPhoneBusy] = useState(false)
  const [phoneError, setPhoneError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const { data, loading, error, retry } = useAsync(loadCabinet)

  const profile = data?.profile ?? null
  const items = data?.items ?? []
  const materials = data?.materials ?? []
  const pending = (data?.leads ?? []).filter((lead) => lead.status === 'new')
  const state: State = loading ? 'loading' : error ? 'error' : profile && !profile.registered ? 'phone' : 'ready'

  const confirm = async () => {
    setPhoneBusy(true)
    setPhoneError(null)
    try {
      const contact = await bridge.requestContact()
      await confirmPhone(
        { phone: contact.phone, contactAuthDate: contact.authDate, contactHash: contact.hash },
        bridge.getInitData(),
      )
      retry()
    } catch (e) {
      setPhoneError(
        e instanceof ApiError && e.errors.phone ? e.errors.phone : ru.cabinet.phoneDenied,
      )
    } finally {
      setPhoneBusy(false)
    }
  }

  const remove = async (id: number) => {
    if (!window.confirm(ru.cabinet.deleteConfirm)) {
      return
    }
    try {
      await deleteMyEquipment(id, bridge.getInitData())
      retry()
    } catch {
      // Повторная загрузка не требуется — оставляем список как есть
    }
  }

  const removeMaterial = async (id: number) => {
    if (!window.confirm(ru.cabinet.deleteMaterialConfirm)) {
      return
    }
    setActionError(null)
    try {
      await deleteMyMaterial(id, bridge.getInitData())
      retry()
    } catch (e) {
      const message =
        e instanceof ApiError && Object.values(e.errors)[0] ? Object.values(e.errors)[0] : ru.cabinet.loadError
      setActionError(message)
    }
  }

  return (
    <Screen title={ru.cabinet.title} onBack={onBack}>
      {state === 'loading' ? <LoadingState /> : null}
      {state === 'error' ? <ErrorState onRetry={retry} /> : null}

      {state === 'phone' ? (
        <div className="prose">
          <p className="menu-item__title">{ru.cabinet.needPhoneTitle}</p>
          <p className="prose__muted">{ru.cabinet.needPhoneText}</p>
          {phoneError ? <p className="field__error">{phoneError}</p> : null}
          <Button
            variant="primary"
            size="large"
            stretched
            loading={phoneBusy}
            disabled={phoneBusy}
            onClick={() => void confirm()}
          >
            {ru.cabinet.confirmPhone}
          </Button>
        </div>
      ) : null}

      {state === 'ready' ? (
        <>
          <div className="cabinet-actions">
            <Button variant="primary" size="large" stretched onClick={() => profile && onAdd(profile)}>
              {ru.cabinet.add}
            </Button>
            <Button variant="secondary" size="large" stretched onClick={() => profile && onAddMaterial(profile)}>
              {ru.cabinet.addMaterial}
            </Button>
          </div>

          {actionError ? <p className="field__error">{actionError}</p> : null}

          {pending.length > 0 ? (
            <section className="cabinet-section">
              <h2 className="screen__subtitle">{ru.cabinet.pendingTitle}</h2>
              <div className="list">
                {pending.map((lead) => (
                  <article className="card" key={lead.id}>
                    <div className="card__body">
                      <div className="card__title">
                        {leadDirection(lead)}
                        <span className="card__badge">{ru.catalog.status.PENDING}</span>
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            </section>
          ) : null}

          <section className="cabinet-section">
            <h2 className="screen__subtitle">{ru.cabinet.title}</h2>
            {items.length === 0 ? (
              <EmptyState text={ru.cabinet.empty} />
            ) : (
              <div className="list">
                {items.map((item) => {
                  const photo = photoOf(item)
                  return (
                    <article className="card" key={item.id}>
                      {photo ? <img className="card__image" src={photo} alt="" /> : null}
                      <div className="card__body">
                        <div className="card__title">
                          {item.type}
                          {item.status === 'PENDING' ? (
                            <span className="card__badge">{ru.catalog.status.PENDING}</span>
                          ) : null}
                        </div>
                        <div className="card__category">
                          {item.category}
                          {item.location ? ` · ${item.location}` : ''}
                        </div>
                        <div className="card__rows">
                          <div className="card__row">
                            <span className="card__row-label">{ru.catalog.price}</span>
                            <span className="card__row-value">{priceLabel(item)}</span>
                          </div>
                        </div>
                        <div className="card__actions">
                          <Button variant="secondary" size="medium" onClick={() => profile && onEdit(item, profile)}>
                            {ru.cabinet.edit}
                          </Button>
                          <Button variant="secondary" size="medium" onClick={() => void remove(item.id)}>
                            {ru.cabinet.delete}
                          </Button>
                        </div>
                      </div>
                    </article>
                  )
                })}
              </div>
            )}
          </section>

          <section className="cabinet-section">
            <h2 className="screen__subtitle">{ru.cabinet.myMaterials}</h2>
            {materials.length === 0 ? (
              <EmptyState text={ru.cabinet.myMaterialsEmpty} />
            ) : (
              <div className="list">
                {materials.map((item) => (
                  <article className="card" key={item.id}>
                    {(() => {
                      const photo = item.imageUrls?.[0] ?? item.imageUrl
                      return photo ? <img className="card__image" src={photo} alt="" /> : null
                    })()}
                    <div className="card__body">
                      <div className="card__title">{materialTitle(item)}</div>
                      <div className="card__category">
                        {item.location ?? ''}
                        {item.supplierType === 'company' ? (
                          <span className="card__badge">{ru.catalog.companyBadge}</span>
                        ) : null}
                      </div>
                      <div className="card__rows">
                        <div className="card__row">
                          <span className="card__row-label">{ru.catalog.price}</span>
                          <span className="card__row-value">{materialPrice(item)}</span>
                        </div>
                      </div>
                      <div className="card__actions">
                        <Button variant="secondary" size="medium" onClick={() => profile && onEditMaterial(item, profile)}>
                          {ru.cabinet.edit}
                        </Button>
                        <Button variant="secondary" size="medium" onClick={() => void removeMaterial(item.id)}>
                          {ru.cabinet.deleteMaterial}
                        </Button>
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>

          {profile?.phone ? (
            <p className="prose__muted">
              {ru.catalog.owner}: {profile.phone}
            </p>
          ) : null}
        </>
      ) : null}
    </Screen>
  )
}
