import { beforeEach, describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import App from './App'
import { AuthProvider } from './context/AuthContext'

describe('App routing shell', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
  })

  it('renders Scanify landing page with split login choices', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <AuthProvider>
          <App />
        </AuthProvider>
      </MemoryRouter>
    )

    expect(screen.getByRole('heading', { name: /welcome to scanify/i })).toBeInTheDocument()
    expect(screen.getByText(/are you an:/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /investor/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /operator/i })).toBeInTheDocument()
  })

  it('redirects unauthenticated investor route to investor login', () => {
    render(
      <MemoryRouter initialEntries={['/investor']}>
        <AuthProvider>
          <App />
        </AuthProvider>
      </MemoryRouter>
    )

    expect(screen.getByRole('heading', { name: /scanify investor/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /enter investor portal/i })).toBeInTheDocument()
  })

  it('redirects unauthenticated operator route to operator login', () => {
    render(
      <MemoryRouter initialEntries={['/operator']}>
        <AuthProvider>
          <App />
        </AuthProvider>
      </MemoryRouter>
    )

    expect(screen.getByRole('heading', { name: /scanify operator/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /enter operator portal/i })).toBeInTheDocument()
  })

  it('logs investor in and navigates to investor portal', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/investor/login']}>
        <AuthProvider>
          <App />
        </AuthProvider>
      </MemoryRouter>
    )

    await user.type(screen.getByLabelText(/email/i), 'test@example.com')
    await user.type(screen.getByLabelText(/password/i), 'anypassword')
    await user.click(screen.getByRole('button', { name: /enter investor portal/i }))

    expect(await screen.findByRole('button', { name: /logout/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /deposit/i })).toBeInTheDocument()
  })

  it('logs operator in and navigates to operator portal', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/operator/login']}>
        <AuthProvider>
          <App />
        </AuthProvider>
      </MemoryRouter>
    )

    await user.type(screen.getByLabelText(/email/i), 'op@example.com')
    await user.type(screen.getByLabelText(/password/i), 'anypassword')
    await user.click(screen.getByRole('button', { name: /enter operator portal/i }))

    expect(await screen.findByRole('button', { name: /logout/i })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /operator queue/i })).toBeInTheDocument()
  })
})
