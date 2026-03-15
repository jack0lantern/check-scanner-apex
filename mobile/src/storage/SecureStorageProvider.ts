import * as SecureStore from 'expo-secure-store'
import type { IStorageProvider } from '@apex/shared'

export const SecureStorageProvider: IStorageProvider = {
  getItem: (key) => SecureStore.getItemAsync(key),
  setItem: (key, val) => SecureStore.setItemAsync(key, val),
  removeItem: (key) => SecureStore.deleteItemAsync(key),
}
