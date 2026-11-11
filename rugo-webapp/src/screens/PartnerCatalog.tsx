import { useCallback, useEffect, useRef, useState } from 'react'
import { Input } from '@maxhub/max-ui'
import { Screen } from '../components/Screen'
import { PartnerCategoryPicker } from '../components/PartnerCategoryPicker'
import { Icon } from '../components/Icon'
import { EmptyState, ErrorState, LoadingState } from '../components/States'
import { fetchPartnerCatalog } from '../api/partnerCatalog'
import { useAsync } from '../lib/useAsync'
import { ru } from '../content/ru'
import { track } from '../analytics/track'

export function PartnerCatalog({
  initialGroup,
  initialQuery,
  onSelect,
  onBack,
}: {
  initialGroup?: string | null
  initialQuery?: string
  onSelect: (category: string, label: string, group: string, query: string) => void
  onBack: () => void
}) {
  const load = useCallback(() => fetchPartnerCatalog(), [])
  const { data, loading, error, retry } = useAsync(load)
  const [query, setQuery] = useState(initialQuery ?? '')
  const opened = useRef(false)
  const lastSearch = useRef('')

  useEffect(() => {
    if (opened.current) {
      return
    }
    opened.current = true
    track('zhbk_open', {
      entry: 'partner_banner',
      initialGroup: initialGroup ?? null,
      query: initialQuery ?? null,
    })
  }, [initialGroup, initialQuery])

  useEffect(() => {
    const next = query.trim()
    if (!next || next === lastSearch.current) {
      return undefined
    }
    const timer = window.setTimeout(() => {
      lastSearch.current = next
      track('zhbk_search', {
        surface: 'catalog',
        query: next,
        queryLength: next.length,
      })
    }, 400)
    return () => window.clearTimeout(timer)
  }, [query])

  if (loading) {
    return (
      <Screen title="" onBack={onBack}>
        <LoadingState />
      </Screen>
    )
  }
  if (error || !data) {
    return (
      <Screen title="" onBack={onBack}>
        <ErrorState onRetry={retry} />
      </Screen>
    )
  }

  const normalized = query.trim().toLocaleLowerCase('ru')
  const visibleGroups = normalized
    ? data.groups
        .map((group) => ({
          ...group,
          categories: group.categories.filter((category) =>
            category.title.toLocaleLowerCase('ru').includes(normalized),
          ),
        }))
        .filter((group) => group.categories.length > 0)
    : data.groups

  const visibleCategoryCount = visibleGroups.reduce(
    (sum, group) => sum + group.categories.length,
    0,
  )

  return (
    <Screen title="" onBack={onBack}>
      <article className="card partner-card">
        <div className="card__body">
          <div className="card__title">{data.info.name}</div>
          <div className="card__category">{data.info.subtitle}</div>

          <div className="partner-facts">
            {data.info.facts.map((fact) => (
              <div className="partner-fact" key={fact.label}>
                <span className="partner-fact__value">{fact.value}</span>
                <span className="partner-fact__label">{fact.label}</span>
              </div>
            ))}
          </div>

          <div className="partner-clients">
            <span className="partner-clients__label">{ru.partnerCatalog.clientsLabel}</span>
            <span className="partner-clients__list">{data.info.clients.join(' · ')}</span>
          </div>
        </div>
      </article>

      <div className="catalog-filters">
        <Input
          size="medium"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Поиск категории"
          iconBefore={<Icon name="search" width={18} height={18} />}
          withClearButton
        />
      </div>

      {normalized ? (
        <div className="catalog-count">Найдено категорий: {visibleCategoryCount}</div>
      ) : null}

      {visibleGroups.length === 0 ? (
        <EmptyState text={ru.catalog.empty} />
      ) : (
        <PartnerCategoryPicker
          key={normalized}
          groups={visibleGroups}
          flat={normalized.length > 0}
          initialOpen={initialGroup}
          onChange={(category) => {
            const group = data.groups.find((item) =>
              item.categories.some((entry) => entry.slug === category.slug),
            )
            track('zhbk_category_open', {
              groupSlug: group?.slug ?? null,
              groupTitle: group?.title ?? null,
              categorySlug: category.slug,
              categoryTitle: category.title,
              productCount: category.productCount,
              viaSearch: normalized.length > 0,
            })
            onSelect(category.slug, category.title, group?.slug ?? '', query)
          }}
        />
      )}
    </Screen>
  )
}
