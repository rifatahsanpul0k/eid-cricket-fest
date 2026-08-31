import { getLiveMatch, getMatches, type LiveMatch, type Match } from "@/lib/api/matches";
import { getStandings, type StandingRow } from "@/lib/api/standings";
import {
  getTournamentEditions,
  getTournaments,
  type Tournament,
  type TournamentEdition,
} from "@/lib/api/tournaments";
import { selectCurrentEdition } from "@/lib/tournament/select-current-edition";

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
  const tournaments = await getTournaments();

  if (!tournaments.ok) {
    return unavailable();
  }

  const tournament = selectTournament(tournaments.data);

  if (!tournament?.id) {
    return unavailable();
  }

  const editions = await getTournamentEditions(tournament.id);

  if (!editions.ok) {
    return unavailable();
  }

  const edition = selectCurrentEdition(editions.data);

  if (!edition?.id) {
    return unavailable();
  }

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

function selectTournament(
  tournaments: Tournament[]
): Tournament | undefined {
  return (
    tournaments.find((tournament) =>
      tournament.name?.toLowerCase().includes("eid cricket fest")
    ) ?? tournaments[0]
  );
}

function unavailable(): HomePageData {
  return {
    status: "unavailable",
    message:
      "Tournament data is temporarily unavailable. Please try again shortly.",
  };
}
