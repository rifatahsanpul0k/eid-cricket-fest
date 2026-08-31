import Link from "next/link";

export function PlayerLink({
  className,
  name,
  playerId,
}: {
  className?: string;
  name?: string;
  playerId?: number;
}) {
  if (!playerId) {
    return <span className={className}>{name ?? "Player"}</span>;
  }

  return (
    <Link
      className={
        className ??
        "font-medium text-secondary underline-offset-4 hover:underline"
      }
      href={`/players/${playerId}`}
    >
      {name ?? "Player"}
    </Link>
  );
}
