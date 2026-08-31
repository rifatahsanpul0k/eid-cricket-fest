import Link from "next/link";

const footerLinks = [
  { href: "/knockout", label: "Knockout" },
  { href: "/awards", label: "Awards" },
  { href: "/history", label: "History" },
];

export function SiteFooter() {
  return (
    <footer className="border-t border-white/10 bg-surface">
      <div className="mx-auto flex w-full max-w-7xl flex-col gap-4 px-4 py-8 text-sm text-muted-foreground sm:px-6 md:flex-row md:items-center md:justify-between lg:px-8">
        <div>
          <p className="font-heading uppercase tracking-normal text-foreground">
            Eid Cricket Fest
          </p>
          <p className="mt-2">Live scoring, fixtures, standings, and tournament history.</p>
        </div>
        <nav aria-label="Secondary navigation" className="flex flex-wrap gap-3">
          {footerLinks.map((link) => (
            <Link
              className="underline-offset-4 hover:text-foreground hover:underline"
              href={link.href}
              key={link.href}
            >
              {link.label}
            </Link>
          ))}
        </nav>
      </div>
    </footer>
  );
}
