import type { Metadata } from "next"
import "./globals.css"

export const metadata: Metadata = {
  title: "RentUp — Property operations, made calmer",
  description: "A thoughtful workspace for managing properties, tenants, rent and documents.",
}

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>
}
