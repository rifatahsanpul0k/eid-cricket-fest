import type { Metadata } from "next";
import Link from "next/link";

import { buttonVariants } from "@/components/ui/button";
import { getMyStatistics } from "@/lib/auth/my-cricket-api";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";
import { cn } from "@/lib/utils";

export const metadata: Metadata = {
  title: "My Statistics",
};

export default async function MyStatisticsPage() {
  const currentEdition = await getCurrentEditionData();
  const statisticsResult =
    currentEdition.status === "ready"
      ? await getMyStatistics(currentEdition.edition.id)
      : undefined;
  const statistics =
    statisticsResult && "ok" in statisticsResult && statisticsResult.ok
      ? statisticsResult.data
      : undefined;

  return (
    <main className="flex-1">
      <section className="border-b border-white/10 bg-background">
        <div className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <p className="font-mono text-xs uppercase text-primary">
            My Cricket
          </p>
          <h1 className="mt-3 font-heading text-4xl font-bold uppercase tracking-normal">
            My Statistics
          </h1>
          {currentEdition.status === "ready" ? (
            <p className="mt-3 text-sm text-muted-foreground">
              {currentEdition.edition.name}
            </p>
          ) : null}
        </div>
      </section>
      <section className="mx-auto grid w-full max-w-7xl gap-4 px-4 py-8 sm:px-6 lg:px-8">
        {currentEdition.status !== "ready" ? (
          <EmptyPanel message={currentEdition.message} />
        ) : statistics ? (
          <>
            <div className="rounded-sm border border-white/10 bg-card p-5 text-sm">
              <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
                {statistics.playerName}
              </h2>
              <dl className="mt-5 grid gap-3 sm:grid-cols-3">
                <Stat label="Matches" value={statistics.matchesPlayed} />
                <Stat label="Runs" value={statistics.batting?.runs} />
                <Stat label="Wickets" value={statistics.bowling?.wickets} />
              </dl>
            </div>
            <div className="grid gap-4 lg:grid-cols-3">
              <StatPanel title="Batting">
                <Stat label="Innings" value={statistics.batting?.innings} />
                <Stat label="Highest" value={statistics.batting?.highestScore} />
                <Stat label="Average" value={statistics.batting?.average} />
                <Stat label="Strike rate" value={statistics.batting?.strikeRate} />
                <Stat label="Fours" value={statistics.batting?.fours} />
                <Stat label="Sixes" value={statistics.batting?.sixes} />
              </StatPanel>
              <StatPanel title="Bowling">
                <Stat label="Overs" value={statistics.bowling?.overs} />
                <Stat label="Runs" value={statistics.bowling?.runsConceded} />
                <Stat label="Best" value={statistics.bowling?.bestBowling} />
                <Stat label="Average" value={statistics.bowling?.average} />
                <Stat label="Economy" value={statistics.bowling?.economy} />
                <Stat label="Legal balls" value={statistics.bowling?.legalBalls} />
              </StatPanel>
              <StatPanel title="Fielding">
                <Stat label="Catches" value={statistics.fielding?.catches} />
                <Stat label="Stumpings" value={statistics.fielding?.stumpings} />
                <Stat label="Run outs" value={statistics.fielding?.runOuts} />
              </StatPanel>
            </div>
          </>
        ) : (
          <EmptyPanel message="Edition statistics are not available yet." />
        )}
        <Link className={cn(buttonVariants({ variant: "outline" }), "w-fit")} href="/account">
          Back to account
        </Link>
      </section>
    </main>
  );
}

function StatPanel({
  children,
  title,
}: {
  children: React.ReactNode;
  title: string;
}) {
  return (
    <div className="rounded-sm border border-white/10 bg-card p-5 text-sm">
      <h2 className="font-heading text-xl font-bold uppercase tracking-normal">
        {title}
      </h2>
      <dl className="mt-5 grid grid-cols-2 gap-3">{children}</dl>
    </div>
  );
}

function Stat({
  label,
  value,
}: {
  label: string;
  value?: number | string;
}) {
  return (
    <div>
      <dt className="text-xs uppercase text-muted-foreground">{label}</dt>
      <dd className="mt-1 font-medium text-foreground">{value ?? 0}</dd>
    </div>
  );
}

function EmptyPanel({ message }: { message: string }) {
  return (
    <div className="rounded-sm border border-white/10 bg-card p-5 text-sm">
      <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
        Statistics unavailable
      </h2>
      <p className="mt-3 text-muted-foreground">{message}</p>
    </div>
  );
}
