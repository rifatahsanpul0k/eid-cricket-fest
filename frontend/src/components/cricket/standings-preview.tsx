import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { StandingRow } from "@/lib/api/standings";

export function StandingsPreview({
  standings,
}: {
  standings: StandingRow[];
}) {
  return (
    <section className="mx-auto w-full max-w-7xl px-4 py-12 sm:px-6 lg:px-8" id="standings">
      <div className="mb-5">
        <p className="font-mono text-xs font-medium uppercase text-primary">
          Standings
        </p>
        <h2 className="font-heading text-3xl font-bold uppercase tracking-normal">
          Table preview
        </h2>
      </div>
      <div className="overflow-x-auto rounded-sm border border-white/10 bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>#</TableHead>
              <TableHead>Team</TableHead>
              <TableHead className="text-right">P</TableHead>
              <TableHead className="text-right">W</TableHead>
              <TableHead className="text-right">L</TableHead>
              <TableHead className="text-right">Pts</TableHead>
              <TableHead className="text-right">NRR</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {standings.length > 0 ? (
              standings.map((row, index) => (
                <TableRow key={row.tournamentTeamId ?? index}>
                  <TableCell className="font-mono text-secondary">
                    {row.rank ?? index + 1}
                  </TableCell>
                  <TableCell className="font-heading font-medium uppercase tracking-normal">
                    {row.shortName ?? row.teamName ?? "Team"}
                  </TableCell>
                  <TableCell className="text-right">{row.played ?? 0}</TableCell>
                  <TableCell className="text-right">{row.won ?? 0}</TableCell>
                  <TableCell className="text-right">{row.lost ?? 0}</TableCell>
                  <TableCell className="text-right font-mono text-secondary">
                    {row.points ?? 0}
                  </TableCell>
                  <TableCell className="text-right">
                    {(row.netRunRate ?? 0).toFixed(2)}
                  </TableCell>
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell
                  className="py-6 text-center text-sm text-muted-foreground"
                  colSpan={7}
                >
                  Standings will appear after matches are completed.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
    </section>
  );
}
