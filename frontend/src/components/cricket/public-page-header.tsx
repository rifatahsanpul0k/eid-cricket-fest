import type { TournamentEdition } from "@/lib/api/tournaments";
import { editionStatusLabel } from "@/lib/tournament/select-current-edition";

export function PublicPageHeader({
  description,
  edition,
  kicker,
  title,
}: {
  description: string;
  edition?: TournamentEdition;
  kicker?: string;
  title: string;
}) {
  return (
    <section className="border-b border-white/10 bg-background">
      <div className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <p className="font-mono text-xs uppercase text-primary">
          {kicker ?? edition?.name ?? "Public tournament"}
          {edition?.status ? ` · ${editionStatusLabel(edition.status)}` : ""}
        </p>
        <h1 className="mt-3 font-heading text-4xl font-bold uppercase tracking-normal">
          {title}
        </h1>
        <p className="mt-3 max-w-2xl text-sm leading-6 text-muted-foreground">
          {description}
        </p>
      </div>
    </section>
  );
}
