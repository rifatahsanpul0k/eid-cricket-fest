import type { Metadata } from "next";
import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import { buttonVariants } from "@/components/ui/button";
import { getMyTeam } from "@/lib/auth/my-cricket-api";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";
import { cn } from "@/lib/utils";

export const metadata: Metadata = {
  title: "My Team",
};

export default async function MyTeamPage() {
  const currentEdition = await getCurrentEditionData();
  const teamResult =
    currentEdition.status === "ready"
      ? await getMyTeam(currentEdition.edition.id)
      : undefined;
  const team = teamResult && "ok" in teamResult && teamResult.ok
    ? teamResult.data
    : undefined;

  return (
    <main className="flex-1">
      <section className="border-b border-white/10 bg-background">
        <div className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <p className="font-mono text-xs uppercase text-primary">
            My Cricket
          </p>
          <h1 className="mt-3 font-heading text-4xl font-bold uppercase tracking-normal">
            My Team
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
        ) : team ? (
          <>
            <div className="rounded-sm border border-white/10 bg-card p-5 text-sm">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
                    {team.teamName}
                  </h2>
                  <p className="mt-2 text-muted-foreground">
                    {team.teamShortName ?? "Team code unavailable"}
                  </p>
                </div>
                <Badge variant={team.me?.captain ? "default" : "outline"}>
                  {team.me?.captain ? "Captain" : "Player"}
                </Badge>
              </div>
              <dl className="mt-5 grid gap-3 sm:grid-cols-3">
                <Info label="Your role" value={team.me?.acquisitionType} />
                <Info label="Jersey" value={team.me?.jerseyNumber} />
                <Info label="Captain" value={team.captain?.playerName} />
              </dl>
            </div>
            <div className="rounded-sm border border-white/10 bg-card p-5 text-sm">
              <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
                Active Squad
              </h2>
              <div className="mt-5 grid gap-3">
                {(team.squad ?? []).map((member) => (
                  <div
                    className="flex flex-wrap items-center justify-between gap-3 rounded-sm border border-white/10 bg-background p-3"
                    key={member.registrationId}
                  >
                    <div>
                      {member.playerId ? (
                        <Link
                          className="font-medium text-foreground underline-offset-4 hover:underline"
                          href={`/players/${member.playerId}`}
                        >
                          {member.playerName}
                        </Link>
                      ) : (
                        <p className="font-medium text-foreground">
                          {member.playerName}
                        </p>
                      )}
                      <p className="mt-1 text-xs uppercase text-muted-foreground">
                        {member.category ?? "Category unavailable"}
                        {member.jerseyNumber ? ` · #${member.jerseyNumber}` : ""}
                      </p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      {member.captain ? (
                        <Badge variant="default">Captain</Badge>
                      ) : null}
                      {member.acquisitionType ? (
                        <Badge variant="outline">{member.acquisitionType}</Badge>
                      ) : null}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </>
        ) : (
          <EmptyPanel message="Your approved registration has not been assigned to a team yet." />
        )}
        <Link className={cn(buttonVariants({ variant: "outline" }), "w-fit")} href="/account">
          Back to account
        </Link>
      </section>
    </main>
  );
}

function EmptyPanel({ message }: { message: string }) {
  return (
    <div className="rounded-sm border border-white/10 bg-card p-5 text-sm">
      <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
        Team unavailable
      </h2>
      <p className="mt-3 text-muted-foreground">{message}</p>
    </div>
  );
}

function Info({
  label,
  value,
}: {
  label: string;
  value?: number | string;
}) {
  return (
    <div>
      <dt className="text-xs uppercase text-muted-foreground">{label}</dt>
      <dd className="mt-1 font-medium text-foreground">
        {value ?? "Not available"}
      </dd>
    </div>
  );
}
