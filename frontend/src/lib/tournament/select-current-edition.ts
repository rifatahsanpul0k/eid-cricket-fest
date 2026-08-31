import type { TournamentEdition } from "@/lib/api/tournaments";

const STATUS_PRIORITY: Record<string, number> = {
  ONGOING: 0,
  SCHEDULED: 1,
  REGISTRATION_OPEN: 2,
  REGISTRATION_CLOSED: 3,
  DRAFTING: 4,
  DRAFT: 5,
  COMPLETED: 6,
  CANCELLED: 7,
};

const DEFAULT_STATUS_PRIORITY = 7;

export function selectCurrentEdition(
  editions: TournamentEdition[]
): TournamentEdition | undefined {
  return [...editions]
    .filter((edition) => edition.id !== undefined)
    .sort(compareEditions)[0];
}

function compareEditions(
  left: TournamentEdition,
  right: TournamentEdition
) {
  const leftPriority = priority(left);
  const rightPriority = priority(right);

  if (leftPriority !== rightPriority) {
    return leftPriority - rightPriority;
  }

  const leftTime = editionTime(left);
  const rightTime = editionTime(right);

  if (leftTime !== rightTime) {
    return rightTime - leftTime;
  }

  return (right.id ?? 0) - (left.id ?? 0);
}

function priority(edition: TournamentEdition) {
  return STATUS_PRIORITY[edition.status ?? ""] ?? DEFAULT_STATUS_PRIORITY;
}

function editionTime(edition: TournamentEdition) {
  const date =
    edition.startDate ??
    edition.registrationStartAt ??
    edition.createdAt ??
    edition.updatedAt;

  if (!date) {
    return 0;
  }

  const time = Date.parse(date);
  return Number.isNaN(time) ? 0 : time;
}

export function editionStatusLabel(status: TournamentEdition["status"]) {
  switch (status) {
    case "ONGOING":
      return "Live Tournament";
    case "SCHEDULED":
      return "Upcoming";
    case "REGISTRATION_OPEN":
      return "Registration Open";
    case "REGISTRATION_CLOSED":
      return "Registration Closed";
    case "DRAFTING":
      return "Draft In Progress";
    case "DRAFT":
      return "Draft";
    case "COMPLETED":
      return "Completed";
    case "CANCELLED":
      return "Cancelled";
    default:
      return "Tournament";
  }
}
