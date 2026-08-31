import Link from "next/link";

import type { MatchStage, MatchStatus } from "@/lib/api/matches";
import { fixturesHref } from "@/components/cricket/fixtures/fixture-filter-nav";

export function PaginationNav({
  hasNext,
  hasPrevious,
  page,
  stage,
  status,
  totalPages,
}: {
  hasNext?: boolean;
  hasPrevious?: boolean;
  page: number;
  stage?: MatchStage;
  status?: MatchStatus;
  totalPages?: number;
}) {
  return (
    <nav
      aria-label="Fixture pages"
      className="mt-6 flex flex-col gap-3 border-t border-white/10 pt-5 sm:flex-row sm:items-center sm:justify-between"
    >
      <p className="font-mono text-xs uppercase text-muted-foreground">
        Page {page + 1}
        {totalPages ? ` of ${totalPages}` : ""}
      </p>
      <div className="flex gap-2">
        <PageLink
          disabled={!hasPrevious}
          href={fixturesHref({ page: Math.max(page - 1, 0), stage, status })}
          label="Previous"
        />
        <PageLink
          disabled={!hasNext}
          href={fixturesHref({ page: page + 1, stage, status })}
          label="Next"
        />
      </div>
    </nav>
  );
}

function PageLink({
  disabled,
  href,
  label,
}: {
  disabled: boolean;
  href: string;
  label: string;
}) {
  if (disabled) {
    return (
      <span className="rounded-sm border border-white/10 px-3 py-2 text-sm text-muted-foreground opacity-50">
        {label}
      </span>
    );
  }

  return (
    <Link
      className="rounded-sm border border-white/10 px-3 py-2 text-sm font-medium transition-colors hover:bg-surface-elevated"
      href={href}
    >
      {label}
    </Link>
  );
}
