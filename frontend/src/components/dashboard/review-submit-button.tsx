"use client";

import { useFormStatus } from "react-dom";

import { Button } from "@/components/ui/button";

export function ReviewSubmitButton({
  children,
  disabled = false,
  variant = "default",
}: {
  children: React.ReactNode;
  disabled?: boolean;
  variant?: "default" | "destructive" | "outline";
}) {
  const { pending } = useFormStatus();

  return (
    <Button disabled={disabled || pending} size="sm" type="submit" variant={variant}>
      {pending ? "Working..." : children}
    </Button>
  );
}
