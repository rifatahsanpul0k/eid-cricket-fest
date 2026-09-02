import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { ScorecardView } from "@/components/cricket/scorecard/scorecard-view";
import type { Scorecard } from "@/lib/api/scorecard";

describe("ScorecardView", () => {
  it("renders abandoned scorecard state while keeping innings rows", () => {
    const html = renderToStaticMarkup(
      <ScorecardView
        scorecard={scorecard({
          status: "ABANDONED",
          resultText: "Rain made the ground unsafe",
        })}
      />
    );

    expect(html).toContain("MATCH ABANDONED");
    expect(html).toContain("Rain made the ground unsafe");
    expect(html).toContain("Alpha XI");
    expect(html).toContain("32/2");
  });

  it("renders reviewed, voided, and superseded result states", () => {
    expect(
      renderToStaticMarkup(
        <ScorecardView
          scorecard={scorecard({
            resultStatus: "UNDER_REVIEW",
          })}
        />
      )
    ).toContain("RESULT UNDER REVIEW");

    expect(
      renderToStaticMarkup(
        <ScorecardView
          scorecard={scorecard({
            resultStatus: "VOID",
            resultText: "Scorecard invalid",
          })}
        />
      )
    ).toContain("RESULT VOIDED");

    const superseded = renderToStaticMarkup(
      <ScorecardView
        scorecard={scorecard({
          resultStatus: "SUPERSEDED",
          supersededByMatchId: 22,
        })}
      />
    );

    expect(superseded).toContain("RESULT SUPERSEDED");
    expect(superseded).toContain("/matches/22/scorecard");
  });
});

function scorecard(overrides: Partial<Scorecard> = {}): Scorecard {
  return {
    matchId: 10,
    matchType: "TOURNAMENT",
    matchNumber: 4,
    stage: "LEAGUE",
    status: "COMPLETED",
    resultStatus: "OFFICIAL",
    resultText: "Alpha XI won by 12 runs",
    rematchOfMatchId: undefined,
    supersededByMatchId: undefined,
    innings: [
      {
        inningsNumber: 1,
        battingTeam: "Alpha XI",
        runs: 32,
        wickets: 2,
        overs: "5.0",
        batting: [],
        bowling: [],
      },
    ],
    ...overrides,
  };
}
