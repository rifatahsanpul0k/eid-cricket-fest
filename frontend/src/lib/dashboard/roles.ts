import type { Session } from "@/lib/auth/session";

const organizerRoles = new Set(["ORGANIZER", "ADMIN"]);

export function hasOrganizerAccess(session?: Session) {
  return Boolean(
    session?.user?.roles?.some((role) => organizerRoles.has(role))
  );
}
