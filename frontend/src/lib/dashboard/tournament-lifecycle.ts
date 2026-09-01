import type { TournamentEdition } from "@/lib/api/tournaments";

type EditionStatus = NonNullable<TournamentEdition["status"]>;

export type LifecycleAction = {
  label: string;
  status: EditionStatus;
};

const LIFECYCLE_ACTIONS: Partial<Record<EditionStatus, LifecycleAction[]>> = {
  DRAFT: [
    { label: "Open Registration", status: "REGISTRATION_OPEN" },
    { label: "Cancel Edition", status: "CANCELLED" },
  ],
  REGISTRATION_OPEN: [
    { label: "Close Registration", status: "REGISTRATION_CLOSED" },
    { label: "Cancel Edition", status: "CANCELLED" },
  ],
  REGISTRATION_CLOSED: [
    { label: "Start Drafting", status: "DRAFTING" },
    { label: "Cancel Edition", status: "CANCELLED" },
  ],
  DRAFTING: [
    { label: "Mark Scheduled", status: "SCHEDULED" },
    { label: "Cancel Edition", status: "CANCELLED" },
  ],
  SCHEDULED: [
    { label: "Start Tournament", status: "ONGOING" },
    { label: "Cancel Edition", status: "CANCELLED" },
  ],
};

export function lifecycleActions(
  status: TournamentEdition["status"]
): LifecycleAction[] {
  return status ? (LIFECYCLE_ACTIONS[status] ?? []) : [];
}
