import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { PlayingXiForm } from "@/components/dashboard/playing-xi-form";

const candidates = [
  {
    playerName: "Captain One",
    registrationId: 101,
  },
  {
    playerName: "Keeper Two",
    registrationId: 102,
  },
  {
    playerName: "Reserve Three",
    registrationId: 103,
  },
];

describe("PlayingXiForm", () => {
  it("renders saved Playing XI and wicketkeeper selections", () => {
    const html = renderToStaticMarkup(
      <PlayingXiForm
        action="/api/dashboard/matches/1"
        candidates={candidates}
        initialRegistrationIds={[101, 102]}
        initialWicketkeeperRegistrationId={102}
        playingXiSize={2}
        returnTo="/dashboard/matches/1"
        teamName="Team A"
        tournamentTeamId={7}
      />
    );

    expect(html).toMatch(/checked="" value="101"/);
    expect(html).toMatch(/checked="" value="102"/);
    expect(html).toMatch(/<option value="102" selected="">Keeper Two<\/option>/);
  });
});
