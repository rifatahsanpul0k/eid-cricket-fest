import {
  getTournamentEditions,
  getTournaments,
  type Tournament,
  type TournamentEdition,
} from "@/lib/api/tournaments";
import { selectCurrentEdition } from "@/lib/tournament/select-current-edition";

type TournamentWithId = Tournament & { id: number };
type TournamentEditionWithId = TournamentEdition & { id: number };

export type CurrentEditionData =
  | {
      status: "ready";
      tournament: TournamentWithId;
      edition: TournamentEditionWithId;
    }
  | {
      status: "unavailable";
      message: string;
    };

export async function getCurrentEditionData(): Promise<CurrentEditionData> {
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

  if (!hasEditionId(edition)) {
    return unavailable();
  }

  return {
    status: "ready",
    tournament,
    edition,
  };
}

function hasEditionId(
  edition: TournamentEdition | undefined
): edition is TournamentEditionWithId {
  return edition?.id !== undefined;
}

function selectTournament(
  tournaments: Tournament[]
): TournamentWithId | undefined {
  const selectableTournaments = tournaments.filter(
    (tournament): tournament is TournamentWithId => tournament.id !== undefined
  );

  return (
    selectableTournaments.find((tournament) =>
      tournament.name?.toLowerCase().includes("eid cricket fest")
    ) ?? selectableTournaments[0]
  );
}

function unavailable(): CurrentEditionData {
  return {
    status: "unavailable",
    message:
      "Tournament data is temporarily unavailable. Please try again shortly.",
  };
}
