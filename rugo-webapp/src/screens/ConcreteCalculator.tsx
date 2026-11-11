import { useCallback, useEffect, useState } from 'react'
import { Button } from '@maxhub/max-ui'
import { Screen } from '../components/Screen'
import { ErrorState, LoadingState } from '../components/States'
import {
  availabilityLabel,
  fetchPartnerProducts,
  type PartnerPrice,
  type PartnerProduct,
} from '../api/partnerCatalog'
import { formatPhoneDisplay } from '../form/validators'
import { useAsync } from '../lib/useAsync'
import { ru } from '../content/ru'
import { track } from '../analytics/track'

const PHONE = '+78003013180'

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

function gradeLabel(item: PartnerProduct): string {
  const classValue = item.attributes.find((attribute) => attribute.label === 'Класс')?.value
  if (classValue) {
    return classValue.split(' ')[0]
  }
  const mark = item.attributes.find((attribute) => attribute.label === 'Марка')?.value
  if (mark) {
    return mark
  }
  return item.name.replace(/^Бетон БСТ /, '').replace(/^Раствор /, '')
}

function fixedPrices(item: PartnerProduct): PartnerPrice[] {
  return item.prices.filter(
    (price) => price.priceType === 'FIXED' && price.amount != null,
  )
}

function sortedVariants(item: PartnerProduct): string[] {
  const order = (value: string): number => {
    const lower = value.toLowerCase()
    if (lower.includes('павловский')) return 0
    if (lower.includes('ростовский')) return 1
    return 2
  }
  return Array.from(
    new Set(
      fixedPrices(item)
        .map((price) => price.variant)
        .filter((value): value is string => Boolean(value)),
    ),
  ).sort((left, right) => order(left) - order(right))
}

function sortedBands(item: PartnerProduct): string[] {
  const order = ['BASE', '0_-10', '-10_-20']
  return Array.from(
    new Set(
      fixedPrices(item)
        .map((price) => price.temperatureBand)
        .filter((value): value is string => Boolean(value)),
    ),
  ).sort((left, right) => order.indexOf(left) - order.indexOf(right))
}

function findPrice(
  item: PartnerProduct,
  variant: string | null,
  band: string,
): PartnerPrice | null {
  const prices = fixedPrices(item)
  return (
    prices.find(
      (price) => price.variant === variant && price.temperatureBand === band,
    ) ??
    prices.find((price) => price.variant === variant) ??
    prices.find((price) => price.temperatureBand === band) ??
    prices[0] ??
    null
  )
}

function CalculatorContent({
  items,
  label,
  heroImage,
  groupSlug,
  onBack,
}: {
  items: PartnerProduct[]
  label: string
  heroImage: string | null
  groupSlug: string
  onBack: () => void
}) {
  const initial = items[0]
  const [selectedId, setSelectedId] = useState<number>(initial.id)
  const selected = items.find((item) => item.id === selectedId) ?? initial
  const variants = sortedVariants(selected)
  const bands = sortedBands(selected)
  const [selectedVariant, setSelectedVariant] = useState<string | null>(variants[0] ?? null)
  const [selectedBand, setSelectedBand] = useState<string>(
    bands.includes('BASE') ? 'BASE' : (bands[0] ?? ''),
  )
  const [volumeInput, setVolumeInput] = useState('')

  const selectGrade = (item: PartnerProduct) => {
    const nextVariants = sortedVariants(item)
    const nextBands = sortedBands(item)
    setSelectedId(item.id)
    setSelectedVariant(nextVariants[0] ?? null)
    setSelectedBand(nextBands.includes('BASE') ? 'BASE' : (nextBands[0] ?? ''))
  }

  const selectedPrice = findPrice(selected, selectedVariant, selectedBand)
  const volumeValue = Number(volumeInput.replace(',', '.'))
  const total =
    selectedPrice && selectedPrice.amount != null && volumeValue > 0
      ? selectedPrice.amount * volumeValue
      : null

  return (
    <Screen title={label} onBack={onBack}>
      {heroImage ? (
        <div className="concrete-hero">
          <img className="concrete-hero__image" src={heroImage} alt="" />
          <div className="concrete-hero__overlay">
            <div className="concrete-hero__title">Бетон от завода</div>
            <div className="concrete-hero__subtitle">
              АО «Завод ЖБК» · цена с НДС, без доставки
            </div>
          </div>
        </div>
      ) : null}
      <article className="card">
        <div className="card__body">
          <div className="price-config">
            <div className="price-config__group">
              <div className="price-config__label">
                {selected.unit === 'м³' && selected.name.startsWith('Раствор') ? 'Марка' : 'Класс бетона'}
              </div>
              <div className="price-options">
                {items.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    className={`price-option${item.id === selected.id ? ' price-option--active' : ''}`}
                    onClick={() => selectGrade(item)}
                  >
                    {gradeLabel(item)}
                  </button>
                ))}
              </div>
            </div>

            <div className="card__rows">
              {selected.attributes.map((attribute) => (
                <div className="card__row" key={`${attribute.label}:${attribute.value}`}>
                  <span className="card__row-label">{attribute.label}</span>
                  <span className="card__row-value">{attribute.value}</span>
                </div>
              ))}
              <div className="card__row">
                <span className="card__row-label">{ru.partnerCatalog.availability}</span>
                <span className="card__row-value">
                  {availabilityLabel(selected.availability)}
                </span>
              </div>
            </div>

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
              <span className="price-highlight__unit">за {selected.unit}</span>
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

          <div className="card__actions">
            <Button
              asChild
              variant="primary"
              size="large"
              stretched
              onClick={() =>
                track('call_click', {
                  surface: 'zhbk',
                  place: 'calculator',
                  categorySlug: selected.categorySlug,
                  categoryTitle: selected.categoryTitle,
                  groupSlug: groupSlug ?? null,
                  productId: selected.id,
                  productName: selected.name,
                  grade: gradeLabel(selected),
                  variant: selectedVariant,
                  temperatureBand: selectedBand,
                  volume: volumeInput || null,
                  total: total,
                })
              }
            >
              <a href={`tel:${PHONE}`}>
                {ru.partnerCatalog.call} {formatPhoneDisplay(PHONE)}
              </a>
            </Button>
          </div>
        </div>
      </article>
    </Screen>
  )
}

export function ConcreteCalculator({
  category,
  label,
  group,
  onBack,
}: {
  category: string
  label: string
  group: string
  onBack: () => void
}) {
  const load = useCallback(
    () => fetchPartnerProducts(category, '', 0, 100, 'name'),
    [category],
  )
  const { data, loading, error, retry } = useAsync(load)

  useEffect(() => {
    if (!data) {
      return
    }
    track('zhbk_calculator_open', {
      categorySlug: category,
      categoryTitle: label,
      groupSlug: group || null,
    })
  }, [data, category, label, group])

  if (loading && !data) {
    return (
      <Screen title={label} onBack={onBack}>
        <LoadingState />
      </Screen>
    )
  }
  if (error || !data || data.items.length === 0) {
    return (
      <Screen title={label} onBack={onBack}>
        <ErrorState onRetry={retry} />
      </Screen>
    )
  }

  return (
    <CalculatorContent
      key={category}
      items={data.items}
      label={label}
      heroImage={category === 'beton-beton-tovarnyy' ? '/partners/zhbk/beton.jpg' : null}
      groupSlug={group}
      onBack={onBack}
    />
  )
}
