import type { Metadata } from "next";
import { connection } from "next/server";

import { DataUnavailable } from "@/components/cricket/data-unavailable";
import { PlayerLink } from "@/components/cricket/player-link";
import { PublicPageHeader } from "@/components/cricket/public-page-header";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  getStatistics,
  type BattingLeader,
  type BowlingLeader,
} from "@/lib/api/statistics";
import { formatRunRate } from "@/lib/cricket/formatters";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";

export const metadata: Metadata = {
  title: "Statistics | Eid Cricket Fest",
  description: "Batting and bowling leaders for Eid Cricket Fest.",
};

export default async function StatisticsPage() {
  await connection();

  const currentEdition = await getCurrentEditionData();

  if (currentEdition.status === "unavailable") {
    return (
      <main>
        <PublicPageHeader
          description="Tournament leaders will appear when the backend is reachable."
          title="Statistics"
        />
        <DataUnavailable message={currentEdition.message} />
      </main>
    );
  }

  const statistics = await getStatistics(currentEdition.edition.id);

  return (
    <main>
      <PublicPageHeader
        description="Batting and bowling leaderboards from the current tournament edition."
        edition={currentEdition.edition}
        title="Statistics"
      />
      <section className="mx-auto grid w-full max-w-7xl gap-5 px-4 py-8 sm:px-6 lg:px-8 xl:grid-cols-2">
        {statistics.ok ? (
          <>
            <BattingTable rows={statistics.data.batting ?? []} />
            <BowlingTable rows={statistics.data.bowling ?? []} />
          </>
        ) : (
          <DataUnavailable message="Statistics are temporarily unavailable." />
        )}
      </section>
    </main>
  );
}

function BattingTable({ rows }: { rows: BattingLeader[] }) {
  return (
    <Leaderboard title="Batting Leaders">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>#</TableHead>
            <TableHead>Player</TableHead>
            <TableHead className="text-right">Runs</TableHead>
            <TableHead className="text-right">Inn</TableHead>
            <TableHead className="text-right">Avg</TableHead>
            <TableHead className="text-right">SR</TableHead>
            <TableHead className="text-right">HS</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {rows.length > 0 ? (
            rows.map((row, index) => (
              <TableRow key={row.playerId ?? index}>
                <TableCell className="font-mono text-secondary">
                  {row.rank ?? index + 1}
                </TableCell>
                <TableCell>
                  <PlayerLink name={row.playerName} playerId={row.playerId} />
                </TableCell>
                <NumberCell value={row.runs} />
                <NumberCell value={row.innings} />
                <TableCell className="text-right font-mono">
                  {formatRunRate(row.average)}
                </TableCell>
                <TableCell className="text-right font-mono">
                  {formatRunRate(row.strikeRate)}
                </TableCell>
                <NumberCell value={row.highestScore} />
              </TableRow>
            ))
          ) : (
            <EmptyRows colSpan={7} message="Batting leaders are not available yet." />
          )}
        </TableBody>
      </Table>
    </Leaderboard>
  );
}

function BowlingTable({ rows }: { rows: BowlingLeader[] }) {
  return (
    <Leaderboard title="Bowling Leaders">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>#</TableHead>
            <TableHead>Player</TableHead>
            <TableHead className="text-right">Wkts</TableHead>
            <TableHead className="text-right">Overs</TableHead>
            <TableHead className="text-right">Avg</TableHead>
            <TableHead className="text-right">Econ</TableHead>
            <TableHead className="text-right">Best</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {rows.length > 0 ? (
            rows.map((row, index) => (
              <TableRow key={row.playerId ?? index}>
                <TableCell className="font-mono text-secondary">
                  {row.rank ?? index + 1}
                </TableCell>
                <TableCell>
                  <PlayerLink name={row.playerName} playerId={row.playerId} />
                </TableCell>
                <NumberCell value={row.wickets} />
                <TableCell className="text-right font-mono">
                  {row.overs ?? "0.0"}
                </TableCell>
                <TableCell className="text-right font-mono">
                  {formatRunRate(row.average)}
                </TableCell>
                <TableCell className="text-right font-mono">
                  {formatRunRate(row.economy)}
                </TableCell>
                <TableCell className="text-right font-mono">
                  {row.bestBowling ?? "-"}
                </TableCell>
              </TableRow>
            ))
          ) : (
            <EmptyRows colSpan={7} message="Bowling leaders are not available yet." />
          )}
        </TableBody>
      </Table>
    </Leaderboard>
  );
}

function Leaderboard({
  children,
  title,
}: {
  children: React.ReactNode;
  title: string;
}) {
  return (
    <div className="overflow-hidden rounded-sm border border-white/10 bg-card">
      <h2 className="bg-surface px-4 py-3 font-heading text-xl font-semibold uppercase tracking-normal">
        {title}
      </h2>
      <div className="overflow-x-auto">{children}</div>
    </div>
  );
}

function NumberCell({ value }: { value?: number }) {
  return <TableCell className="text-right font-mono">{value ?? 0}</TableCell>;
}

function EmptyRows({ colSpan, message }: { colSpan: number; message: string }) {
  return (
    <TableRow>
      <TableCell
        className="py-6 text-center text-sm text-muted-foreground"
        colSpan={colSpan}
      >
        {message}
      </TableCell>
    </TableRow>
  );
}
