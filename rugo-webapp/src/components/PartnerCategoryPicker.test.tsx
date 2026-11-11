import { render, screen } from '@testing-library/react'
import { vi } from 'vitest'
import { PartnerCategoryPicker } from './PartnerCategoryPicker'
import type { PartnerGroup } from '../api/partnerCatalog'

const GROUPS: PartnerGroup[] = [
  {
    slug: 'fundamenty',
    title: 'Фундаменты',
    categories: [
      { slug: 'bloki', title: 'Блоки', imageUrl: null, productCount: 2 },
    ],
  },
  {
    slug: 'beton',
    title: 'Бетон',
    categories: [
      { slug: 'beton-tovarnyy', title: 'Товарный бетон', imageUrl: null, productCount: 5 },
    ],
  },
]

test('при возврате в каталог не прокручивает к раскрытому разделу', () => {
  const scrollTo = vi.spyOn(window, 'scrollTo')

  render(
    <PartnerCategoryPicker groups={GROUPS} initialOpen="beton" onChange={() => undefined} />,
  )

  expect(screen.getByText('Товарный бетон')).toBeInTheDocument()
  expect(scrollTo).not.toHaveBeenCalled()

  scrollTo.mockRestore()
})
