import type { SVGProps } from 'react'

export type IconName = 'search' | 'calendar' | 'handshake' | 'map' | 'phone' | 'mail' | 'telegram' | 'check' | 'copy' | 'share' | 'chevronLeft' | 'chevronRight'

const paths: Record<IconName, string> = {
  search: 'M11 4a7 7 0 1 0 4.2 12.6l3.6 3.6 1.4-1.4-3.6-3.6A7 7 0 0 0 11 4Zm0 2a5 5 0 1 1 0 10 5 5 0 0 1 0-10Z',
  calendar:
    'M7 2v2H5a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-2V2h-2v2H9V2H7Zm12 7v10H5V9h14ZM5 7h14v-1H5v1Z',
  handshake:
    'M12 6.5 15 4l5 4.5-2 4.5-2-1.5v4.5h-4v3H6v-5l-3-2 3-4 3 2 3-3Zm0 2.8-3 3-3-2-1 1.3 2.5 1.7H8v3.5h3v3h2v-3h2v-4.4l-3-2.1Z',
  map: 'M9 2 3 4v18l6-2 6 2 6-2V2l-6 2-6-2Zm0 2.2 5 1.6v13.9l-5-1.6V4.2Zm-4 .1 2-.7v13.8l-2 .7V4.3Zm12 1 2-.7v13.8l-2 .7V5.3Z',
  phone:
    'M6.6 2.9a1.5 1.5 0 0 1 2 .3l1.6 2.1a1.5 1.5 0 0 1-.1 1.9l-1.1 1.1a12 12 0 0 0 5.7 5.7l1.1-1.1a1.5 1.5 0 0 1 1.9-.1l2.1 1.6a1.5 1.5 0 0 1 .3 2l-1 1.3c-.6.8-1.6 1.2-2.6 1A17 17 0 0 1 4.4 7.5c-.2-1 .2-2 1-2.6l1.2-1Z',
  mail: 'M3 5h18a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1Zm1 2v.5l8 5 8-5V7H4Zm16 2.8-7.5 4.7a1 1 0 0 1-1 0L4 9.8V17h16v-7.2Z',
  telegram:
    'M21.94 4.4l-3.2 15.1c-.24 1.07-.87 1.33-1.77.83l-4.9-3.6-2.36 2.27c-.26.26-.48.48-.98.48l.35-4.97 9.05-8.18c.39-.35-.09-.55-.61-.2L5.4 13.36 1.54 12.15c-1.05-.33-1.07-1.05.22-1.55L20.5 3.5c.87-.32 1.64.2 1.44.9Z',
  check: 'M9.6 16.2 5.4 12l-1.4 1.4 5.6 5.6 12-12-1.4-1.4-10.6 10.6Z',
  copy: 'M8 2h9a2 2 0 0 1 2 2v12h-2V4H8V2Zm-3 4h9a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2Zm0 2v12h9V8H5Z',
  share:
    'M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81a3 3 0 1 0-3-3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9a3 3 0 1 0 0 6c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65a2.92 2.92 0 1 0 2.91-2.92Z',
  chevronLeft: 'M14.7 5.3 8.1 12l6.6 6.7 1.4-1.4L10.9 12l5.2-5.3z',
  chevronRight: 'M9.3 5.3 15.9 12l-6.6 6.7-1.4-1.4L13.1 12 7.9 6.7z',
}

export function Icon({ name, ...rest }: { name: IconName } & SVGProps<SVGSVGElement>) {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" focusable="false" {...rest}>
      <path d={paths[name]} />
    </svg>
  )
}
