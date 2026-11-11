import { useEffect, useRef, useState } from 'react'
import { Icon } from './Icon'
import type { PartnerCategory, PartnerGroup } from '../api/partnerCatalog'
import { track } from '../analytics/track'

function CategoryTile({
  category,
  groupTitle,
  onChange,
}: {
  category: PartnerCategory
  groupTitle?: string
  onChange: (category: PartnerCategory) => void
}) {
  const hasImage = Boolean(category.imageUrl)
  return (
    <button
      type="button"
      className={`partner-category-tile${hasImage ? '' : ' partner-category-tile--text'}`}
      onClick={() => onChange(category)}
    >
      {category.imageUrl ? (
        <img
          className="partner-category-tile__image"
          src={category.imageUrl}
          alt=""
          loading="lazy"
        />
      ) : null}
      <span className="partner-category-tile__body">
        <span className="partner-category-tile__title">{category.title}</span>
        <span className="partner-category-tile__meta">
          {category.productCount > 0 ? `${category.productCount} изд.` : 'скоро'}
        </span>
        {groupTitle ? (
          <span className="partner-category-tile__group">{groupTitle}</span>
        ) : null}
      </span>
    </button>
  )
}

function CategoryGrid({
  categories,
  groupTitle,
  onChange,
}: {
  categories: readonly PartnerCategory[]
  groupTitle?: string
  onChange: (category: PartnerCategory) => void
}) {
  const hasImages = categories.some((category) => Boolean(category.imageUrl))
  return (
    <div className={`partner-category-grid${hasImages ? '' : ' partner-category-grid--text'}`}>
      {categories.map((category) => (
        <CategoryTile
          key={category.slug}
          category={category}
          groupTitle={groupTitle}
          onChange={onChange}
        />
      ))}
    </div>
  )
}

function FlatCategoryGrid({
  groups,
  onChange,
}: {
  groups: readonly PartnerGroup[]
  onChange: (category: PartnerCategory) => void
}) {
  return (
    <div className="partner-category-grid">
      {groups.flatMap((group) =>
        group.categories.map((category) => (
          <CategoryTile
            key={`${group.slug}:${category.slug}`}
            category={category}
            groupTitle={group.title}
            onChange={onChange}
          />
        )),
      )}
    </div>
  )
}

function groupTotal(group: PartnerGroup): number {
  return group.categories.reduce((sum, category) => sum + category.productCount, 0)
}

const GROUP_COVERS: Record<string, string> = {
  beton: '/partners/zhbk/beton-thumb.jpg',
}

function groupCover(group: PartnerGroup): string | null {
  return GROUP_COVERS[group.slug] ?? group.categories.find((category) => category.imageUrl)?.imageUrl ?? null
}

export function PartnerCategoryPicker({
  groups,
  onChange,
  flat = false,
  initialOpen = null,
}: {
  groups: readonly PartnerGroup[]
  onChange: (category: PartnerCategory) => void
  flat?: boolean
  initialOpen?: string | null
}) {
  const [open, setOpen] = useState<string | null>(initialOpen)
  const groupRefs = useRef<Record<string, HTMLDivElement | null>>({})
  const scrollFrame = useRef<number | null>(null)

  useEffect(
    () => () => {
      if (scrollFrame.current != null) {
        window.cancelAnimationFrame(scrollFrame.current)
      }
    },
    [],
  )

  const scrollToGroup = (slug: string) => {
    const node = groupRefs.current[slug]
    if (!node) {
      return
    }
    if (scrollFrame.current != null) {
      window.cancelAnimationFrame(scrollFrame.current)
    }

    scrollFrame.current = window.requestAnimationFrame(() => {
      scrollFrame.current = null
      const header = document.querySelector<HTMLElement>('.screen__header')
      const headerHeight = header?.getBoundingClientRect().height ?? 0
      const offset = headerHeight + 16
      const rect = node.getBoundingClientRect()
      const viewportBottom = window.visualViewport?.height ?? window.innerHeight
      const fits = rect.top >= offset - 1 && rect.bottom <= viewportBottom - 16
      if (fits) {
        return
      }
      const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
      window.scrollTo({
        top: Math.max(0, window.scrollY + rect.top - offset),
        behavior: reducedMotion ? 'auto' : 'smooth',
      })
    })
  }

  const toggleGroup = (slug: string, expanded: boolean) => {
    const next = expanded ? null : slug
    setOpen(next)
    if (next) {
      scrollToGroup(next)
    }
  }

  if (flat) {
    return <FlatCategoryGrid groups={groups} onChange={onChange} />
  }

  if (groups.length === 1) {
    const group = groups[0]
    return (
      <div className="partner-group-single">
        <div className="partner-group-single__meta">
          {group.categories.length} категорий · {groupTotal(group)} изделий
        </div>
        <CategoryGrid categories={group.categories} onChange={onChange} />
      </div>
    )
  }

  return (
    <div className="partner-group-list">
      {groups.map((group) => {
        const expanded = open === group.slug
        const cover = groupCover(group)
        const total = groupTotal(group)

        return (
          <div
            className={`partner-group-tile${expanded ? ' partner-group-tile--open' : ''}`}
            key={group.slug}
            ref={(node) => {
              groupRefs.current[group.slug] = node
            }}
          >
            <button
              type="button"
              className="partner-group-tile__header"
              aria-expanded={expanded}
              onClick={() => {
                if (!expanded) {
                  track('zhbk_group_open', { groupSlug: group.slug, groupTitle: group.title })
                }
                toggleGroup(group.slug, expanded)
              }}
            >
              {cover ? (
                <img className="partner-group-tile__image" src={cover} alt="" loading="lazy" />
              ) : (
                <span className="partner-group-tile__placeholder" aria-hidden="true">
                  {group.title.slice(0, 1)}
                </span>
              )}
              <span className="partner-group-tile__body">
                <span className="partner-group-tile__title">{group.title}</span>
                <span className="partner-group-tile__meta">
                  {group.categories.length} категорий · {total} изделий
                </span>
              </span>
              <span
                className={
                  expanded
                    ? 'partner-group-tile__chevron partner-group-tile__chevron--open'
                    : 'partner-group-tile__chevron'
                }
              >
                <Icon name="chevronRight" width={18} height={18} />
              </span>
            </button>
            {expanded ? (
              <CategoryGrid categories={group.categories} onChange={onChange} />
            ) : null}
          </div>
        )
      })}
    </div>
  )
}
