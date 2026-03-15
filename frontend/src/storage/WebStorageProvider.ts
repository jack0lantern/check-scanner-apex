import type { IStorageProvider } from '@apex/shared'

export const WebStorageProvider: IStorageProvider = {
  getItem: (key) => window.sessionStorage.getItem(key),
  setItem: (key, val) => window.sessionStorage.setItem(key, val),
  removeItem: (key) => window.sessionStorage.removeItem(key),
}
