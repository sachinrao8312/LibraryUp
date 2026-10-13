'use client'

import { useState } from 'react'
import {
  Bell, Building2, CheckCircle2, ChevronRight, CircleHelp, CreditCard, FileText,
  Home, LayoutDashboard, Menu, Plus, Settings, Users, Wallet, X,
} from 'lucide-react'

const navItems = [
  { label: 'Overview', icon: LayoutDashboard },
  { label: 'Properties', icon: Building2 },
  { label: 'Tenants', icon: Users },
  { label: 'Payments', icon: CreditCard },
  { label: 'Documents', icon: FileText },
]

const properties = [
  { name: 'Oakwood Apartments', location: '24 units · Portland, OR', amount: '$18,420', occupancy: '92%', tone: 'thumb-a', glyph: '⌂' },
  { name: 'The Linden House', location: '8 units · Portland, OR', amount: '$7,850', occupancy: '100%', tone: 'thumb-b', glyph: '▥' },
  { name: 'Riverside Lofts', location: '12 units · Beaverton, OR', amount: '$10,200', occupancy: '83%', tone: 'thumb-c', glyph: '⌂' },
]

const activities = [
  { icon: CreditCard, title: 'Rent received', detail: 'Maya Chen paid $1,640 for Oakwood #204', time: '12 minutes ago' },
  { icon: Users, title: 'New tenant added', detail: 'Jordan Ellis joined The Linden House #3B', time: '2 hours ago' },
  { icon: FileText, title: 'Document uploaded', detail: 'Lease agreement added to Oakwood #108', time: 'Yesterday' },
]

const viewCopy: Record<string, { eyebrow: string; title: string; description: string }> = {
  Overview: { eyebrow: 'Monday, October 13, 2026', title: 'Good morning, Alex', description: 'Here is what is happening across your portfolio.' },
  Properties: { eyebrow: 'Portfolio', title: 'Your properties', description: 'Keep every building, unit, and maintenance detail in view.' },
  Tenants: { eyebrow: 'People', title: 'Your tenants', description: 'Manage residents and keep communication moving.' },
  Payments: { eyebrow: 'Cash flow', title: 'Payments', description: 'Track collections, upcoming rent, and payment history.' },
  Documents: { eyebrow: 'Records', title: 'Documents', description: 'Find leases, invoices, and important property records.' },
  Settings: { eyebrow: 'Workspace', title: 'Settings', description: 'Tune your RentUp workspace and account preferences.' },
}

function EmptyView({ active, onCreate }: { active: string; onCreate: () => void }) {
  const copy = {
    Properties: ['Add your first property', 'Create a clean home for units, occupancy, and property details.'],
    Tenants: ['Invite a tenant', 'Your tenant directory will appear here once you add residents.'],
    Payments: ['No payments to review', 'Connect your rent workflow to see collections and upcoming payments.'],
    Documents: ['Upload a document', 'Keep leases and property records easy to find in one place.'],
    Settings: ['Workspace settings', 'Account preferences are ready to be configured.'],
  }[active as 'Properties' | 'Tenants' | 'Payments' | 'Documents' | 'Settings'] ?? ['Nothing here yet', 'Choose an action to get started.']

  return <section className="empty-view panel"><div className="empty-icon"><Home size={21} /></div><h2>{copy[0]}</h2><p>{copy[1]}</p><button className="new-button" onClick={onCreate}><Plus size={16} />{active === 'Settings' ? 'Open preferences' : 'Create new'}</button></section>
}

export default function Dashboard() {
  const [active, setActive] = useState('Overview')
  const [showToast, setShowToast] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)

  function notify(message: string) {
    setShowToast(true)
    window.setTimeout(() => setShowToast(false), 2600)
    if (message) setNotificationsOpen(false)
  }

  const copy = viewCopy[active]

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><div className="brand-mark">R</div><span>RentUp</span></div>
        <nav className="nav" aria-label="Primary navigation">
          {navItems.map(({ label, icon: Icon }) => <button key={label} className={`nav-item ${active === label ? 'active' : ''}`} onClick={() => setActive(label)} aria-current={active === label ? 'page' : undefined}><Icon size={17} /><span>{label}</span></button>)}
        </nav>
        <div className="sidebar-bottom">
          <div className="help-card"><strong>Need a hand?</strong><p>Find answers in our help center or chat with support.</p><button onClick={() => notify('Help center is ready for you.')}>Visit help center <ChevronRight size={12} /></button></div>
          <button className={`nav-item ${active === 'Settings' ? 'active' : ''}`} onClick={() => setActive('Settings')}><Settings size={17} /><span>Settings</span></button>
          <button className="profile" onClick={() => setActive('Settings')} aria-label="Open profile settings"><div className="avatar">AR</div><div><strong style={{ fontSize: 12 }}>Alex Rivera</strong><small>Property owner</small></div><ChevronRight size={14} style={{ marginLeft: 'auto', color: '#9d9990' }} /></button>
        </div>
      </aside>

      <main className="main">
        <header className="topbar">
          <div><p className="eyebrow">{copy.eyebrow}</p><h1 className="title">{copy.title}</h1><p className="page-description">{copy.description}</p></div>
          <div className="top-actions"><div className="notification-wrap"><button className="icon-button" aria-label="Notifications" onClick={() => setNotificationsOpen(!notificationsOpen)}><Bell size={17} />{notificationsOpen && <span className="notification-dot" />}</button>{notificationsOpen && <div className="notification-popover"><div className="popover-heading"><strong>Notifications</strong><button aria-label="Close notifications" onClick={() => setNotificationsOpen(false)}><X size={14} /></button></div><p><CheckCircle2 size={15} /> Rent from Oakwood #204 received.</p><p><FileText size={15} /> New lease uploaded yesterday.</p></div>}</div><button className="new-button" onClick={() => notify(`New ${active === 'Overview' ? 'activity' : active.toLowerCase().replace('s', '')} started.`)}><Plus size={16} /><span>New activity</span></button></div>
        </header>

        {active === 'Overview' ? <>
          <section className="stats" aria-label="Portfolio summary">
            <div className="stat"><div className="stat-label">Monthly revenue <Wallet size={15} /></div><div className="stat-value">$36,470</div><div className="stat-note">↑ 8.4% from last month</div></div>
            <div className="stat"><div className="stat-label">Collected this month <CreditCard size={15} /></div><div className="stat-value">94.2%</div><div className="stat-note">↑ 2.1% from last month</div></div>
            <div className="stat"><div className="stat-label">Active tenants <Users size={15} /></div><div className="stat-value">38</div><div className="stat-note">2 move-ins this month</div></div>
            <div className="stat"><div className="stat-label">Open tasks <CircleHelp size={15} /></div><div className="stat-value">7</div><div className="stat-note warn">3 need attention</div></div>
          </section>
          <div className="content-grid"><div className="left-column"><section className="panel" style={{ marginBottom: 20 }}><div className="panel-header"><div><h2 className="panel-title">Your properties</h2><p className="panel-sub">A quick look at your portfolio</p></div><button className="text-button" onClick={() => setActive('Properties')}>View all <ChevronRight size={13} /></button></div><div className="property-list">{properties.map((property) => <button className="property" key={property.name} onClick={() => setActive('Properties')}><div className={`property-thumb ${property.tone}`}>{property.glyph}</div><div className="property-info"><strong>{property.name}</strong><span>{property.location}</span></div><div className="occupancy"><strong>{property.amount}</strong><span>{property.occupancy} occupied</span></div><ChevronRight size={15} color="#aaa49b" /></button>)}</div></section><section className="panel"><div className="panel-header"><div><h2 className="panel-title">Revenue overview</h2><p className="panel-sub">Your collected rent over the past 6 months</p></div><button className="text-button" onClick={() => notify('Revenue range updated.')}>This year <ChevronRight size={13} /></button></div><div className="chart">{[44,60,54,72,65,90].map((height, i) => <div className="bar-wrap" key={i}><div className={`bar ${i === 5 ? 'current' : ''}`} style={{ height: `${height}%` }} /><span className="bar-label">{['May','Jun','Jul','Aug','Sep','Oct'][i]}</span></div>)}</div><div className="chart-legend"><span>Average monthly collected</span><strong>$32,880</strong></div></section></div><section className="panel"><div className="panel-header"><div><h2 className="panel-title">Recent activity</h2><p className="panel-sub">Stay up to date with your portfolio</p></div><button className="icon-button" style={{ width: 30, height: 30 }} aria-label="More activity" onClick={() => notify('Activity menu opened.')}><Menu size={15} /></button></div><div className="activity-list">{activities.map(({ icon: Icon, title, detail, time }) => <button className="activity" key={title} onClick={() => notify(`${title} opened.`)}><div className="activity-icon"><Icon size={15} /></div><div><strong>{title}</strong><p>{detail}</p><time>{time}</time></div></button>)}</div><button className="text-button" style={{ marginTop: 22, padding: 0 }} onClick={() => notify('Showing all activity.')}>See all activity <ChevronRight size={13} /></button></section></div>
        </> : <EmptyView active={active} onCreate={() => notify(`${active} action started.`)} />}
      </main>
      <nav className="mobile-nav" aria-label="Mobile navigation">{[...navItems, { label: 'Settings', icon: Settings }].map(({ label, icon: Icon }) => <button key={label} className={active === label ? 'active' : ''} onClick={() => setActive(label)} aria-current={active === label ? 'page' : undefined}><Icon size={18} /><span>{label}</span></button>)}</nav>
      {showToast && <div className="toast" role="status"><CheckCircle2 size={16} />Action ready — your workspace is up to date.</div>}
    </div>
  )
}
