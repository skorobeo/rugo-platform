import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { bridge } from './bridge'
import { ru } from './content/ru'
import { Menu } from './screens/Menu'
import { SelectScreen } from './screens/SelectScreen'
import { EquipmentCategories } from './screens/EquipmentCategories'
import { MaterialTypes } from './screens/MaterialTypes'
import { EquipmentList } from './screens/EquipmentList'
import { EquipmentDetail } from './screens/EquipmentDetail'
import { EquipmentEditor } from './screens/EquipmentEditor'
import { MaterialEditor } from './screens/MaterialEditor'
import { Cabinet } from './screens/Cabinet'
import { MaterialsList } from './screens/MaterialsList'
import { PartnerCatalog } from './screens/PartnerCatalog'
import { PartnerProducts } from './screens/PartnerProducts'
import { PartnerProductDetail } from './screens/PartnerProductDetail'
import { Repair } from './screens/Repair'
import { About } from './screens/About'
import { Help } from './screens/Help'
import { Share } from './screens/Share'
import { LeadForm } from './form/LeadForm'
import type { MenuTarget, OfferType, Screen } from './navigation'
import { useViewport } from './lib/useViewport'

export default function App() {
  const [stack, setStack] = useState<Screen[]>([{ name: 'menu' }])
  const [equipmentCategory, setEquipmentCategory] = useState('')
  const current = stack[stack.length - 1]
  const scrollPositions = useRef<number[]>([])
  const pendingScroll = useRef<number | null>(null)
  useViewport()

  const push = useCallback(
    (screen: Screen) => {
      scrollPositions.current[stack.length - 1] = window.scrollY
      pendingScroll.current = 0
      setStack((prev) => [...prev, screen])
    },
    [stack.length],
  )
  const replace = useCallback(
    (screen: Screen) => {
      scrollPositions.current[stack.length - 1] = window.scrollY
      pendingScroll.current = 0
      setStack((prev) => [...prev.slice(0, -1), screen])
    },
    [stack.length],
  )
  const back = useCallback(() => {
    if (stack.length <= 1) {
      return
    }
    const targetIndex = stack.length - 2
    pendingScroll.current = scrollPositions.current[targetIndex] ?? 0
    setStack((prev) => prev.slice(0, -1))
  }, [stack.length])
  const reset = useCallback(() => {
    pendingScroll.current = 0
    setStack([{ name: 'menu' }])
  }, [])

  useLayoutEffect(() => {
    if (pendingScroll.current == null) {
      return
    }
    window.scrollTo(0, pendingScroll.current)
    pendingScroll.current = null
  }, [stack])

  useEffect(() => {
    bridge.ready()
  }, [])

  const canGoBack = stack.length > 1
  const backRef = useRef(back)
  useEffect(() => {
    backRef.current = back
  }, [back])
  useEffect(() => {
    const callback = () => backRef.current()
    if (!canGoBack || !bridge.hasNativeBackButton()) {
      bridge.hideBackButton(callback)
      return undefined
    }
    bridge.showBackButton(callback)
    return () => bridge.hideBackButton(callback)
  }, [canGoBack])

  const handleMenu = (target: MenuTarget) => {
    push({ name: target === 'orders' ? 'cabinet' : target })
  }

  const openPartnerProducts = useCallback(
    (category: string, label: string, group: string, query: string) => {
      scrollPositions.current[stack.length - 1] = window.scrollY
      pendingScroll.current = 0
      setStack((prev) => {
        const next = [...prev]
        const catalogIndex = next.map((screen) => screen.name).lastIndexOf('partnerCatalog')
        if (catalogIndex >= 0) {
          next[catalogIndex] = { name: 'partnerCatalog', openGroup: group, query }
        }
        return [...next, { name: 'partnerProducts', category, label, group }]
      })
    },
    [stack.length],
  )

  const openMaterials = (value: string) => {
    const selected = ru.materials.types.find((type) => type.value === value)
    push({ name: 'materialsList', type: value, label: selected?.label ?? value })
  }

  const renderScreen = () => {
    switch (current.name) {
      case 'menu':
        return <Menu onSelect={handleMenu} />
      case 'equipment':
        return (
          <EquipmentCategories
            value={equipmentCategory}
            onSelect={(category) => {
              setEquipmentCategory(category)
              push({ name: 'equipmentList', category })
            }}
            onBack={back}
          />
        )
      case 'equipmentList':
        return (
          <EquipmentList
            category={current.category}
            onBack={back}
            onOpen={(item) => push({ name: 'equipmentDetail', item })}
          />
        )
      case 'equipmentDetail':
        return (
          <EquipmentDetail
            item={current.item}
            onBack={back}
            onOpen={(item) => push({ name: 'equipmentDetail', item })}
          />
        )
      case 'equipmentEditor':
        return <EquipmentEditor item={current.item} profile={current.profile} onClose={back} />
      case 'materialEditor':
        return <MaterialEditor item={current.item} profile={current.profile} onClose={back} />
      case 'materials':
        return <MaterialTypes onSelect={openMaterials} onBack={back} />
      case 'materialsList':
        return <MaterialsList type={current.type} label={current.label} onBack={back} />
      case 'partnerCatalog':
        return (
          <PartnerCatalog
            initialGroup={current.openGroup}
            initialQuery={current.query}
            onBack={back}
            onSelect={openPartnerProducts}
          />
        )
      case 'partnerProducts':
        return (
          <PartnerProducts
            key={current.category}
            category={current.category}
            label={current.label}
            group={current.group}
            onBack={back}
            onOpen={(item) =>
              push({
                name: 'partnerProductDetail',
                id: item.id,
                category: item.categorySlug,
                label: item.categoryTitle,
              })
            }
          />
        )
      case 'partnerProductDetail':
        return (
          <PartnerProductDetail
            id={current.id}
            onBack={back}
            onOpen={(item) =>
              replace({
                name: 'partnerProductDetail',
                id: item.id,
                category: item.categorySlug,
                label: item.categoryTitle,
              })
            }
          />
        )
      case 'repair':
        return <Repair onBack={back} />
      case 'partner':
        return (
          <SelectScreen
            title={ru.partner.chooseTitle}
            options={[
              { key: 'equipment', title: ru.partner.equipment },
              { key: 'materials', title: ru.partner.materials },
              { key: 'repair', title: ru.partner.repair },
            ]}
            onSelect={(key) => {
              if (key === 'equipment') {
                push({ name: 'cabinet' })
                return
              }
              push({ name: 'form', offerType: key as OfferType })
            }}
            onBack={back}
          />
        )
      case 'form':
        return <LeadForm offerType={current.offerType} onClose={reset} />
      case 'cabinet':
        return (
          <Cabinet
            onBack={back}
            onAdd={(profile) => push({ name: 'equipmentEditor', profile })}
            onEdit={(item, profile) => push({ name: 'equipmentEditor', item, profile })}
            onAddMaterial={(profile) => push({ name: 'materialEditor', profile })}
            onEditMaterial={(item, profile) => push({ name: 'materialEditor', item, profile })}
          />
        )
      case 'about':
        return <About onBack={back} />
      case 'help':
        return <Help onBack={back} />
      case 'share':
        return <Share onBack={back} />
    }
  }

  return <main className="app">{renderScreen()}</main>
}
