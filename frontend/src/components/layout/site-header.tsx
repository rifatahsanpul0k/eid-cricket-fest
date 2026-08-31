import Link from "next/link";

import { MobileNavigation } from "@/components/layout/mobile-navigation";

const navigation = [
  { label: "Home", href: "/" },
  { label: "Live", href: "/live" },
  { label: "Fixtures", href: "/fixtures" },
  { label: "Standings", href: "/standings" },
  { label: "Teams", href: "/teams" },
  { label: "Players", href: "/players" },
  { label: "Statistics", href: "/statistics" },
];

export function SiteHeader() {
  return (
    <header className="sticky top-0 z-40 border-b border-white/10 bg-background/92 backdrop-blur-md">
      <div className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <Link
          className="flex items-center gap-3 whitespace-nowrap font-heading text-base font-semibold uppercase tracking-normal outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          href="/"
        >
          <span className="grid size-9 place-items-center rounded-sm bg-secondary font-mono text-sm font-bold text-secondary-foreground">
            ECF
          </span>
          <span>Eid Cricket Fest</span>
        </Link>
        <nav
          aria-label="Primary navigation"
          className="hidden items-center gap-1 lg:flex"
        >
          {navigation.map((item) => (
            <Link
              className="rounded-sm px-3 py-2 text-sm font-medium text-muted-foreground outline-none transition-colors hover:bg-surface-elevated hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50"
              href={item.href}
              key={item.label}
            >
              {item.label}
            </Link>
          ))}
        </nav>
        <div className="hidden lg:block" />
        <MobileNavigation />
      </div>
    </header>
  );
}
