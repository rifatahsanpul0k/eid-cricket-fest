import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import type { TournamentEdition } from "@/lib/api/tournaments";
import { TOURNAMENT_PRODUCT_NAME } from "@/lib/tournament/branding";
import { formatDate, formatNumber } from "@/lib/utils/format";

export function TournamentHero({
  edition,
}: {
  edition: TournamentEdition;
}) {
  const editionName = edition.name ?? "Current edition";

  return (
    <section className="border-b border-white/10 bg-background text-foreground">
      <div className="mx-auto grid min-h-[460px] w-full max-w-7xl items-center gap-8 px-4 py-12 sm:px-6 lg:grid-cols-[1.15fr_0.85fr] lg:px-8">
        <div className="max-w-3xl">
          <Badge className="mb-5 rounded-full bg-secondary text-secondary-foreground">
            {edition.status ?? "TOURNAMENT"}
          </Badge>
          <h1 className="font-heading text-4xl font-bold uppercase leading-tight tracking-normal sm:text-5xl lg:text-6xl">
            {TOURNAMENT_PRODUCT_NAME}
          </h1>
          <p className="mt-4 font-heading text-2xl font-semibold uppercase tracking-normal text-primary sm:text-3xl">
            {editionName}
          </p>
          <p className="mt-5 max-w-2xl text-base leading-7 text-muted-foreground sm:text-lg">
            Fixtures,
            live scoring, standings, and tournament stories into one public
            match-day home.
          </p>
          <div className="mt-8 flex flex-col gap-3 sm:flex-row">
            <Link
              className="rounded-sm bg-secondary px-4 py-3 text-center text-sm font-semibold text-secondary-foreground outline-none transition-colors hover:bg-secondary/85 focus-visible:ring-3 focus-visible:ring-ring/50"
              href="/live"
            >
              View Live Match
            </Link>
            <Link
              className="rounded-sm border border-white/20 px-4 py-3 text-center text-sm font-semibold outline-none transition-colors hover:bg-surface-elevated focus-visible:ring-3 focus-visible:ring-ring/50"
              href="/fixtures"
            >
              View Matches
            </Link>
          </div>
        </div>
        <dl className="grid gap-3 rounded-sm border border-white/10 bg-surface p-4 sm:grid-cols-2">
          <HeroStat label="Start" value={formatDate(edition.startDate)} />
          <HeroStat label="End" value={formatDate(edition.endDate)} />
          <HeroStat
            label="Overs"
            value={`${formatNumber(edition.oversPerInnings)} per innings`}
          />
          <HeroStat
            label="Registration"
            value={registrationLabel(edition.status)}
          />
        </dl>
      </div>
    </section>
  );
}

function HeroStat({
  label,
  value,
}: {
  label: string;
  value: string | number;
}) {
  return (
    <div className="rounded-sm border border-white/10 bg-surface-elevated p-4">
      <dt className="font-mono text-xs uppercase text-muted-foreground">
        {label}
      </dt>
      <dd className="mt-2 font-heading text-lg font-semibold uppercase tracking-normal">
        {value}
      </dd>
    </div>
  );
}

function registrationLabel(status: TournamentEdition["status"]) {
  return status === "REGISTRATION_OPEN" ? "Open" : "See tournament updates";
}
