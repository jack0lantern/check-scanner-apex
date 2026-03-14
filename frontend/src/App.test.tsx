import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import App from './App'

describe('App shell branding', () => {
  it('renders the branded dark shell wrappers', () => {
    const { container } = render(<App />)

    expect(container.querySelector('.app-shell')).toBeInTheDocument()
    expect(container.querySelector('.app-shell__glow')).toBeInTheDocument()
    expect(container.querySelector('.app-shell__shape')).toBeInTheDocument()
    expect(container.querySelector('.app-main__surface')).toBeInTheDocument()

    expect(screen.getByRole('heading', { name: /check scanner/i })).toBeInTheDocument()
  })
})
