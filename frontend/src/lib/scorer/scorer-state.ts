import type {
  LiveMatchResponse,
  RecordDeliveryRequest,
  ScorerMatchStateResponse,
  ScorerPlayingXiPlayer,
} from "@/lib/api/schema-helpers";

export type DeliveryInput = {
  runsOffBat?: number;
  wideRuns?: number;
  noBallRuns?: number;
  byeRuns?: number;
  legByeRuns?: number;
  penaltyRuns?: number;
  swapEnds?: boolean;
  wicket?: RecordDeliveryRequest["wicket"];
};

export class ScoringIntentStore {
  private readonly pending = new Map<string, string>();
  private readonly createId: () => string;

  constructor(createId: () => string = randomUuid) {
    this.createId = createId;
  }

  begin(actionKey: string) {
    const existing = this.pending.get(actionKey);

    if (existing) {
      return existing;
    }

    const clientEventId = this.createId();
    this.pending.set(actionKey, clientEventId);

    return clientEventId;
  }

  finish(actionKey: string) {
    this.pending.delete(actionKey);
  }

  isPending(actionKey: string) {
    return this.pending.has(actionKey);
  }
}

export function buildDeliveryRequest(
  clientEventId: string,
  input: DeliveryInput
): RecordDeliveryRequest {
  return {
    clientEventId,
    runsOffBat: input.runsOffBat ?? 0,
    wideRuns: input.wideRuns ?? 0,
    noBallRuns: input.noBallRuns ?? 0,
    byeRuns: input.byeRuns ?? 0,
    legByeRuns: input.legByeRuns ?? 0,
    penaltyRuns: input.penaltyRuns ?? 0,
    swapEnds: input.swapEnds,
    wicket: input.wicket,
  };
}

export function validateDeliveryInput(input: DeliveryInput) {
  const runsOffBat = input.runsOffBat ?? 0;
  const wideRuns = input.wideRuns ?? 0;
  const noBallRuns = input.noBallRuns ?? 0;
  const byeRuns = input.byeRuns ?? 0;
  const legByeRuns = input.legByeRuns ?? 0;

  if (wideRuns > 0 && noBallRuns > 0) {
    return "A delivery cannot be both wide and no-ball.";
  }

  if (byeRuns > 0 && legByeRuns > 0) {
    return "Choose byes or leg-byes, not both.";
  }

  if ((byeRuns > 0 || legByeRuns > 0) && runsOffBat > 0) {
    return "Bat runs cannot be combined with byes or leg-byes.";
  }

  if (wideRuns > 0 && (runsOffBat > 0 || byeRuns > 0 || legByeRuns > 0)) {
    return "Record all wide runs in the wide field.";
  }

  return undefined;
}

export function deliveryLabel(
  ball: NonNullable<LiveMatchResponse["recentBalls"]>[number]
) {
  const runs = ball.runs ?? 0;

  if (ball.legal === false) {
    return runs > 0 ? `+${runs}` : "Extra";
  }

  return runs === 0 ? "." : String(runs);
}

export function canUndo(state: ScorerMatchStateResponse) {
  return (
    state.live?.innings?.inningsId !== undefined &&
    (state.live?.recentBalls?.length ?? 0) > 0 &&
    state.match?.status === "LIVE"
  );
}

export function canCorrectDelivery(state: ScorerMatchStateResponse) {
  return (
    state.live?.innings?.inningsId !== undefined &&
    state.match?.status === "LIVE" &&
    (state.live?.recentBalls?.length ?? 0) > 0
  );
}

export function needsBatters(state: ScorerMatchStateResponse) {
  const innings = state.live?.innings;

  return Boolean(
    innings?.inningsId &&
      (!innings.striker?.playerId || !innings.nonStriker?.playerId) &&
      state.match?.status === "LIVE"
  );
}

export function needsBowler(state: ScorerMatchStateResponse) {
  const innings = state.live?.innings;

  return Boolean(
    innings?.inningsId && !innings.bowler?.playerId && state.match?.status === "LIVE"
  );
}

export function activeBattingXi(state: ScorerMatchStateResponse) {
  const batting = state.live?.innings?.battingTeam;

  return playersByTeamName(state, batting);
}

export function activeBowlingXi(state: ScorerMatchStateResponse) {
  const bowling = state.live?.innings?.bowlingTeam;

  return playersByTeamName(state, bowling);
}

export function nextBattingXi(state: ScorerMatchStateResponse) {
  return playersByTeamId(state, state.nextInningsBattingTeamId);
}

export function nextBowlingXi(state: ScorerMatchStateResponse) {
  return playersByTeamId(state, state.nextInningsBowlingTeamId);
}

export function eligibleActiveBatters(state: ScorerMatchStateResponse) {
  return excludeDismissed(activeBattingXi(state), state);
}

export function batterOptions(
  players: ScorerPlayingXiPlayer[],
  excludedPlayingXiId: number | "" = ""
) {
  return players.filter(
    (player) =>
      player.playingXiId !== undefined &&
      player.playingXiId !== excludedPlayingXiId
  );
}

export function currentBatters(state: ScorerMatchStateResponse) {
  const batters = activeBattingXi(state);
  const innings = state.live?.innings;

  return [
    xiForLivePlayer(batters, innings?.striker?.playerId),
    xiForLivePlayer(batters, innings?.nonStriker?.playerId),
  ].filter((player): player is ScorerPlayingXiPlayer => player !== undefined);
}

export function wicketDismissalOptions(state: ScorerMatchStateResponse) {
  return currentBatters(state);
}

export function eligibleIncomingBatters(state: ScorerMatchStateResponse) {
  const currentIds = new Set(
    currentBatters(state)
      .map((player) => player.playingXiId)
      .filter((id): id is number => id !== undefined)
  );

  return eligibleActiveBatters(state).filter(
    (player) =>
      player.playingXiId !== undefined && !currentIds.has(player.playingXiId)
  );
}

export function eligibleNextOverBowlers(state: ScorerMatchStateResponse) {
  const previous = state.previousOverBowlerPlayingXiId;

  return activeBowlingXi(state).filter(
    (player) =>
      player.playingXiId !== undefined && player.playingXiId !== previous
  );
}

export function xiIdForLivePlayer(
  players: ScorerPlayingXiPlayer[],
  playerId?: number
) {
  return players.find((player) => player.playerId === playerId)?.playingXiId;
}

function xiForLivePlayer(
  players: ScorerPlayingXiPlayer[],
  playerId?: number
) {
  return players.find((player) => player.playerId === playerId);
}

function excludeDismissed(
  players: ScorerPlayingXiPlayer[],
  state: ScorerMatchStateResponse
) {
  const dismissed = new Set(state.dismissedPlayingXiIds ?? []);

  return players.filter(
    (player) =>
      player.playingXiId !== undefined && !dismissed.has(player.playingXiId)
  );
}

function playersByTeamId(
  state: ScorerMatchStateResponse,
  tournamentTeamId?: number
) {
  return allPlayingXi(state).filter(
    (player) => player.tournamentTeamId === tournamentTeamId
  );
}

function playersByTeamName(
  state: ScorerMatchStateResponse,
  teamName?: string
) {
  return allPlayingXi(state).filter((player) => player.teamName === teamName);
}

function allPlayingXi(state: ScorerMatchStateResponse) {
  return [
    ...(state.teamAPlayingXi ?? []),
    ...(state.teamBPlayingXi ?? []),
  ];
}

function randomUuid() {
  return crypto.randomUUID();
}
