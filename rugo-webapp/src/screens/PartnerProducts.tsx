import { useCallback, useEffect, useState } from 'react'
import { Button, Input } from '@maxhub/max-ui'
import { Screen } from '../components/Screen'
import { PartnerProductCard } from '../components/PartnerProductCard'
import { ConcreteCalculator } from './ConcreteCalculator'
import { Icon } from '../components/Icon'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import {
  fetchPartnerProducts,
  type PartnerProduct,
  type PartnerProductSort,
} from '../api/partnerCatalog'
import { useAsync } from '../lib/useAsync'
import { ru } from '../content/ru'
import { track } from '../analytics/track'

const PAGE_SIZE = 12

const SORT_OPTIONS: ReadonlyArray<{ value: PartnerProductSort; label: string }> = [
  { value: 'name', label: 'По названию' },
  { value: 'priceAsc', label: 'Цена: сначала дешёвые' },
  { value: 'priceDesc', label: 'Цена: сначала дорогие' },
  { value: 'weightAsc', label: 'Вес: сначала лёгкие' },
  { value: 'lengthAsc', label: 'Длина: сначала короткие' },
  { value: 'widthAsc', label: 'Ширина: сначала узкие' },
]

function PartnerProductsList({
  category,
  label,
  group,
  onBack,
  onOpen,
}: {
  category: string
  label: string
  group: string
  onBack: () => void
  onOpen: (item: PartnerProduct) => void
}) {
  const [input, setInput] = useState('')
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState<PartnerProductSort>('name')
  const [page, setPage] = useState(0)

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const next = input.trim()
      setQuery(next)
      setPage(0)
      if (next) {
        track('zhbk_search', {
          surface: 'category',
          categorySlug: category,
          query: next,
          queryLength: next.length,
        })
      }
    }, 300)
    return () => window.clearTimeout(timer)
  }, [input, category])

  const load = useCallback(
    () => fetchPartnerProducts(category, query, page, PAGE_SIZE, sort),
    [category, query, page, sort],
  )
  const { data, loading, error, retry } = useAsync(load)

  useEffect(() => {
    if (!data) {
      return
    }
    track('zhbk_products_view', {
      categorySlug: category,
      categoryTitle: label,
      groupSlug: group || null,
      query: query || null,
      sort,
      page: data.page,
      total: data.total,
    })
  }, [data, category, label, group, query, sort])

  const goToPage = (next: number) => {
    setPage(next)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const reset = () => {
    setInput('')
    setQuery('')
    setSort('name')
    setPage(0)
  }

  if (loading && !data) {
    return (
      <Screen title={label} onBack={onBack} stickyHeader={false}>
        <LoadingState />
      </Screen>
    )
  }
  if (error && !data) {
    return (
      <Screen title={label} onBack={onBack} stickyHeader={false}>
        <ErrorState onRetry={retry} />
      </Screen>
    )
  }
  if (!data) {
    return (
      <Screen title={label} onBack={onBack} stickyHeader={false}>
        <LoadingState />
      </Screen>
    )
  }

  const totalPages = Math.max(1, Math.ceil(data.total / PAGE_SIZE))
  const currentPage = data.page
  const isFiltered = query.trim().length > 0 || sort !== 'name'
  const hasPrice = data.items.some((item) => item.priceAmount != null)
  const hasWeight = data.items.some((item) => item.weightT != null)
  const hasLength = data.items.some((item) => item.lengthM != null)
  const hasWidth = data.items.some((item) => item.widthM != null)
  const availableSorts = SORT_OPTIONS.filter((option) => {
    switch (option.value) {
      case 'priceAsc':
      case 'priceDesc':
        return sort === option.value || hasPrice
      case 'weightAsc':
        return sort === option.value || hasWeight
      case 'lengthAsc':
        return sort === option.value || hasLength
      case 'widthAsc':
        return sort === option.value || hasWidth
      default:
        return true
    }
  })

  return (
    <Screen title={label} onBack={onBack} stickyHeader={false}>
      <div className="catalog-search-sticky">
        <Input
          size="medium"
          value={input}
          onChange={(event) => setInput(event.target.value)}
          placeholder="Поиск по названию, например ПК60"
          iconBefore={<Icon name="search" width={18} height={18} />}
          withClearButton
        />
        <div className="product-sort-row">
          <label className="product-sort">
            <span className="product-sort__label">Сортировка</span>
            <select
              className="product-sort__select"
              value={sort}
              onChange={(event) => {
                setSort(event.target.value as PartnerProductSort)
                setPage(0)
              }}
            >
              {availableSorts.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          {isFiltered ? (
            <button type="button" className="product-reset" onClick={reset}>
              Сбросить
            </button>
          ) : null}
        </div>
        <div className="product-list-meta">
          <span>Найдено: {data.total}</span>
          {data.total > PAGE_SIZE ? (
            <span>
              Страница {currentPage + 1} из {totalPages}
            </span>
          ) : null}
        </div>
      </div>

      {error ? <ErrorState onRetry={retry} /> : null}

      {data.items.length === 0 ? (
        <EmptyState text={ru.catalog.empty} />
      ) : (
        <div className={`partner-product-list${loading ? ' partner-product-list--loading' : ''}`}>
          {data.items.map((item, index) => (
            <PartnerProductCard
              key={item.id}
              item={item}
              onOpen={() => {
                track('zhbk_product_open', {
                  productId: item.id,
                  productName: item.name,
                  categorySlug: item.categorySlug,
                  categoryTitle: item.categoryTitle,
                  groupSlug: group || null,
                  source: 'list',
                  position: index,
                  page: data.page,
                  query: query || null,
                  sort,
                })
                onOpen(item)
              }}
            />
          ))}
        </div>
      )}

      {data.total > PAGE_SIZE ? (
        <div className="catalog-pagination">
          <Button
            variant="secondary"
            size="medium"
            disabled={loading || currentPage <= 0}
            onClick={() => goToPage(currentPage - 1)}
          >
            Назад
          </Button>
          <span className="catalog-pagination__info">
            {currentPage + 1} / {totalPages}
          </span>
          <Button
            variant="secondary"
            size="medium"
            disabled={loading || currentPage + 1 >= totalPages}
            onClick={() => goToPage(currentPage + 1)}
          >
            Вперёд
          </Button>
        </div>
      ) : null}
    </Screen>
  )
}

export function PartnerProducts(props: {
  category: string
  label: string
  group: string
  onBack: () => void
  onOpen: (item: PartnerProduct) => void
}) {
  if (props.category.startsWith('beton-')) {
    return (
      <ConcreteCalculator
        category={props.category}
        label={props.label}
        group={props.group}
        onBack={props.onBack}
      />
    )
  }
  return <PartnerProductsList {...props} />
}
