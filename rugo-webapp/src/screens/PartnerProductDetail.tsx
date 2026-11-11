import { useCallback, useState } from 'react'
import { Button } from '@maxhub/max-ui'
import { Screen } from '../components/Screen'
import { PhotoGallery } from '../components/PhotoGallery'
import {
  availabilityLabel,
  fetchPartnerCatalog,
  fetchPartnerProduct,
  type PartnerCatalogData,
  type PartnerPrice,
  type PartnerProduct,
  type PartnerProductDetail as PartnerProductDetailData,
} from '../api/partnerCatalog'
import { formatPhoneDisplay } from '../form/validators'
import { useAsync } from '../lib/useAsync'
import { ru } from '../content/ru'
import { track } from '../analytics/track'

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="card__row">
      <span className="card__row-label">{label}</span>
      <span className="card__row-value">{value}</span>
    </div>
  )
}

function formatNumber(value: number | null, unit: string, digits = 3): string | null {
  if (value == null) {
    return null
  }
  return `${value.toLocaleString('ru-RU', { maximumFractionDigits: digits })} ${unit}`
}

function formatMoney(value: number, digits = 0): string {
  return value.toLocaleString('ru-RU', { maximumFractionDigits: digits })
}

function bandLabel(band: string | null): string {
  switch (band) {
    case 'BASE':
      return 'от 0 °C'
    case '0_-10':
      return '0…−10 °C'
    case '-10_-20':
      return '−10…−20 °C'
    default:
      return band ?? 'стандарт'
  }
}

function variantLabel(variant: string): string {
  if (variant.toLowerCase().includes('павловский')) {
    return 'Павловский гранит'
  }
  if (variant.toLowerCase().includes('ростовский')) {
    return 'Ростовский щебень'
  }
  return variant
}

function volumePrice(price: PartnerPrice): number | null {
  return price.priceType === 'FIXED' ? price.amount : null
}

function ProductDetailContent({
  detail,
  catalog,
  onBack,
  onOpen,
}: {
  detail: PartnerProductDetailData
  catalog: PartnerCatalogData
  onBack: () => void
  onOpen: (item: PartnerProduct) => void
}) {
  const item = detail.product
  const related = detail.related
  const group = catalog.groups.find((entry) =>
    entry.categories.some((category) => category.slug === item.categorySlug),
  )
  const phone = catalog.info.phone ?? '+78003013180'
  const tel = `tel:${phone}`

  const specs: Array<[string, string]> = []
  if (item.tu) specs.push(['ТУ / серия', item.tu])
  const weight = formatNumber(item.weightT, 'т')
  if (weight) specs.push(['Вес', weight])
  const length = formatNumber(item.lengthM, 'м')
  if (length) specs.push(['Длина', length])
  const width = formatNumber(item.widthM, 'м')
  if (width) specs.push(['Ширина', width])
  const thickness = formatNumber(item.thicknessM, 'м')
  if (thickness) specs.push(['Толщина', thickness])
  const volume = formatNumber(item.volumeM3, 'м³', 4)
  if (volume) specs.push(['Объём', volume])

  const fixedPrices = item.prices.filter((price) => volumePrice(price) != null)
  const variantOrder = (value: string): number => {
    const lower = value.toLowerCase()
    if (lower.includes('павловский')) return 0
    if (lower.includes('ростовский')) return 1
    return 2
  }
  const variants = Array.from(
    new Set(
      fixedPrices
        .map((price) => price.variant)
        .filter((value): value is string => Boolean(value)),
    ),
  ).sort((left, right) => variantOrder(left) - variantOrder(right))
  const bandOrder = ['BASE', '0_-10', '-10_-20']
  const bands = Array.from(
    new Set(
      fixedPrices
        .map((price) => price.temperatureBand)
        .filter((value): value is string => Boolean(value)),
    ),
  ).sort((left, right) => bandOrder.indexOf(left) - bandOrder.indexOf(right))
  const isVolumePriced = item.unit === 'м³' && fixedPrices.length > 0

  const [selectedVariant, setSelectedVariant] = useState<string | null>(variants[0] ?? null)
  const [selectedBand, setSelectedBand] = useState<string>(
    bands.includes('BASE') ? 'BASE' : (bands[0] ?? ''),
  )
  const [volumeInput, setVolumeInput] = useState('')

  const selectedPrice =
    fixedPrices.find(
      (price) =>
        price.variant === selectedVariant && price.temperatureBand === selectedBand,
    ) ??
    fixedPrices.find((price) => price.variant === selectedVariant) ??
    fixedPrices.find((price) => price.temperatureBand === selectedBand) ??
    fixedPrices[0] ??
    null

  const volumeValue = Number(volumeInput.replace(',', '.'))
  const total =
    selectedPrice && selectedPrice.amount != null && volumeValue > 0
      ? selectedPrice.amount * volumeValue
      : null

  return (
    <Screen title={item.name} onBack={onBack}>
      <article className="card">
        {item.imageUrl ? <PhotoGallery photos={[item.imageUrl]} alt={item.name} /> : null}
        <div className="card__body">
          <div className="card__category">{item.categoryTitle}</div>
          <div className="card__rows">
            {specs.map(([label, value]) => (
              <Row key={label} label={label} value={value} />
            ))}
            {item.attributes.map((attribute) => (
              <Row
                key={`${attribute.label}:${attribute.value}`}
                label={attribute.label}
                value={attribute.value}
              />
            ))}
            <Row label={ru.partnerCatalog.availability} value={availabilityLabel(item.availability)} />
            {!isVolumePriced ? (
              <Row label={ru.partnerCatalog.price} value={item.priceLabel} />
            ) : null}
          </div>

          {isVolumePriced ? (
            <div className="price-config">
              {variants.length > 0 ? (
                <div className="price-config__group">
                  <div className="price-config__label">Наполнитель</div>
                  <div className="price-options">
                    {variants.map((variant) => (
                      <button
                        key={variant}
                        type="button"
                        className={`price-option${selectedVariant === variant ? ' price-option--active' : ''}`}
                        onClick={() => setSelectedVariant(variant)}
                      >
                        {variantLabel(variant)}
                      </button>
                    ))}
                  </div>
                </div>
              ) : null}

              {bands.length > 0 ? (
                <div className="price-config__group">
                  <div className="price-config__label">Температурный режим</div>
                  <div className="price-options">
                    {bands.map((band) => (
                      <button
                        key={band}
                        type="button"
                        className={`price-option${selectedBand === band ? ' price-option--active' : ''}`}
                        onClick={() => setSelectedBand(band)}
                      >
                        {bandLabel(band)}
                      </button>
                    ))}
                  </div>
                </div>
              ) : null}

              <div className="price-highlight">
                <span className="price-highlight__value">
                  {selectedPrice?.amount != null
                    ? `${formatMoney(selectedPrice.amount)} ₽`
                    : ru.partnerCatalog.byRequest}
                </span>
                <span className="price-highlight__unit">за {item.unit}</span>
              </div>
              <div className="price-config__note">
                Цена с НДС, без доставки.
                {selectedPrice?.note ? ` ${selectedPrice.note}.` : ''}
              </div>

              <div className="price-calculator">
                <label className="price-calculator__field">
                  <span>Объём, м³</span>
                  <input
                    type="text"
                    inputMode="decimal"
                    value={volumeInput}
                    onChange={(event) =>
                      setVolumeInput(event.target.value.replace(/[^\d.,]/g, ''))
                    }
                    placeholder="например 5"
                  />
                </label>
                {total != null ? (
                  <div className="price-calculator__total">
                    Итого: <strong>{formatMoney(total, 2)} ₽</strong>
                  </div>
                ) : null}
              </div>
            </div>
          ) : null}

          <div className="card__actions">
            <Button
              asChild
              variant="primary"
              size="large"
              stretched
              onClick={() =>
                track('call_click', {
                  surface: 'zhbk',
                  place: 'product',
                  productId: item.id,
                  productName: item.name,
                  categorySlug: item.categorySlug,
                  categoryTitle: item.categoryTitle,
                  groupSlug: group?.slug ?? null,
                  groupTitle: group?.title ?? null,
                  price: item.priceAmount,
                  unit: item.unit,
                })
              }
            >
              <a href={tel}>
                {ru.partnerCatalog.call} {formatPhoneDisplay(phone)}
              </a>
            </Button>
          </div>
        </div>
      </article>

      {related.length > 0 ? (
        <div className="section">
          <h2 className="screen__subtitle">{ru.partnerCatalog.relatedTitle}</h2>
          <div className="list">
            {related.map((other) => (
              <button
                type="button"
                className="related-card"
                key={other.id}
                onClick={() => {
                  track('zhbk_product_open', {
                    productId: other.id,
                    productName: other.name,
                    categorySlug: other.categorySlug,
                    categoryTitle: other.categoryTitle,
                    groupSlug: group?.slug ?? null,
                    source: 'related',
                  })
                  onOpen(other)
                }}
              >
                {other.imageUrl ? (
                  <img className="related-card__image" src={other.imageUrl} alt="" loading="lazy" />
                ) : null}
                <span className="related-card__body">
                  <span className="related-card__title">{other.name}</span>
                  <span className="related-card__meta">{other.categoryTitle}</span>
                  <span className="related-card__price">{other.priceLabel}</span>
                </span>
              </button>
            ))}
          </div>
        </div>
      ) : null}
    </Screen>
  )
}

export function PartnerProductDetail({
  id,
  onBack,
  onOpen,
}: {
  id: number
  onBack: () => void
  onOpen: (item: PartnerProduct) => void
}) {
  const load = useCallback(
    () => Promise.all([fetchPartnerProduct(id), fetchPartnerCatalog()]),
    [id],
  )
  const { data, loading, error, retry } = useAsync(load)

  if (loading && !data) {
    return (
      <Screen title="" onBack={onBack}>
        <div className="catalog-count">Загрузка…</div>
      </Screen>
    )
  }
  if (error || !data) {
    return (
      <Screen title="" onBack={onBack}>
        <div className="catalog-count">Не удалось загрузить изделие.</div>
        <Button variant="secondary" size="medium" onClick={retry}>
          Повторить
        </Button>
      </Screen>
    )
  }

  const [detail, catalog] = data
  return (
    <ProductDetailContent
      key={detail.product.id}
      detail={detail}
      catalog={catalog}
      onBack={onBack}
      onOpen={onOpen}
    />
  )
}
