"use client";

import { useFormStatus } from "react-dom";

import { Button } from "@/components/ui/button";

export function ReviewSubmitButton({
  children,
  variant = "default",
}: {
  children: React.ReactNode;
  variant?: "default" | "destructive" | "outline";
}) {
  const { pending } = useFormStatus();

  return (
    <Button disabled={pending} size="sm" type="submit" variant={variant}>
      {pending ? "Working..." : children}
    </Button>
  );
}
