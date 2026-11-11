/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_VERSION?: string
  readonly VITE_ANALYTICS_ENABLED?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
