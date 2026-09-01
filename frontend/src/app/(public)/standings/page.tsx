import type { Metadata } from "next";
import { connection } from "next/server";

import { DataUnavailable } from "@/components/cricket/data-unavailable";
import { PublicPageHeader } from "@/components/cricket/public-page-header";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { getStandings, type StandingRow } from "@/lib/api/standings";
import {
  formatNetRunRate,
  formatPoints,
  formatRunRate,
} from "@/lib/cricket/formatters";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";

export const metadata: Metadata = {
  title: "Standings | Eid Cricket Fest",
  description: "Current Eid Cricket Fest standings table.",
};

export default async function StandingsPage() {
  await connection();

  const currentEdition = await getCurrentEditionData();

  if (currentEdition.status !== "ready") {
    return (
      <main>
        <PublicPageHeader
          description="Tournament standings will appear when the backend is reachable."
          title="Standings"
        />
        <DataUnavailable message={currentEdition.message} />
      </main>
    );
  }

  const standings = await getStandings(currentEdition.edition.id);

  return (
    <main>
      <PublicPageHeader
        description="Points table with wins, losses, ties, no-results, points, and net run rate."
        edition={currentEdition.edition}
        title="Standings"
      />
      <section className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        {standings.ok ? (
          <StandingsTable rows={standings.data.standings ?? []} />
        ) : (
          <DataUnavailable message="Standings are temporarily unavailable." />
        )}
      </section>
    </main>
  );
}

function StandingsTable({ rows }: { rows: StandingRow[] }) {
  return (
    <div className="overflow-x-auto rounded-sm border border-white/10 bg-card">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>#</TableHead>
            <TableHead>Team</TableHead>
            <TableHead className="text-right">P</TableHead>
            <TableHead className="text-right">W</TableHead>
            <TableHead className="text-right">L</TableHead>
            <TableHead className="text-right">T</TableHead>
            <TableHead className="text-right">NR</TableHead>
            <TableHead className="text-right">Pts</TableHead>
            <TableHead className="text-right">NRR</TableHead>
            <TableHead className="hidden text-right lg:table-cell">For</TableHead>
            <TableHead className="hidden text-right lg:table-cell">Against</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {rows.length > 0 ? (
            rows.map((row, index) => (
              <TableRow key={row.tournamentTeamId ?? index}>
                <TableCell className="font-mono text-secondary">
                  {row.rank ?? index + 1}
                </TableCell>
                <TableCell>
                  <span className="font-heading font-medium uppercase tracking-normal">
                    {row.shortName ?? row.teamName ?? "Team"}
                  </span>
                  {row.teamName && row.shortName ? (
                    <span className="block text-xs text-muted-foreground">
                      {row.teamName}
                    </span>
                  ) : null}
                </TableCell>
                <NumberCell value={row.played} />
                <NumberCell value={row.won} />
                <NumberCell value={row.lost} />
                <NumberCell value={row.tied} />
                <NumberCell value={row.noResult} />
                <TableCell className="text-right font-mono text-secondary">
                  {formatPoints(row.points)}
                </TableCell>
                <TableCell className="text-right font-mono">
                  {formatNetRunRate(row.netRunRate)}
                </TableCell>
                <TableCell className="hidden text-right font-mono lg:table-cell">
                  {formatRunRate(row.runRateFor)}
                </TableCell>
                <TableCell className="hidden text-right font-mono lg:table-cell">
                  {formatRunRate(row.runRateAgainst)}
                </TableCell>
              </TableRow>
            ))
          ) : (
            <TableRow>
              <TableCell
                className="py-6 text-center text-sm text-muted-foreground"
                colSpan={11}
              >
                Standings will appear after matches are completed.
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </div>
  );
}

function NumberCell({ value }: { value?: number }) {
  return <TableCell className="text-right font-mono">{value ?? 0}</TableCell>;
}
