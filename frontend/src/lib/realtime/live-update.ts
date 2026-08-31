import type { LiveMatch } from "@/lib/api/matches";

export type LiveConnectionState =
  | "connecting"
  | "connected"
  | "reconnecting"
  | "disconnected"
  | "error";

export function liveMatchTopic(matchId: number) {
  return `/topic/matches/${matchId}`;
}

export function parseLiveUpdate(body: string): LiveMatch | undefined {
  try {
    const parsed: unknown = JSON.parse(body);

    if (!isLiveMatchUpdate(parsed)) {
      return undefined;
    }

    return parsed;
  } catch {
    return undefined;
  }
}

export function shouldAcceptLiveUpdate(
  current: LiveMatch | undefined,
  incoming: LiveMatch
) {
  if (!current) {
    return true;
  }

  if (current.matchId !== incoming.matchId) {
    return false;
  }

  const currentInnings = current.innings;
  const incomingInnings = incoming.innings;

  if (!currentInnings || !incomingInnings) {
    return true;
  }

  if (currentInnings.inningsId !== incomingInnings.inningsId) {
    return true;
  }

  const currentRevision = currentInnings.scoreRevision;
  const incomingRevision = incomingInnings.scoreRevision;

  if (currentRevision === undefined || incomingRevision === undefined) {
    return true;
  }

  return incomingRevision > currentRevision;
}

function isLiveMatchUpdate(value: unknown): value is LiveMatch {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const candidate = value as LiveMatch;

  return typeof candidate.matchId === "number";
}
