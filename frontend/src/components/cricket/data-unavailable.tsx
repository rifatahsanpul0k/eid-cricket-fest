export function DataUnavailable({ message }: { message: string }) {
  return (
    <section className="mx-auto w-full max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
      <div className="rounded-sm border border-white/10 bg-card p-6 text-sm text-muted-foreground">
        {message}
      </div>
    </section>
  );
}
