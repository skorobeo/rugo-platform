import type { SVGProps } from 'react'

export type IconName =
  | 'search'
  | 'phone'
  | 'check'
  | 'close'
  | 'copy'
  | 'chevronLeft'
  | 'chevronRight'
  | 'refresh'
  | 'dashboard'
  | 'leads'
  | 'catalog'
  | 'users'
  | 'zhbk'
  | 'plus'
  | 'edit'
  | 'trash'
  | 'eye'
  | 'arrowRight'
  | 'alert'

const paths: Record<IconName, string> = {
  search: 'M11 4a7 7 0 1 0 4.2 12.6l3.6 3.6 1.4-1.4-3.6-3.6A7 7 0 0 0 11 4Zm0 2a5 5 0 1 1 0 10 5 5 0 0 1 0-10Z',
  phone:
    'M6.6 2.9a1.5 1.5 0 0 1 2 .3l1.6 2.1a1.5 1.5 0 0 1-.1 1.9l-1.1 1.1a12 12 0 0 0 5.7 5.7l1.1-1.1a1.5 1.5 0 0 1 1.9-.1l2.1 1.6a1.5 1.5 0 0 1 .3 2l-1 1.3c-.6.8-1.6 1.2-2.6 1A17 17 0 0 1 4.4 7.5c-.2-1 .2-2 1-2.6l1.2-1Z',
  check: 'M9.6 16.2 5.4 12l-1.4 1.4 5.6 5.6 12-12-1.4-1.4-10.6 10.6Z',
  close:
    'M18.3 5.7 12 12l6.3 6.3-1.4 1.4L10.6 13.4 4.3 19.7 2.9 18.3 9.2 12 2.9 5.7 4.3 4.3 10.6 10.6 16.9 4.3z',
  copy: 'M8 2h9a2 2 0 0 1 2 2v12h-2V4H8V2Zm-3 4h9a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2Zm0 2v12h9V8H5Z',
  chevronLeft: 'M14.7 5.3 8.1 12l6.6 6.7 1.4-1.4L10.9 12l5.2-5.3z',
  chevronRight: 'M9.3 5.3 15.9 12l-6.6 6.7-1.4-1.4L13.1 12 7.9 6.7z',
  refresh:
    'M12 5V2L7 6l5 4V7a5 5 0 1 1-5 5H5a7 7 0 1 0 7-7Z',
  dashboard:
    'M3 3h8v8H3V3Zm10 0h8v5h-8V3ZM3 13h8v8H3v-8Zm10-3h8v11h-8V10Z',
  leads:
    'M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Zm3 5v2h8V8H8Zm0 4v2h8v-2H8Zm0 4v2h5v-2H8Z',
  catalog:
    'M4 4h7v7H4V4Zm9 0h7v7h-7V4ZM4 13h7v7H4v-7Zm9 0h7v7h-7v-7Z',
  users:
    'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm7 0a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM2 20c0-3.3 3.1-6 7-6s7 2.7 7 6H2Zm14.5 0c-.2-2.2-1.3-4.1-3-5.4.8-.4 1.6-.6 2.5-.6 3.3 0 6 2.2 6 5v1h-5.5Z',
  zhbk:
    'M3 4h18v4H3V4Zm1 6h16v10H4V10Zm4 2v6h3v-6H8Zm5 0v6h3v-6h-3Z',
  plus: 'M11 5h2v6h6v2h-6v6h-2v-6H5v-2h6V5Z',
  edit: 'M4 17.3V20h2.7L17.8 8.9l-2.7-2.7L4 17.3ZM20.7 5.6a1 1 0 0 0 0-1.4l-1-1a1 1 0 0 0-1.4 0l-1.3 1.3 2.7 2.7 1-1.6Z',
  trash:
    'M9 3h6l1 2h4v2H4V5h4l1-2Zm-3 6h12l-1 12H7L6 9Zm4 2v8h2v-8h-2Z',
  eye: 'M12 5c5 0 9 4.5 10 7-1 2.5-5 7-10 7S3 14.5 2 12c1-2.5 5-7 10-7Zm0 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8Zm0 2a2 2 0 1 1 0 4 2 2 0 0 1 0-4Z',
  arrowRight: 'M13.2 5.3 11.8 6.7l4.3 4.3H4v2h12.1l-4.3 4.3 1.4 1.4 6.7-6.7-6.7-6.7Z',
  alert:
    'M12 2 1 21h22L12 2Zm0 5 7.5 12h-15L12 7Zm-1 4v4h2v-4h-2Zm0 5v2h2v-2h-2Z',
}

export function Icon({ name, ...rest }: { name: IconName } & SVGProps<SVGSVGElement>) {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" focusable="false" {...rest}>
      <path d={paths[name]} />
    </svg>
  )
}
