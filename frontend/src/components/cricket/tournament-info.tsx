import type { Tournament, TournamentEdition } from "@/lib/api/tournaments";
import { editionStatusLabel } from "@/lib/tournament/select-current-edition";
import { formatDate } from "@/lib/utils/format";

export function TournamentInfo({
  tournament,
  edition,
}: {
  tournament: Tournament;
  edition: TournamentEdition;
}) {
  return (
    <section className="border-y border-white/10 bg-surface py-12" id="tournament">
      <div className="mx-auto grid w-full max-w-7xl gap-6 px-4 sm:px-6 lg:grid-cols-3 lg:px-8">
        <div className="lg:col-span-1">
          <p className="font-mono text-xs font-medium uppercase text-primary">
            Tournament
          </p>
          <h2 className="mt-1 font-heading text-3xl font-bold uppercase tracking-normal">
            Current edition
          </h2>
        </div>
        <div className="grid gap-4 lg:col-span-2 sm:grid-cols-2">
          <InfoItem label="Edition" value={edition.name ?? "Current edition"} />
          <InfoItem label="Status" value={editionStatusLabel(edition.status)} />
          <InfoItem
            label="Dates"
            value={`${formatDate(edition.startDate)} to ${formatDate(
              edition.endDate
            )}`}
          />
          <InfoItem
            label="Tournament"
            value={tournament.description ?? tournament.name ?? "Eid Cricket Fest"}
          />
        </div>
      </div>
    </section>
  );
}

function InfoItem({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-sm border border-white/10 bg-card p-4">
      <p className="font-mono text-xs uppercase text-muted-foreground">
        {label}
      </p>
      <p className="mt-2 font-medium">{value}</p>
    </div>
  );
}
