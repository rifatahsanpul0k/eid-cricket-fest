import { connection } from "next/server";

import { DataUnavailable } from "@/components/cricket/data-unavailable";
import { LiveMatchCard } from "@/components/cricket/live-match-card";
import { RegistrationCta } from "@/components/cricket/registration-cta";
import { StandingsPreview } from "@/components/cricket/standings-preview";
import { TournamentHero } from "@/components/cricket/tournament-hero";
import { TournamentInfo } from "@/components/cricket/tournament-info";
import { UpcomingMatches } from "@/components/cricket/upcoming-matches";
import { getHomePageData } from "@/lib/tournament/home-page-data";

export default async function HomePage() {
  await connection();

  const data = await getHomePageData();

  if (data.status !== "ready") {
    return (
      <main>
        <FallbackHero />
        <DataUnavailable message={data.message} />
      </main>
    );
  }

  return (
    <main>
      <TournamentHero edition={data.edition} />
      <LiveMatchCard liveMatch={data.liveMatch} />
      <UpcomingMatches matches={data.upcomingMatches} />
      <StandingsPreview standings={data.standings} />
      <TournamentInfo edition={data.edition} />
      <RegistrationCta edition={data.edition} />
    </main>
  );
}

function FallbackHero() {
  return (
    <section className="border-b border-white/10 bg-background text-foreground">
      <div className="mx-auto flex min-h-[420px] w-full max-w-7xl flex-col justify-center px-4 py-16 sm:px-6 lg:px-8">
        <p className="font-mono text-xs font-medium uppercase text-primary">
          Public Home
        </p>
        <h1 className="mt-4 max-w-3xl font-heading text-4xl font-bold uppercase leading-tight tracking-normal sm:text-5xl">
          Eid Cricket Fest
        </h1>
        <p className="mt-5 max-w-2xl text-base leading-7 text-muted-foreground">
          Fixtures, live scores, standings, and tournament updates will appear
          when the backend is reachable.
        </p>
      </div>
    </section>
  );
}
