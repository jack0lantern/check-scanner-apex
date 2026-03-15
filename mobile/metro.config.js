const { getDefaultConfig } = require('expo/metro-config')
const path = require('path')

const projectRoot = __dirname
const workspaceRoot = path.resolve(projectRoot, '..')

const config = getDefaultConfig(projectRoot)

// Monorepo: @apex/shared resolves to TypeScript source
config.resolver.extraNodeModules = {
  '@apex/shared': path.resolve(workspaceRoot, 'packages/shared/src'),
}

module.exports = config
