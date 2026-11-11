import { Icon } from './Icon'
import { ru } from '../content/ru'

/** Лента обновлений сервиса: версии и списки изменений. */
export function UpdatesList() {
  return (
    <div className="updates">
      {ru.updates.map((release) => (
        <section className="update" key={release.version}>
          <div className="update__meta">
            {release.version} · {release.date}
          </div>
          <div className="update__title">{release.title}</div>
          <ul className="update__list">
            {release.items.map((item) => (
              <li className="update__item" key={item}>
                <Icon className="update__check" name="check" width={16} height={16} />
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  )
}
