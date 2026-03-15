import type { ExpoConfig, ConfigContext } from 'expo/config'

export default ({ config }: ConfigContext): ExpoConfig => ({
  ...config,
  name: 'Scanify',
  slug: 'scanify-mobile',
  version: '1.0.0',
  orientation: 'portrait',
  scheme: 'scanify',
  extra: {
    apiBaseUrl: process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080',
  },
})
