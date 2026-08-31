import { getLiveMatch, getMatches, type LiveMatch, type Match } from "@/lib/api/matches";
import { getStandings, type StandingRow } from "@/lib/api/standings";
import type { Tournament, TournamentEdition } from "@/lib/api/tournaments";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";

export type HomePageData =
  | {
      status: "ready";
      tournament: Tournament;
      edition: TournamentEdition;
      liveMatch?: LiveMatch;
      upcomingMatches: Match[];
      standings: StandingRow[];
    }
  | {
      status: "unavailable";
      message: string;
    };

export async function getHomePageData(): Promise<HomePageData> {
  const currentEdition = await getCurrentEditionData();

  if (currentEdition.status === "unavailable") {
    return currentEdition;
  }

  const { tournament, edition } = currentEdition;

  const [liveMatches, upcomingMatches, standings] = await Promise.all([
    getMatches(edition.id, {
      status: "LIVE",
      size: 3,
      sortBy: "matchNumber",
      direction: "asc",
    }),
    getMatches(edition.id, {
      status: "SCHEDULED",
      size: 3,
      sortBy: "scheduledAt",
      direction: "asc",
    }),
    getStandings(edition.id),
  ]);

  const liveMatchSummary =
    liveMatches.ok && liveMatches.data[0]?.id
      ? await getLiveMatch(liveMatches.data[0].id)
      : undefined;

  return {
    status: "ready",
    tournament,
    edition,
    liveMatch: liveMatchSummary?.ok ? liveMatchSummary.data : undefined,
    upcomingMatches: upcomingMatches.ok ? upcomingMatches.data.slice(0, 3) : [],
    standings: standings.ok ? standings.data.standings?.slice(0, 5) ?? [] : [],
  };
}
