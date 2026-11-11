import { ru } from '../content/ru'
import { bridge } from '../bridge'

/** Юридический подвал: контакты, данные оператора, ссылки на политику/условия. */
export function LegalFooter() {
  return (
    <footer className="footer">
      <div className="footer__contacts">
        <a href={`tel:${ru.contacts.phone.replace(/[^\d+]/g, '')}`}>{ru.contacts.phone}</a>
        <a
          href={`mailto:${ru.contacts.email}`}
          onClick={() => bridge.openLink(`mailto:${ru.contacts.email}`)}
        >
          {ru.contacts.email}
        </a>
      </div>
      {/* ЗАГЛУШКА скрыта: реквизиты оператора заполнить перед публикацией (требования MAX) */}
      {/* <div>{ru.footer.operatorName}</div> */}
      {/* <div>{ru.footer.operatorDetails}</div> */}
      <div>
        <a href="/legal.html#privacy">{ru.footer.privacy}</a>
      </div>
      <div>
        <a href="/legal.html#terms">{ru.footer.terms}</a>
      </div>
      <div>{ru.footer.copyright}</div>
    </footer>
  )
}
