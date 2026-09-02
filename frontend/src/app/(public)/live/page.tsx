import type { Metadata } from "next";
import { connection } from "next/server";

import { DataUnavailable } from "@/components/cricket/data-unavailable";
import { LiveCentreClient } from "@/components/cricket/live/live-centre-client";
import { getLiveCentreMatches } from "@/lib/api/matches";

export const metadata: Metadata = {
  title: "Live | Eid Cricket Fest",
  description: "Follow active Eid Cricket Fest matches in real time.",
};

export default async function LivePage() {
  await connection();

  const matches = await getLiveCentreMatches();

  if (!matches.ok) {
    return (
      <main>
        <LiveHero />
        <DataUnavailable message="Live match data is temporarily unavailable." />
      </main>
    );
  }

  return (
    <main>
      <LiveHero />
      <LiveCentreClient initialMatches={matches.data} />
    </main>
  );
}

function LiveHero() {
  return (
    <section className="border-b border-white/10 bg-background">
      <div className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <p className="font-mono text-xs uppercase text-live">Live Centre</p>
        <h1 className="mt-3 font-heading text-4xl font-bold uppercase tracking-normal">
          Live Matches
        </h1>
        <p className="mt-3 max-w-2xl text-sm leading-6 text-muted-foreground">
          Tournament and Friendly match centres, from toss through final result.
        </p>
      </div>
    </section>
  );
}
