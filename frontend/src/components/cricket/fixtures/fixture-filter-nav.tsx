import Link from "next/link";

import {
  MATCH_STAGE_LABELS,
  MATCH_STATUS_LABELS,
  type MatchStage,
  type MatchStatus,
} from "@/lib/cricket/match-labels";

export function FixtureFilterNav({
  status,
  stage,
}: {
  status?: MatchStatus;
  stage?: MatchStage;
}) {
  return (
    <div className="grid gap-5 border-b border-white/10 pb-6">
      <FilterGroup
        active={status}
        allHref={fixturesHref({ stage })}
        items={Object.entries(MATCH_STATUS_LABELS).map(([value, label]) => ({
          href: fixturesHref({ status: value as MatchStatus, stage }),
          label,
          value: value as MatchStatus,
        }))}
        label="Status"
      />
      <FilterGroup
        active={stage}
        allHref={fixturesHref({ status })}
        items={Object.entries(MATCH_STAGE_LABELS).map(([value, label]) => ({
          href: fixturesHref({ status, stage: value as MatchStage }),
          label,
          value: value as MatchStage,
        }))}
        label="Stage"
      />
    </div>
  );
}

function FilterGroup<T extends string>({
  active,
  allHref,
  items,
  label,
}: {
  active?: T;
  allHref: string;
  items: { href: string; label: string; value: T }[];
  label: string;
}) {
  return (
    <div>
      <p className="mb-3 font-mono text-xs uppercase text-muted-foreground">
        {label}
      </p>
      <div className="flex flex-wrap gap-2">
        <FilterLink active={!active} href={allHref} label="All" />
        {items.map((item) => (
          <FilterLink
            active={active === item.value}
            href={item.href}
            key={item.value}
            label={item.label}
          />
        ))}
      </div>
    </div>
  );
}

function FilterLink({
  active,
  href,
  label,
}: {
  active: boolean;
  href: string;
  label: string;
}) {
  return (
    <Link
      className={
        active
          ? "rounded-sm bg-secondary px-3 py-2 text-sm font-semibold text-secondary-foreground"
          : "rounded-sm border border-white/10 px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-surface-elevated hover:text-foreground"
      }
      href={href}
    >
      {label}
    </Link>
  );
}

function fixturesHref({
  page,
  stage,
  status,
}: {
  page?: number;
  stage?: MatchStage;
  status?: MatchStatus;
}) {
  const params = new URLSearchParams();

  if (status) {
    params.set("status", status);
  }

  if (stage) {
    params.set("stage", stage);
  }

  if (page && page > 0) {
    params.set("page", page.toString());
  }

  const query = params.toString();

  return query ? `/fixtures?${query}` : "/fixtures";
}

export { fixturesHref };
