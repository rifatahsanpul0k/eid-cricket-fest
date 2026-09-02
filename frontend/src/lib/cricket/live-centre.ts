import type { LiveCentreMatch } from "@/lib/api/matches";

export type LiveCentreSectionId =
  | "live-now"
  | "toss-completed"
  | "innings-break"
  | "suspended"
  | "recent-results";

export type LiveCentreSection = {
  id: LiveCentreSectionId;
  title: string;
  matches: LiveCentreMatch[];
};

export const LIVE_CENTRE_SECTION_ORDER: LiveCentreSection[] = [
  { id: "live-now", title: "LIVE NOW", matches: [] },
  { id: "toss-completed", title: "TOSS COMPLETED", matches: [] },
  { id: "innings-break", title: "INNINGS BREAK", matches: [] },
  { id: "suspended", title: "SUSPENDED", matches: [] },
  { id: "recent-results", title: "RECENT RESULTS", matches: [] },
];

export function liveCentreSections(matches: LiveCentreMatch[]) {
  return LIVE_CENTRE_SECTION_ORDER.map((section) => ({
    ...section,
    matches: matches.filter((match) => sectionForMatch(match) === section.id),
  }));
}

export function sectionForMatch(
  match: LiveCentreMatch
): LiveCentreSectionId | undefined {
  if (match.status === "LIVE") {
    return "live-now";
  }

  if (match.status === "TOSS_COMPLETED") {
    return "toss-completed";
  }

  if (match.status === "INNINGS_BREAK") {
    return "innings-break";
  }

  if (match.status === "SUSPENDED") {
    return "suspended";
  }

  if (match.status === "COMPLETED") {
    return "recent-results";
  }

  return undefined;
}

export function liveCentreEyebrow(match: LiveCentreMatch) {
  const type =
    match.matchType === "FRIENDLY"
      ? "FRIENDLY"
      : match.stage
        ? `TOURNAMENT · ${stageLabel(match.stage).toUpperCase()}`
        : "TOURNAMENT";

  if (match.matchType === "FRIENDLY") {
    return type;
  }

  return match.matchNumber ? `${type} · MATCH ${match.matchNumber}` : type;
}

export function liveCentrePrimaryText(match: LiveCentreMatch) {
  const innings = match.innings;

  if (match.status === "TOSS_COMPLETED") {
    return "Match starting shortly";
  }

  if (!innings) {
    return match.status === "COMPLETED"
      ? (resultText(match) ?? "Final result")
      : "Score will appear when play begins";
  }

  return `${innings.runs ?? 0}/${innings.wickets ?? 0} · ${
    innings.overs ?? "0.0"
  } overs`;
}

export function liveCentreDetailText(match: LiveCentreMatch) {
  const innings = match.innings;

  if (match.status === "TOSS_COMPLETED") {
    return tossText(match);
  }

  if (match.status === "INNINGS_BREAK") {
    const target = innings?.target;
    return target
      ? `${innings?.battingTeam ?? "First innings"} finished. Target ${target}.`
      : "First innings complete. Target will appear shortly.";
  }

  if (match.status === "SUSPENDED") {
    return innings
      ? `Suspended with ${innings.battingTeam ?? "batting side"} at ${
          innings.runs ?? 0
        }/${innings.wickets ?? 0}`
      : "Match suspended before scoring started.";
  }

  if (match.status === "COMPLETED") {
    return resultText(match) ?? "Scorecard available";
  }

  if (innings?.striker?.name) {
    return `${innings.striker.name}${innings.bowler?.name ? ` · Bowler: ${innings.bowler.name}` : ""}`;
  }

  return innings?.bowler?.name ? `Bowler: ${innings.bowler.name}` : "";
}

export function liveCentreAction(match: LiveCentreMatch) {
  if (!match.matchId) {
    return undefined;
  }

  if (match.status === "COMPLETED") {
    return {
      href: `/matches/${match.matchId}/scorecard`,
      label: "Scorecard",
    };
  }

  return {
    href: `/matches/${match.matchId}/live`,
    label: match.status === "TOSS_COMPLETED" ? "Match Centre" : "Watch Live",
  };
}

export function resultText(match: LiveCentreMatch) {
  if (match.resultStatus === "UNDER_REVIEW") {
    return "Result under review";
  }

  if (match.resultStatus === "VOID") {
    return "Result voided";
  }

  if (match.resultStatus === "SUPERSEDED") {
    return "Result superseded";
  }

  return match.resultText ?? undefined;
}

export function tossText(match: LiveCentreMatch) {
  if (!match.toss) {
    return "Toss details will appear shortly.";
  }

  const decision =
    match.toss.decision === "BOWL" ? "bowl first" : "bat first";

  return `${match.toss.winnerName ?? "Toss winner"} won the toss and chose to ${decision}.`;
}

function stageLabel(stage: NonNullable<LiveCentreMatch["stage"]>) {
  return stage === "SEMI_FINAL"
    ? "Semi-final"
    : stage === "FINAL"
      ? "Final"
      : stage === "OTHER"
        ? "Other"
        : "League";
}
